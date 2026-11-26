/**
 * Controlador principal del Módulo de Auditoría del Sistema (PROMPT 14 - Secciones 25 a 30)
 * Municipalidad Distrital de Andrés Avelino Cáceres Dorregaray
 */
import { clearChildren, showToast } from "../ui.js";
import { renderDataTable, formatDateTimePE, createTableActions } from "../components/data-table.js";
import { renderPagination } from "../components/pagination.js";
import { getAuditorias } from "./auditoria-api.js";
import { openAuditoriaDetalleModal } from "./auditoria-detalle.js";

const ACCIONES_COMUNES = [
  { value: "", label: "Todas las acciones" },
  { value: "INICIO_SESION", label: "Inicio de sesión" },
  { value: "CIERRE_SESION", label: "Cierre de sesión" },
  { value: "CREACION", label: "Creación" },
  { value: "ACTUALIZACION", label: "Actualización" },
  { value: "PRESENTACION", label: "Presentación" },
  { value: "OBSERVACION", label: "Observación" },
  { value: "SUBSANACION", label: "Subsanación" },
  { value: "APROBACION", label: "Aprobación" },
  { value: "RECHAZO", label: "Rechazo" },
  { value: "PROGRAMACION", label: "Programación" },
  { value: "EMISION", label: "Emisión de Licencia" },
  { value: "SUSPENSION", label: "Suspensión de Licencia" },
  { value: "REVOCACION", label: "Revocación de Licencia" },
  { value: "VERIFICACION", label: "Verificación de Pago" },
  { value: "ANULACION", label: "Anulación de Pago" }
];

const ENTIDADES_COMUNES = [
  { value: "", label: "Todas las entidades" },
  { value: "Solicitud", label: "Solicitud / Expediente" },
  { value: "Licencia", label: "Licencia de Funcionamiento" },
  { value: "Inspeccion", label: "Inspección ITSE" },
  { value: "Pago", label: "Pago Municipal" },
  { value: "Documento", label: "Documento Adjunto" },
  { value: "Establecimiento", label: "Establecimiento" },
  { value: "Ciudadano", label: "Ciudadano" },
  { value: "Representante", label: "Representante Legal" },
  { value: "Usuario", label: "Usuario del Sistema" }
];

