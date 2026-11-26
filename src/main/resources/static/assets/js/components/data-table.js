/**
 * Componente reutilizable de Tabla Dinámica Institucional (PROMPT 11 - Sección 16)
 * Construye tablas mediante creación segura de nodos DOM (textContent), sin innerHTML con datos del backend.
 */
import { clearChildren } from "../ui.js";

/**
 * Crea un badge de estado visual accesible (ACTIVO / INACTIVO).
 */
export function createStatusBadge(activo, activeLabel = "ACTIVO", inactiveLabel = "INACTIVO") {
  const badge = document.createElement("span");
  badge.className = `badge ${activo ? "badge-success" : "badge-danger"}`;
  badge.textContent = activo ? activeLabel : inactiveLabel;
  return badge;
}

/**
 * Crea un grupo de botones de acción para una fila de tabla.
 *
 * @param {Array<{label: string, title?: string, variant?: string, disabled?: boolean, onClick: () => void}>} actions
 */
export function createTableActions(actions = []) {
  const group = document.createElement("div");
  group.className = "table-actions-group";

  for (const act of actions) {
    if (!act) continue;
    const btn = document.createElement("button");
    btn.type = "button";
    const variantClass = act.variant ? ` is-${act.variant}` : "";
    btn.className = `btn-table-action${variantClass}`;
    btn.textContent = act.label;
    if (act.title) {
      btn.title = act.title;
      btn.setAttribute("aria-label", act.title);
    }
    if (act.disabled) {
      btn.disabled = true;
    }
    if (typeof act.onClick === "function") {
      btn.addEventListener("click", act.onClick);
    }
    group.appendChild(btn);
  }

  return group;
}

/**
 * Formatea una fecha ISO LocalDateTime a formato legible en español peruano (dd/mm/aaaa hh:mm).
 */
export function formatDateTimePE(isoValue) {
  if (!isoValue) return "—";
  const date = new Date(isoValue);
  if (Number.isNaN(date.getTime())) return String(isoValue);
  return new Intl.DateTimeFormat("es-PE", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  }).format(date);
}

/**
 * Enmascara parcialmente un documento de identidad personal cuando el modo de privacidad está activo
 * (Ley N.º 29733 - Minimización de exposición visual en listados generales).
 */
export function maskDocumentNumber(numero, maskEnabled = false) {
  const str = String(numero || "").trim();
  if (!str) return "—";
  if (!maskEnabled || str.length <= 4) {
    return str;
  }
  const visibleTail = str.slice(-4);
  return `${"•".repeat(Math.max(4, str.length - 4))}${visibleTail}`;
}

/**
 * Renderiza una tabla institucional reutilizable con soporte de estados de carga, vacío y error.
 *
 * @param {HTMLElement} container Contenedor donde se montará la tabla
 * @param {Object} config
 * @param {string} [config.caption] Descripción accesible de la tabla
 * @param {Array<{ key?: string, label: string, render?: (row: any) => Node|string }>} config.columns
 * @param {Array<any>} [config.rows=[]] Filas de datos
 * @param {boolean} [config.loading=false] Estado de carga
 * @param {string|null} [config.error=null] Mensaje de error si ocurrió fallo
 * @param {string} [config.emptyTitle="No se encontraron registros"]
 * @param {string} [config.emptySubtitle="Ajuste los filtros de búsqueda o registre un nuevo elemento."]
 * @param {() => void} [config.onRetry] Callback para reintentar en caso de error
 */
export function renderDataTable(
  container,
  {
    caption = "Listado de registros municipales",
    columns = [],
    rows = [],
    loading = false,
    error = null,
    emptyTitle = "No se encontraron registros",
    emptySubtitle = "Ajuste los criterios de búsqueda o registre un nuevo elemento si cuenta con permisos.",
    onRetry = null
  } = {}
) {
  if (!container) return;
  clearChildren(container);

  const wrap = document.createElement("div");
  wrap.className = "table-responsive-wrap";

  if (loading) {
    const stateBox = document.createElement("div");
    stateBox.className = "table-state-box";
    stateBox.setAttribute("role", "status");
    stateBox.setAttribute("aria-live", "polite");

    const spinner = document.createElement("div");
    spinner.className = "spinner spinner-primary";
    spinner.setAttribute("aria-hidden", "true");

    const title = document.createElement("p");
    title.className = "table-state-title";
    title.textContent = "Consultando registros en el servidor municipal...";

    stateBox.append(spinner, title);
    wrap.appendChild(stateBox);
    container.appendChild(wrap);
    return;
  }

  if (error) {
    const stateBox = document.createElement("div");
    stateBox.className = "table-state-box";
    stateBox.setAttribute("role", "alert");

    const title = document.createElement("p");
    title.className = "table-state-title";
    title.style.color = "var(--color-danger)";
    title.textContent = "No fue posible cargar la información";

    const sub = document.createElement("p");
    sub.className = "table-state-subtitle";
    sub.textContent = error;

    stateBox.append(title, sub);

    if (typeof onRetry === "function") {
      const retryBtn = document.createElement("button");
      retryBtn.type = "button";
      retryBtn.className = "btn btn-secondary btn-sm";
      retryBtn.style.marginTop = "0.5rem";
      retryBtn.textContent = "Reintentar consulta";
      retryBtn.addEventListener("click", onRetry);
      stateBox.appendChild(retryBtn);
    }

    wrap.appendChild(stateBox);
    container.appendChild(wrap);
    return;
  }

  if (!Array.isArray(rows) || rows.length === 0) {
    const stateBox = document.createElement("div");
    stateBox.className = "table-state-box";

    const title = document.createElement("p");
    title.className = "table-state-title";
    title.textContent = emptyTitle;

    const sub = document.createElement("p");
    sub.className = "table-state-subtitle";
    sub.textContent = emptySubtitle;

    stateBox.append(title, sub);
    wrap.appendChild(stateBox);
    container.appendChild(wrap);
    return;
  }

  const table = document.createElement("table");
  table.className = "data-table";

  const captionEl = document.createElement("caption");
  captionEl.className = "sr-only";
  captionEl.textContent = caption;
  table.appendChild(captionEl);

  const thead = document.createElement("thead");
  const headTr = document.createElement("tr");

  for (const col of columns) {
    const th = document.createElement("th");
    th.scope = "col";
    th.textContent = col.label;
    headTr.appendChild(th);
  }
  thead.appendChild(headTr);

  const tbody = document.createElement("tbody");

  for (const row of rows) {
    const tr = document.createElement("tr");
    for (const col of columns) {
      const td = document.createElement("td");
      if (typeof col.render === "function") {
        const rendered = col.render(row);
        if (rendered instanceof Node) {
          td.appendChild(rendered);
        } else {
          td.textContent = rendered !== null && rendered !== undefined && rendered !== "" ? String(rendered) : "—";
        }
      } else if (col.key) {
        const val = row ? row[col.key] : null;
        td.textContent = val !== null && val !== undefined && val !== "" ? String(val) : "—";
      } else {
        td.textContent = "—";
      }
      tr.appendChild(td);
    }
    tbody.appendChild(tr);
  }

  table.append(thead, tbody);
  wrap.appendChild(table);
  container.appendChild(wrap);
}
