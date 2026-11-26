/**
 * Construcción del layout institucional: Encabezado superior, Menú lateral por roles,
 * Panel de notificaciones y Vistas del contenedor administrativo (PROMPTS 09 y 10)
 */
import { APP_CONFIG, MENU_ITEMS, ROLE_LABELS } from "./config.js";
import { changePassword, hasPermission, logout } from "./auth.js";
import { navigateToSection } from "./router.js";
import {
  clearChildren,
  confirmDialog,
  createIcon,
  hideInlineAlert,
  setButtonLoading,
  showInlineAlert,
  showToast
} from "./ui.js";
import {
  getUnreadNotificationsCount,
  getUnreadNotificationsList,
  markAllNotificationsAsRead
} from "./dashboard/dashboard-api.js";
import { formatLocalDateTime } from "./dashboard/dashboard-activity.js";
import { renderDashboardModule } from "./dashboard/dashboard.js";
import { renderUsuariosModule } from "./usuarios/usuarios.js";
import { renderCiudadanosModule } from "./ciudadanos/ciudadanos.js";
import { renderRepresentantesModule } from "./representantes/representantes.js";
import { renderEstablecimientosModule } from "./establecimientos/establecimientos.js";
import { renderSolicitudesModule } from "./solicitudes/solicitudes.js";
import { renderLicenciasModule } from "./licencias/licencias.js";
import { renderInspeccionesModule } from "./inspecciones/inspecciones.js";
import { renderPagosModule } from "./pagos/pagos.js";
import { renderReportesModule } from "./reportes/reportes.js";
import { renderNotificacionesModule } from "./notificaciones/notificaciones.js";
import { renderAuditoriaModule } from "./auditoria/auditoria.js";

const MODULE_HEADERS = Object.freeze({
  dashboard: {
    title: "DASHBOARD ADMINISTRATIVO",
    subtitle: "Resumen general del Sistema de Gestión de Licencias Municipales."
  },
  usuarios: {
    title: "GESTIÓN DE USUARIOS",
    subtitle: "Administración de cuentas y permisos del sistema municipal."
  },
  ciudadanos: {
    title: "GESTIÓN DE CIUDADANOS",
    subtitle: "Registro y administración de ciudadanos vinculados a trámites municipales."
  },
  representantes: {
    title: "GESTIÓN DE REPRESENTANTES",
    subtitle: "Administración de representantes vinculados a trámites y establecimientos."
  },
  establecimientos: {
    title: "GESTIÓN DE ESTABLECIMIENTOS",
    subtitle: "Registro y administración de establecimientos comerciales del distrito."
  },
  solicitudes: {
    title: "GESTIÓN DE SOLICITUDES Y EXPEDIENTES",
    subtitle: "Registro guiado, evaluación técnica, requisitos documentales y seguimiento de expedientes."
  },
  licencias: {
    title: "GESTIÓN DE LICENCIAS DE FUNCIONAMIENTO",
    subtitle: "Emisión oficial, certificados digitales, trazabilidad de vigencia y código QR."
  },
  inspecciones: {
    title: "GESTIÓN DE INSPECCIONES MUNICIPALES",
    subtitle: "Programación, diligencias in situ y dictamen técnico de seguridad en edificaciones (ITSE)."
  },
  pagos: {
    title: "GESTIÓN DE PAGOS MUNICIPALES",
    subtitle: "Registro de declaraciones de pago, verificación administrativa y constancias oficiales."
  },
  reportes: {
    title: "REPORTES MUNICIPALES",
    subtitle: "Consulta, análisis y exportación de información administrativa."
  },
  notificaciones: {
    title: "CENTRO DE NOTIFICACIONES",
    subtitle: "Avisos y comunicaciones relacionadas con los trámites municipales."
  },
  auditoria: {
    title: "AUDITORÍA DEL SISTEMA",
    subtitle: "Consulta y seguimiento de operaciones administrativas registradas."
  }
});

