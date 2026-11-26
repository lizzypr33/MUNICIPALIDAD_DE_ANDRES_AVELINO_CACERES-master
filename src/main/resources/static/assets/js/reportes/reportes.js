/**
 * Controlador principal del Módulo de Reportes Municipales (PROMPT 14 - Secciones 7 a 16)
 * Municipalidad Distrital de Andrés Avelino Cáceres Dorregaray
 */
import { clearChildren, showToast } from "../ui.js";
import { renderDataTable, formatDateTimePE } from "../components/data-table.js";
import { renderPagination } from "../components/pagination.js";
import {
  getReporteSolicitudes,
  getReporteLicencias,
  getReporteInspecciones,
  getReportePagos,
  getReporteEstablecimientos,
  getReporteCiudadanos
} from "./reportes-api.js";
import { renderReportFilters } from "./reportes-filtros.js";
import { renderDonutChart } from "./reportes-graficos.js";
import { exportarReportePdf, exportarReporteCsv } from "./reportes-exportacion.js";

const REPORT_TABS = [
  { key: "solicitudes", label: "Solicitudes y Expedientes", icon: "folder" },
  { key: "licencias", label: "Licencias Emitidas", icon: "certificate" },
  { key: "inspecciones", label: "Inspecciones ITSE", icon: "clipboard" },
  { key: "pagos", label: "Recaudación y Pagos", icon: "payment" },
  { key: "establecimientos", label: "Establecimientos", icon: "store" },
  { key: "ciudadanos", label: "Padrón de Ciudadanos", icon: "users" }
];

