/**
 * Módulo de Línea de Tiempo e Historial del Expediente Municipal (PROMPT 12 - Secciones 13, 20 y 24)
 */
import { createElement, clearChildren } from "../dom.js";
import { getSolicitudEstadoLabel, createSolicitudEstadoBadge } from "./solicitud-estados.js";
import { listarHistorialSolicitud } from "./solicitudes-api.js";

function formatDateTime(isoString) {
  if (!isoString) return "—";
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) return String(isoString);
  return date.toLocaleString("es-PE", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit"
  });
}

function resolveTimelineIconText(estado) {
  const clean = String(estado || "").toUpperCase();
  switch (clean) {
    case "BORRADOR":
      return "📝";
    case "PRESENTADA":
      return "📥";
    case "EN_REVISION":
      return "🔍";
    case "OBSERVADA":
      return "⚠️";
    case "SUBSANADA":
      return "🔄";
    case "APROBADA":
      return "✅";
    case "RECHAZADA":
      return "❌";
    case "ARCHIVADA":
      return "📁";
    default:
      return "📌";
  }
}

/**
 * Renderiza la lista de hitos de la línea de tiempo en el contenedor provisto.
 */
export function renderTimelineItems(container, historialList = []) {
  if (!container) return;
  clearChildren(container);

  const items = Array.isArray(historialList) ? [...historialList] : [];
  if (items.length === 0) {
    container.appendChild(
      createElement("div", {
        className: "empty-state",
        textContent: "No se registran eventos de transición de estado en este expediente."
      })
    );
    return;
  }

  // Orden cronológico descendente (el más reciente arriba)
  items.sort((a, b) => new Date(b.fechaCambio || 0) - new Date(a.fechaCambio || 0));

  const timelineList = createElement("div", { className: "timeline-list" });

  items.forEach((item, index) => {
    const isCurrent = index === 0;
    const itemEl = createElement("div", {
      className: `timeline-item ${isCurrent ? "timeline-item--current" : ""}`.trim()
    });

    const marker = createElement("div", {
      className: `timeline-marker ${isCurrent ? "timeline-marker--current" : ""}`.trim(),
      textContent: resolveTimelineIconText(item.estadoNuevo)
    });

    const content = createElement("div", { className: "timeline-content" });

    const header = createElement("div", { className: "timeline-header" });
    const title = createElement("div", { className: "timeline-title" });

    if (item.estadoAnterior && item.estadoAnterior !== item.estadoNuevo) {
      title.appendChild(
        createElement("span", {
          className: "text-muted",
          textContent: `${getSolicitudEstadoLabel(item.estadoAnterior)} → `
        })
      );
    }
    title.appendChild(createSolicitudEstadoBadge(item.estadoNuevo));

    const dateEl = createElement("time", {
      className: "timeline-date",
      textContent: formatDateTime(item.fechaCambio),
      attrs: { datetime: item.fechaCambio || "" }
    });

    header.append(title, dateEl);

    const actor = createElement("div", {
      className: "timeline-actor",
      textContent: `Responsable: ${item.usuarioResponsableUsername || "Sistema Municipal"}`
    });

    content.append(header, actor);

    if (item.observaciones && item.observaciones.trim().length > 0) {
      const obs = createElement("div", {
        className: "timeline-observations",
        textContent: item.observaciones.trim()
      });
      content.appendChild(obs);
    }

    itemEl.append(marker, content);
    timelineList.appendChild(itemEl);
  });

  container.appendChild(timelineList);
}

/**
 * Carga desde el backend el historial completo de una solicitud y lo renderiza en el contenedor.
 */
export async function loadAndRenderSolicitudHistorial(solicitudId, container) {
  if (!container) return;
  clearChildren(container);
  container.appendChild(
    createElement("div", {
      className: "loading-state",
      textContent: "Cargando historial del expediente..."
    })
  );

  try {
    const list = await listarHistorialSolicitud(solicitudId);
    renderTimelineItems(container, list);
  } catch (err) {
    clearChildren(container);
    container.appendChild(
      createElement("div", {
        className: "field-error",
        textContent: `Error al consultar historial: ${err.message || "Servicio no disponible"}`
      })
    );
  }
}
