/**
 * Coordinador principal del módulo Dashboard Administrativo Municipal (PROMPT 10)
 * Gestiona filtros por período, botón Actualizar, estados de carga/error, permisos por rol
 * y consumo concurrente mediante Promise.allSettled.
 */
import { ROLES } from "../config.js";
import { hasPermission, hasRole } from "../auth.js";
import { navigateToSection } from "../router.js";
import {
  clearChildren,
  hideInlineAlert,
  setButtonLoading,
  showInlineAlert,
  showToast
} from "../ui.js";
import {
  getActividadReciente,
  getBackendHealth,
  getDashboardResumen,
  getInspeccionesEstadisticas,
  getSolicitudesEstadisticas
} from "./dashboard-api.js";
import {
  renderKpiCards,
  renderKpiCardsSkeleton,
  renderQuickActionsCard,
  renderSolicitudesSummaryCard
} from "./dashboard-cards.js";
import {
  renderSolicitudesPorEstadoChart,
  renderSolicitudesPorMesChart
} from "./dashboard-charts.js";
import {
  formatLocalDateTime,
  renderRecentActivityTable,
  renderSystemInfoCard
} from "./dashboard-activity.js";

let isFetchingDashboard = false;

/**
 * Renderiza el Dashboard completo dentro del contenedor principal (#main-content-area).
 */