export async function renderReportesModule(user, container, { initialReporteType = "solicitudes" } = {}) {
  if (!container) return;
  clearChildren(container);

  const root = document.createElement("div");
  root.className = "reportes-module";

  // Estado reactivo del módulo
  let activeTab = initialReporteType || "solicitudes";
  let activeFilters = {};
  let currentPage = 0;
  let pageSize = 20;
  let currentReportData = null;

  // 1. Pestañas de navegación por tipo de reporte
  const navTabs = document.createElement("nav");
  navTabs.className = "reportes-nav-tabs";
  navTabs.setAttribute("aria-label", "Tipos de reporte municipal");

  REPORT_TABS.forEach(tab => {
    const btn = document.createElement("button");
    btn.type = "button";
    btn.className = `reportes-tab-btn ${tab.key === activeTab ? "is-active" : ""}`;
    btn.setAttribute("role", "tab");
    btn.setAttribute("aria-selected", tab.key === activeTab ? "true" : "false");
    btn.textContent = tab.label;

    btn.addEventListener("click", () => {
      if (activeTab === tab.key) return;
      activeTab = tab.key;
      activeFilters = {};
      currentPage = 0;
      updateActiveTabVisuals();
      mountFilters();
      loadReport();
    });

    navTabs.appendChild(btn);
  });

  root.appendChild(navTabs);

  function updateActiveTabVisuals() {
    const buttons = navTabs.querySelectorAll(".reportes-tab-btn");
    buttons.forEach((btn, idx) => {
      const match = REPORT_TABS[idx]?.key === activeTab;
      btn.classList.toggle("is-active", match);
      btn.setAttribute("aria-selected", match ? "true" : "false");
    });
  }

  // 2. Contenedor de filtros dinámicos
  const filtersMount = document.createElement("div");
  root.appendChild(filtersMount);

  // 3. Contenedor de tarjetas resumen (KPIs)
  const kpiMount = document.createElement("div");
  kpiMount.className = "reportes-kpi-grid";
  root.appendChild(kpiMount);

  // 4. Contenedor de gráficos estadísticos
  const chartMount = document.createElement("div");
  root.appendChild(chartMount);

  // 5. Contenedor de la tabla y paginación
  const tableCard = document.createElement("div");
  tableCard.className = "reportes-table-card";

  const tableHeader = document.createElement("div");
  tableHeader.className = "reportes-table-header";

  const tableTitle = document.createElement("h3");
  tableTitle.className = "reportes-chart-title";
  tableTitle.textContent = "Resultados del Reporte";

  const exportActionsGroup = document.createElement("div");
  exportActionsGroup.style.display = "flex";
  exportActionsGroup.style.gap = "0.5rem";

  const btnExportPdf = document.createElement("button");
  btnExportPdf.type = "button";
  btnExportPdf.className = "btn-export-pdf";
  btnExportPdf.innerHTML = `<span>Exportar PDF</span>`;

  const btnExportCsv = document.createElement("button");
  btnExportCsv.type = "button";
  btnExportCsv.className = "btn-export-csv";
  btnExportCsv.innerHTML = `<span>Exportar CSV</span>`;

  btnExportPdf.addEventListener("click", () => {
    exportarReportePdf(activeTab, activeFilters, btnExportPdf);
  });

  btnExportCsv.addEventListener("click", () => {
    exportarReporteCsv(activeTab, activeFilters, btnExportCsv);
  });

  exportActionsGroup.append(btnExportPdf, btnExportCsv);
  tableHeader.append(tableTitle, exportActionsGroup);
  tableCard.appendChild(tableHeader);

  const tableMount = document.createElement("div");
  tableCard.appendChild(tableMount);

  const paginationMount = document.createElement("div");
  tableCard.appendChild(paginationMount);

  root.appendChild(tableCard);
  container.appendChild(root);

  function mountFilters() {
    renderReportFilters(filtersMount, activeTab, {
      onFilter: (values) => {
        activeFilters = values;
        currentPage = 0;
        loadReport();
      },
      onReset: () => {
        activeFilters = {};
        currentPage = 0;
        loadReport();
      }
    });

    // En el backend no existen endpoints de PDF/CSV para ciudadanos
    const hasExport = activeTab !== "ciudadanos";
    btnExportPdf.style.display = hasExport ? "inline-flex" : "none";
    btnExportCsv.style.display = hasExport ? "inline-flex" : "none";
  }

  async function loadReport() {
    renderDataTable(tableMount, { loading: true });
    clearChildren(paginationMount);
    clearChildren(kpiMount);
    clearChildren(chartMount);

    try {
      const queryParams = {
        ...activeFilters,
        page: currentPage,
        size: pageSize
      };

      let response;
      if (activeTab === "solicitudes") {
        response = await getReporteSolicitudes(queryParams);
      } else if (activeTab === "licencias") {
        response = await getReporteLicencias(queryParams);
      } else if (activeTab === "inspecciones") {
        response = await getReporteInspecciones(queryParams);
      } else if (activeTab === "pagos") {
        response = await getReportePagos(queryParams);
      } else if (activeTab === "establecimientos") {
        response = await getReporteEstablecimientos(queryParams);
      } else if (activeTab === "ciudadanos") {
        response = await getReporteCiudadanos(queryParams);
      }

      currentReportData = response;
      renderReportView(response);
    } catch (err) {
      renderDataTable(tableMount, {
        error: err.message || "Error al consultar el reporte municipal.",
        onRetry: () => loadReport()
      });
      showToast({
        type: "error",
        title: "Error de consulta",
        message: err.message || "No se pudo obtener el reporte solicitado."
      });
    }
  }

  function renderReportView(reporte) {
    if (!reporte || !reporte.datos) {
      renderDataTable(tableMount, {
        emptyTitle: "Sin datos",
        emptySubtitle: "No se recibieron registros para el reporte seleccionado."
      });
      return;
    }

    const { datos, tituloReporte, filtrosAplicados } = reporte;
    const filas = Array.isArray(datos.contenido) ? datos.contenido : [];
    tableTitle.textContent = tituloReporte || "Resultados del Reporte";

    // 1. Renderizar Tarjetas Resumen (KPIs)
    renderKpiCards(filas, datos.totalElementos);

    // 2. Renderizar Gráfico Estadístico
    renderReportChart(filas);

    // 3. Renderizar Tabla según tipo
    const columns = getColumnsForTab(activeTab);
    renderDataTable(tableMount, {
      caption: tituloReporte,
      columns,
      rows: filas,
      emptyTitle: "No se encontraron registros",
      emptySubtitle: "Ajuste los filtros de búsqueda o consulte en otro rango de fechas."
    });

    // 4. Renderizar Paginación
    renderPagination(paginationMount, {
      pagina: datos.pagina,
      tamano: datos.tamanio,
      totalElementos: datos.totalElementos,
      totalPaginas: datos.totalPaginas,
      primera: datos.primera,
      ultima: datos.ultima,
      onPageChange: (newPage) => {
        currentPage = newPage;
        loadReport();
      },
      onSizeChange: (newSize) => {
        pageSize = newSize;
        currentPage = 0;
        loadReport();
      }
    });
  }

  function renderKpiCards(filas, totalElementos) {
    clearChildren(kpiMount);

    if (activeTab === "solicitudes") {
      const aprobadas = filas.filter(f => f.estado === "APROBADA").length;
      const observadas = filas.filter(f => f.estado === "OBSERVADA").length;
      const revision = filas.filter(f => f.estado === "EN_REVISION" || f.estado === "PRESENTADA").length;

      kpiMount.append(
        createKpiCard("TOTAL GENERAL", totalElementos, "Registros totales", "kpi-info"),
        createKpiCard("EN TRÁMITE", revision, "En página actual", "kpi-warning"),
        createKpiCard("OBSERVADAS", observadas, "En página actual", "kpi-danger"),
        createKpiCard("APROBADAS", aprobadas, "En página actual", "kpi-success")
      );
    } else if (activeTab === "licencias") {
      const vigentes = filas.filter(f => f.estado === "VIGENTE").length;
      const suspendidas = filas.filter(f => f.estado === "SUSPENDIDA").length;
      const revocadas = filas.filter(f => f.estado === "REVOCADA").length;

      kpiMount.append(
        createKpiCard("TOTAL LICENCIAS", totalElementos, "Padrón general", "kpi-info"),
        createKpiCard("VIGENTES", vigentes, "En página actual", "kpi-success"),
        createKpiCard("SUSPENDIDAS", suspendidas, "En página actual", "kpi-warning"),
        createKpiCard("REVOCADAS", revocadas, "En página actual", "kpi-danger")
      );
    } else if (activeTab === "inspecciones") {
      const favorables = filas.filter(f => f.resultado === "FAVORABLE").length;
      const noFavorables = filas.filter(f => f.resultado === "NO_FAVORABLE").length;
      const pendientes = filas.filter(f => f.estado === "PROGRAMADA" || f.estado === "EN_CURSO").length;

      kpiMount.append(
        createKpiCard("TOTAL INSPECCIONES", totalElementos, "Expedientes ITSE", "kpi-info"),
        createKpiCard("PENDIENTES", pendientes, "En página actual", "kpi-warning"),
        createKpiCard("FAVORABLES", favorables, "En página actual", "kpi-success"),
        createKpiCard("NO FAVORABLES", noFavorables, "En página actual", "kpi-danger")
      );
    } else if (activeTab === "pagos") {
      const verificados = filas.filter(f => f.estado === "VERIFICADO" || f.estado === "PAGADO");
      const sumaVerificada = verificados.reduce((acc, f) => acc + Number(f.monto || 0), 0);
      const pendientes = filas.filter(f => f.estado === "PENDIENTE" || f.estado === "PENDIENTE_VERIFICACION").length;

      kpiMount.append(
        createKpiCard("TOTAL OPERACIONES", totalElementos, "Padrón de caja", "kpi-info"),
        createKpiCard("RECAUDACIÓN VERIFICADA", `S/ ${sumaVerificada.toFixed(2)}`, "Página actual", "kpi-success"),
        createKpiCard("VERIFICADOS", verificados.length, "En página actual", "kpi-success"),
        createKpiCard("PENDIENTES DE CAJA", pendientes, "En página actual", "kpi-warning")
      );
    } else if (activeTab === "establecimientos") {
      const activos = filas.filter(f => String(f.estado).toLowerCase().includes("act") || f.estado === "ACTIVO").length;
      kpiMount.append(
        createKpiCard("ESTABLECIMIENTOS", totalElementos, "Locales registrados", "kpi-info"),
        createKpiCard("REGISTROS ACTIVOS", activos, "En página actual", "kpi-success"),
        createKpiCard("MUESTRA VISIBLE", filas.length, "En página actual", "kpi-info")
      );
    } else if (activeTab === "ciudadanos") {
      kpiMount.append(
        createKpiCard("TOTAL CIUDADANOS", totalElementos, "Padrón de administrados", "kpi-info"),
        createKpiCard("MUESTRA VISIBLE", filas.length, "En página actual", "kpi-info")
      );
    }
  }

  function createKpiCard(title, value, sub, variantClass = "") {
    const card = document.createElement("div");
    card.className = `reportes-kpi-card ${variantClass}`;

    const titleEl = document.createElement("span");
    titleEl.className = "reportes-kpi-title";
    titleEl.textContent = title;

    const valEl = document.createElement("span");
    valEl.className = "reportes-kpi-value";
    valEl.textContent = String(value);

    const subEl = document.createElement("span");
    subEl.className = "reportes-kpi-sub";
    subEl.textContent = sub;

    card.append(titleEl, valEl, subEl);
    return card;
  }

  function renderReportChart(filas) {
    clearChildren(chartMount);
    if (!filas || filas.length === 0) return;

    if (activeTab === "solicitudes") {
      const counts = {};
      filas.forEach(f => {
        const est = f.estado || "OTRO";
        counts[est] = (counts[est] || 0) + 1;
      });
      const data = Object.entries(counts).map(([label, value]) => ({ label, value }));
      renderDonutChart(chartMount, data, "Distribución de Solicitudes por Estado");
    } else if (activeTab === "licencias") {
      const counts = {};
      filas.forEach(f => {
        const est = f.estado || "OTRO";
        counts[est] = (counts[est] || 0) + 1;
      });
      const data = Object.entries(counts).map(([label, value]) => ({ label, value }));
      renderDonutChart(chartMount, data, "Distribución de Licencias por Estado de Vigencia");
    } else if (activeTab === "inspecciones") {
      const counts = {};
      filas.forEach(f => {
        const res = f.resultado || (f.estado === "PROGRAMADA" ? "PROGRAMADA" : "EN_CURSO");
        counts[res] = (counts[res] || 0) + 1;
      });
      const data = Object.entries(counts).map(([label, value]) => ({ label, value }));
      renderDonutChart(chartMount, data, "Distribución de Inspecciones Técnicas");
    } else if (activeTab === "pagos") {
      const counts = {};
      filas.forEach(f => {
        const est = f.estado || "OTRO";
        counts[est] = (counts[est] || 0) + 1;
      });
      const data = Object.entries(counts).map(([label, value]) => ({ label, value }));
      renderDonutChart(chartMount, data, "Distribución de Comprobantes de Pago");
    }
  }

  function getColumnsForTab(tabKey) {
    if (tabKey === "solicitudes") {
      return [
        { label: "ID", render: r => r.id },
        { label: "Expediente", render: r => r.numeroExpediente || "—" },
        { label: "Fecha Presentación", render: r => formatDateTimePE(r.fechaPresentacion) },
        { label: "Tipo Trámite", render: r => r.tipoTramite || "—" },
        { label: "Establecimiento", render: r => r.establecimiento || "—" },
        { label: "Responsable", render: r => r.responsable || "—" },
        {
          label: "Estado",
          render: r => {
            const badge = document.createElement("span");
            badge.className = "badge badge-info";
            badge.textContent = r.estado || "—";
            return badge;
          }
        }
      ];
    } else if (tabKey === "licencias") {
      return [
        { label: "ID", render: r => r.id },
        { label: "Código Licencia", render: r => r.codigoLicencia || "—" },
        { label: "Expediente", render: r => r.numeroExpediente || "—" },
        { label: "Establecimiento", render: r => r.establecimiento || "—" },
        { label: "Giro Comercial", render: r => r.giro || "—" },
        { label: "Fecha Emisión", render: r => r.fechaEmision || "—" },
        {
          label: "Estado",
          render: r => {
            const badge = document.createElement("span");
            badge.className = r.estado === "VIGENTE" ? "badge badge-success" : "badge badge-warning";
            badge.textContent = r.estado || "—";
            return badge;
          }
        }
      ];
    } else if (tabKey === "inspecciones") {
      return [
        { label: "ID", render: r => r.id },
        { label: "Expediente", render: r => r.numeroExpedienteSolicitud || "—" },
        { label: "Inspector Asignado", render: r => r.inspector || "—" },
        { label: "Fecha Programada", render: r => formatDateTimePE(r.fechaProgramada) },
        { label: "Fecha Realizada", render: r => formatDateTimePE(r.fechaRealizada) },
        {
          label: "Estado",
          render: r => {
            const badge = document.createElement("span");
            badge.className = "badge badge-info";
            badge.textContent = r.estado || "—";
            return badge;
          }
        },
        {
          label: "Resultado",
          render: r => {
            const badge = document.createElement("span");
            badge.className = r.resultado === "FAVORABLE" ? "badge badge-success" : "badge badge-neutral";
            badge.textContent = r.resultado || "PENDIENTE";
            return badge;
          }
        }
      ];
    } else if (tabKey === "pagos") {
      return [
        { label: "ID", render: r => r.id },
        { label: "Cód. Operación", render: r => r.codigoOperacion || "—" },
        { label: "Expediente", render: r => r.numeroExpediente || "—" },
        { label: "Concepto", render: r => r.concepto || "—" },
        {
          label: "Monto",
          render: r => `${r.moneda === "USD" ? "$" : "S/"} ${Number(r.monto || 0).toFixed(2)}`
        },
        { label: "Fecha Pago", render: r => formatDateTimePE(r.fecha) },
        {
          label: "Estado",
          render: r => {
            const badge = document.createElement("span");
            badge.className = (r.estado === "VERIFICADO" || r.estado === "PAGADO") ? "badge badge-success" : "badge badge-warning";
            badge.textContent = r.estado || "—";
            return badge;
          }
        }
      ];
    } else if (tabKey === "establecimientos") {
      return [
        { label: "ID", render: r => r.id },
        { label: "Nombre Comercial", render: r => r.nombreComercial || "—" },
        { label: "Razón Social", render: r => r.razonSocial || "—" },
        { label: "RUC", render: r => r.ruc || "—" },
        { label: "Dirección", render: r => r.direccion || "—" },
        { label: "Giro Comercial", render: r => r.giro || "—" },
        {
          label: "Estado",
          render: r => {
            const badge = document.createElement("span");
            badge.className = "badge badge-info";
            badge.textContent = r.estado || "ACTIVO";
            return badge;
          }
        }
      ];
    } else if (tabKey === "ciudadanos") {
      return [
        { label: "ID", render: r => r.id },
        { label: "Nombre Completo", render: r => r.nombreCompleto || "—" },
        { label: "Tipo Doc.", render: r => r.tipoDocumento || "—" },
        { label: "Nro. Documento", render: r => r.documentoEnmascarado || "—" },
        { label: "Fecha Registro", render: r => formatDateTimePE(r.fechaRegistro) },
        {
          label: "Estado",
          render: r => {
            const badge = document.createElement("span");
            badge.className = "badge badge-success";
            badge.textContent = r.estado || "ACTIVO";
            return badge;
          }
        }
      ];
    }
    return [];
  }

  // Inicializar filtros y cargar reporte
  mountFilters();
  await loadReport();
}
