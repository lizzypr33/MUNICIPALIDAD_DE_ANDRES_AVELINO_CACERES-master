/**
 * Módulo Principal de Gestión de Inspecciones Municipales (PROMPT 13 - Secciones 14 y 15)
 */
import { ROLES } from "../config.js";
import { hasRole } from "../auth.js";
import { clearChildren, showToast } from "../ui.js";
import { renderDataTable, createTableActions, formatDateTimePE } from "../components/data-table.js";
import { renderPagination } from "../components/pagination.js";
import { listarInspecciones, iniciarInspeccion } from "./inspecciones-api.js";
import { renderProgramarInspeccionView } from "./inspeccion-form.js";
import { renderInspeccionDetalle } from "./inspeccion-detalle.js";

function createEstadoBadge(estado) {
  const clean = String(estado || "PROGRAMADA").toUpperCase();
  const span = document.createElement("span");
  let cls = "badge-neutral";
  if (clean === "PROGRAMADA" || clean === "REPROGRAMADA") cls = "badge-info";
  else if (clean === "EN_PROCESO") cls = "badge-warning";
  else if (clean === "FINALIZADA" || clean === "REALIZADA") cls = "badge-success";
  else if (clean === "CANCELADA") cls = "badge-danger";

  span.className = `badge ${cls}`;
  span.textContent = clean;
  return span;
}

function createResultadoBadge(resultado) {
  const span = document.createElement("span");
  if (!resultado) {
    span.className = "badge badge-neutral";
    span.textContent = "PENDIENTE";
    return span;
  }
  const clean = String(resultado).toUpperCase();
  let cls = "badge--resultado-favorable";
  if (clean.includes("OBSERVACIONES")) cls = "badge--resultado-observaciones";
  else if (clean.includes("DESFAVORABLE")) cls = "badge--resultado-desfavorable";

  span.className = `badge ${cls}`;
  span.textContent = clean;
  return span;
}

/**
 * Renderiza el módulo completo de Gestión de Inspecciones en el contenedor principal.
 */
