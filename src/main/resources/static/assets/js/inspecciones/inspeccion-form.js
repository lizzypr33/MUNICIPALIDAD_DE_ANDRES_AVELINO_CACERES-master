/**
 * Formularios de Programación y Reprogramación de Inspecciones Técnicas (PROMPT 13 - Secciones 16, 17 y 18)
 */
import { createElement, clearChildren } from "../dom.js";
import { showToast } from "../alerts.js";
import { openModal } from "../components/modal.js";
import { listarSolicitudes } from "../solicitudes/solicitudes-api.js";
import { listarUsuarios } from "../usuarios/usuarios-api.js";
import { programarInspeccion, reprogramarInspeccion } from "./inspecciones-api.js";

/**
 * Renderiza la pantalla o vista de programación de una nueva inspección técnica.
 */
export async function renderProgramarInspeccionView({
  container,
  preselectedSolicitudId = null,
  onProgramada = null,
  onCancel = null
} = {}) {
  if (!container) return;
  clearChildren(container);

  const wrapper = createElement("div", { className: "card" });
  const cardHeader = createElement("div", { className: "card-header" });
  cardHeader.appendChild(
    createElement("h2", { className: "card-title", textContent: "Programar Inspección Técnica de Seguridad (ITSE)" })
  );
  wrapper.appendChild(cardHeader);

  const cardBody = createElement("div", { className: "card-body" });
  wrapper.appendChild(cardBody);
  container.appendChild(wrapper);

  cardBody.appendChild(
    createElement("div", { className: "loading-state", textContent: "Cargando solicitudes e inspectores municipales..." })
  );

  let solicitudesList = [];
  let inspectoresList = [];

  try {
    const [solPage, userPage] = await Promise.all([
      listarSolicitudes({ size: 50 }),
      listarUsuarios({ activo: true, size: 50 })
    ]);
    solicitudesList = Array.isArray(solPage?.contenido) ? solPage.contenido : [];
    // Inspectores: usuarios activos
    inspectoresList = Array.isArray(userPage?.contenido) ? userPage.contenido : [];
  } catch (_e) {
    solicitudesList = [];
    inspectoresList = [];
  }

  clearChildren(cardBody);

  const form = createElement("form", { className: "modal-form", attrs: { novalidate: "novalidate" } });

  // Selección de Solicitud
  const solGroup = createElement("div", { className: "form-group" });
  solGroup.appendChild(
    createElement("label", { className: "form-label", attrs: { for: "insp-solicitud-select" }, textContent: "Expediente / Solicitud a Inspeccionar *" })
  );
  const solSelect = createElement("select", { id: "insp-solicitud-select", className: "form-control" });
  solSelect.appendChild(createElement("option", { value: "", textContent: "— Seleccione expediente —" }));

  for (const s of solicitudesList) {
    const opt = createElement("option", {
      value: String(s.id),
      textContent: `${s.numeroExpediente || `Borrador #${s.id}`} — ${s.establecimiento?.nombreComercial || s.establecimientoNombreComercial || "Establecimiento"} (${s.estado})`
    });
    if (preselectedSolicitudId && Number(preselectedSolicitudId) === Number(s.id)) {
      opt.selected = true;
    }
    solSelect.appendChild(opt);
  }
  solGroup.appendChild(solSelect);

  // Selección de Inspector
  const inspGroup = createElement("div", { className: "form-group" });
  inspGroup.appendChild(
    createElement("label", { className: "form-label", attrs: { for: "insp-inspector-select" }, textContent: "Inspector Técnico Responsable *" })
  );
  const inspSelect = createElement("select", { id: "insp-inspector-select", className: "form-control" });
  inspSelect.appendChild(createElement("option", { value: "", textContent: "— Seleccione inspector —" }));

  for (const u of inspectoresList) {
    inspSelect.appendChild(
      createElement("option", {
        value: String(u.id),
        textContent: `${u.nombreCompleto} (${u.username})`
      })
    );
  }
  inspGroup.appendChild(inspSelect);

  // Fecha Programada
  const dateGroup = createElement("div", { className: "form-group", attrs: { style: "max-width: 320px;" } });
  dateGroup.appendChild(
    createElement("label", { className: "form-label", attrs: { for: "insp-fecha-programada" }, textContent: "Fecha y Hora Programada *" })
  );
  const dateIn = createElement("input", {
    type: "datetime-local",
    id: "insp-fecha-programada",
    className: "form-control"
  });
  dateGroup.appendChild(dateIn);

  // Observaciones preliminares
  const obsGroup = createElement("div", { className: "form-group" });
  obsGroup.appendChild(
    createElement("label", { className: "form-label", attrs: { for: "insp-observaciones" }, textContent: "Instrucciones u Observaciones Preliminares" })
  );
  const obsIn = createElement("textarea", {
    id: "insp-observaciones",
    className: "form-control",
    attrs: { rows: "3", maxlength: "2000", placeholder: "Pautas de inspección de seguridad, accesibilidad, aforo..." }
  });
  obsGroup.appendChild(obsIn);

  form.append(solGroup, inspGroup, dateGroup, obsGroup);

  // Botones
  const actionsRow = createElement("div", { className: "table-actions", attrs: { style: "justify-content: flex-end; gap: 0.75rem;" } });
  const cancelBtn = createElement("button", { type: "button", className: "btn btn-secondary", textContent: "Cancelar" });
  const submitBtn = createElement("button", { type: "submit", className: "btn btn-primary", textContent: "Programar Inspección" });

  actionsRow.append(cancelBtn, submitBtn);
  form.appendChild(actionsRow);
  cardBody.appendChild(form);

  cancelBtn.addEventListener("click", () => {
    if (typeof onCancel === "function") onCancel();
  });

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    if (!solSelect.value) {
      showToast("Seleccione el expediente a inspeccionar.", "warning");
      return;
    }
    if (!inspSelect.value) {
      showToast("Seleccione el inspector responsable.", "warning");
      return;
    }
    if (!dateIn.value) {
      showToast("Indique la fecha y hora programada.", "warning");
      return;
    }

    try {
      submitBtn.disabled = true;
      submitBtn.textContent = "Programando...";

      // Formato ISO: YYYY-MM-DDTHH:mm:ss
      const formattedDate = dateIn.value.length === 16 ? `${dateIn.value}:00` : dateIn.value;

      const payload = {
        solicitudId: Number(solSelect.value),
        inspectorId: Number(inspSelect.value),
        fechaProgramada: formattedDate,
        observaciones: obsIn.value.trim() || null
      };

      const result = await programarInspeccion(payload);
      showToast(`Inspección N.º ${result.id} programada exitosamente.`, "success");

      if (typeof onProgramada === "function") {
        onProgramada(result);
      }
    } catch (err) {
      showToast(err.message || "Error al programar inspección.", "error");
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = "Programar Inspección";
    }
  });
}

