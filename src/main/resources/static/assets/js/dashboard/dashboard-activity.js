/**
 * Renderizado de la tabla de Actividad Reciente e Información del Sistema (PROMPT 10 - Secciones 12 y 15)
 */
import { APP_CONFIG } from "../config.js";
import { clearChildren } from "../ui.js";

const localDateTimeFormatter = new Intl.DateTimeFormat("es-PE", {
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  second: "2-digit",
  hour12: false
});

/**
 * Formatea una fecha LocalDateTime ISO del backend al huso horario local sin tratarla erróneamente como UTC.
 */
export function formatLocalDateTime(value) {
  if (!value) {
    return "—";
  }
  if (value instanceof Date) {
    return Number.isNaN(value.getTime()) ? "—" : localDateTimeFormatter.format(value);
  }
  const str = String(value).trim();
  // Si viene como LocalDateTime sin zona (ej. 2026-11-12T22:15:30), se interpreta en hora local del sistema
  const parsed = new Date(str);
  if (Number.isNaN(parsed.getTime())) {
    return str;
  }
  return localDateTimeFormatter.format(parsed);
}

/**
 * Traduce códigos de operación de auditoría a etiquetas institucionales legibles.
 */
function formatOperationLabel(op) {
  if (!op) return "Operación";
  return String(op).replace(/_/g, " ");
}

/**
 * Renderiza la tabla de Actividad Reciente (Sección 12).
 */
export function renderRecentActivityTable(container, actividades) {
  if (!container) return;
  clearChildren(container);

  const list = Array.isArray(actividades) ? actividades : [];
  if (list.length === 0) {
    const empty = document.createElement("div");
    empty.className = "empty-state";
    const msg = document.createElement("p");
    msg.textContent = "Aún no se registran eventos de actividad reciente en el sistema.";
    empty.appendChild(msg);
    container.appendChild(empty);
    return;
  }

  const tableWrap = document.createElement("div");
  tableWrap.className = "table-container";

  const table = document.createElement("table");
  table.className = "table";

  const thead = document.createElement("thead");
  const headRow = document.createElement("tr");
  for (const col of ["Fecha y hora", "Operación", "Módulo", "Descripción", "Usuario"]) {
    const th = document.createElement("th");
    th.scope = "col";
    th.textContent = col;
    headRow.appendChild(th);
  }
  thead.appendChild(headRow);

  const tbody = document.createElement("tbody");
  for (const item of list) {
    const tr = document.createElement("tr");

    const tdFecha = document.createElement("td");
    tdFecha.className = "text-mono";
    tdFecha.style.whiteSpace = "nowrap";
    tdFecha.textContent = formatLocalDateTime(item.fecha);

    const tdOp = document.createElement("td");
    const opBadge = document.createElement("span");
    opBadge.className = "badge badge-info";
    opBadge.textContent = formatOperationLabel(item.tipoOperacion);
    tdOp.appendChild(opBadge);

    const tdMod = document.createElement("td");
    tdMod.textContent = item.modulo || "General";

    const tdDesc = document.createElement("td");
    tdDesc.textContent = item.descripcionBreve || "Sin descripción adicional";

    const tdUser = document.createElement("td");
    tdUser.textContent = item.usuarioResponsable || "Sistema";

    tr.append(tdFecha, tdOp, tdMod, tdDesc, tdUser);
    tbody.appendChild(tr);
  }

  table.append(thead, tbody);
  tableWrap.appendChild(table);
  container.appendChild(tableWrap);
}

/**
 * Renderiza la tarjeta "Información del sistema" consumiendo GET /api/public/health (Sección 15).
 * Etiqueta con precisión que el endpoint verifica la disponibilidad del servicio Backend (API).
 */
export function renderSystemInfoCard(container, healthData, lastUpdatedDate) {
  if (!container) return;
  clearChildren(container);

  const isUp = healthData && healthData.status === "UP";

  const list = document.createElement("div");
  list.className = "sysinfo-list";

  const items = [
    {
      label: "Nombre del sistema",
      value: APP_CONFIG.systemName
    },
    {
      label: "Entidad municipal",
      value: APP_CONFIG.municipalityName
    },
    {
      label: "Identificador de aplicación",
      value: healthData?.application || "MUNICIPALIDAD_DE_ANDRES_AVELINO_CACERES"
    },
    {
      label: "Estado del servicio Backend (API REST)",
      badge: {
        text: isUp ? "OPERATIVO (UP)" : "NO VERIFICADO / SIN CONEXIÓN",
        className: isUp ? "badge-success" : "badge-danger"
      },
      note: healthData?.message || "El diagnóstico /api/public/health comprueba la disponibilidad de la API del backend."
    },
    {
      label: "Última actualización de datos",
      value: lastUpdatedDate ? formatLocalDateTime(lastUpdatedDate) : "Pendiente"
    }
  ];

  for (const row of items) {
    const itemEl = document.createElement("div");
    itemEl.className = "sysinfo-item";

    const labelEl = document.createElement("span");
    labelEl.className = "sysinfo-label";
    labelEl.textContent = row.label;
    itemEl.appendChild(labelEl);

    if (row.badge) {
      const badgeEl = document.createElement("span");
      badgeEl.className = `badge ${row.badge.className}`;
      badgeEl.style.alignSelf = "flex-start";
      badgeEl.style.marginTop = "4px";
      badgeEl.textContent = row.badge.text;
      itemEl.appendChild(badgeEl);

      if (row.note) {
        const noteEl = document.createElement("span");
        noteEl.className = "form-help";
        noteEl.textContent = row.note;
        itemEl.appendChild(noteEl);
      }
    } else {
      const valueEl = document.createElement("span");
      valueEl.className = "sysinfo-value";
      valueEl.textContent = row.value;
      itemEl.appendChild(valueEl);
    }

    list.appendChild(itemEl);
  }

  container.appendChild(list);
}
