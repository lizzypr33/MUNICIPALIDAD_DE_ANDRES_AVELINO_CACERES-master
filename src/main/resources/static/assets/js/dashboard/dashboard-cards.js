/**
 * Renderizado de las 6 tarjetas estadísticas principales, resumen de solicitudes y accesos rápidos (PROMPT 10 - Secciones 7, 11 y 13)
 */
import { ROLES } from "../config.js";
import { hasRole } from "../auth.js";
import { navigateToSection } from "../router.js";
import { clearChildren, createIcon, showToast } from "../ui.js";

const numberFormatter = new Intl.NumberFormat("es-PE");
const currencyFormatter = new Intl.NumberFormat("es-PE", {
  style: "currency",
  currency: "PEN",
  minimumFractionDigits: 2,
  maximumFractionDigits: 2
});

/**
 * Formatea un monto numérico o cadena numérica como moneda en soles peruanos (S/ 0.00).
 */
export function formatCurrencyPEN(value) {
  const numeric = Number(value);
  if (!Number.isFinite(numeric)) {
    return "S/ 0.00";
  }
  return currencyFormatter.format(numeric).replace(/\u00A0/g, " ");
}

export function formatCount(value) {
  const numeric = Number(value);
  if (!Number.isFinite(numeric)) {
    return "0";
  }
  return numberFormatter.format(numeric);
}

/**
 * Muestra 6 tarjetas skeleton mientras se consultan las API del dashboard.
 */
export function renderKpiCardsSkeleton(container) {
  if (!container) return;
  clearChildren(container);
  for (let i = 0; i < 6; i++) {
    const sk = document.createElement("div");
    sk.className = "skeleton skeleton-kpi";
    sk.setAttribute("aria-hidden", "true");
    container.appendChild(sk);
  }
}

/**
 * Renderiza las 6 tarjetas estadísticas principales con datos reales provenientes del backend (Sección 7).
 */
export function renderKpiCards(container, resumen, estadisticasInspecciones) {
  if (!container) return;
  clearChildren(container);

  const totalCiudadanos = resumen?.ciudadanos?.total ?? 0;
  const totalEstablecimientos = resumen?.establecimientos?.total ?? 0;
  const totalSolicitudes = resumen?.solicitudes?.total ?? 0;
  const pendientesSolicitudes = resumen?.solicitudes?.pendientes ?? 0;
  const totalLicencias = resumen?.licencias?.total ?? 0;
  const licenciasVigentes = resumen?.licencias?.vigentes ?? 0;

  const inspProgramadas = resumen?.inspecciones?.programadas ?? 0;
  const inspFinalizadas = resumen?.inspecciones?.finalizadas ?? 0;
  const totalInspecciones = estadisticasInspecciones
    ? (estadisticasInspecciones.totalProgramadas ?? 0) +
      (estadisticasInspecciones.totalEnProceso ?? 0) +
      (estadisticasInspecciones.totalFinalizadas ?? 0) +
      (estadisticasInspecciones.totalCanceladas ?? 0)
    : inspProgramadas + inspFinalizadas;

  const montoVerificado = resumen?.pagos?.montoVerificadoPEN ?? 0;
  const pagosVerificados = resumen?.pagos?.verificados ?? 0;
  const pagosPendientes = resumen?.pagos?.pendientes ?? 0;

  const cards = [
    {
      title: "Ciudadanos",
      value: formatCount(totalCiudadanos),
      subtext: "Ciudadanos registrados en el padrón",
      icon: "users",
      colorClass: "kpi-blue"
    },
    {
      title: "Establecimientos",
      value: formatCount(totalEstablecimientos),
      subtext: "Locales comerciales registrados",
      icon: "store",
      colorClass: "kpi-green"
    },
    {
      title: "Solicitudes",
      value: formatCount(totalSolicitudes),
      subtext: `${formatCount(pendientesSolicitudes)} pendientes de evaluación`,
      icon: "folder",
      colorClass: "kpi-orange"
    },
    {
      title: "Licencias",
      value: formatCount(totalLicencias),
      subtext: `${formatCount(licenciasVigentes)} licencias vigentes`,
      icon: "certificate",
      colorClass: "kpi-purple"
    },
    {
      title: "Inspecciones",
      value: formatCount(totalInspecciones),
      subtext: `${formatCount(inspProgramadas)} programadas · ${formatCount(inspFinalizadas)} finalizadas`,
      icon: "clipboard",
      colorClass: "kpi-red-soft"
    },
    {
      title: "Pagos verificados",
      value: formatCurrencyPEN(montoVerificado),
      subtext: `${formatCount(pagosVerificados)} verificados · ${formatCount(pagosPendientes)} por verificar`,
      icon: "payment",
      colorClass: "kpi-emerald"
    }
  ];

  for (const item of cards) {
    const article = document.createElement("article");
    article.className = `kpi-card ${item.colorClass}`;

    const content = document.createElement("div");
    content.className = "kpi-content";

    const label = document.createElement("span");
    label.className = "kpi-label";
    label.textContent = item.title;

    const val = document.createElement("strong");
    val.className = "kpi-value";
    val.textContent = item.value;

    const sub = document.createElement("span");
    sub.className = "kpi-subtext";
    sub.textContent = item.subtext;

    content.append(label, val, sub);

    const iconBox = document.createElement("div");
    iconBox.className = "kpi-icon-box";
    iconBox.appendChild(createIcon(item.icon, "nav-icon"));

    article.append(content, iconBox);
    container.appendChild(article);
  }
}

