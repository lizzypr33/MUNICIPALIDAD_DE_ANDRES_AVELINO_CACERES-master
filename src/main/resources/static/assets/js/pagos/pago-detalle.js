/**
 * Módulo de Detalle Integral de Pago Municipal y Verificación Administrativa (PROMPT 13 - Secciones 24, 25, 26 y 27)
 */
import { ROLES } from "../config.js";
import { hasRole } from "../auth.js";
import { createElement, clearChildren } from "../dom.js";
import { showToast } from "../alerts.js";
import { openModal } from "../components/modal.js";
import { confirmAction } from "../components/confirm-dialog.js";
import {
  obtenerPago,
  verificarPago,
  rechazarPago,
  anularPago,
  descargarConstanciaPago
} from "./pagos-api.js";

function formatCurrency(amount, currency = "PEN") {
  const num = Number(amount || 0);
  try {
    return new Intl.NumberFormat("es-PE", {
      style: "currency",
      currency: currency === "USD" ? "USD" : "PEN"
    }).format(num);
  } catch (_e) {
    return `S/ ${num.toFixed(2)}`;
  }
}

function formatDateTime(isoString) {
  if (!isoString) return "—";
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) return String(isoString);
  return date.toLocaleString("es-PE", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  });
}

function createPagoEstadoBadge(estado) {
  const clean = String(estado || "PENDIENTE_VERIFICACION").toUpperCase();
  let cls = "badge-neutral";
  let label = clean;

  if (clean === "VERIFICADO" || clean === "PAGADO") {
    cls = "badge-success";
    label = "VERIFICADO";
  } else if (clean === "PENDIENTE_VERIFICACION" || clean === "PENDIENTE") {
    cls = "badge-warning";
    label = "PENDIENTE DE VERIFICACIÓN";
  } else if (clean === "RECHAZADO") {
    cls = "badge-danger";
    label = "RECHAZADO";
  } else if (clean === "ANULADO") {
    cls = "badge-neutral";
    label = "ANULADO";
  }

  return createElement("span", { className: `badge ${cls}`, textContent: label });
}

