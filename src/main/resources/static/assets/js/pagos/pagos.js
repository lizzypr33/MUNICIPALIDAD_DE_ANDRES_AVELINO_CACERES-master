/**
 * Módulo Principal de Gestión de Pagos Municipales (PROMPT 13 - Secciones 22 y 28)
 */
import { ROLES } from "../config.js";
import { hasRole } from "../auth.js";
import { clearChildren, showToast } from "../ui.js";
import { renderDataTable, createTableActions, formatDateTimePE } from "../components/data-table.js";
import { renderPagination } from "../components/pagination.js";
import { listarPagos, obtenerResumenPagos, descargarConstanciaPago } from "./pagos-api.js";
import { renderRegistrarPagoView } from "./pago-form.js";
import { renderPagoDetalle } from "./pago-detalle.js";

function formatCurrency(amount, currency = "PEN") {
  const num = Number(amount || 0);
  try {
    return new Intl.NumberFormat("es-PE", {
      style: "currency",
      currency: currency === "USD" ? "USD" : "PEN"
    }).format(num);
  } catch (_e) {
    return `S/ ${num.toFixed(2)}`;
  }
}

function createEstadoBadge(estado) {
  const clean = String(estado || "PENDIENTE_VERIFICACION").toUpperCase();
  const span = document.createElement("span");
  let cls = "badge-neutral";
  let label = clean;

  if (clean === "VERIFICADO" || clean === "PAGADO") {
    cls = "badge-success";
    label = "VERIFICADO";
  } else if (clean === "PENDIENTE_VERIFICACION" || clean === "PENDIENTE") {
    cls = "badge-warning";
    label = "PENDIENTE";
  } else if (clean === "RECHAZADO") {
    cls = "badge-danger";
    label = "RECHAZADO";
  } else if (clean === "ANULADO") {
    cls = "badge-neutral";
    label = "ANULADO";
  }

  span.className = `badge ${cls}`;
  span.textContent = label;
  return span;
}

/**
 * Renderiza el módulo completo de Gestión de Pagos en el contenedor principal.
 */
