/**
 * Generador y validador dinámico de filtros para Reportes Municipales (PROMPT 14 - Sección 10)
 * Municipalidad Distrital de Andrés Avelino Cáceres Dorregaray
 */
import { clearChildren } from "../ui.js";

const ESTADOS_SOLICITUD = [
  { value: "", label: "Todos los estados" },
  { value: "BORRADOR", label: "Borrador" },
  { value: "PRESENTADA", label: "Presentada" },
  { value: "EN_REVISION", label: "En revisión" },
  { value: "OBSERVADA", label: "Observada" },
  { value: "SUBSANADA", label: "Subsanada" },
  { value: "APROBADA", label: "Aprobada" },
  { value: "RECHAZADA", label: "Rechazada" },
  { value: "ARCHIVADA", label: "Archivada" }
];

const ESTADOS_LICENCIA = [
  { value: "", label: "Todos los estados" },
  { value: "VIGENTE", label: "Vigente" },
  { value: "SUSPENDIDA", label: "Suspendida" },
  { value: "REVOCADA", label: "Revocada" },
  { value: "VENCIDA", label: "Vencida" }
];

const ESTADOS_INSPECCION = [
  { value: "", label: "Todos los estados" },
  { value: "PROGRAMADA", label: "Programada" },
  { value: "EN_CURSO", label: "En curso" },
  { value: "REALIZADA", label: "Realizada" },
  { value: "REPROGRAMADA", label: "Reprogramada" },
  { value: "CANCELADA", label: "Cancelada" }
];

const RESULTADOS_INSPECCION = [
  { value: "", label: "Todos los resultados" },
  { value: "FAVORABLE", label: "Favorable" },
  { value: "NO_FAVORABLE", label: "No favorable" },
  { value: "CON_OBSERVACIONES", label: "Con observaciones" }
];

const ESTADOS_PAGO = [
  { value: "", label: "Todos los estados" },
  { value: "PENDIENTE", label: "Pendiente" },
  { value: "VERIFICADO", label: "Verificado" },
  { value: "RECHAZADO", label: "Rechazado" },
  { value: "ANULADO", label: "Anulado" }
];

const TIPOS_DOCUMENTO = [
  { value: "", label: "Todos los tipos" },
  { value: "DNI", label: "DNI" },
  { value: "CARNET_EXTRANJERIA", label: "Carnet de Extranjería" },
  { value: "PASAPORTE", label: "Pasaporte" },
  { value: "RUC", label: "RUC" }
];

const ESTADOS_ACTIVO = [
  { value: "", label: "Todos" },
  { value: "true", label: "Activos" },
  { value: "false", label: "Inactivos" }
];

function createInputGroup(id, labelText, inputEl) {
  const group = document.createElement("div");
  group.className = "form-group";

  const label = document.createElement("label");
  label.htmlFor = id;
  label.className = "form-label";
  label.textContent = labelText;

  group.append(label, inputEl);
  return group;
}

function createSelect(id, options = []) {
  const select = document.createElement("select");
  select.id = id;
  select.name = id;
  select.className = "form-select";

  for (const opt of options) {
    const o = document.createElement("option");
    o.value = opt.value;
    o.textContent = opt.label;
    select.appendChild(o);
  }
  return select;
}

function createDateInput(id) {
  const input = document.createElement("input");
  input.type = "date";
  input.id = id;
  input.name = id;
  input.className = "form-input";
  return input;
}

function createTextInput(id, placeholder = "") {
  const input = document.createElement("input");
  input.type = "text";
  input.id = id;
  input.name = id;
  input.placeholder = placeholder;
  input.className = "form-input";
  return input;
}

/**
 * Renderiza los campos de filtro adecuados para el tipo de reporte seleccionado.
 */