function formatPrimaryRole(user) {
  if (!user || !Array.isArray(user.roles) || user.roles.length === 0) {
    return "Usuario Autenticado";
  }
  const primary = user.roles[0];
  return ROLE_LABELS[primary] || primary;
}

function getUserInitials(user) {
  if (!user) return "MU";
  const first = (user.nombres || user.username || "M").trim().charAt(0);
  const second = (user.apellidos || "").trim().charAt(0);
  return `${first}${second}`.toUpperCase() || "MU";
}

/**
 * Consulta de forma segura el contador real de notificaciones no leídas (GET /api/notificaciones/contador).
 */
export async function refreshUnreadNotificationsBadge() {
  const badgeEl = document.getElementById("header-notif-badge");
  const titleEl = document.getElementById("notif-panel-title");
  const markAllBtn = document.getElementById("notif-mark-all-btn");
  if (!badgeEl) return 0;

  try {
    const res = await getUnreadNotificationsCount();
    const count = res && typeof res.noLeidas === "number" ? res.noLeidas : 0;
    if (count > 0) {
      badgeEl.textContent = count > 99 ? "99+" : String(count);
      badgeEl.hidden = false;
      if (titleEl) titleEl.textContent = `Notificaciones (${count} sin leer)`;
      if (markAllBtn) markAllBtn.hidden = false;
    } else {
      badgeEl.hidden = true;
      if (titleEl) titleEl.textContent = "Notificaciones (0 sin leer)";
      if (markAllBtn) markAllBtn.hidden = true;
    }
    return count;
  } catch (_err) {
    badgeEl.hidden = true;
    return 0;
  }
}

/**
 * Carga y renderiza la lista breve de notificaciones no leídas en el panel del encabezado (Sección 14).
 */
async function loadAndRenderNotificationsPanel() {
  const listEl = document.getElementById("notif-panel-list");
  if (!listEl) return;
  clearChildren(listEl);

  const loadingItem = document.createElement("div");
  loadingItem.className = "notif-panel-item";
  loadingItem.textContent = "Consultando notificaciones...";
  listEl.appendChild(loadingItem);

  await refreshUnreadNotificationsBadge();

  try {
    const page = await getUnreadNotificationsList(5);
    clearChildren(listEl);

    const items = Array.isArray(page?.contenido) ? page.contenido : [];
    if (items.length === 0) {
      const empty = document.createElement("div");
      empty.className = "notif-panel-item";
      const msg = document.createElement("span");
      msg.className = "text-muted";
      msg.textContent = "No tiene notificaciones pendientes de lectura.";
      empty.appendChild(msg);
      listEl.appendChild(empty);
      return;
    }

    for (const notif of items) {
      const row = document.createElement("div");
      row.className = "notif-panel-item";

      const meta = document.createElement("div");
      meta.className = "notif-panel-meta";

      const typeBadge = document.createElement("span");
      typeBadge.className = "badge badge-info";
      typeBadge.textContent = notif.tipo || "AVISO";

      const dateSpan = document.createElement("span");
      dateSpan.textContent = formatLocalDateTime(notif.fechaCreacion);

      meta.append(typeBadge, dateSpan);

      const titleStrong = document.createElement("strong");
      titleStrong.style.fontSize = "0.82rem";
      titleStrong.textContent = notif.titulo || "Notificación municipal";

      const msgP = document.createElement("p");
      msgP.className = "notif-panel-msg";
      const rawMsg = notif.mensaje || "";
      msgP.textContent = rawMsg.length > 110 ? `${rawMsg.substring(0, 107)}...` : rawMsg;

      row.append(meta, titleStrong, msgP);
      listEl.appendChild(row);
    }
  } catch (_err) {
    clearChildren(listEl);
    const errItem = document.createElement("div");
    errItem.className = "notif-panel-item";
    errItem.textContent = "No se pudieron cargar las notificaciones en este momento.";
    listEl.appendChild(errItem);
  }
}

/**
 * Renderiza los datos reales del usuario en el encabezado superior.
 */