export async function renderPagosModule(
  currentUser,
  container,
  { subview = "bandeja", initialId = null, preselectedSolicitudId = null } = {}
) {
  if (!container) return;
  clearChildren(container);

  let activeTab = subview || "bandeja";
  let activePagoId = initialId ? Number(initialId) : null;

  const state = {
    filters: {
      codigoOperacion: "",
      solicitudId: "",
      estado: "",
      concepto: "",
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
  subnav.className = "pagos-subnav";
  subnav.setAttribute("aria-label", "Pestañas del módulo de pagos");

  const tabBandejaBtn = createTabBtn("bandeja", "Bandeja de Pagos", true);
  const tabRegBtn = createTabBtn("registrar", "+ Registrar Pago", false);
  subnav.append(tabBandejaBtn, tabRegBtn);

  wrapper.appendChild(subnav);

  // Contenedor dinámico de vista
  const tabContentMount = document.createElement("div");
  tabContentMount.className = "pagos-tab-content";
  wrapper.appendChild(tabContentMount);
  container.appendChild(wrapper);

  function createTabBtn(key, label, isActive) {
    const btn = document.createElement("button");
    btn.type = "button";
    btn.className = `pagos-subnav-btn ${isActive ? "is-active" : ""}`;
    btn.textContent = label;
    btn.dataset.tabKey = key;
    btn.addEventListener("click", () => switchTab(key));
    return btn;
  }

  function updateSubnavActiveState(selectedKey) {
    subnav.querySelectorAll(".pagos-subnav-btn").forEach((btn) => {
      btn.classList.toggle("is-active", btn.dataset.tabKey === selectedKey);
    });
  }

  function switchTab(targetKey, targetId = null) {
    activeTab = targetKey;
    activePagoId = targetId ? Number(targetId) : null;
    updateSubnavActiveState(targetKey);
    clearChildren(tabContentMount);

    if (targetKey === "registrar") {
      renderRegistrarPagoView({
        container: tabContentMount,
        preselectedSolicitudId,
        onPagoRegistrado: (pago) => switchTab("detalle", pago.id),
        onCancel: () => switchTab("bandeja")
      });
      return;
    }

    if (targetKey === "detalle" && activePagoId) {
      renderPagoDetalle({
        container: tabContentMount,
        pagoId: activePagoId,
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

    // 1. Resumen de Recaudación (ResumenPagosResponse)
    const summaryMount = document.createElement("section");
    summaryMount.className = "pagos-summary-section";
    summaryMount.setAttribute("aria-label", "Resumen de recaudación de pagos");
    tabContentMount.appendChild(summaryMount);
    loadResumenCards(summaryMount);

    // 2. Toolbar y Filtros
    const toolbarCard = document.createElement("section");
    toolbarCard.className = "module-toolbar-card";
    toolbarCard.setAttribute("aria-label", "Filtros y búsqueda de pagos");

    const topRow = document.createElement("div");
    topRow.className = "module-toolbar-top";

    const titleGroup = document.createElement("div");
    titleGroup.className = "module-toolbar-title-group";

    const heading = document.createElement("h2");
    heading.className = "module-toolbar-heading";
    heading.textContent = "Control Financiero de Tasas y Pagos Municipales";

    const caption = document.createElement("p");
    caption.className = "module-toolbar-caption";
    caption.textContent = "Consulta de depósitos, verificación administrativa y emisión de constancias oficiales.";
    titleGroup.append(heading, caption);
    topRow.appendChild(titleGroup);
    toolbarCard.appendChild(topRow);

    // Formulario de filtros
    const filterForm = document.createElement("form");
    filterForm.className = "module-filter-grid";

    // Código de operación
    const codGroup = document.createElement("div");
    codGroup.className = "form-group";
    const codLabel = document.createElement("label");
    codLabel.className = "form-label";
    codLabel.textContent = "N° de Operación";
    const codIn = document.createElement("input");
    codIn.type = "text";
    codIn.className = "form-control";
    codIn.placeholder = "BCP-... o BN-...";
    codIn.value = state.filters.codigoOperacion;
    codGroup.append(codLabel, codIn);

    // Estado
    const estGroup = document.createElement("div");
    estGroup.className = "form-group";
    const estLabel = document.createElement("label");
    estLabel.className = "form-label";
    estLabel.textContent = "Estado de Verificación";
    const estSelect = document.createElement("select");
    estSelect.className = "form-control";
    estSelect.appendChild(new Option("— Todos los estados —", ""));
    for (const st of ["PENDIENTE_VERIFICACION", "VERIFICADO", "RECHAZADO", "ANULADO"]) {
      estSelect.appendChild(new Option(st === "PENDIENTE_VERIFICACION" ? "PENDIENTE DE VERIFICACIÓN" : st, st));
    }
    estSelect.value = state.filters.estado;
    estGroup.append(estLabel, estSelect);

    // Fecha Desde
    const fDesdeGroup = document.createElement("div");
    fDesdeGroup.className = "form-group";
    const fDesdeLabel = document.createElement("label");
    fDesdeLabel.className = "form-label";
    fDesdeLabel.textContent = "Fecha Pago Desde";
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
    fHastaLabel.textContent = "Fecha Pago Hasta";
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
    filterBtn.textContent = "Filtrar Pagos";

    actionsRow.append(clearBtn, filterBtn);

    filterForm.append(codGroup, estGroup, fDesdeGroup, fHastaGroup, actionsRow);
    toolbarCard.appendChild(filterForm);
    tabContentMount.appendChild(toolbarCard);

    // Contenedores de tabla y paginación
    const tableMount = document.createElement("div");
    const pagMount = document.createElement("div");
    tabContentMount.append(tableMount, pagMount);

    filterForm.addEventListener("submit", (e) => {
      e.preventDefault();
      state.filters.codigoOperacion = codIn.value.trim();
      state.filters.estado = estSelect.value;
      state.filters.fechaDesde = fDesdeIn.value;
      state.filters.fechaHasta = fHastaIn.value;
      state.page = 0;
      loadResumenCards(summaryMount);
      loadPagosPage(tableMount, pagMount);
    });

    clearBtn.addEventListener("click", () => {
      filterForm.reset();
      state.filters = { codigoOperacion: "", solicitudId: "", estado: "", concepto: "", fechaDesde: "", fechaHasta: "" };
      state.page = 0;
      loadResumenCards(summaryMount);
      loadPagosPage(tableMount, pagMount);
    });

    await loadPagosPage(tableMount, pagMount);
  }

  async function loadResumenCards(mount) {
    try {
      const res = await obtenerResumenPagos({
        fechaDesde: state.filters.fechaDesde,
        fechaHasta: state.filters.fechaHasta,
        estado: state.filters.estado
      });

      clearChildren(mount);

      const grid = document.createElement("div");
      grid.className = "pagos-summary-grid";

      const cardsData = [
        { label: "Monto Total Verificado", value: formatCurrency(res.montoTotalVerificado, res.moneda), isCurrency: true, type: "is-verified", hint: "Ingreso fiscal confirmado en cuentas municipales" },
        { label: "Pagos Verificados", value: String(res.totalPagosVerificados || 0), type: "is-verified", hint: "Comprobantes confirmados administrativamente" },
        { label: "Pendientes de Verificación", value: String(res.cantidadPagosPendientes || 0), type: "is-pending", hint: "Declaraciones registradas por verificar" },
        { label: "Pagos Rechazados", value: String(res.cantidadPagosRechazados || 0), type: "is-rejected", hint: "Comprobantes inconsistentes no computados" },
        { label: "Total Registrados", value: String(res.cantidadTotalPagos || 0), type: "", hint: "Total de transacciones procesadas" }
      ];

      for (const c of cardsData) {
        const card = document.createElement("div");
        card.className = `pagos-summary-card ${c.type}`.trim();

        const lbl = document.createElement("span");
        lbl.className = "pagos-summary-label";
        lbl.textContent = c.label;

        const val = document.createElement("span");
        val.className = `pagos-summary-value ${c.isCurrency ? "is-currency" : ""}`.trim();
        val.textContent = c.value;

        const hint = document.createElement("span");
        hint.className = "pagos-summary-hint";
        hint.textContent = c.hint;

        card.append(lbl, val, hint);
        grid.appendChild(card);
      }

      mount.appendChild(grid);
    } catch (_e) {
      clearChildren(mount);
    }
  }

  async function loadPagosPage(tableMount, pagMount) {
    state.loading = true;
    renderDataTable(tableMount, { loading: true, caption: "Padrón de pagos municipales" });

    try {
      const pageData = await listarPagos({
        codigoOperacion: state.filters.codigoOperacion,
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
        caption: "Padrón de pagos municipales",
        columns: [
          {
            label: "N° Operación",
            render: (row) => {
              const btn = document.createElement("button");
              btn.type = "button";
              btn.className = "btn-link";
              btn.style.fontWeight = "700";
              btn.textContent = row.codigoOperacion;
              btn.addEventListener("click", () => switchTab("detalle", row.id));
              return btn;
            }
          },
          {
            label: "Expediente",
            render: (row) => row.numeroExpediente || `Solicitud #${row.solicitudId}`
          },
          {
            label: "Titular",
            render: (row) => row.nombreCiudadano || "—"
          },
          {
            label: "Concepto",
            render: (row) => row.concepto || "—"
          },
          {
            label: "Monto",
            render: (row) => {
              const span = document.createElement("span");
              span.style.fontWeight = "700";
              span.textContent = formatCurrency(row.monto, row.moneda);
              return span;
            }
          },
          {
            label: "Medio de Pago",
            render: (row) => row.medioPago || "—"
          },
          {
            label: "Fecha Pago",
            render: (row) => formatDateTimePE(row.fechaPago)
          },
          {
            label: "Estado",
            render: (row) => createEstadoBadge(row.estado)
          },
          {
            label: "Acciones",
            render: (row) => {
              const acts = [
                {
                  label: "Ver Detalle",
                  title: `Consultar pago ${row.codigoOperacion}`,
                  onClick: () => switchTab("detalle", row.id)
                },
                {
                  label: "Constancia",
                  title: `Descargar constancia de pago ${row.codigoOperacion}`,
                  onClick: async () => {
                    try {
                      await descargarConstanciaPago(row.id, row.codigoOperacion);
                      showToast("Constancia de pago descargada.", "success");
                    } catch (err) {
                      showToast(err.message || "Error al descargar constancia.", "error");
                    }
                  }
                }
              ];
              return createTableActions(acts);
            }
          }
        ],
        rows,
        emptyTitle: "No se encontraron pagos",
        emptySubtitle: "Ajuste los criterios de búsqueda o registre una nueva declaración de pago."
      });

      renderPagination(pagMount, {
        currentPage: pageData?.pagina || 0,
        totalPages: pageData?.totalPaginas || 0,
        totalElements: pageData?.totalElementos || 0,
        pageSize: state.size,
        onPageChange: (newPage) => {
          state.page = newPage;
          loadPagosPage(tableMount, pagMount);
        }
      });
    } catch (err) {
      state.loading = false;
      renderDataTable(tableMount, {
        error: err.message || "Error al consultar pagos en el servidor.",
        onRetry: () => loadPagosPage(tableMount, pagMount)
      });
    }
  }

  switchTab(activeTab, activePagoId);
}
