/**
 * Vista de Detalle de Notificación Municipal (PROMPT 14 - Sección 23)
 * Municipalidad Distrital de Andrés Avelino Cáceres Dorregaray
 */
import { clearChildren, showToast } from "../ui.js";
import { openModal } from "../components/modal.js";
import { formatDateTimePE } from "../components/data-table.js";
import { navigateToSection } from "../router.js";
import { getNotificacion, marcarNotificacionLeida } from "./notificaciones-api.js";

function resolveResourceTarget(notif) {
  const tipo = String(notif.tipo || "").toUpperCase();
  const text = `${notif.titulo || ""} ${notif.mensaje || ""}`;

  if (tipo === "SOLICITUD" || text.includes("expediente") || text.includes("solicitud")) {
    return { section: "solicitudes", label: "Ir a Expedientes y Solicitudes" };
  }
  if (tipo === "LICENCIA" || text.includes("licencia")) {
    return { section: "licencias", label: "Ir a Licencias de Funcionamiento" };
  }
  if (tipo === "INSPECCION" || text.includes("inspección") || text.includes("inspeccion") || text.includes("ITSE")) {
    return { section: "inspecciones", label: "Ir a Inspecciones Municipales" };
  }
  if (tipo === "PAGO" || text.includes("pago") || text.includes("caja")) {
    return { section: "pagos", label: "Ir a Gestión de Pagos" };
  }
  return null;
}

/**
 * Abre el modal con el detalle completo de la notificación.
 */
export async function openNotificacionDetalleModal(notificacionId, { onReadUpdated } = {}) {
  let notif;
  try {
    notif = await getNotificacion(notificacionId);
  } catch (err) {
    showToast({
      type: "error",
      title: "Error",
      message: err.message || "No se pudo cargar el detalle de la notificación."
    });
    return;
  }

  // Si no estaba leída, marcarla automáticamente como leída
  if (!notif.leida) {
    try {
      await marcarNotificacionLeida(notif.id);
      notif = { ...notif, leida: true, fechaLectura: new Date().toISOString() };
      if (typeof onReadUpdated === "function") {
        onReadUpdated(notif);
      }
    } catch (_e) {
      // Continuar mostrando el detalle aunque falle marcarla
    }
  }

  const container = document.createElement("div");
  container.className = "notif-detail-modal-body";

  // Metadatos
  const metaBox = document.createElement("div");
  metaBox.className = "notif-detail-meta-box";

  metaBox.innerHTML = `
    <div class="notif-detail-meta-item">
      <span class="notif-detail-meta-label">Tipo de Notificación</span>
      <span class="notif-detail-meta-val">${notif.tipo || "INFORMATIVA"}</span>
    </div>
    <div class="notif-detail-meta-item">
      <span class="notif-detail-meta-label">Fecha y Hora de Emisión</span>
      <span class="notif-detail-meta-val">${formatDateTimePE(notif.fechaCreacion)}</span>
    </div>
    <div class="notif-detail-meta-item">
      <span class="notif-detail-meta-label">Estado de Lectura</span>
      <span class="notif-detail-meta-val">${notif.leida ? "Leída" : "No leída"}</span>
    </div>
    <div class="notif-detail-meta-item">
      <span class="notif-detail-meta-label">Fecha de Lectura</span>
      <span class="notif-detail-meta-val">${notif.fechaLectura ? formatDateTimePE(notif.fechaLectura) : "Recién leída"}</span>
    </div>
  `;
  container.appendChild(metaBox);

  // Mensaje completo
  const msgBox = document.createElement("div");
  msgBox.className = "notif-detail-message-box";
  msgBox.textContent = notif.mensaje || "Sin contenido de mensaje adicional.";
  container.appendChild(msgBox);

  // Recurso vinculado si aplica
  const resource = resolveResourceTarget(notif);
  if (resource) {
    const linkBox = document.createElement("div");
    linkBox.className = "notif-resource-link-box";

    const desc = document.createElement("span");
    desc.style.fontSize = "0.85rem";
    desc.style.fontWeight = "600";
    desc.textContent = "Esta notificación hace referencia a un proceso municipal:";

    const goBtn = document.createElement("button");
    goBtn.type = "button";
    goBtn.className = "btn btn-primary";
    goBtn.textContent = resource.label;

    goBtn.addEventListener("click", () => {
      modal.close();
      navigateToSection(resource.section);
    });

    linkBox.append(desc, goBtn);
    container.appendChild(linkBox);
  }

  const closeBtn = document.createElement("button");
  closeBtn.type = "button";
  closeBtn.className = "btn btn-secondary";
  closeBtn.textContent = "Cerrar";

  const modal = openModal({
    title: notif.titulo || "Notificación Municipal",
    content: container,
    footerActions: [closeBtn]
  });

  closeBtn.addEventListener("click", () => modal.close());
}