export function renderHeaderUser(user) {
  const avatarEl = document.getElementById("header-user-avatar");
  const fullnameEl = document.getElementById("header-user-fullname");
  const roleEl = document.getElementById("header-user-role");
  const menuNameEl = document.getElementById("profile-menu-fullname");
  const menuUsernameEl = document.getElementById("profile-menu-username");

  if (avatarEl) avatarEl.textContent = getUserInitials(user);
  if (fullnameEl) fullnameEl.textContent = user.nombreCompleto;
  if (roleEl) roleEl.textContent = formatPrimaryRole(user);
  if (menuNameEl) menuNameEl.textContent = user.nombreCompleto;
  if (menuUsernameEl) {
    menuUsernameEl.textContent = user.email ? `${user.username} · ${user.email}` : user.username;
  }
}

/**
 * Construye el menú lateral filtrando estrictamente según los roles del usuario autenticado.
 */
export function renderSidebarMenu(user, activeKey) {
  const navContainer = document.getElementById("sidebar-nav-list");
  if (!navContainer) return;
  clearChildren(navContainer);

  const visibleItems = MENU_ITEMS.filter((item) => hasPermission(user, item.key));
  let currentSection = null;

  for (const item of visibleItems) {
    if (item.section !== currentSection) {
      currentSection = item.section;
      const sectionLabel = document.createElement("div");
      sectionLabel.className = "nav-section-label";
      sectionLabel.textContent = currentSection;
      navContainer.appendChild(sectionLabel);
    }

    const button = document.createElement("button");
    button.type = "button";
    button.className = `nav-item-link${item.key === activeKey ? " is-active" : ""}`;
    button.dataset.routeKey = item.key;
    if (item.key === activeKey) {
      button.setAttribute("aria-current", "page");
    }

    const icon = createIcon(item.icon, "nav-icon");
    const label = document.createElement("span");
    label.textContent = item.label;

    button.append(icon, label);
    button.addEventListener("click", () => {
      navigateToSection(item.key);
      closeMobileSidebar();
    });

    navContainer.appendChild(button);
  }
}

export function updateSidebarActiveState(activeKey) {
  const links = document.querySelectorAll("#sidebar-nav-list .nav-item-link");
  links.forEach((btn) => {
    const isCurrent = btn.dataset.routeKey === activeKey;
    btn.classList.toggle("is-active", isCurrent);
    if (isCurrent) {
      btn.setAttribute("aria-current", "page");
    } else {
      btn.removeAttribute("aria-current");
    }
  });
}

export function closeMobileSidebar() {
  const shell = document.getElementById("app-shell");
  const backdrop = document.getElementById("sidebar-backdrop");
  if (shell) shell.classList.remove("is-mobile-sidebar-open");
  if (backdrop) backdrop.hidden = true;
}

/**
 * Configura eventos interactivos del encabezado, panel de notificaciones, menú lateral y cierre de sesión.
 */
