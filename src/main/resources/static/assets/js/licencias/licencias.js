/**
 * Módulo Principal de Gestión de Licencias de Funcionamiento (PROMPT 13 - Secciones 5 y 6)
 */
import { ROLES } from "../config.js";
import { hasRole } from "../auth.js";
import { clearChildren, showToast } from "../ui.js";
import { renderDataTable, createTableActions, formatDateTimePE } from "../components/data-table.js";
import { renderPagination } from "../components/pagination.js";
import { listarLicencias, obtenerLicenciaPorCodigo, descargarCertificadoLicencia } from "./licencias-api.js";
import { renderEmitirLicenciaView } from "./licencia-emision.js";
import { renderLicenciaDetalle } from "./licencia-detalle.js";

function formatDate(isoDate) {
  if (!isoDate) return "—";
  const parts = String(isoDate).split("-");
  if (parts.length === 3) return `${parts[2]}/${parts[1]}/${parts[0]}`;
  return String(isoDate);
}

function createLicenciaBadge(estado) {
  const clean = String(estado || "VIGENTE").toUpperCase();
  const span = document.createElement("span");
  span.className = `badge ${
    clean === "VIGENTE"
      ? "badge-success"
      : clean === "SUSPENDIDA"
        ? "badge-warning"
        : "badge-danger"
  }`;
  span.textContent = clean;
  return span;
}

/**
 * Renderiza el módulo completo de Gestión de Licencias de Funcionamiento en el contenedor principal.
 */
