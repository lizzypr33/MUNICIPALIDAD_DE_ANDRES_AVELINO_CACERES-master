/**
 * Módulo de Emisión Administrativa de Licencias de Funcionamiento (PROMPT 13 - Secciones 7 y 8)
 */
import { createElement, clearChildren } from "../dom.js";
import { showToast } from "../alerts.js";
import { confirmAction } from "../components/confirm-dialog.js";
import { listarSolicitudes, obtenerSolicitud } from "../solicitudes/solicitudes-api.js";
import { emitirLicencia, obtenerLicenciaPorSolicitud } from "./licencias-api.js";
import { listarPagosPorSolicitud } from "../pagos/pagos-api.js";
import { listarInspeccionesPorSolicitud } from "../inspecciones/inspecciones-api.js";

function formatDate(isoDate) {
  if (!isoDate) return "—";
  const parts = String(isoDate).split("-");
  if (parts.length === 3) return `${parts[2]}/${parts[1]}/${parts[0]}`;
  return String(isoDate);
}

export async function renderEmitirLicenciaView({
  container,
  preselectedSolicitudId = null,
  onLicenciaEmitida = null,
  onCancel = null
} = {}) {
  if (!container) return;
  clearChildren(container);

  const wrapper = createElement("div", { className: "card" });
  const cardHeader = createElement("div", { className: "card-header" });
  cardHeader.appendChild(
    createElement("h2", { className: "card-title", textContent: "Emisión de Licencia de Funcionamiento" })
  );
  wrapper.appendChild(cardHeader);

  const cardBody = createElement("div", { className: "card-body" });
  wrapper.appendChild(cardBody);
  container.appendChild(wrapper);

  cardBody.appendChild(
    createElement("div", { className: "loading-state", textContent: "Cargando solicitudes aprobadas elegibles..." })
  );

  let solicitudesAprobadas = [];
  let selectedSolicitud = null;
  let existingLicencia = null;

  try {
    const page = await listarSolicitudes({ estado: "APROBADA", size: 50 });
    solicitudesAprobadas = Array.isArray(page?.contenido) ? page.contenido : [];
  } catch (_e) {
    solicitudesAprobadas = [];
  }

  clearChildren(cardBody);

  // Formulario principal
  const form = createElement("form", { className: "modal-form", attrs: { novalidate: "novalidate" } });

  // --- SECCIÓN 1: SOLICITUD APROBADA ---
  const sec1 = createElement("fieldset", { attrs: { style: "border: none; padding: 0; margin-bottom: 2rem;" } });
  sec1.appendChild(
    createElement("legend", {
      attrs: { style: "font-size: 1.05rem; font-weight: 700; color: var(--color-primary-dark); margin-bottom: 1rem;" },
      textContent: "1. Selección del Expediente Aprobado"
    })
  );

  const solSelectGroup = createElement("div", { className: "form-group" });
  solSelectGroup.appendChild(
    createElement("label", { className: "form-label", attrs: { for: "emitir-solicitud-select" }, textContent: "Expediente Aprobado *" })
  );

  const solSelect = createElement("select", { id: "emitir-solicitud-select", className: "form-select" });
  solSelect.appendChild(createElement("option", { value: "", textContent: "— Seleccione una solicitud aprobada —" }));

  for (const s of solicitudesAprobadas) {
    const opt = createElement("option", {
      value: String(s.id),
      textContent: `${s.numeroExpediente || `Borrador #${s.id}`} — ${s.ciudadano?.nombres || s.ciudadanoNombreCompleto || "Titular"} — ${s.establecimiento?.nombreComercial || s.establecimientoNombreComercial || "Local"}`
    });
    if (preselectedSolicitudId && Number(preselectedSolicitudId) === Number(s.id)) {
      opt.selected = true;
    }
    solSelect.appendChild(opt);
  }
  solSelectGroup.appendChild(solSelect);
  sec1.appendChild(solSelectGroup);

  // Contenedor de detalles de la solicitud seleccionada
  const solDetailsMount = createElement("div", { attrs: { style: "margin-top: 1rem;" } });
  sec1.appendChild(solDetailsMount);
  form.appendChild(sec1);

  // --- SECCIÓN 2: VALIDACIONES TÉCNICAS Y ADMINISTRATIVAS ---
  const sec2 = createElement("fieldset", { attrs: { style: "border: none; padding: 0; margin-bottom: 2rem;" } });
  sec2.appendChild(
    createElement("legend", {
      attrs: { style: "font-size: 1.05rem; font-weight: 700; color: var(--color-primary-dark); margin-bottom: 1rem;" },
      textContent: "2. Comprobación de Condiciones Administrativas"
    })
  );

  const conditionsMount = createElement("div");
  sec2.appendChild(conditionsMount);
  form.appendChild(sec2);

  // --- SECCIÓN 3: PARÁMETROS DE EMISIÓN Y CONFIRMACIÓN ---
  const sec3 = createElement("fieldset", { attrs: { style: "border: none; padding: 0; margin-bottom: 1.5rem;" } });
  sec3.appendChild(
    createElement("legend", {
      attrs: { style: "font-size: 1.05rem; font-weight: 700; color: var(--color-primary-dark); margin-bottom: 1rem;" },
      textContent: "3. Parámetros de Emisión"
    })
  );

  const dateGroup = createElement("div", { className: "form-group", attrs: { style: "max-width: 320px;" } });
  dateGroup.appendChild(
    createElement("label", {
      className: "form-label",
      attrs: { for: "emitir-fecha-vencimiento" },
      textContent: "Fecha de Vencimiento (opcional para licencias temporales)"
    })
  );
  const dateInput = createElement("input", {
    type: "date",
    id: "emitir-fecha-vencimiento",
    className: "form-control"
  });
  dateGroup.appendChild(dateInput);

  const obsGroup = createElement("div", { className: "form-group" });
  obsGroup.appendChild(
    createElement("label", {
      className: "form-label",
      attrs: { for: "emitir-observaciones" },
      textContent: "Observaciones de la emisión administrativa"
    })
  );
  const obsInput = createElement("textarea", {
    id: "emitir-observaciones",
    className: "form-control",
    attrs: { rows: "3", maxlength: "2000", placeholder: "Resolución de gerencia o anotaciones oficiales..." }
  });
  obsGroup.appendChild(obsInput);

  sec3.append(dateGroup, obsGroup);
  form.appendChild(sec3);

  // Botones de acción del formulario
  const actionsRow = createElement("div", { className: "table-actions", attrs: { style: "justify-content: flex-end; gap: 0.75rem;" } });
  const cancelBtn = createElement("button", { type: "button", className: "btn btn-secondary", textContent: "Cancelar" });
  const submitBtn = createElement("button", { type: "submit", className: "btn btn-primary", textContent: "Emitir Licencia de Funcionamiento" });
  submitBtn.disabled = true;

  actionsRow.append(cancelBtn, submitBtn);
  form.appendChild(actionsRow);
  cardBody.appendChild(form);

  cancelBtn.addEventListener("click", () => {
    if (typeof onCancel === "function") onCancel();
  });

  async function handleSolicitudSelected(solicitudId) {
    clearChildren(solDetailsMount);
    clearChildren(conditionsMount);
    submitBtn.disabled = true;
    selectedSolicitud = null;
    existingLicencia = null;

    if (!solicitudId) return;

    solDetailsMount.appendChild(
      createElement("div", { className: "loading-state", textContent: "Consultando datos del expediente..." })
    );

    try {
      const [sol, licCheck, pagos, inspecciones] = await Promise.all([
        obtenerSolicitud(solicitudId),
        obtenerLicenciaPorSolicitud(solicitudId).catch(() => null),
        listarPagosPorSolicitud(solicitudId).catch(() => []),
        listarInspeccionesPorSolicitud(solicitudId).catch(() => [])
      ]);

      selectedSolicitud = sol;
      existingLicencia = licCheck;
      clearChildren(solDetailsMount);

      // Si ya existe una licencia para esta solicitud
      if (existingLicencia && existingLicencia.id) {
        const warnDiv = createElement("div", { className: "alert alert--warning" });
        warnDiv.appendChild(
          createElement("strong", { textContent: `Aviso de Prevención: Este expediente ya cuenta con la Licencia emitida N.º ${existingLicencia.codigoLicencia} (${existingLicencia.estado}). ` })
        );
        const viewLink = createElement("button", {
          type: "button",
          className: "btn btn-secondary btn--sm",
          attrs: { style: "margin-left: 0.5rem;" },
          textContent: "Ver Licencia Existente"
        });
        viewLink.addEventListener("click", () => {
          if (typeof onLicenciaEmitida === "function") onLicenciaEmitida(existingLicencia);
        });
        warnDiv.appendChild(viewLink);
        solDetailsMount.appendChild(warnDiv);
        submitBtn.disabled = true;
        return;
      }

      // Cuadro de datos del expediente
      const dataGrid = createElement("div", { className: "expediente-data-grid", attrs: { style: "margin-bottom: 1rem;" } });
      const c = sol.ciudadano;
      const e = sol.establecimiento;
      const t = sol.tipoTramite;

      const items = [
        ["N° de Expediente", sol.numeroExpediente || `ID #${sol.id}`],
        ["Titular / Administrado", c ? `${c.nombres} ${c.apellidos} (${c.tipoDocumento} ${c.numeroDocumento})` : "—"],
        ["Establecimiento", e ? `${e.nombreComercial} (RUC: ${e.ruc || "Sin RUC"})` : "—"],
        ["Dirección", e?.direccion || "—"],
        ["Tipo de Trámite", t ? `${t.codigo} — ${t.nombre}` : "—"],
        ["Estado Actual", sol.estado]
      ];

      for (const [k, v] of items) {
        const it = createElement("div", { className: "expediente-data-item" });
        it.appendChild(createElement("div", { className: "expediente-data-item__label", textContent: k }));
        it.appendChild(createElement("div", { className: "expediente-data-item__value", textContent: v }));
        dataGrid.appendChild(it);
      }
      solDetailsMount.appendChild(dataGrid);

      // Calcular fecha de vencimiento sugerida si el trámite tiene vigencia en años
      if (t && t.vigenciaAnios && Number(t.vigenciaAnios) > 0) {
        const today = new Date();
        today.setFullYear(today.getFullYear() + Number(t.vigenciaAnios));
        dateInput.value = today.toISOString().split("T")[0];
      } else {
        dateInput.value = "";
      }

      // Validaciones y condiciones
      const hasVerifiedPayment = pagos.some((p) => p.estado === "VERIFICADO" || p.estado === "PAGADO");
      const requiresPriorInspection = Boolean(t?.requiereInspeccionPrevia);
      const hasFavorableInspection = inspecciones.some(
        (i) => i.resultado === "FAVORABLE" || i.resultado === "FAVORABLE_CON_OBSERVACIONES"
      );

      const condList = createElement("div", { className: "licencia-cert-grid" });

      const condItems = [
        ["Inspección Técnica Exigible", requiresPriorInspection ? (hasFavorableInspection ? "Cumplida (Favorable)" : "Pendiente de Dictamen Favorable") : "Inspección ITSE Posterior"],
        ["Pagos Registrados", hasVerifiedPayment ? "Verificado conforme" : "Sin pagos verificados registrados"],
        ["Documentos y Requisitos", `${sol.documentos?.length || 0} documento(s) validados`]
      ];

      for (const [k, v] of condItems) {
        const it = createElement("div", { className: "licencia-cert-item" });
        it.appendChild(createElement("div", { className: "licencia-cert-item__label", textContent: k }));
        it.appendChild(createElement("div", { className: "licencia-cert-item__val", textContent: v }));
        condList.appendChild(it);
      }
      conditionsMount.appendChild(condList);

      submitBtn.disabled = false;
    } catch (err) {
      clearChildren(solDetailsMount);
      solDetailsMount.appendChild(
        createElement("div", { className: "alert alert--danger", textContent: err.message || "Error al verificar condiciones." })
      );
    }
  }

  solSelect.addEventListener("change", () => {
    handleSolicitudSelected(solSelect.value);
  });

  if (preselectedSolicitudId) {
    handleSolicitudSelected(preselectedSolicitudId);
  }

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    if (!solSelect.value) {
      showToast("Seleccione una solicitud aprobada para emitir la licencia.", "warning");
      return;
    }

    const confirmed = await confirmAction({
      title: "Confirmar Emisión de Licencia de Funcionamiento",
      message: `¿Confirma la emisión oficial de la licencia para el expediente "${selectedSolicitud?.numeroExpediente || solSelect.value}"? Esta operación generará el Código Oficial y el Certificado con firma y código QR.`,
      confirmText: "Emitir Licencia Oficial",
      variant: "primary"
    });
    if (!confirmed) return;

    try {
      submitBtn.disabled = true;
      submitBtn.textContent = "Emitiendo licencia...";

      const payload = {
        solicitudId: Number(solSelect.value),
        fechaVencimiento: dateInput.value || null,
        observaciones: obsInput.value.trim() || null
      };

      const emitted = await emitirLicencia(payload);
      showToast(`Licencia emitida exitosamente con Código N.º ${emitted.codigoLicencia}.`, "success");

      if (typeof onLicenciaEmitida === "function") {
        onLicenciaEmitida(emitted);
      }
    } catch (err) {
      showToast(err.message || "Error al emitir la licencia.", "error");
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = "Emitir Licencia de Funcionamiento";
    }
  });
}