export async function renderInspeccionesModule(
  currentUser,
  container,
  { subview = "bandeja", initialId = null, preselectedSolicitudId = null } = {}
) {
  if (!container) return;
  clearChildren(container);

  const canProgram =
    hasRole(currentUser, ROLES.ADMINISTRADOR) ||
    hasRole(currentUser, ROLES.FUNCIONARIO) ||
    hasRole(currentUser, ROLES.SUPERVISOR);

  let activeTab = subview || "bandeja";
  let activeInspeccionId = initialId ? Number(initialId) : null;

  const state = {
    filters: {
      solicitudId: "",
      estado: "",
      resultado: "",
      fechaDesde: "",
      fechaHasta: ""
    },
    page: 0,
    size: 15,
    loading: false
  };

  const wrapper = document.createElement("div");
  wrapper.className = "management-module";

  // Subnavegación
  const subnav = document.createElement("nav");
  subnav.className = "inspecciones-subnav";
  subnav.setAttribute("aria-label", "Pestañas del módulo de inspecciones");

  const tabBandejaBtn = createTabBtn("bandeja", "Bandeja de Inspecciones", true);
  subnav.appendChild(tabBandejaBtn);

  let tabProgBtn = null;
  if (canProgram) {
    tabProgBtn = createTabBtn("programar", "+ Programar Inspección", false);
    subnav.appendChild(tabProgBtn);
  }

  wrapper.appendChild(subnav);

  // Contenedor dinámico de vista
  const tabContentMount = document.createElement("div");
  tabContentMount.className = "inspecciones-tab-content";
  wrapper.appendChild(tabContentMount);
  container.appendChild(wrapper);

  function createTabBtn(key, label, isActive) {
    const btn = document.createElement("button");
    btn.type = "button";
    btn.className = `inspecciones-subnav-btn ${isActive ? "is-active" : ""}`;
    btn.textContent = label;
    btn.dataset.tabKey = key;
    btn.addEventListener("click", () => switchTab(key));
    return btn;
  }

  function updateSubnavActiveState(selectedKey) {
    subnav.querySelectorAll(".inspecciones-subnav-btn").forEach((btn) => {
      btn.classList.toggle("is-active", btn.dataset.tabKey === selectedKey);
    });
  }

  function switchTab(targetKey, targetId = null) {
    activeTab = targetKey;
    activeInspeccionId = targetId ? Number(targetId) : null;
    updateSubnavActiveState(targetKey);
    clearChildren(tabContentMount);

    if (targetKey === "programar" && canProgram) {
      renderProgramarInspeccionView({
        container: tabContentMount,
        preselectedSolicitudId,
        onProgramada: (insp) => switchTab("detalle", insp.id),
        onCancel: () => switchTab("bandeja")
      });
      return;
    }

    if (targetKey === "detalle" && activeInspeccionId) {
      renderInspeccionDetalle({
        container: tabContentMount,
        inspeccionId: activeInspeccionId,
        currentUser,
        onBack: () => switchTab("bandeja"),
        onNavigateToSolicitud: (solId) => {
          window.location.hash = `#solicitudes?subview=detalle&id=${encodeURIComponent(solId)}`;
        }
      });
      return;
    }

    renderBandejaView();
  }

  // Vista de bandeja
  async function renderBandejaView() {
    clearChildren(tabContentMount);

    const toolbarCard = document.createElement("section");
    toolbarCard.className = "module-toolbar-card";
    toolbarCard.setAttribute("aria-label", "Filtros y búsqueda de inspecciones");

    const topRow = document.createElement("div");
    topRow.className = "module-toolbar-top";

    const titleGroup = document.createElement("div");
    titleGroup.className = "module-toolbar-title-group";

    const heading = document.createElement("h2");
    heading.className = "module-toolbar-heading";
    heading.textContent = "Control y Seguimiento de Inspecciones Técnicas (ITSE)";

    const caption = document.createElement("p");
    caption.className = "module-toolbar-caption";
    caption.textContent = "Diligencias de seguridad en edificaciones, aforos y condiciones técnicas.";
    titleGroup.append(heading, caption);
    topRow.appendChild(titleGroup);
    toolbarCard.appendChild(topRow);

    // Formulario de filtros
    const filterForm = document.createElement("form");
    filterForm.className = "module-filter-grid";

    // Filtro por Estado
    const estGroup = document.createElement("div");
    estGroup.className = "form-group";
    const estLabel = document.createElement("label");
    estLabel.className = "form-label";
    estLabel.textContent = "Estado de Inspección";
    const estSelect = document.createElement("select");
    estSelect.className = "form-control";
    estSelect.appendChild(new Option("— Todos los estados —", ""));
    for (const st of ["PROGRAMADA", "EN_PROCESO", "FINALIZADA", "CANCELADA", "REPROGRAMADA"]) {
      estSelect.appendChild(new Option(st, st));
    }
    estSelect.value = state.filters.estado;
    estGroup.append(estLabel, estSelect);

    // Filtro por Resultado
    const resGroup = document.createElement("div");
    resGroup.className = "form-group";
    const resLabel = document.createElement("label");
    resLabel.className = "form-label";
    resLabel.textContent = "Calificación / Resultado";
    const resSelect = document.createElement("select");
    resSelect.className = "form-control";
    resSelect.appendChild(new Option("— Todas las calificaciones —", ""));
    for (const res of ["FAVORABLE", "CON_OBSERVACIONES", "FAVORABLE_CON_OBSERVACIONES", "DESFAVORABLE"]) {
      resSelect.appendChild(new Option(res, res));
    }
    resSelect.value = state.filters.resultado;
    resGroup.append(resLabel, resSelect);

    // Fecha Desde
    const fDesdeGroup = document.createElement("div");
    fDesdeGroup.className = "form-group";
    const fDesdeLabel = document.createElement("label");
    fDesdeLabel.className = "form-label";
    fDesdeLabel.textContent = "Fecha Programada Desde";
    const fDesdeIn = document.createElement("input");
    fDesdeIn.type = "date";
    fDesdeIn.className = "form-control";
    fDesdeIn.value = state.filters.fechaDesde;
    fDesdeGroup.append(fDesdeLabel, fDesdeIn);

    // Fecha Hasta
    const fHastaGroup = document.createElement("div");
    fHastaGroup.className = "form-group";
    const fHastaLabel = document.createElement("label");
    fHastaLabel.className = "form-label";
    fHastaLabel.textContent = "Fecha Programada Hasta";
    const fHastaIn = document.createElement("input");
    fHastaIn.type = "date";
    fHastaIn.className = "form-control";
    fHastaIn.value = state.filters.fechaHasta;
    fHastaGroup.append(fHastaLabel, fHastaIn);

    // Acciones de filtro
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
    filterBtn.textContent = "Filtrar Inspecciones";

    actionsRow.append(clearBtn, filterBtn);

    filterForm.append(estGroup, resGroup, fDesdeGroup, fHastaGroup, actionsRow);
    toolbarCard.appendChild(filterForm);
    tabContentMount.appendChild(toolbarCard);

    // Contenedores de tabla y paginación
    const tableMount = document.createElement("div");
    const pagMount = document.createElement("div");
    tabContentMount.append(tableMount, pagMount);

    filterForm.addEventListener("submit", (e) => {
      e.preventDefault();
      state.filters.estado = estSelect.value;
      state.filters.resultado = resSelect.value;
      state.filters.fechaDesde = fDesdeIn.value;
      state.filters.fechaHasta = fHastaIn.value;
      state.page = 0;
      loadInspeccionesPage(tableMount, pagMount);
    });

    clearBtn.addEventListener("click", () => {
      filterForm.reset();
      state.filters = { solicitudId: "", estado: "", resultado: "", fechaDesde: "", fechaHasta: "" };
      state.page = 0;
      loadInspeccionesPage(tableMount, pagMount);
    });

    await loadInspeccionesPage(tableMount, pagMount);
  }

  async function loadInspeccionesPage(tableMount, pagMount) {
    state.loading = true;
    renderDataTable(tableMount, { loading: true, caption: "Bandeja de inspecciones técnicas" });

    try {
      const pageData = await listarInspecciones({
        solicitudId: state.filters.solicitudId,
        estado: state.filters.estado,
        resultado: state.filters.resultado,
        fechaDesde: state.filters.fechaDesde,
        fechaHasta: state.filters.fechaHasta,
        page: state.page,
        size: state.size,
        sort: "id,desc"
      });

      state.loading = false;
      const rows = Array.isArray(pageData?.contenido) ? pageData.contenido : [];

      renderDataTable(tableMount, {
        caption: "Bandeja de inspecciones técnicas",
        columns: [
          {
            label: "ID",
            render: (row) => {
              const btn = document.createElement("button");
              btn.type = "button";
              btn.className = "btn-link";
              btn.style.fontWeight = "700";
              btn.textContent = `#${row.id}`;
              btn.addEventListener("click", () => switchTab("detalle", row.id));
              return btn;
            }
          },
          {
            label: "N° Expediente",
            render: (row) => row.numeroExpediente || `Solicitud #${row.solicitudId}`
          },
          {
            label: "Establecimiento",
            render: (row) => row.nombreComercialEstablecimiento || "—"
          },
          {
            label: "Inspector",
            render: (row) => row.nombreInspector || "Pendiente"
          },
          {
            label: "Fecha Programada",
            render: (row) => formatDateTimePE(row.fechaProgramada)
          },
          {
            label: "Estado",
            render: (row) => createEstadoBadge(row.estado)
          },
          {
            label: "Resultado",
            render: (row) => createResultadoBadge(row.resultado)
          },
          {
            label: "Acciones",
            render: (row) => {
              const acts = [
                {
                  label: "Ver Detalle",
                  title: `Consultar inspección #${row.id}`,
                  onClick: () => switchTab("detalle", row.id)
                }
              ];

              if (
                canProgram &&
                (row.estado === "PROGRAMADA" || row.estado === "REPROGRAMADA")
              ) {
                acts.push({
                  label: "Iniciar",
                  variant: "primary",
                  title: `Iniciar inspección #${row.id}`,
                  onClick: async () => {
                    try {
                      await iniciarInspeccion(row.id);
                      showToast(`Inspección #${row.id} iniciada (EN PROCESO).`, "success");
                      loadInspeccionesPage(tableMount, pagMount);
                    } catch (err) {
                      showToast(err.message || "Error al iniciar.", "error");
                    }
                  }
                });
              }

              return createTableActions(acts);
            }
          }
        ],
        rows,
        emptyTitle: "No se encontraron inspecciones",
        emptySubtitle: "Ajuste los criterios de búsqueda o programe una nueva inspección técnica."
      });

      renderPagination(pagMount, {
        currentPage: pageData?.pagina || 0,
        totalPages: pageData?.totalPaginas || 0,
        totalElements: pageData?.totalElementos || 0,
        pageSize: state.size,
        onPageChange: (newPage) => {
          state.page = newPage;
          loadInspeccionesPage(tableMount, pagMount);
        }
      });
    } catch (err) {
      state.loading = false;
      renderDataTable(tableMount, {
        error: err.message || "Error al consultar inspecciones en el servidor.",
        onRetry: () => loadInspeccionesPage(tableMount, pagMount)
      });
    }
  }

  switchTab(activeTab, activeInspeccionId);
}
