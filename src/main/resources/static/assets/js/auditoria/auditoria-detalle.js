/**
 * Vista de Detalle de Auditoría Municipal de Solo Lectura (PROMPT 14 - Sección 28)
 * Municipalidad Distrital de Andrés Avelino Cáceres Dorregaray
 */
import { showToast } from "../ui.js";
import { openModal } from "../components/modal.js";
import { formatDateTimePE } from "../components/data-table.js";
import { getAuditoria } from "./auditoria-api.js";

/**
 * Abre el modal con el detalle inmutable de un evento de auditoría.
 */
export async function openAuditoriaDetalleModal(auditoriaId) {
  let item;
  try {
    item = await getAuditoria(auditoriaId);
  } catch (err) {
    showToast({
      type: "error",
      title: "Error",
      message: err.message || "No se pudo cargar el registro de auditoría."
    });
    return;
  }

  const container = document.createElement("div");
  container.className = "auditoria-detail-grid";

  // Identificador
  container.appendChild(createDetailItem("ID de Auditoría", `#${item.id}`));

  // Fecha y hora
  container.appendChild(createDetailItem("Fecha y Hora de Operación", formatDateTimePE(item.fechaOperacion)));

  // Usuario responsable
  const userInfo = item.nombreCompletoUsuario
    ? `${item.nombreCompletoUsuario} (@${item.usernameUsuario || "desconocido"})`
    : `@${item.usernameUsuario || "desconocido"}`;
  container.appendChild(createDetailItem("Usuario Responsable", userInfo));

  // Dirección IP
  const ipWrap = document.createElement("div");
  ipWrap.className = "auditoria-detail-item";
  const ipLabel = document.createElement("span");
  ipLabel.className = "auditoria-detail-label";
  ipLabel.textContent = "Dirección IP de Origen";
  const ipVal = document.createElement("span");
  ipVal.className = "auditoria-ip-badge";
  ipVal.textContent = item.direccionIp || "127.0.0.1 (Local)";
  ipWrap.append(ipLabel, ipVal);
  container.appendChild(ipWrap);

  // Módulo / Entidad afectada
  container.appendChild(createDetailItem("Entidad / Módulo Afectado", item.entidadAfectada || "SISTEMA"));

  // ID del registro afectado
  container.appendChild(createDetailItem("Identificador Afectado", item.entidadId ? `ID: ${item.entidadId}` : "N/A"));

  // Acción realizada
  container.appendChild(createDetailItem("Acción Realizada", item.accion || "OPERACION"));

  // Carácter inmutable
  container.appendChild(createDetailItem("Integridad de Registro", "Inmutable (Solo Lectura)"));

  // Descripción completa (sanitizada mediante textContent)
  const descItem = document.createElement("div");
  descItem.className = "auditoria-detail-item full-width";

  const descLabel = document.createElement("span");
  descLabel.className = "auditoria-detail-label";
  descLabel.textContent = "Detalle y Descripción de la Operación";

  const descBox = document.createElement("div");
  descBox.className = "auditoria-description-box";
  descBox.textContent = item.descripcion || "Sin descripción adicional de la operación registrada.";

  descItem.append(descLabel, descBox);
  container.appendChild(descItem);

  const closeBtn = document.createElement("button");
  closeBtn.type = "button";
  closeBtn.className = "btn btn-secondary";
  closeBtn.textContent = "Cerrar";

  const modal = openModal({
    title: `Registro de Auditoría #${item.id}`,
    content: container,
    footerActions: [closeBtn]
  });

  closeBtn.addEventListener("click", () => modal.close());
}

function createDetailItem(label, value) {
  const item = document.createElement("div");
  item.className = "auditoria-detail-item";

  const labelEl = document.createElement("span");
  labelEl.className = "auditoria-detail-label";
  labelEl.textContent = label;

  const valEl = document.createElement("span");
  valEl.className = "auditoria-detail-value";
  valEl.textContent = String(value);

  item.append(labelEl, valEl);
  return item;
}