export async function renderDashboardModule(user, container, onRefreshNotifications) {
  if (!container) return;
  clearChildren(container);

  const isStaff = hasRole(user, [ROLES.ADMINISTRADOR, ROLES.SUPERVISOR, ROLES.FUNCIONARIO]);

  // Si el usuario tiene únicamente rol CIUDADANO, mostrar panel ciudadano con sus accesos rápidos y estado del sistema
  if (!isStaff) {
    await renderCitizenDashboardView(user, container);
    return;
  }

  // 1. Barra de filtros por período y botón Actualizar (Secciones 6, 16 y 17)
  const toolbar = document.createElement("section");
  toolbar.className = "dashboard-toolbar";
  toolbar.setAttribute("aria-label", "Controles y filtros del dashboard");

  const filterForm = document.createElement("form");
  filterForm.className = "dashboard-filter-form";
  filterForm.noValidate = true;

  const fieldDesde = createDateFilterField("dash-fecha-desde", "Fecha inicial");
  const fieldHasta = createDateFilterField("dash-fecha-hasta", "Fecha final");

  const actionsDiv = document.createElement("div");
  actionsDiv.className = "dashboard-filter-actions";

  const applyBtn = document.createElement("button");
  applyBtn.type = "submit";
  applyBtn.className = "btn btn-primary btn-sm";
  applyBtn.textContent = "Aplicar filtros";

  const resetBtn = document.createElement("button");
  resetBtn.type = "button";
  resetBtn.className = "btn btn-secondary btn-sm";
  resetBtn.textContent = "Restablecer";

  const refreshBtn = document.createElement("button");
  refreshBtn.type = "button";
  refreshBtn.className = "btn btn-outline btn-sm";
  refreshBtn.textContent = "Actualizar";

  actionsDiv.append(applyBtn, resetBtn, refreshBtn);
  filterForm.append(fieldDesde.wrapper, fieldHasta.wrapper, actionsDiv);

  const metaDiv = document.createElement("div");
  metaDiv.className = "dashboard-toolbar-meta";

  const lastUpdatedEl = document.createElement("span");
  lastUpdatedEl.className = "dashboard-last-updated";
  lastUpdatedEl.textContent = "Última actualización: consultando...";

  const filterScopeNotice = document.createElement("span");
  filterScopeNotice.className = "dashboard-filter-notice";
  filterScopeNotice.hidden = true;

  metaDiv.append(lastUpdatedEl, filterScopeNotice);
  toolbar.append(filterForm, metaDiv);

  // Alerta general de errores de validación o red
  const errorBanner = document.createElement("div");
  errorBanner.hidden = true;

  // 2. Contenedor de las 6 tarjetas estadísticas (Sección 7)
  const kpiSection = document.createElement("section");
  kpiSection.className = "kpi-grid";
  kpiSection.setAttribute("aria-label", "Indicadores principales");

  // 3. Grilla de gráficos estadísticos (Secciones 9 y 10)
  const chartsGrid = document.createElement("div");
  chartsGrid.className = "dashboard-charts-grid";

  const lineChartCard = createDashboardCard(
    "Solicitudes por mes",
    "Evolución mensual de expedientes registrados"
  );
  const donutChartCard = createDashboardCard(
    "Solicitudes por estado",
    "Distribución porcentual según estado administrativo"
  );
  chartsGrid.append(lineChartCard.card, donutChartCard.card);

  // 4. Grilla intermedia: Resumen de solicitudes + Accesos rápidos (Secciones 11 y 13)
  const middleGrid = document.createElement("div");
  middleGrid.className = "dashboard-middle-grid";

  const verSolicitudesBtn = document.createElement("button");
  verSolicitudesBtn.type = "button";
  verSolicitudesBtn.className = "btn btn-outline btn-sm";
  verSolicitudesBtn.textContent = "Ver solicitudes";
  verSolicitudesBtn.addEventListener("click", () => navigateToSection("solicitudes"));

  const summaryCard = createDashboardCard(
    "Resumen de solicitudes",
    "Conteo operativo por etapa de evaluación",
    verSolicitudesBtn
  );

  const quickActionsCard = createDashboardCard(
    "Accesos rápidos",
    "Operaciones frecuentes habilitadas para su rol"
  );
  renderQuickActionsCard(quickActionsCard.body, user);

  middleGrid.append(summaryCard.card, quickActionsCard.card);

  // 5. Grilla inferior: Actividad reciente + Información del sistema (Secciones 12 y 15)
  const bottomGrid = document.createElement("div");
  bottomGrid.className = "dashboard-bottom-grid";

  let verAuditoriaBtn = null;
  if (hasPermission(user, "auditoria")) {
    verAuditoriaBtn = document.createElement("button");
    verAuditoriaBtn.type = "button";
    verAuditoriaBtn.className = "btn btn-outline btn-sm";
    verAuditoriaBtn.textContent = "Ver más";
    verAuditoriaBtn.addEventListener("click", () => navigateToSection("auditoria"));
  }

  const activityCard = createDashboardCard(
    "Actividad reciente",
    "Últimas operaciones registradas en el sistema",
    verAuditoriaBtn
  );

  const sysInfoCard = createDashboardCard(
    "Información del sistema",
    "Estado de disponibilidad y metadatos verificados"
  );

  bottomGrid.append(activityCard.card, sysInfoCard.card);

  container.append(toolbar, errorBanner, kpiSection, chartsGrid, middleGrid, bottomGrid);

  // Función que consulta todas las API con Promise.allSettled y actualiza el DOM
  const loadDashboardData = async (filtros = {}, showSuccessToast = false) => {
    if (isFetchingDashboard) return;
    isFetchingDashboard = true;
    hideInlineAlert(errorBanner);

    setButtonLoading(refreshBtn, true, "Actualizando...");
    renderKpiCardsSkeleton(kpiSection);

    const hasDateFilter = Boolean(filtros.fechaDesde || filtros.fechaHasta);
    if (hasDateFilter) {
      filterScopeNotice.textContent =
        "Nota: El rango de fechas filtra los gráficos y el resumen de solicitudes. Las tarjetas superiores muestran el acumulado global del padrón.";
      filterScopeNotice.hidden = false;
    } else {
      filterScopeNotice.hidden = true;
    }

    try {
      const [resumenRes, solEstRes, inspEstRes, actividadRes, healthRes] = await Promise.allSettled([
        getDashboardResumen(),
        getSolicitudesEstadisticas(filtros),
        getInspeccionesEstadisticas(filtros),
        getActividadReciente(8),
        getBackendHealth()
      ]);

      const now = new Date();
      lastUpdatedEl.textContent = `Última actualización: ${formatLocalDateTime(now)}`;

      const failures = [];

      const resumenData = resumenRes.status === "fulfilled" ? resumenRes.value : null;
      const solEstData = solEstRes.status === "fulfilled" ? solEstRes.value : null;
      const inspEstData = inspEstRes.status === "fulfilled" ? inspEstRes.value : null;
      const actividadData = actividadRes.status === "fulfilled" ? actividadRes.value : null;
      const healthData = healthRes.status === "fulfilled" ? healthRes.value : null;

      if (resumenRes.status === "rejected") {
        failures.push(resumenRes.reason?.message || "No se pudo cargar el resumen ejecutivo.");
      }
      if (solEstRes.status === "rejected") {
        failures.push(solEstRes.reason?.message || "No se pudieron cargar las estadísticas de solicitudes.");
      }
      if (actividadRes.status === "rejected") {
        failures.push(actividadRes.reason?.message || "No se pudo cargar la actividad reciente.");
      }

      if (resumenData) {
        renderKpiCards(kpiSection, resumenData, inspEstData);
      } else {
        clearChildren(kpiSection);
      }

      if (solEstData) {
        renderSolicitudesPorMesChart(lineChartCard.body, solEstData);
        renderSolicitudesPorEstadoChart(donutChartCard.body, solEstData);
        renderSolicitudesSummaryCard(summaryCard.body, solEstData, resumenData);
      } else {
        renderCardErrorState(lineChartCard.body, "No se pudieron obtener los datos mensuales de solicitudes.");
        renderCardErrorState(donutChartCard.body, "No se pudo obtener la distribución por estado.");
        renderCardErrorState(summaryCard.body, "No se pudo cargar el resumen de solicitudes.");
      }

      if (actividadData) {
        renderRecentActivityTable(activityCard.body, actividadData);
      } else {
        renderCardErrorState(activityCard.body, "No se pudo consultar la actividad reciente.");
      }

      renderSystemInfoCard(sysInfoCard.body, healthData, now);

      if (typeof onRefreshNotifications === "function") {
        onRefreshNotifications();
      }

      if (failures.length > 0) {
        showInlineAlert(errorBanner, failures[0], "error");
      } else if (showSuccessToast) {
        showToast({
          type: "success",
          title: "Dashboard actualizado",
          message: "Los indicadores y gráficos se actualizaron correctamente."
        });
      }
    } finally {
      isFetchingDashboard = false;
      setButtonLoading(refreshBtn, false);
    }
  };

  // Eventos de filtrado por rango de fechas (Sección 16)
  filterForm.addEventListener("submit", (e) => {
    e.preventDefault();
    const desde = fieldDesde.input.value;
    const hasta = fieldHasta.input.value;

    fieldDesde.input.removeAttribute("aria-invalid");
    fieldHasta.input.removeAttribute("aria-invalid");

    if (desde && hasta && desde > hasta) {
      fieldDesde.input.setAttribute("aria-invalid", "true");
      fieldHasta.input.setAttribute("aria-invalid", "true");
      showInlineAlert(
        errorBanner,
        "La fecha inicial no puede ser posterior a la fecha final.",
        "error"
      );
      return;
    }

    loadDashboardData({ fechaDesde: desde, fechaHasta: hasta }, true);
  });

  resetBtn.addEventListener("click", () => {
    fieldDesde.input.value = "";
    fieldHasta.input.value = "";
    fieldDesde.input.removeAttribute("aria-invalid");
    fieldHasta.input.removeAttribute("aria-invalid");
    hideInlineAlert(errorBanner);
    loadDashboardData({}, true);
  });

  refreshBtn.addEventListener("click", () => {
    const desde = fieldDesde.input.value;
    const hasta = fieldHasta.input.value;
    if (desde && hasta && desde > hasta) {
      showInlineAlert(
        errorBanner,
        "Corrija el rango de fechas antes de actualizar.",
        "error"
      );
      return;
    }
    loadDashboardData({ fechaDesde: desde, fechaHasta: hasta }, true);
  });

  await loadDashboardData({});
}