export function renderReportFilters(container, tipoReporte, { onFilter, onReset } = {}) {
  if (!container) return;
  clearChildren(container);

  const card = document.createElement("div");
  card.className = "reportes-filters-card";

  const form = document.createElement("form");
  form.noValidate = true;

  const grid = document.createElement("div");
  grid.className = "reportes-filters-grid";

  // Filtros condicionales por tipo
  if (tipoReporte === "solicitudes") {
    grid.appendChild(createInputGroup("filter-fechaDesde", "Fecha Desde", createDateInput("filter-fechaDesde")));
    grid.appendChild(createInputGroup("filter-fechaHasta", "Fecha Hasta", createDateInput("filter-fechaHasta")));
    grid.appendChild(createInputGroup("filter-estado", "Estado de Solicitud", createSelect("filter-estado", ESTADOS_SOLICITUD)));
    grid.appendChild(createInputGroup("filter-tipoTramiteId", "ID Tipo Trámite", createTextInput("filter-tipoTramiteId", "Ej. 1")));
  } else if (tipoReporte === "licencias") {
    grid.appendChild(createInputGroup("filter-fechaDesde", "Fecha Emisión Desde", createDateInput("filter-fechaDesde")));
    grid.appendChild(createInputGroup("filter-fechaHasta", "Fecha Emisión Hasta", createDateInput("filter-fechaHasta")));
    grid.appendChild(createInputGroup("filter-estado", "Estado de Licencia", createSelect("filter-estado", ESTADOS_LICENCIA)));
    grid.appendChild(createInputGroup("filter-giro", "Giro Comercial", createTextInput("filter-giro", "Buscar por giro...")));
  } else if (tipoReporte === "inspecciones") {
    grid.appendChild(createInputGroup("filter-fechaDesde", "Fecha Programada Desde", createDateInput("filter-fechaDesde")));
    grid.appendChild(createInputGroup("filter-fechaHasta", "Fecha Programada Hasta", createDateInput("filter-fechaHasta")));
    grid.appendChild(createInputGroup("filter-estado", "Estado Inspección", createSelect("filter-estado", ESTADOS_INSPECCION)));
    grid.appendChild(createInputGroup("filter-resultado", "Resultado Técnico", createSelect("filter-resultado", RESULTADOS_INSPECCION)));
    grid.appendChild(createInputGroup("filter-inspectorId", "ID Inspector", createTextInput("filter-inspectorId", "Ej. 2")));
  } else if (tipoReporte === "pagos") {
    grid.appendChild(createInputGroup("filter-fechaDesde", "Fecha Pago Desde", createDateInput("filter-fechaDesde")));
    grid.appendChild(createInputGroup("filter-fechaHasta", "Fecha Pago Hasta", createDateInput("filter-fechaHasta")));
    grid.appendChild(createInputGroup("filter-estado", "Estado de Pago", createSelect("filter-estado", ESTADOS_PAGO)));
    grid.appendChild(createInputGroup("filter-concepto", "Concepto", createTextInput("filter-concepto", "Buscar por concepto...")));
  } else if (tipoReporte === "establecimientos") {
    grid.appendChild(createInputGroup("filter-nombreComercial", "Nombre Comercial", createTextInput("filter-nombreComercial", "Buscar nombre...")));
    grid.appendChild(createInputGroup("filter-giro", "Giro Comercial", createTextInput("filter-giro", "Buscar giro...")));
    grid.appendChild(createInputGroup("filter-activo", "Estado de Registro", createSelect("filter-activo", ESTADOS_ACTIVO)));
  } else if (tipoReporte === "ciudadanos") {
    grid.appendChild(createInputGroup("filter-nombre", "Nombre o Razón Social", createTextInput("filter-nombre", "Buscar ciudadano...")));
    grid.appendChild(createInputGroup("filter-tipoDocumento", "Tipo Documento", createSelect("filter-tipoDocumento", TIPOS_DOCUMENTO)));
    grid.appendChild(createInputGroup("filter-activo", "Estado de Registro", createSelect("filter-activo", ESTADOS_ACTIVO)));
  }

  // Barra de acciones
  const actions = document.createElement("div");
  actions.className = "reportes-filters-actions";

  const btnReset = document.createElement("button");
  btnReset.type = "button";
  btnReset.className = "btn btn-secondary";
  btnReset.textContent = "Limpiar Filtros";

  const btnSubmit = document.createElement("button");
  btnSubmit.type = "submit";
  btnSubmit.className = "btn btn-primary";
  btnSubmit.textContent = "Generar Reporte";

  actions.append(btnReset, btnSubmit);
  form.append(grid, actions);
  card.appendChild(form);
  container.appendChild(card);

  // Manejadores
  form.addEventListener("submit", (e) => {
    e.preventDefault();
    const values = getFilterValues(form);
    if (values.fechaDesde && values.fechaHasta && values.fechaDesde > values.fechaHasta) {
      alert("La fecha inicial no puede ser posterior a la fecha final.");
      return;
    }
    if (typeof onFilter === "function") {
      onFilter(values);
    }
  });

  btnReset.addEventListener("click", () => {
    form.reset();
    if (typeof onReset === "function") {
      onReset();
    }
  });

  return {
    getValues: () => getFilterValues(form),
    reset: () => form.reset()
  };
}

function getFilterValues(form) {
  const result = {};
  const elements = form.elements;
  for (let i = 0; i < elements.length; i++) {
    const el = elements[i];
    if (el.name && el.name.startsWith("filter-")) {
      const key = el.name.replace(/^filter-/, "");
      const val = el.value.trim();
      if (val !== "") {
        if (key === "activo") {
          result[key] = val === "true";
        } else if (key === "tipoTramiteId" || key === "inspectorId") {
          const num = Number(val);
          if (!Number.isNaN(num)) result[key] = num;
        } else {
          result[key] = val;
        }
      }
    }
  }
  return result;
}