export function bindLayoutInteractions() {
  const shell = document.getElementById("app-shell");
  const menuToggleBtn = document.getElementById("header-menu-toggle");
  const sidebarCloseBtn = document.getElementById("sidebar-close-btn");
  const backdrop = document.getElementById("sidebar-backdrop");

  const notifBtn = document.getElementById("header-notif-btn");
  const notifPanel = document.getElementById("header-notif-panel");
  const notifMarkAllBtn = document.getElementById("notif-mark-all-btn");
  const notifGoAllBtn = document.getElementById("notif-go-all-btn");

  const profileTrigger = document.getElementById("user-profile-trigger");
  const profileMenu = document.getElementById("profile-menu");
  const menuConfigBtn = document.getElementById("profile-go-config");
  const headerLogoutBtn = document.getElementById("header-logout-btn");
  const sidebarLogoutBtn = document.getElementById("sidebar-logout-btn");

  if (menuToggleBtn && shell) {
    menuToggleBtn.addEventListener("click", () => {
      const isMobile = window.matchMedia("(max-width: 1023px)").matches;
      if (isMobile) {
        const willOpen = !shell.classList.contains("is-mobile-sidebar-open");
        shell.classList.toggle("is-mobile-sidebar-open", willOpen);
        if (backdrop) backdrop.hidden = !willOpen;
      } else {
        shell.classList.toggle("is-sidebar-collapsed");
      }
    });
  }

  if (sidebarCloseBtn) sidebarCloseBtn.addEventListener("click", closeMobileSidebar);
  if (backdrop) backdrop.addEventListener("click", closeMobileSidebar);

  // Panel de notificaciones del encabezado (PROMPT 10 - Sección 14)
  if (notifBtn && notifPanel) {
    notifBtn.addEventListener("click", (e) => {
      e.stopPropagation();
      if (profileMenu && !profileMenu.hidden) {
        profileMenu.hidden = true;
        if (profileTrigger) profileTrigger.setAttribute("aria-expanded", "false");
      }
      const willOpen = notifPanel.hidden;
      notifPanel.hidden = !willOpen;
      notifBtn.setAttribute("aria-expanded", String(willOpen));
      if (willOpen) {
        loadAndRenderNotificationsPanel();
      }
    });
  }

  if (notifMarkAllBtn) {
    notifMarkAllBtn.addEventListener("click", async () => {
      try {
        await markAllNotificationsAsRead();
        await loadAndRenderNotificationsPanel();
        showToast({
          type: "success",
          title: "Notificaciones",
          message: "Todas las notificaciones fueron marcadas como leídas."
        });
      } catch (err) {
        showToast({
          type: "error",
          title: "Error",
          message: err.message || "No se pudieron actualizar las notificaciones."
        });
      }
    });
  }

  if (notifGoAllBtn && notifPanel) {
    notifGoAllBtn.addEventListener("click", () => {
      notifPanel.hidden = true;
      if (notifBtn) notifBtn.setAttribute("aria-expanded", "false");
      navigateToSection("notificaciones");
    });
  }

  if (profileTrigger && profileMenu) {
    profileTrigger.addEventListener("click", (e) => {
      e.stopPropagation();
      if (notifPanel && !notifPanel.hidden) {
        notifPanel.hidden = true;
        if (notifBtn) notifBtn.setAttribute("aria-expanded", "false");
      }
      const willShow = profileMenu.hidden;
      profileMenu.hidden = !willShow;
      profileTrigger.setAttribute("aria-expanded", String(willShow));
    });
  }

  document.addEventListener("click", (e) => {
    if (profileMenu && !profileMenu.hidden && !profileMenu.contains(e.target) && !profileTrigger.contains(e.target)) {
      profileMenu.hidden = true;
      profileTrigger.setAttribute("aria-expanded", "false");
    }
    if (notifPanel && !notifPanel.hidden && !notifPanel.contains(e.target) && !notifBtn.contains(e.target)) {
      notifPanel.hidden = true;
      notifBtn.setAttribute("aria-expanded", "false");
    }
  });

  if (menuConfigBtn && profileMenu) {
    menuConfigBtn.addEventListener("click", () => {
      profileMenu.hidden = true;
      if (profileTrigger) profileTrigger.setAttribute("aria-expanded", "false");
      navigateToSection("configuracion");
    });
  }

  const handleLogoutClick = async () => {
    if (profileMenu) profileMenu.hidden = true;
    const confirmed = await confirmDialog({
      title: "Cerrar sesión municipal",
      message: "¿Desea cerrar su sesión actual en el Sistema de Gestión de Licencias Municipales?",
      confirmText: "Sí, cerrar sesión",
      cancelText: "Cancelar",
      danger: true
    });
    if (!confirmed) return;

    await logout();
    window.location.replace(`${APP_CONFIG.pages.login}?logout=1`);
  };

  if (headerLogoutBtn) headerLogoutBtn.addEventListener("click", handleLogoutClick);
  if (sidebarLogoutBtn) sidebarLogoutBtn.addEventListener("click", handleLogoutClick);
}

/**
 * Renderiza la sección seleccionada dentro del contenedor principal (#main-content-area).
 */
