/**
 * Módulo Principal de Gestión de Solicitudes y Expedientes Municipales (PROMPT 12 - Secciones 10, 11, 12, 17 y 21)
 */
import { ROLES } from "../config.js";
import { hasRole } from "../auth.js";
import { clearChildren, showToast } from "../ui.js";
import { renderDataTable, createTableActions, formatDateTimePE } from "../components/data-table.js";
import { renderPagination } from "../components/pagination.js";
import { listarSolicitudes, consultarPorExpediente } from "./solicitudes-api.js";
import { listarTiposTramite } from "../tramites/tramites-api.js";
import { createSolicitudEstadoBadge, ESTADOS_SOLICITUD } from "./solicitud-estados.js";
import { renderNuevaSolicitudWizard } from "./solicitud-form.js";
import { renderSolicitudDetalle } from "./solicitud-detalle.js";
import { renderTiposTramiteView } from "../tramites/tramites.js";
import { renderRequisitosView } from "../requisitos/requisitos.js";

/**
 * Renderiza el módulo completo de Gestión de Solicitudes en el contenedor principal.
 */
export async function renderSolicitudesModule(
  currentUser,
  container,
  { subview = "bandeja", initialId = null } = {}
) {
  if (!container) return;
  clearChildren(container);

  const isAdmin = hasRole(currentUser, ROLES.ADMINISTRADOR);

  let activeTab = subview || "bandeja";
  let activeSolicitudId = initialId ? Number(initialId) : null;

  // Estado del listado y filtros de la bandeja
  const state = {
    filters: {
      numeroExpediente: "",
      estado: "",
      tipoTramiteId: "",
      fechaInicial: "",
      fechaFinal: ""
    },
    page: 0,
    size: 15,
    loading: false,
    tiposTramiteCatalog: []
  };

  const wrapper = document.createElement("div");
  wrapper.className = "management-module";

  // 1. Barra de Subnavegación interna por pestañas
  const subnav = document.createElement("nav");
  subnav.className = "solicitudes-subnav";
  subnav.setAttribute("aria-label", "Pestañas del módulo de solicitudes");

  const tabBandejaBtn = createTabButton("bandeja", "Bandeja de Expedientes", true);
  const tabNuevaBtn = createTabButton("nueva", "+ Nueva Solicitud", false);
  subnav.append(tabBandejaBtn, tabNuevaBtn);

  let tabTramitesBtn = null;
  let tabRequisitosBtn = null;
  if (isAdmin) {
    tabTramitesBtn = createTabButton("tramites", "Tipos de Trámite", false);
    tabRequisitosBtn = createTabButton("requisitos", "Catálogo de Requisitos", false);
    subnav.append(tabTramitesBtn, tabRequisitosBtn);
  }

  wrapper.appendChild(subnav);

  // 2. Contenedor dinámico de la pestaña activa
  const tabContentMount = document.createElement("div");
  tabContentMount.className = "solicitudes-tab-content";
  wrapper.appendChild(tabContentMount);
  container.appendChild(wrapper);

  function createTabButton(key, labelText, isActive) {
    const btn = document.createElement("button");
    btn.type = "button";
    btn.className = `solicitudes-subnav-btn ${isActive ? "is-active" : ""}`;
    btn.textContent = labelText;
    btn.dataset.tabKey = key;
    btn.addEventListener("click", () => switchTab(key));
    return btn;
  }

  function updateSubnavActiveState(selectedKey) {
    subnav.querySelectorAll(".solicitudes-subnav-btn").forEach((btn) => {
      btn.classList.toggle("is-active", btn.dataset.tabKey === selectedKey);
    });
  }

  function switchTab(targetKey, targetId = null) {
    activeTab = targetKey;
    activeSolicitudId = targetId ? Number(targetId) : null;
    updateSubnavActiveState(targetKey);
    clearChildren(tabContentMount);

    if (targetKey === "nueva") {
      renderNuevaSolicitudWizard({
        container: tabContentMount,
        initialDraftId: activeSolicitudId,
        onFinished: (result) => {
          switchTab("detalle", result.id);
        },
        onCancel: () => switchTab("bandeja")
      });
      return;
    }

    if (targetKey === "detalle" && activeSolicitudId) {
      renderSolicitudDetalle({
        container: tabContentMount,
        solicitudId: activeSolicitudId,
        currentUser,
        onBack: () => switchTab("bandeja"),
        onNavigateToEditDraft: (draftId) => switchTab("nueva", draftId)
      });
      return;
    }

    if (targetKey === "tramites" && isAdmin) {
      renderTiposTramiteView(currentUser, tabContentMount);
      return;
    }

    if (targetKey === "requisitos" && isAdmin) {
      renderRequisitosView(currentUser, tabContentMount);
      return;
    }

    renderBandejaView();
  }

  // --- VISTA DE BANDEJA DE EXPEDIENTES ---
  async function renderBandejaView() {
    clearChildren(tabContentMount);

    // Barra superior: Consulta directa por N° de Expediente + Toolbar de filtros
    const toolbarCard = document.createElement("section");
    toolbarCard.className = "module-toolbar-card";
    toolbarCard.setAttribute("aria-label", "Filtros y búsqueda de expedientes municipales");

    const topRow = document.createElement("div");
    topRow.className = "module-toolbar-top";

    const titleGroup = document.createElement("div");
    titleGroup.className = "module-toolbar-title-group";

    const heading = document.createElement("h2");
    heading.className = "module-toolbar-heading";
    heading.textContent = "Bandeja General de Solicitudes y Expedientes";

    const caption = document.createElement("p");
    caption.className = "module-toolbar-caption";
    caption.textContent = "Seguimiento administrativo del ciclo de vida de licencias, evaluación y estados.";
    titleGroup.append(heading, caption);

    // Consulta rápida por expediente
    const quickSearchForm = document.createElement("form");
    quickSearchForm.style.display = "flex";
    quickSearchForm.style.gap = "0.5rem";
    quickSearchForm.style.alignItems = "center";

    const quickInput = document.createElement("input");
    quickInput.type = "search";
    quickInput.className = "form-control";
    quickInput.style.minWidth = "220px";
    quickInput.placeholder = "Ej.: EXP-2026-00001";
    quickInput.setAttribute("aria-label", "Buscar por número de expediente");

    const quickBtn = document.createElement("button");
    quickBtn.type = "submit";
    quickBtn.className = "btn btn-secondary";
    quickBtn.textContent = "Ir a Expediente";

    quickSearchForm.append(quickInput, quickBtn);

    quickSearchForm.addEventListener("submit", async (e) => {
      e.preventDefault();
      const code = quickInput.value.trim();
      if (!code) {
        showToast({ type: "warning", message: "Ingrese el número de expediente a buscar." });
        return;
      }
      try {
        quickBtn.disabled = true;
        const res = await consultarPorExpediente(code);
        if (res && (res.solicitudId || res.id)) {
          switchTab("detalle", res.solicitudId || res.id);
        } else {
          showToast({ type: "error", message: `No se encontró el expediente "${code}".` });
        }
      } catch (err) {
        showToast({
          type: "error",
          message: err.message || `No se encontró el expediente "${code}".`
        });
      } finally {
        quickBtn.disabled = false;
      }
    });

    topRow.append(titleGroup, quickSearchForm);
    toolbarCard.appendChild(topRow);

    // Formulario de filtros avanzados
    const filterForm = document.createElement("form");
    filterForm.className = "module-filter-grid";
    filterForm.noValidate = true;

    // Filtro por N° Expediente
    const numExpGroup = createFilterField("N° Expediente", "text", "Filtro expediente");
    const numExpInput = numExpGroup.input;
    numExpInput.value = state.filters.numeroExpediente;

    // Filtro por Estado
    const estadoGroup = document.createElement("div");
    estadoGroup.className = "form-group";
    const estadoLabel = document.createElement("label");
    estadoLabel.className = "form-label";
    estadoLabel.textContent = "Estado de Solicitud";
    const estadoSelect = document.createElement("select");
    estadoSelect.className = "form-control";
    estadoSelect.appendChild(new Option("— Todos los estados —", ""));
    for (const [key, val] of Object.entries(ESTADOS_SOLICITUD)) {
      estadoSelect.appendChild(new Option(val.label, key));
    }
    estadoSelect.value = state.filters.estado;
    estadoGroup.append(estadoLabel, estadoSelect);

    // Filtro por Tipo de Trámite
    const tramiteGroup = document.createElement("div");
    tramiteGroup.className = "form-group";
    const tramiteLabel = document.createElement("label");
    tramiteLabel.className = "form-label";
    tramiteLabel.textContent = "Tipo de Trámite";
    const tramiteSelect = document.createElement("select");
    tramiteSelect.className = "form-control";
    tramiteSelect.appendChild(new Option("— Todos los trámites —", ""));
    for (const t of state.tiposTramiteCatalog) {
      tramiteSelect.appendChild(new Option(`${t.codigo} - ${t.nombre}`, String(t.id)));
    }
    tramiteSelect.value = state.filters.tipoTramiteId;
    tramiteGroup.append(tramiteLabel, tramiteSelect);

    // Filtro por Fecha Desde
    const fDesdeGroup = createFilterField("Fecha Desde", "date", "Fecha inicial");
    const fDesdeInput = fDesdeGroup.input;
    fDesdeInput.value = state.filters.fechaInicial;

    // Filtro por Fecha Hasta
    const fHastaGroup = createFilterField("Fecha Hasta", "date", "Fecha final");
    const fHastaInput = fHastaGroup.input;
    fHastaInput.value = state.filters.fechaFinal;

    // Botones de filtro
    const actionsRow = document.createElement("div");
    actionsRow.className = "module-filter-actions";
    actionsRow.style.gridColumn = "1 / -1";
    actionsRow.style.display = "flex";
    actionsRow.style.gap = "0.75rem";
    actionsRow.style.justifyContent = "flex-end";

    const clearBtn = document.createElement("button");
    clearBtn.type = "button";
    clearBtn.className = "btn btn-secondary";
    clearBtn.textContent = "Limpiar Filtros";

    const filterBtn = document.createElement("button");
    filterBtn.type = "submit";
    filterBtn.className = "btn btn-primary";
    filterBtn.textContent = "Filtrar Expedientes";

    actionsRow.append(clearBtn, filterBtn);

    filterForm.append(
      numExpGroup.group,
      estadoGroup,
      tramiteGroup,
      fDesdeGroup.group,
      fHastaGroup.group,
      actionsRow
    );
    toolbarCard.appendChild(filterForm);
    tabContentMount.appendChild(toolbarCard);

    // Contenedores de tabla y paginación
    const tableContainer = document.createElement("div");
    tableContainer.id = "solicitudes-table-container";
    const paginationContainer = document.createElement("div");
    paginationContainer.id = "solicitudes-pagination-container";

    tabContentMount.append(tableContainer, paginationContainer);

    filterForm.addEventListener("submit", (e) => {
      e.preventDefault();
      state.filters.numeroExpediente = numExpInput.value.trim();
      state.filters.estado = estadoSelect.value;
      state.filters.tipoTramiteId = tramiteSelect.value;
      state.filters.fechaInicial = fDesdeInput.value;
      state.filters.fechaFinal = fHastaInput.value;
      state.page = 0;
      loadSolicitudesPage(tableContainer, paginationContainer);
    });

    clearBtn.addEventListener("click", () => {
      filterForm.reset();
      state.filters = {
        numeroExpediente: "",
        estado: "",
        tipoTramiteId: "",
        fechaInicial: "",
        fechaFinal: ""
      };
      state.page = 0;
      loadSolicitudesPage(tableContainer, paginationContainer);
    });

    // Cargar catálogo de tipos de trámite si está vacío
    if (state.tiposTramiteCatalog.length === 0) {
      try {
        const pageTramites = await listarTiposTramite({ activo: true, size: 50 });
        state.tiposTramiteCatalog = Array.isArray(pageTramites?.contenido)
          ? pageTramites.contenido
          : [];
        clearChildren(tramiteSelect);
        tramiteSelect.appendChild(new Option("— Todos los trámites —", ""));
        for (const t of state.tiposTramiteCatalog) {
          tramiteSelect.appendChild(new Option(`${t.codigo} - ${t.nombre}`, String(t.id)));
        }
      } catch (_e) {
        state.tiposTramiteCatalog = [];
      }
    }

    await loadSolicitudesPage(tableContainer, paginationContainer);
  }

  function createFilterField(labelText, type, placeholder) {
    const group = document.createElement("div");
    group.className = "form-group";
    const label = document.createElement("label");
    label.className = "form-label";
    label.textContent = labelText;
    const input = document.createElement("input");
    input.type = type;
    input.className = "form-control";
    input.placeholder = placeholder || "";
    group.append(label, input);
    return { group, input };
  }

  async function loadSolicitudesPage(tableMount, pagMount) {
    state.loading = true;
    renderDataTable(tableMount, {
      loading: true,
      caption: "Bandeja de expedientes municipales"
    });

    try {
      const pageData = await listarSolicitudes({
        numeroExpediente: state.filters.numeroExpediente,
        estado: state.filters.estado,
        tipoTramiteId: state.filters.tipoTramiteId,
        fechaInicial: state.filters.fechaInicial,
        fechaFinal: state.filters.fechaFinal,
        page: state.page,
        size: state.size,
        sort: "fechaPresentacion,desc"
      });

      state.loading = false;
      const rows = Array.isArray(pageData?.contenido) ? pageData.contenido : [];

      renderDataTable(tableMount, {
        caption: "Bandeja de expedientes municipales",
        columns: [
          {
            label: "N° Expediente",
            render: (row) => {
              const code = row.numeroExpediente || `Borrador #${row.id}`;
              const link = document.createElement("button");
              link.type = "button";
              link.className = "btn-link";
              link.style.fontWeight = "700";
              link.textContent = code;
              link.title = `Ver detalle del expediente ${code}`;
              link.addEventListener("click", () => switchTab("detalle", row.id));
              return link;
            }
          },
          {
            label: "Trámite",
            render: (row) => {
              const t = row.tipoTramite;
              return t ? `${t.codigo} - ${t.nombre}` : (row.tipoTramiteNombre || "—");
            }
          },
          {
            label: "Administrado",
            render: (row) => {
              const c = row.ciudadano;
              if (c) {
                return `${c.nombres} ${c.apellidos} (${c.tipoDocumento} ${c.numeroDocumento})`;
              }
              return row.ciudadanoNombreCompleto || "—";
            }
          },
          {
            label: "Establecimiento",
            render: (row) => {
              const e = row.establecimiento;
              return e ? `${e.nombreComercial} (RUC: ${e.ruc || "Sin RUC"})` : (row.establecimientoNombreComercial || "—");
            }
          },
          {
            label: "Estado",
            render: (row) => createSolicitudEstadoBadge(row.estado)
          },
          {
            label: "Fecha Ingreso",
            render: (row) => formatDateTimePE(row.fechaPresentacion)
          },
          {
            label: "Acciones",
            render: (row) => {
              const acts = [
                {
                  label: "Ver Detalle",
                  title: `Consultar expediente ${row.numeroExpediente || row.id}`,
                  onClick: () => switchTab("detalle", row.id)
                }
              ];

              if (row.estado === "BORRADOR") {
                acts.push({
                  label: "Editar / Presentar",
                  title: `Continuar registro del borrador #${row.id}`,
                  variant: "primary",
                  onClick: () => switchTab("nueva", row.id)
                });
              }

              return createTableActions(acts);
            }
          }
        ],
        rows,
        emptyTitle: "No se encontraron expedientes",
        emptySubtitle: "No hay solicitudes que coincidan con los filtros aplicados o no se registran expedientes en el sistema."
      });

      renderPagination(pagMount, {
        currentPage: pageData?.pagina || 0,
        totalPages: pageData?.totalPaginas || 0,
        totalElements: pageData?.totalElementos || 0,
        pageSize: state.size,
        onPageChange: (newPage) => {
          state.page = newPage;
          loadSolicitudesPage(tableMount, pagMount);
        }
      });
    } catch (err) {
      state.loading = false;
      renderDataTable(tableMount, {
        error: err.message || "Error al consultar expedientes en el servidor.",
        onRetry: () => loadSolicitudesPage(tableMount, pagMount)
      });
    }
  }

  // Montar vista inicial según parámetro subview
  switchTab(activeTab, activeSolicitudId);
}