export async function renderPagoDetalle({
  container,
  pagoId,
  currentUser = null,
  onBack = null,
  onNavigateToSolicitud = null
} = {}) {
  if (!container) return;
  clearChildren(container);

  container.appendChild(
    createElement("div", {
      className: "loading-state",
      textContent: "Consultando datos del comprobante de pago..."
    })
  );

  let pago = null;

  async function reloadAll() {
    try {
      pago = await obtenerPago(pagoId);
      renderView();
    } catch (err) {
      clearChildren(container);
      container.appendChild(
        createElement("div", {
          className: "alert alert--danger",
          textContent: `Error al consultar pago: ${err.message || "No encontrado"}`
        })
      );
    }
  }

  function renderView() {
    clearChildren(container);

    const isOfficialOrAdmin =
      hasRole(currentUser, ROLES.ADMINISTRADOR) ||
      hasRole(currentUser, ROLES.FUNCIONARIO) ||
      hasRole(currentUser, ROLES.SUPERVISOR);

    const isCiudadano = hasRole(currentUser, ROLES.CIUDADANO);

    // 1. Barra superior
    const topBar = createElement("div", {
      className: "table-actions",
      attrs: { style: "justify-content: space-between; margin-bottom: 1.25rem;" }
    });

    const backBtn = createElement("button", {
      type: "button",
      className: "btn btn-secondary",
      textContent: "← Volver a Pagos"
    });
    if (typeof onBack === "function") {
      backBtn.addEventListener("click", onBack);
    }
    topBar.appendChild(backBtn);

    const rightTop = createElement("div", { className: "table-actions", attrs: { style: "gap: 0.5rem;" } });
    const refreshBtn = createElement("button", {
      type: "button",
      className: "btn btn-secondary",
      textContent: "Actualizar"
    });
    refreshBtn.addEventListener("click", reloadAll);
    rightTop.appendChild(refreshBtn);

    if (pago.solicitudId && typeof onNavigateToSolicitud === "function") {
      const expBtn = createElement("button", {
        type: "button",
        className: "btn btn-secondary",
        textContent: "Ver Expediente"
      });
      expBtn.addEventListener("click", () => onNavigateToSolicitud(pago.solicitudId));
      rightTop.appendChild(expBtn);
    }
    topBar.appendChild(rightTop);
    container.appendChild(topBar);

    // 2. Encabezado principal
    const headerBox = createElement("div", {
      className: "expediente-header",
      attrs: { style: "margin-bottom: 1.5rem;" }
    });

    const titleRow = createElement("div", {
      className: "table-actions",
      attrs: { style: "gap: 0.75rem; align-items: center;" }
    });
    titleRow.appendChild(
      createElement("h2", {
        className: "expediente-header__number",
        textContent: `OPERACIÓN: ${pago.codigoOperacion}`
      })
    );
    titleRow.appendChild(createPagoEstadoBadge(pago.estado));
    headerBox.appendChild(titleRow);

    const subMeta = createElement("div", {
      className: "expediente-header__meta",
      textContent: `Expediente: ${pago.numeroExpediente || `Solicitud #${pago.solicitudId}`} • Concepto: ${pago.concepto} • Monto: ${formatCurrency(pago.monto, pago.moneda)}`
    });
    headerBox.appendChild(subMeta);
    container.appendChild(headerBox);

    // 3. Barra de acciones operativas
    const actionsBar = createElement("div", { className: "expediente-actions-bar" });

    // Descargar Constancia PDF
    const pdfBtn = createElement("button", {
      type: "button",
      className: "btn btn-primary",
      textContent: "📄 Descargar Constancia PDF"
    });
    pdfBtn.addEventListener("click", async () => {
      try {
        pdfBtn.disabled = true;
        await descargarConstanciaPago(pago.id, pago.codigoOperacion);
        showToast("Constancia de pago descargada exitosamente.", "success");
      } catch (err) {
        showToast(err.message || "Error al descargar constancia.", "error");
      } finally {
        pdfBtn.disabled = false;
      }
    });
    actionsBar.appendChild(pdfBtn);

    // Verificar Pago (Solo funcionarios/administradores, no ciudadanos)
    const isPending =
      pago.estado === "PENDIENTE_VERIFICACION" || pago.estado === "PENDIENTE";

    if (isOfficialOrAdmin && !isCiudadano && isPending) {
      const verifBtn = createElement("button", {
        type: "button",
        className: "btn btn--success",
        textContent: "Verificar y Confirmar Pago"
      });
      verifBtn.addEventListener("click", () => openVerificarModal());
      actionsBar.appendChild(verifBtn);
    }

    // Rechazar Pago (Solo funcionarios/administradores)
    if (isOfficialOrAdmin && !isCiudadano && isPending) {
      const rechBtn = createElement("button", {
        type: "button",
        className: "btn btn--danger",
        textContent: "Rechazar Pago Inconsistente"
      });
      rechBtn.addEventListener("click", () => openRechazarModal());
      actionsBar.appendChild(rechBtn);
    }

    // Anular Pago (Solo funcionarios/administradores cuando está verificado o pendiente)
    if (
      isOfficialOrAdmin &&
      !isCiudadano &&
      (pago.estado === "VERIFICADO" || isPending)
    ) {
      const anulBtn = createElement("button", {
        type: "button",
        className: "btn btn-secondary",
        textContent: "Anular Pago"
      });
      anulBtn.addEventListener("click", () => openAnularModal());
      actionsBar.appendChild(anulBtn);
    }

    container.appendChild(actionsBar);

    // Aviso si está rechazado o anulado
    if ((pago.estado === "RECHAZADO" || pago.estado === "ANULADO") && pago.motivoRechazoAnulacion) {
      const warnBox = createElement("div", {
        className: `alert ${pago.estado === "RECHAZADO" ? "alert--danger" : "alert--warning"}`,
        attrs: { style: "margin-bottom: 1.5rem;" },
        textContent: `Motivo administrativo de ${pago.estado.toLowerCase()}: ${pago.motivoRechazoAnulacion}`
      });
      container.appendChild(warnBox);
    }

    // 4. Tarjeta de Datos Financieros
    const finCard = createElement("div", { className: "card", attrs: { style: "margin-bottom: 1.5rem;" } });
    const finHeader = createElement("div", { className: "card-header" });
    finHeader.appendChild(
      createElement("h3", { className: "card-title", textContent: "Información Financiera del Pago" })
    );
    finCard.appendChild(finHeader);

    const finBody = createElement("div", { className: "card-body" });
    const grid = createElement("div", { className: "expediente-data-grid" });

    const fields = [
      ["Código de Operación", pago.codigoOperacion],
      ["Monto Abonado", formatCurrency(pago.monto, pago.moneda)],
      ["Concepto Municipal", pago.concepto],
      ["Medio de Pago", pago.medioPago || "—"],
      ["Expediente Asociado", pago.numeroExpediente || `Solicitud #${pago.solicitudId}`],
      ["Titular / Administrado", pago.nombreCiudadano || "—"],
      ["Establecimiento", pago.nombreComercialEstablecimiento || "—"],
      ["Fecha Declarada de Pago", formatDateTime(pago.fechaPago)],
      ["Fecha de Registro en Sistema", formatDateTime(pago.fechaRegistro)],
      ["Fecha de Verificación", formatDateTime(pago.fechaVerificacion)],
      ["Usuario Verificador", pago.nombreUsuarioVerificador || "Pendiente de verificación"],
      ["Observaciones", pago.observaciones || "Sin observaciones adicionales"]
    ];

    for (const [k, v] of fields) {
      const item = createElement("div", { className: "expediente-data-item" });
      item.appendChild(createElement("div", { className: "expediente-data-item__label", textContent: k }));
      item.appendChild(createElement("div", { className: "expediente-data-item__value", textContent: v }));
      grid.appendChild(item);
    }
    finBody.appendChild(grid);
    finCard.appendChild(finBody);
    container.appendChild(finCard);

    // 5. Historial de Cambios de Estado del Pago
    if (Array.isArray(pago.historial) && pago.historial.length > 0) {
      const histCard = createElement("div", { className: "card" });
      const histHeader = createElement("div", { className: "card-header" });
      histHeader.appendChild(
        createElement("h3", { className: "card-title", textContent: "Historial de Cambios de Estado del Pago" })
      );
      histCard.appendChild(histHeader);

      const histBody = createElement("div", { className: "card-body" });
      const list = createElement("div", { className: "timeline-list" });

      for (const h of pago.historial) {
        const item = createElement("div", { className: "timeline-item" });
        const marker = createElement("div", { className: "timeline-marker", textContent: "💳" });
        const content = createElement("div", { className: "timeline-content" });

        const header = createElement("div", { className: "timeline-header" });
        const title = createElement("div", { className: "timeline-title" });
        title.textContent = `${h.estadoAnterior || "REGISTRO"} → ${h.estadoNuevo}`;
        const date = createElement("time", { className: "timeline-date", textContent: formatDateTime(h.fechaCambio) });
        header.append(title, date);

        const actor = createElement("div", {
          className: "timeline-actor",
          textContent: `Responsable: ${h.nombreUsuarioResponsable || "Sistema Municipal"}`
        });
        content.append(header, actor);

        if (h.motivo) {
          content.appendChild(
            createElement("div", { className: "timeline-observations", textContent: `Motivo: ${h.motivo}` })
          );
        }

        item.append(marker, content);
        list.appendChild(item);
      }

      histBody.appendChild(list);
      histCard.appendChild(histBody);
      container.appendChild(histCard);
    }
  }

  // Modal para verificar pago
  function openVerificarModal() {
    const form = createElement("form", { className: "modal-form" });

    const codGroup = createElement("div", { className: "form-group" });
    codGroup.appendChild(
      createElement("label", { className: "form-label", textContent: "Código de Operación Confirmado (opcional si coincide)" })
    );
    const codIn = createElement("input", {
      type: "text",
      className: "form-control",
      attrs: { placeholder: pago.codigoOperacion }
    });
    codGroup.appendChild(codIn);

    const obsGroup = createElement("div", { className: "form-group" });
    obsGroup.appendChild(
      createElement("label", { className: "form-label", textContent: "Respaldo u Observaciones de Verificación *" })
    );
    const obsText = createElement("textarea", {
      className: "form-control",
      attrs: { rows: "3", placeholder: "Ej.: Verificado contra extracto bancario BCP / cuenta recaudadora municipal..." }
    });
    obsGroup.appendChild(obsText);

    form.append(codGroup, obsGroup);

    const cancelBtn = createElement("button", { type: "button", className: "btn btn-secondary", textContent: "Cancelar" });
    const submitBtn = createElement("button", { type: "button", className: "btn btn--success", textContent: "Confirmar Verificación" });

    const modal = openModal({
      title: "Verificar y Confirmar Pago Municipal",
      content: form,
      footerActions: [cancelBtn, submitBtn]
    });

    cancelBtn.addEventListener("click", () => modal.close());

    submitBtn.addEventListener("click", async () => {
      const obs = obsText.value.trim();
      if (!obs) {
        showToast("Las observaciones o respaldo de verificación son obligatorios.", "warning");
        return;
      }

      try {
        submitBtn.disabled = true;
        await verificarPago(pago.id, {
          codigoOperacionConfirmado: codIn.value.trim() || null,
          observaciones: obs
        });

        showToast("Pago verificado exitosamente.", "success");
        modal.close();
        await reloadAll();
      } catch (err) {
        showToast(err.message || "Error al verificar pago.", "error");
      } finally {
        submitBtn.disabled = false;
      }
    });
  }

  // Modal para rechazar pago
  function openRechazarModal() {
    const form = createElement("form", { className: "modal-form" });
    const motGroup = createElement("div", { className: "form-group" });
    motGroup.appendChild(
      createElement("label", { className: "form-label", textContent: "Motivo de Rechazo del Pago *" })
    );
    const motText = createElement("textarea", {
      className: "form-control",
      attrs: { rows: "3", placeholder: "Ej.: Código de operación inexistente o monto inferior a la tasa..." }
    });
    motGroup.appendChild(motText);
    form.appendChild(motGroup);

    const cancelBtn = createElement("button", { type: "button", className: "btn btn-secondary", textContent: "Cancelar" });
    const submitBtn = createElement("button", { type: "button", className: "btn btn--danger", textContent: "Confirmar Rechazo" });

    const modal = openModal({
      title: "Rechazar Declaración de Pago Inconsistente",
      content: form,
      footerActions: [cancelBtn, submitBtn]
    });

    cancelBtn.addEventListener("click", () => modal.close());

    submitBtn.addEventListener("click", async () => {
      const motivo = motText.value.trim();
      if (!motivo) {
        showToast("El motivo de rechazo es obligatorio.", "warning");
        return;
      }

      try {
        submitBtn.disabled = true;
        await rechazarPago(pago.id, { motivo });
        showToast("Pago rechazado.", "success");
        modal.close();
        await reloadAll();
      } catch (err) {
        showToast(err.message || "Error al rechazar pago.", "error");
      } finally {
        submitBtn.disabled = false;
      }
    });
  }

  // Modal para anular pago
  function openAnularModal() {
    const form = createElement("form", { className: "modal-form" });
    const motGroup = createElement("div", { className: "form-group" });
    motGroup.appendChild(
      createElement("label", { className: "form-label", textContent: "Motivo de Anulación Administrativa *" })
    );
    const motText = createElement("textarea", {
      className: "form-control",
      attrs: { rows: "3", placeholder: "Ej.: Duplicidad de comprobante o error administrativo..." }
    });
    motGroup.appendChild(motText);
    form.appendChild(motGroup);

    const cancelBtn = createElement("button", { type: "button", className: "btn btn-secondary", textContent: "Cancelar" });
    const submitBtn = createElement("button", { type: "button", className: "btn btn-secondary", textContent: "Confirmar Anulación" });

    const modal = openModal({
      title: "Anular Registro de Pago",
      content: form,
      footerActions: [cancelBtn, submitBtn]
    });

    cancelBtn.addEventListener("click", () => modal.close());

    submitBtn.addEventListener("click", async () => {
      const motivo = motText.value.trim();
      if (!motivo) {
        showToast("El motivo de anulación es obligatorio.", "warning");
        return;
      }

      try {
        submitBtn.disabled = true;
        await anularPago(pago.id, { motivo });
        showToast("Pago anulado.", "success");
        modal.close();
        await reloadAll();
      } catch (err) {
        showToast(err.message || "Error al anular pago.", "error");
      } finally {
        submitBtn.disabled = false;
      }
    });
  }

  await reloadAll();
}