/**
 * Vista de panel para usuarios con rol exclusivo CIUDADANO (sin acceso a /api/dashboard/**).
 */
async function renderCitizenDashboardView(user, container) {
  const infoAlert = document.createElement("div");
  infoAlert.className = "alert alert-info";
  infoAlert.textContent =
    `Bienvenido(a), ${user.nombreCompleto}. Desde su panel ciudadano puede acceder a la gestión de sus trámites, establecimientos, pagos y notificaciones.`;

  const middleGrid = document.createElement("div");
  middleGrid.className = "dashboard-middle-grid";

  const quickCard = createDashboardCard(
    "Accesos rápidos del ciudadano",
    "Trámites y consultas habilitadas para su cuenta"
  );
  renderQuickActionsCard(quickCard.body, user);

  const sysCard = createDashboardCard(
    "Información del sistema",
    "Estado de disponibilidad de la plataforma municipal"
  );

  middleGrid.append(quickCard.card, sysCard.card);
  container.append(infoAlert, middleGrid);

  try {
    const health = await getBackendHealth();
    renderSystemInfoCard(sysCard.body, health, new Date());
  } catch (_err) {
    renderSystemInfoCard(sysCard.body, null, new Date());
  }
}

function createDateFilterField(id, labelText) {
  const wrapper = document.createElement("div");
  wrapper.className = "dashboard-filter-field";

  const label = document.createElement("label");
  label.className = "dashboard-filter-label";
  label.setAttribute("for", id);
  label.textContent = labelText;

  const input = document.createElement("input");
  input.type = "date";
  input.id = id;
  input.className = "dashboard-filter-input";

  wrapper.append(label, input);
  return { wrapper, input };
}

function createDashboardCard(titleText, subtitleText, headerActionEl = null) {
  const card = document.createElement("section");
  card.className = "card";

  const header = document.createElement("div");
  header.className = "card-header";

  const titleWrap = document.createElement("div");
  const title = document.createElement("h2");
  title.className = "card-title";
  title.textContent = titleText;

  const subtitle = document.createElement("p");
  subtitle.className = "card-subtitle";
  subtitle.textContent = subtitleText;

  titleWrap.append(title, subtitle);
  header.appendChild(titleWrap);

  if (headerActionEl) {
    header.appendChild(headerActionEl);
  }

  const body = document.createElement("div");
  body.className = "card-body";

  card.append(header, body);
  return { card, body };
}

function renderCardErrorState(container, message) {
  if (!container) return;
  clearChildren(container);
  const alert = document.createElement("div");
  alert.className = "alert alert-error";
  alert.setAttribute("role", "alert");
  alert.textContent = message;
  container.appendChild(alert);
}