export async function renderLicenciasModule(
  currentUser,
  container,
  { subview = "bandeja", initialId = null, preselectedSolicitudId = null } = {}
) {
  if (!container) return;
  clearChildren(container);

  const canEmit =
    hasRole(currentUser, ROLES.ADMINISTRADOR) ||
    hasRole(currentUser, ROLES.FUNCIONARIO) ||
    hasRole(currentUser, ROLES.SUPERVISOR);

  let activeTab = subview || "bandeja";
  let activeLicenciaId = initialId ? Number(initialId) : null;

  const state = {
    filters: {
      codigo: "",
      estado: "",
      fechaDesde: "",
      fechaHasta: ""
    },
    page: 0,
    size: 15,
    loading: false
  };

  const wrapper = document.createElement("div");
  wrapper.className = "management-module";

  // Barra de subnavegación
  const subnav = document.createElement("nav");
  subnav.className = "licencias-subnav";
  subnav.setAttribute("aria-label", "Pestañas del módulo de licencias");

  const tabBandejaBtn = createTabBtn("bandeja", "Padrón de Licencias", true);
  subnav.appendChild(tabBandejaBtn);

  let tabEmitirBtn = null;
  if (canEmit) {
    tabEmitirBtn = createTabBtn("emitir", "+ Emitir Licencia", false);
    subnav.appendChild(tabEmitirBtn);
  }

  wrapper.appendChild(subnav);

  // Contenedor dinámico de vista
  const tabContentMount = document.createElement("div");
  tabContentMount.className = "licencias-tab-content";
  wrapper.appendChild(tabContentMount);
  container.appendChild(wrapper);

  function createTabBtn(key, label, isActive) {
    const btn = document.createElement("button");
    btn.type = "button";
    btn.className = `licencias-subnav-btn ${isActive ? "is-active" : ""}`;
    btn.textContent = label;
    btn.dataset.tabKey = key;
    btn.addEventListener("click", () => switchTab(key));
    return btn;
  }

  function updateSubnavActiveState(selectedKey) {
    subnav.querySelectorAll(".licencias-subnav-btn").forEach((btn) => {
      btn.classList.toggle("is-active", btn.dataset.tabKey === selectedKey);
    });
  }

  function switchTab(targetKey, targetId = null) {
    activeTab = targetKey;
    activeLicenciaId = targetId ? Number(targetId) : null;
    updateSubnavActiveState(targetKey);
    clearChildren(tabContentMount);

    if (targetKey === "emitir" && canEmit) {
      renderEmitirLicenciaView({
        container: tabContentMount,
        preselectedSolicitudId,
        onLicenciaEmitida: (lic) => switchTab("detalle", lic.id),
        onCancel: () => switchTab("bandeja")
      });
      return;
    }

    if (targetKey === "detalle" && activeLicenciaId) {
      renderLicenciaDetalle({
        container: tabContentMount,
        licenciaId: activeLicenciaId,
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
    toolbarCard.setAttribute("aria-label", "Filtros y búsqueda de licencias de funcionamiento");

    const topRow = document.createElement("div");
    topRow.className = "module-toolbar-top";

    const titleGroup = document.createElement("div");
    titleGroup.className = "module-toolbar-title-group";

    const heading = document.createElement("h2");
    heading.className = "module-toolbar-heading";
    heading.textContent = "Padrón Oficial de Licencias de Funcionamiento";

    const caption = document.createElement("p");
    caption.className = "module-toolbar-caption";
    caption.textContent = "Consulta, seguimiento de vigencia y certificados digitales autorizados.";
    titleGroup.append(heading, caption);

    // Consulta rápida por código de licencia
    const quickForm = document.createElement("form");
    quickForm.style.display = "flex";
    quickForm.style.gap = "0.5rem";
    quickForm.style.alignItems = "center";

    const quickInput = document.createElement("input");
    quickInput.type = "search";
    quickInput.className = "form-control";
    quickInput.style.minWidth = "220px";
    quickInput.placeholder = "Ej.: LIC-2026-00001";
    quickInput.setAttribute("aria-label", "Buscar por código de licencia");

    const quickBtn = document.createElement("button");
    quickBtn.type = "submit";
    quickBtn.className = "btn btn-secondary";
    quickBtn.textContent = "Buscar Código";

    quickForm.append(quickInput, quickBtn);

    quickForm.addEventListener("submit", async (e) => {
      e.preventDefault();
      const code = quickInput.value.trim();
      if (!code) {
        showToast({ type: "warning", message: "Ingrese el código de licencia a consultar." });
        return;
      }
      try {
        quickBtn.disabled = true;
        const res = await obtenerLicenciaPorCodigo(code);
        if (res && res.id) {
          switchTab("detalle", res.id);
        } else {
          showToast({ type: "error", message: `No se encontró la licencia "${code}".` });
        }
      } catch (err) {
        showToast({ type: "error", message: err.message || `No se encontró la licencia "${code}".` });
      } finally {
        quickBtn.disabled = false;
      }
    });

    topRow.append(titleGroup, quickForm);
    toolbarCard.appendChild(topRow);

    // Formulario de filtros
    const filterForm = document.createElement("form");
    filterForm.className = "module-filter-grid";

    // Filtro por Código
    const codeGroup = document.createElement("div");
    codeGroup.className = "form-group";
    const codeLabel = document.createElement("label");
    codeLabel.className = "form-label";
    codeLabel.textContent = "Código Licencia";
    const codeIn = document.createElement("input");
    codeIn.type = "text";
    codeIn.className = "form-control";
    codeIn.placeholder = "LIC-...";
    codeIn.value = state.filters.codigo;
    codeGroup.append(codeLabel, codeIn);

    // Filtro por Estado
    const estGroup = document.createElement("div");
    estGroup.className = "form-group";
    const estLabel = document.createElement("label");
    estLabel.className = "form-label";
    estLabel.textContent = "Estado de Licencia";
    const estSelect = document.createElement("select");
    estSelect.className = "form-control";
    estSelect.appendChild(new Option("— Todos los estados —", ""));
    for (const est of ["VIGENTE", "VENCIDA", "SUSPENDIDA", "REVOCADA", "ANULADA"]) {
      estSelect.appendChild(new Option(est, est));
    }
    estSelect.value = state.filters.estado;
    estGroup.append(estLabel, estSelect);

    // Fecha Desde
    const fDesdeGroup = document.createElement("div");
    fDesdeGroup.className = "form-group";
    const fDesdeLabel = document.createElement("label");
    fDesdeLabel.className = "form-label";
    fDesdeLabel.textContent = "Fecha Emisión Desde";
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
    fHastaLabel.textContent = "Fecha Emisión Hasta";
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
    filterBtn.textContent = "Filtrar Licencias";

    actionsRow.append(clearBtn, filterBtn);

    filterForm.append(codeGroup, estGroup, fDesdeGroup, fHastaGroup, actionsRow);
    toolbarCard.appendChild(filterForm);
    tabContentMount.appendChild(toolbarCard);

    // Contenedores de tabla y paginación
    const tableMount = document.createElement("div");
    const pagMount = document.createElement("div");
    tabContentMount.append(tableMount, pagMount);

    filterForm.addEventListener("submit", (e) => {
      e.preventDefault();
      state.filters.codigo = codeIn.value.trim();
      state.filters.estado = estSelect.value;
      state.filters.fechaDesde = fDesdeIn.value;
      state.filters.fechaHasta = fHastaIn.value;
      state.page = 0;
      loadLicenciasPage(tableMount, pagMount);
    });

    clearBtn.addEventListener("click", () => {
      filterForm.reset();
      state.filters = { codigo: "", estado: "", fechaDesde: "", fechaHasta: "" };
      state.page = 0;
      loadLicenciasPage(tableMount, pagMount);
    });

    await loadLicenciasPage(tableMount, pagMount);
  }

  async function loadLicenciasPage(tableMount, pagMount) {
    state.loading = true;
    renderDataTable(tableMount, { loading: true, caption: "Padrón de licencias de funcionamiento" });

    try {
      const pageData = await listarLicencias({
        codigo: state.filters.codigo,
        estado: state.filters.estado,
        fechaDesde: state.filters.fechaDesde,
        fechaHasta: state.filters.fechaHasta,
        page: state.page,
        size: state.size,
        sort: "id,desc"
      });

      state.loading = false;
      const rows = Array.isArray(pageData?.contenido) ? pageData.contenido : [];

      renderDataTable(tableMount, {
        caption: "Padrón de licencias de funcionamiento",
        columns: [
          {
            label: "Código Licencia",
            render: (row) => {
              const btn = document.createElement("button");
              btn.type = "button";
              btn.className = "btn-link";
              btn.style.fontWeight = "700";
              btn.textContent = row.codigoLicencia;
              btn.addEventListener("click", () => switchTab("detalle", row.id));
              return btn;
            }
          },
          {
            label: "N° Expediente",
            render: (row) => row.numeroExpediente || `Solicitud #${row.solicitudId}`
          },
          {
            label: "Titular",
            render: (row) => row.nombreTitular || "—"
          },
          {
            label: "Establecimiento",
            render: (row) => {
              const rucTxt = row.ruc ? ` (${row.ruc})` : "";
              return `${row.nombreComercial || "—"}${rucTxt}`;
            }
          },
          {
            label: "Giro Comercial",
            render: (row) => row.giroNegocio || "—"
          },
          {
            label: "Fecha Emisión",
            render: (row) => formatDate(row.fechaEmision)
          },
          {
            label: "Estado",
            render: (row) => createLicenciaBadge(row.estado)
          },
          {
            label: "Acciones",
            render: (row) => {
              const acts = [
                {
                  label: "Ver Detalle",
                  title: `Consultar licencia ${row.codigoLicencia}`,
                  onClick: () => switchTab("detalle", row.id)
                },
                {
                  label: "PDF",
                  title: `Descargar Certificado PDF ${row.codigoLicencia}`,
                  onClick: async () => {
                    try {
                      await descargarCertificadoLicencia(row.id, row.codigoLicencia);
                      showToast("Certificado PDF descargado.", "success");
                    } catch (err) {
                      showToast(err.message || "Error al descargar PDF.", "error");
                    }
                  }
                }
              ];
              return createTableActions(acts);
            }
          }
        ],
        rows,
        emptyTitle: "No se encontraron licencias",
        emptySubtitle: "Ajuste los filtros de búsqueda o emita una nueva licencia desde una solicitud aprobada."
      });

      renderPagination(pagMount, {
        currentPage: pageData?.pagina || 0,
        totalPages: pageData?.totalPaginas || 0,
        totalElements: pageData?.totalElementos || 0,
        pageSize: state.size,
        onPageChange: (newPage) => {
          state.page = newPage;
          loadLicenciasPage(tableMount, pagMount);
        }
      });
    } catch (err) {
      state.loading = false;
      renderDataTable(tableMount, {
        error: err.message || "Error al consultar licencias en el servidor.",
        onRetry: () => loadLicenciasPage(tableMount, pagMount)
      });
    }
  }

  switchTab(activeTab, activeLicenciaId);
}