export function renderSectionView(user, routeItem, isAllowed) {
  const breadcrumbCurrent = document.getElementById("breadcrumb-current");
  const pageTitle = document.getElementById("main-page-title");
  const pageSubtitle = document.getElementById("main-page-subtitle");
  const contentArea = document.getElementById("main-content-area");

  const customHeader = MODULE_HEADERS[routeItem.key];
  if (breadcrumbCurrent) breadcrumbCurrent.textContent = routeItem.label;
  if (pageTitle) {
    pageTitle.textContent = customHeader ? customHeader.title : routeItem.label;
  }
  if (pageSubtitle) {
    pageSubtitle.textContent = customHeader ? customHeader.subtitle : routeItem.description;
  }
  if (!contentArea) return;

  clearChildren(contentArea);

  if (!isAllowed) {
    const alert = document.createElement("div");
    alert.className = "alert alert-error";
    alert.setAttribute("role", "alert");
    alert.textContent = "Su perfil actual no cuenta con permisos para acceder a este módulo municipal.";
    contentArea.appendChild(alert);
    return;
  }

  if (routeItem.key === "dashboard") {
    renderDashboardModule(user, contentArea, refreshUnreadNotificationsBadge);
    return;
  }

  if (routeItem.key === "usuarios") {
    renderUsuariosModule(user, contentArea);
    return;
  }

  if (routeItem.key === "ciudadanos") {
    renderCiudadanosModule(user, contentArea);
    return;
  }

  if (routeItem.key === "representantes") {
    renderRepresentantesModule(user, contentArea);
    return;
  }

  if (routeItem.key === "establecimientos") {
    renderEstablecimientosModule(user, contentArea);
    return;
  }

  if (routeItem.key === "solicitudes") {
    const rawHash = window.location.hash ? window.location.hash.replace(/^#/, "").trim() : "";
    let subview = "bandeja";
    let initialId = null;
    if (rawHash.includes("/nueva") || rawHash.includes("subview=nueva")) {
      subview = "nueva";
    } else if (rawHash.includes("/tramites") || rawHash.includes("subview=tramites")) {
      subview = "tramites";
    } else if (rawHash.includes("/requisitos") || rawHash.includes("subview=requisitos")) {
      subview = "requisitos";
    } else if (rawHash.includes("/detalle") || rawHash.includes("subview=detalle")) {
      subview = "detalle";
      const idMatch = rawHash.match(/id=([0-9]+)/);
      if (idMatch) initialId = idMatch[1];
    }
    renderSolicitudesModule(user, contentArea, { subview, initialId });
    return;
  }

  if (routeItem.key === "licencias") {
    const rawHash = window.location.hash ? window.location.hash.replace(/^#/, "").trim() : "";
    let subview = "bandeja";
    let initialId = null;
    let preselectedSolicitudId = null;
    if (rawHash.includes("/emitir") || rawHash.includes("subview=emitir")) {
      subview = "emitir";
    } else if (rawHash.includes("/detalle") || rawHash.includes("subview=detalle")) {
      subview = "detalle";
      const idMatch = rawHash.match(/id=([0-9]+)/);
      if (idMatch) initialId = idMatch[1];
    }
    const solMatch = rawHash.match(/solicitudId=([0-9]+)/);
    if (solMatch) preselectedSolicitudId = solMatch[1];

    renderLicenciasModule(user, contentArea, { subview, initialId, preselectedSolicitudId });
    return;
  }

  if (routeItem.key === "inspecciones") {
    const rawHash = window.location.hash ? window.location.hash.replace(/^#/, "").trim() : "";
    let subview = "bandeja";
    let initialId = null;
    let preselectedSolicitudId = null;
    if (rawHash.includes("/programar") || rawHash.includes("subview=programar")) {
      subview = "programar";
    } else if (rawHash.includes("/detalle") || rawHash.includes("subview=detalle")) {
      subview = "detalle";
      const idMatch = rawHash.match(/id=([0-9]+)/);
      if (idMatch) initialId = idMatch[1];
    }
    const solMatch = rawHash.match(/solicitudId=([0-9]+)/);
    if (solMatch) preselectedSolicitudId = solMatch[1];

    renderInspeccionesModule(user, contentArea, { subview, initialId, preselectedSolicitudId });
    return;
  }

  if (routeItem.key === "pagos") {
    const rawHash = window.location.hash ? window.location.hash.replace(/^#/, "").trim() : "";
    let subview = "bandeja";
    let initialId = null;
    let preselectedSolicitudId = null;
    if (rawHash.includes("/registrar") || rawHash.includes("subview=registrar")) {
      subview = "registrar";
    } else if (rawHash.includes("/detalle") || rawHash.includes("subview=detalle")) {
      subview = "detalle";
      const idMatch = rawHash.match(/id=([0-9]+)/);
      if (idMatch) initialId = idMatch[1];
    }
    const solMatch = rawHash.match(/solicitudId=([0-9]+)/);
    if (solMatch) preselectedSolicitudId = solMatch[1];

    renderPagosModule(user, contentArea, { subview, initialId, preselectedSolicitudId });
    return;
  }

  if (routeItem.key === "reportes") {
    const rawHash = window.location.hash ? window.location.hash.replace(/^#/, "").trim() : "";
    let initialReporteType = "solicitudes";
    const tipoMatch = rawHash.match(/tipo=([a-zA-Z0-9_-]+)/);
    if (tipoMatch) initialReporteType = tipoMatch[1];
    renderReportesModule(user, contentArea, { initialReporteType });
    return;
  }

  if (routeItem.key === "notificaciones") {
    const rawHash = window.location.hash ? window.location.hash.replace(/^#/, "").trim() : "";
    let initialId = null;
    const idMatch = rawHash.match(/id=([0-9]+)/);
    if (idMatch) initialId = idMatch[1];
    renderNotificacionesModule(user, contentArea, { initialId });
    return;
  }

  if (routeItem.key === "auditoria") {
    const rawHash = window.location.hash ? window.location.hash.replace(/^#/, "").trim() : "";
    let initialId = null;
    const idMatch = rawHash.match(/id=([0-9]+)/);
    if (idMatch) initialId = idMatch[1];
    renderAuditoriaModule(user, contentArea, { initialId });
    return;
  }

  if (routeItem.key === "configuracion") {
    renderConfigurationView(user, contentArea);
    return;
  }

  renderModulePlaceholderView(user, routeItem, contentArea);
}

function renderConfigurationView(user, container) {
  const profileCard = document.createElement("section");
  profileCard.className = "card";

  const profileHeader = document.createElement("div");
  profileHeader.className = "card-header";
  const profileTitle = document.createElement("h2");
  profileTitle.className = "card-title";
  profileTitle.textContent = "Información del Usuario Autenticado";
  profileHeader.appendChild(profileTitle);

  const profileBody = document.createElement("div");
  profileBody.className = "card-body";

  const rows = [
    ["Nombre completo", user.nombreCompleto],
    ["Usuario", user.username],
    ["Correo electrónico", user.email || "No registrado"],
    ["Roles asignados", user.roles.join(", ") || "Ninguno"],
    ["Estado de cuenta", user.activo ? "ACTIVO" : "INACTIVO"]
  ];

  const tableWrap = document.createElement("div");
  tableWrap.className = "table-container";
  const table = document.createElement("table");
  table.className = "table";
  const tbody = document.createElement("tbody");

  for (const [label, val] of rows) {
    const tr = document.createElement("tr");
    const th = document.createElement("th");
    th.textContent = label;
    const td = document.createElement("td");
    td.textContent = val;
    tr.append(th, td);
    tbody.appendChild(tr);
  }
  table.appendChild(tbody);
  tableWrap.appendChild(table);
  profileBody.appendChild(tableWrap);
  profileCard.append(profileHeader, profileBody);

  const pwdCard = document.createElement("section");
  pwdCard.className = "card";

  const pwdHeader = document.createElement("div");
  pwdHeader.className = "card-header";
  const pwdTitle = document.createElement("h2");
  pwdTitle.className = "card-title";
  pwdTitle.textContent = "Cambiar Contraseña";
  pwdHeader.appendChild(pwdTitle);

  const pwdBody = document.createElement("div");
  pwdBody.className = "card-body";

  const alertBox = document.createElement("div");
  alertBox.hidden = true;
  alertBox.style.marginBottom = "1rem";

  const form = document.createElement("form");
  form.noValidate = true;

  const currentGroup = createPasswordFieldGroup(
    "cfg-current-password",
    "Contraseña actual",
    "current-password"
  );
  const newGroup = createPasswordFieldGroup(
    "cfg-new-password",
    "Nueva contraseña (mínimo 12 caracteres)",
    "new-password"
  );

  const submitBtn = document.createElement("button");
  submitBtn.type = "submit";
  submitBtn.className = "btn btn-primary";
  submitBtn.textContent = "Actualizar contraseña";

  form.append(currentGroup.group, newGroup.group, submitBtn);

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    hideInlineAlert(alertBox);

    const currentPassword = currentGroup.input.value;
    const newPassword = newGroup.input.value;

    if (!currentPassword || !newPassword) {
      showInlineAlert(alertBox, "Complete su contraseña actual y la nueva contraseña.", "error");
      return;
    }
    if (newPassword.length < 12) {
      showInlineAlert(alertBox, "La nueva contraseña debe tener al menos 12 caracteres.", "error");
      return;
    }

    setButtonLoading(submitBtn, true, "Actualizando...");
    try {
      const response = await changePassword(currentPassword, newPassword);
      form.reset();
      showInlineAlert(
        alertBox,
        (response && response.message) || "Contraseña actualizada correctamente.",
        "success"
      );
      showToast({
        type: "success",
        title: "Contraseña actualizada",
        message: "Las demás sesiones activas fueron cerradas por seguridad."
      });
    } catch (err) {
      showInlineAlert(alertBox, err.message || "No se pudo actualizar la contraseña.", "error");
    } finally {
      setButtonLoading(submitBtn, false);
    }
  });

  pwdBody.append(alertBox, form);
  pwdCard.append(pwdHeader, pwdBody);

  container.append(profileCard, pwdCard);
}

function createPasswordFieldGroup(id, labelText, autocomplete) {
  const group = document.createElement("div");
  group.className = "form-group";

  const label = document.createElement("label");
  label.className = "form-label";
  label.setAttribute("for", id);
  label.textContent = labelText;

  const input = document.createElement("input");
  input.type = "password";
  input.id = id;
  input.className = "form-control";
  input.required = true;
  input.maxLength = 72;
  input.autocomplete = autocomplete;

  group.append(label, input);
  return { group, input };
}

function renderModulePlaceholderView(user, routeItem, container) {
  const card = document.createElement("section");
  card.className = "card";

  const header = document.createElement("div");
  header.className = "card-header";

  const title = document.createElement("h2");
  title.className = "card-title";
  title.textContent = `Módulo de ${routeItem.label}`;

  const badge = document.createElement("span");
  badge.className = "badge badge-neutral";
  badge.textContent = `Programado para ${routeItem.promptTarget}`;

  header.append(title, badge);

  const body = document.createElement("div");
  body.className = "card-body";

  const desc = document.createElement("p");
  desc.textContent = routeItem.description;

  const notice = document.createElement("div");
  notice.className = "alert alert-info";
  notice.style.marginTop = "1rem";
  notice.textContent = `Los servicios REST del backend para "${routeItem.label}" ya se encuentran operativos y protegidos por Spring Security. La interfaz interactiva completa de este módulo se implementará en el ${routeItem.promptTarget}.`;

  const backBtn = document.createElement("button");
  backBtn.type = "button";
  backBtn.className = "btn btn-secondary";
  backBtn.style.marginTop = "1.25rem";
  backBtn.textContent = "Volver al Dashboard Administrativo";
  backBtn.addEventListener("click", () => navigateToSection("dashboard"));

  body.append(desc, notice, backBtn);
  card.append(header, body);
  container.appendChild(card);
}