/**
 * Abre un modal interactivo para reprogramar una inspección pendiente.
 */
export async function openReprogramarInspeccionModal({
  inspeccionId,
  currentInspectorId = null,
  onReprogramada = null
} = {}) {
  const form = createElement("form", { className: "modal-form" });

  const dateGroup = createElement("div", { className: "form-group" });
  dateGroup.appendChild(
    createElement("label", { className: "form-label", textContent: "Nueva Fecha y Hora Programada *" })
  );
  const dateIn = createElement("input", { type: "datetime-local", className: "form-control" });
  dateGroup.appendChild(dateIn);

  const motGroup = createElement("div", { className: "form-group" });
  motGroup.appendChild(
    createElement("label", { className: "form-label", textContent: "Motivo de la Reprogramación *" })
  );
  const motIn = createElement("textarea", {
    className: "form-control",
    attrs: { rows: "3", placeholder: "Causa justificada de la postergación o cambio de turno..." }
  });
  motGroup.appendChild(motIn);

  form.append(dateGroup, motGroup);

  const cancelBtn = createElement("button", { type: "button", className: "btn btn-secondary", textContent: "Cancelar" });
  const submitBtn = createElement("button", { type: "button", className: "btn btn-primary", textContent: "Guardar Reprogramación" });

  const modal = openModal({
    title: "Reprogramar Inspección Técnica",
    content: form,
    footerActions: [cancelBtn, submitBtn]
  });

  cancelBtn.addEventListener("click", () => modal.close());

  submitBtn.addEventListener("click", async () => {
    if (!dateIn.value) {
      showToast("Seleccione la nueva fecha y hora.", "warning");
      return;
    }
    const motivo = motIn.value.trim();
    if (!motivo) {
      showToast("El motivo de reprogramación es obligatorio.", "warning");
      return;
    }

    try {
      submitBtn.disabled = true;
      const formattedDate = dateIn.value.length === 16 ? `${dateIn.value}:00` : dateIn.value;

      await reprogramarInspeccion(inspeccionId, {
        fechaProgramada: formattedDate,
        inspectorId: currentInspectorId || null,
        motivo
      });

      showToast("Inspección reprogramada exitosamente.", "success");
      modal.close();
      if (typeof onReprogramada === "function") {
        onReprogramada();
      }
    } catch (err) {
      showToast(err.message || "Error al reprogramar inspección.", "error");
    } finally {
      submitBtn.disabled = false;
    }
  });
}