/**
 * Renderiza la tarjeta "Resumen de solicitudes" con etiquetas de estado y enlace a Solicitudes (Sección 11).
 */
export function renderSolicitudesSummaryCard(container, estadisticasSolicitudes, resumenGlobal) {
  if (!container) return;
  clearChildren(container);

  const porEstado = estadisticasSolicitudes?.porEstado || {};
  const presentadas = porEstado.PRESENTADA ?? 0;
  const subsanadas = porEstado.SUBSANADA ?? 0;
  const enRevision = porEstado.EN_REVISION ?? 0;
  const observadas = porEstado.OBSERVADA ?? (resumenGlobal?.solicitudes?.observadas ?? 0);
  const aprobadas = porEstado.APROBADA ?? (resumenGlobal?.solicitudes?.aprobadas ?? 0);
  const rechazadas = porEstado.RECHAZADA ?? (resumenGlobal?.solicitudes?.rechazadas ?? 0);
  const total = estadisticasSolicitudes?.total ?? (resumenGlobal?.solicitudes?.total ?? 0);

  const pendientesRevision = estadisticasSolicitudes
    ? presentadas + subsanadas
    : (resumenGlobal?.solicitudes?.pendientes ?? 0);

  const rows = [
    { label: "Pendientes de revisión", badgeClass: "badge-warning", count: pendientesRevision },
    { label: "En revisión", badgeClass: "badge-info", count: enRevision },
    { label: "Observadas", badgeClass: "badge-warning", count: observadas },
    { label: "Aprobadas", badgeClass: "badge-success", count: aprobadas },
    { label: "Rechazadas", badgeClass: "badge-danger", count: rechazadas },
    { label: "Total de solicitudes", badgeClass: "badge-neutral", count: total }
  ];

  const list = document.createElement("div");
  list.className = "summary-status-list";

  for (const r of rows) {
    const rowEl = document.createElement("div");
    rowEl.className = "summary-status-row";

    const badge = document.createElement("span");
    badge.className = `badge ${r.badgeClass}`;
    badge.textContent = r.label;

    const countEl = document.createElement("span");
    countEl.className = "summary-status-count";
    countEl.textContent = formatCount(r.count);

    rowEl.append(badge, countEl);
    list.appendChild(rowEl);
  }

  container.appendChild(list);
}

/**
 * Renderiza la tarjeta "Accesos rápidos" filtrando acciones según el rol autenticado (Sección 13).
 */
export function renderQuickActionsCard(container, user) {
  if (!container) return;
  clearChildren(container);

  const allActions = [
    {
      label: "Nueva solicitud",
      icon: "folder",
      targetSection: "solicitudes",
      promptInfo: "PROMPT 12",
      roles: [ROLES.ADMINISTRADOR, ROLES.FUNCIONARIO, ROLES.CIUDADANO]
    },
    {
      label: "Registrar ciudadano",
      icon: "users",
      targetSection: "ciudadanos",
      promptInfo: "PROMPT 11",
      roles: [ROLES.ADMINISTRADOR, ROLES.FUNCIONARIO]
    },
    {
      label: "Registrar establecimiento",
      icon: "store",
      targetSection: "establecimientos",
      promptInfo: "PROMPT 11",
      roles: [ROLES.ADMINISTRADOR, ROLES.FUNCIONARIO]
    },
    {
      label: "Programar inspección",
      icon: "clipboard",
      targetSection: "inspecciones",
      promptInfo: "PROMPT 13",
      roles: [ROLES.ADMINISTRADOR, ROLES.FUNCIONARIO, ROLES.SUPERVISOR]
    },
    {
      label: "Emitir licencia",
      icon: "certificate",
      targetSection: "licencias",
      promptInfo: "PROMPT 13",
      roles: [ROLES.ADMINISTRADOR, ROLES.FUNCIONARIO, ROLES.SUPERVISOR]
    },
    {
      label: "Registrar pago",
      icon: "payment",
      targetSection: "pagos",
      promptInfo: "PROMPT 14",
      roles: [ROLES.ADMINISTRADOR, ROLES.FUNCIONARIO, ROLES.CIUDADANO]
    },
    {
      label: "Generar reporte",
      icon: "chart",
      targetSection: "reportes",
      promptInfo: "PROMPT 15",
      roles: [ROLES.ADMINISTRADOR, ROLES.FUNCIONARIO, ROLES.SUPERVISOR]
    }
  ];

  const allowedActions = allActions.filter((action) => hasRole(user, action.roles));

  if (allowedActions.length === 0) {
    const empty = document.createElement("p");
    empty.className = "text-muted";
    empty.textContent = "No hay acciones rápidas asignadas a su perfil.";
    container.appendChild(empty);
    return;
  }

  const grid = document.createElement("div");
  grid.className = "quick-actions-grid";

  for (const action of allowedActions) {
    const btn = document.createElement("button");
    btn.type = "button";
    btn.className = "quick-action-btn";

    const iconWrap = document.createElement("span");
    iconWrap.className = "quick-action-icon";
    iconWrap.appendChild(createIcon(action.icon, "nav-icon"));

    const textWrap = document.createElement("span");
    textWrap.textContent = action.label;

    btn.append(iconWrap, textWrap);
    btn.addEventListener("click", () => {
      navigateToSection(action.targetSection);
      showToast({
        type: "info",
        title: action.label,
        message: `Navegando al módulo correspondiente (interfaz de formulario programada en ${action.promptInfo}).`
      });
    });

    grid.appendChild(btn);
  }

  container.appendChild(grid);
}