export async function renderAuditoriaModule(user, container, { initialId = null } = {}) {
  if (!container) return;
  clearChildren(container);

  // Control de acceso por rol
  const userRoles = Array.isArray(user?.roles) ? user.roles : [];
  const canAccess = userRoles.includes("ADMINISTRADOR") || userRoles.includes("SUPERVISOR");
  if (!canAccess) {
    const deniedBox = document.createElement("div");
    deniedBox.className = "alert alert-error";
    deniedBox.textContent = "Acceso restringido: Solo los roles Administrador y Supervisor pueden consultar la bitácora de auditoría.";
    container.appendChild(deniedBox);
    return;
  }

  const root = document.createElement("div");
  root.className = "auditoria-module";

  let currentPage = 0;
  let pageSize = 20;
  let currentFilters = {};

  // 1. Tarjeta informativa de bitácora inmutable
  const noticeCard = document.createElement("div");
  noticeCard.className = "auditoria-notice-card";

  const noticeText = document.createElement("div");
  noticeText.className = "auditoria-notice-text";
  noticeText.innerHTML = `
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
    </svg>
    <span>Bitácora de seguridad institucional inmutable. Todos los eventos quedan registrados con fines de trazabilidad y transparencia administrativa.</span>
  `;

  const immutableBadge = document.createElement("span");
  immutableBadge.className = "auditoria-badge-immutable";
  immutableBadge.textContent = "Solo Lectura";

  noticeCard.append(noticeText, immutableBadge);
  root.appendChild(noticeCard);

  // 2. Tarjeta de filtros de búsqueda
  const filtersCard = document.createElement("div");
  filtersCard.className = "auditoria-filters-card";

  const form = document.createElement("form");
  form.noValidate = true;

  const grid = document.createElement("div");
  grid.className = "auditoria-filters-grid";

  grid.innerHTML = `
    <div class="form-group">
      <label for="audit-entidad" class="form-label">Módulo / Entidad</label>
      <select id="audit-entidad" name="entidad" class="form-select">
        ${ENTIDADES_COMUNES.map(e => `<option value="${e.value}">${e.label}</option>`).join("")}
      </select>
    </div>
    <div class="form-group">
      <label for="audit-accion" class="form-label">Tipo de Acción</label>
      <select id="audit-accion" name="accion" class="form-select">
        ${ACCIONES_COMUNES.map(a => `<option value="${a.value}">${a.label}</option>`).join("")}
      </select>
    </div>
    <div class="form-group">
      <label for="audit-fechaDesde" class="form-label">Fecha Desde</label>
      <input type="date" id="audit-fechaDesde" name="fechaDesde" class="form-input" />
    </div>
    <div class="form-group">
      <label for="audit-fechaHasta" class="form-label">Fecha Hasta</label>
      <input type="date" id="audit-fechaHasta" name="fechaHasta" class="form-input" />
    </div>
    <div class="form-group">
      <label for="audit-entidadId" class="form-label">ID Registro Afectado</label>
      <input type="number" id="audit-entidadId" name="entidadId" placeholder="Ej. 10" class="form-input" />
    </div>
  `;

  const actions = document.createElement("div");
  actions.className = "auditoria-filters-actions";

  const resetBtn = document.createElement("button");
  resetBtn.type = "button";
  resetBtn.className = "btn btn-secondary";
  resetBtn.textContent = "Limpiar";

  const submitBtn = document.createElement("button");
  submitBtn.type = "submit";
  submitBtn.className = "btn btn-primary";
  submitBtn.textContent = "Buscar Operaciones";

  actions.append(resetBtn, submitBtn);
  form.append(grid, actions);
  filtersCard.appendChild(form);
  root.appendChild(filtersCard);

  // 3. Tarjeta de tabla y paginación
  const tableCard = document.createElement("div");
  tableCard.className = "table-card";

  const tableHeader = document.createElement("div");
  tableHeader.className = "table-header";

  const tableTitle = document.createElement("h3");
  tableTitle.className = "table-title";
  tableTitle.textContent = "Registro de Eventos y Operaciones";

  tableHeader.appendChild(tableTitle);
  tableCard.appendChild(tableHeader);

  const tableMount = document.createElement("div");
  tableCard.appendChild(tableMount);

  const paginationMount = document.createElement("div");
  tableCard.appendChild(paginationMount);

  root.appendChild(tableCard);
  container.appendChild(root);

  // Manejadores de formulario
  form.addEventListener("submit", (e) => {
    e.preventDefault();
    const fd = new FormData(form);
    const filters = {};
    for (const [k, v] of fd.entries()) {
      const val = String(v).trim();
      if (val !== "") {
        if (k === "entidadId") {
          const num = Number(val);
          if (!Number.isNaN(num)) filters[k] = num;
        } else {
          filters[k] = val;
        }
      }
    }
    if (filters.fechaDesde && filters.fechaHasta && filters.fechaDesde > filters.fechaHasta) {
      showToast({
        type: "warning",
        title: "Fechas inválidas",
        message: "La fecha inicial no puede ser posterior a la fecha final."
      });
      return;
    }
    currentFilters = filters;
    currentPage = 0;
    loadAudit();
  });

  resetBtn.addEventListener("click", () => {
    form.reset();
    currentFilters = {};
    currentPage = 0;
    loadAudit();
  });

  async function loadAudit() {
    renderDataTable(tableMount, { loading: true });
    clearChildren(paginationMount);

    try {
      const queryParams = {
        ...currentFilters,
        page: currentPage,
        size: pageSize
      };

      const response = await getAuditorias(queryParams);
      const items = Array.isArray(response?.contenido) ? response.contenido : [];

      renderDataTable(tableMount, {
        caption: "Bitácora de eventos de auditoría",
        columns: getAuditColumns(),
        rows: items,
        emptyTitle: "No se encontraron operaciones registradas",
        emptySubtitle: "Ajuste los criterios de búsqueda o verifique las fechas seleccionadas."
      });

      renderPagination(paginationMount, {
        pagina: response.pagina,
        tamano: response.tamanio,
        totalElementos: response.totalElementos,
        totalPaginas: response.totalPaginas,
        primera: response.primera,
        ultima: response.ultima,
        onPageChange: (newPage) => {
          currentPage = newPage;
          loadAudit();
        },
        onSizeChange: (newSize) => {
          pageSize = newSize;
          currentPage = 0;
          loadAudit();
        }
      });
    } catch (err) {
      renderDataTable(tableMount, {
        error: err.message || "Error al cargar la bitácora de auditoría.",
        onRetry: () => loadAudit()
      });
    }
  }

  function getAuditColumns() {
    return [
      { label: "ID", render: r => `#${r.id}` },
      { label: "Fecha y Hora", render: r => formatDateTimePE(r.fechaOperacion) },
      {
        label: "Usuario",
        render: r => {
          const span = document.createElement("span");
          span.style.fontWeight = "600";
          span.textContent = r.usernameUsuario ? `@${r.usernameUsuario}` : (r.nombreCompletoUsuario || "Sistema");
          return span;
        }
      },
      {
        label: "Acción",
        render: r => {
          const badge = document.createElement("span");
          const act = String(r.accion || "").toUpperCase();
          let cls = "badge";
          if (act.includes("CREA") || act.includes("REGISTRO")) cls += " badge-accion-creacion";
          else if (act.includes("ACTUALIZ")) cls += " badge-accion-actualizacion";
          else if (act.includes("APROB") || act.includes("EMISION")) cls += " badge-accion-aprobacion";
          else if (act.includes("OBSERV")) cls += " badge-accion-observacion";
          else if (act.includes("RECHAZ") || act.includes("SUSPEND") || act.includes("REVOC") || act.includes("ANUL")) cls += " badge-accion-rechazo";
          else cls += " badge-info";
          badge.className = cls;
          badge.textContent = r.accion || "OPERACION";
          return badge;
        }
      },
      { label: "Entidad", render: r => r.entidadAfectada || "—" },
      { label: "ID Afectado", render: r => r.entidadId ? `#${r.entidadId}` : "—" },
      {
        label: "IP Origen",
        render: r => {
          const span = document.createElement("span");
          span.className = "auditoria-ip-badge";
          span.textContent = r.direccionIp || "127.0.0.1";
          return span;
        }
      },
      {
        label: "Acciones",
        render: r => {
          return createTableActions([
            {
              label: "Ver Detalle",
              title: "Ver detalle completo del evento",
              onClick: () => openAuditoriaDetalleModal(r.id)
            }
          ]);
        }
      }
    ];
  }

  // Carga inicial
  await loadAudit();

  if (initialId) {
    openAuditoriaDetalleModal(initialId);
  }
}
