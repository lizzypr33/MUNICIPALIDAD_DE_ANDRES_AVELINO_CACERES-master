/**
 * Punto de entrada principal del Frontend Municipal (PROMPT 09)
 * Coordina index.html, login.html y dashboard.html con validación de sesión y CSRF.
 */
import { APP_CONFIG } from "./config.js";
import { ensureCsrfToken } from "./api.js";
import { clearAuthCache, getCurrentUser, isAuthenticated, login, requireAuth } from "./auth.js";
import {
  bindLayoutInteractions,
  refreshUnreadNotificationsBadge,
  renderHeaderUser,
  renderSectionView,
  renderSidebarMenu,
  updateSidebarActiveState
} from "./layout.js";
import { initRouter } from "./router.js";
import { hideInlineAlert, setButtonLoading, showInlineAlert, showToast } from "./ui.js";
import { initVerificacionPublicaPage } from "./licencias/licencia-verificacion.js";

/**
 * Inicialización de index.html: redirige sin bucles según el estado real de la sesión HTTP.
 */
async function initIndexPage() {
  // Tiempo límite de seguridad: si la comprobación tarda más de 3 segundos, redirigir a login
  const fallbackTimer = window.setTimeout(() => {
    window.location.replace(APP_CONFIG.pages.login);
  }, 3000);

  try {
    const authenticated = await isAuthenticated(true);
    window.clearTimeout(fallbackTimer);
    if (authenticated) {
      window.location.replace(APP_CONFIG.pages.dashboard);
    } else {
      window.location.replace(APP_CONFIG.pages.login);
    }
  } catch (_err) {
    window.clearTimeout(fallbackTimer);
    window.location.replace(APP_CONFIG.pages.login);
  }
}

/**
 * Inicialización de login.html: valida sesión previa, maneja CSRF, mostrar/ocultar contraseña y envío.
 */
async function initLoginPage() {
  try {
    const existingUser = await getCurrentUser(true);
    if (existingUser && existingUser.activo) {
      window.location.replace(APP_CONFIG.pages.dashboard);
      return;
    }
  } catch (_err) {
    // Si falla la verificación, continuar y mostrar formulario de login
  }

  const guardScreen = document.getElementById("login-guard-screen");
  const loginContainer = document.getElementById("login-main-container");
  if (guardScreen) guardScreen.hidden = true;
  if (loginContainer) loginContainer.hidden = false;

  const form = document.getElementById("login-form");
  const usernameInput = document.getElementById("login-username");
  const passwordInput = document.getElementById("login-password");
  const togglePwdBtn = document.getElementById("toggle-password-btn");
  const submitBtn = document.getElementById("login-submit-btn");
  const alertBox = document.getElementById("login-alert-box");

  // Mostrar avisos contextuales si viene de cierre de sesión o sesión expirada
  const params = new URLSearchParams(window.location.search);
  if (params.get("logout") === "1") {
    showInlineAlert(alertBox, "Su sesión ha sido cerrada correctamente.", "success");
  } else if (params.get("expired") === "1") {
    showInlineAlert(alertBox, "Su sesión ha expirado o requiere iniciar sesión para continuar.", "warning");
  }

  // Pre-inicializar cookie CSRF de forma silenciosa
  ensureCsrfToken().catch(() => {});

  if (togglePwdBtn && passwordInput) {
    togglePwdBtn.addEventListener("click", () => {
      const showing = passwordInput.type === "text";
      passwordInput.type = showing ? "password" : "text";
      togglePwdBtn.textContent = showing ? "Mostrar" : "Ocultar";
      togglePwdBtn.setAttribute(
        "aria-label",
        showing ? "Mostrar contraseña" : "Ocultar contraseña"
      );
      togglePwdBtn.setAttribute("aria-pressed", String(!showing));
    });
  }

  if (!form || !usernameInput || !passwordInput || !submitBtn) {
    return;
  }

  let isSubmitting = false;

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    if (isSubmitting) return;

    hideInlineAlert(alertBox);
    usernameInput.removeAttribute("aria-invalid");
    passwordInput.removeAttribute("aria-invalid");

    const username = usernameInput.value.trim();
    const password = passwordInput.value;

    if (!username) {
      usernameInput.setAttribute("aria-invalid", "true");
      usernameInput.focus();
      showInlineAlert(alertBox, "Ingrese su nombre de usuario municipal.", "error");
      return;
    }

    if (!password) {
      passwordInput.setAttribute("aria-invalid", "true");
      passwordInput.focus();
      showInlineAlert(alertBox, "Ingrese su contraseña para continuar.", "error");
      return;
    }

    isSubmitting = true;
    setButtonLoading(submitBtn, true, "Verificando credenciales...");

    try {
      await login(username, password);
      passwordInput.value = "";

      const requestedSection = params.get("section");
      const safeHash = requestedSection && /^[a-z0-9_-]+$/i.test(requestedSection)
        ? `#${requestedSection}`
        : "";
      window.location.replace(`${APP_CONFIG.pages.dashboard}${safeHash}`);
    } catch (err) {
      passwordInput.value = "";
      passwordInput.setAttribute("aria-invalid", "true");
      showInlineAlert(
        alertBox,
        err.message || "Credenciales inválidas o error de conexión con el servidor.",
        "error"
      );
    } finally {
      isSubmitting = false;
      setButtonLoading(submitBtn, false);
    }
  });
}

/**
 * Inicialización de dashboard.html: protege ruta privada, carga usuario real, menú por roles y enrutador.
 */
async function initDashboardPage() {
  const user = await requireAuth();
  if (!user) {
    return;
  }

  const guardScreen = document.getElementById("dashboard-guard-screen");
  const appShell = document.getElementById("app-shell");
  if (guardScreen) guardScreen.hidden = true;
  if (appShell) appShell.hidden = false;

  renderHeaderUser(user);
  renderSidebarMenu(user, "dashboard");
  bindLayoutInteractions();
  refreshUnreadNotificationsBadge();

  initRouter(user, (routeItem, isAllowed) => {
    updateSidebarActiveState(routeItem.key);
    renderSectionView(user, routeItem, isAllowed);
  });

  // Escuchar expiración de sesión detectada por cualquier llamada API (401)
  window.addEventListener("municipal:unauthorized", () => {
    clearAuthCache();
    showToast({
      type: "warning",
      title: "Sesión expirada",
      message: "Redirigiendo a la pantalla de inicio de sesión..."
    });
    window.setTimeout(() => {
      window.location.replace(`${APP_CONFIG.pages.login}?expired=1`);
    }, 600);
  });

  // Evitar que al navegar hacia atrás (bfcache) después de logout se muestre contenido privado
  window.addEventListener("pageshow", async (event) => {
    if (event.persisted) {
      const valid = await isAuthenticated(true);
      if (!valid) {
        window.location.replace(APP_CONFIG.pages.login);
      }
    }
  });
}

/**
 * Despachador automático según el atributo data-page del elemento <body>.
 */
document.addEventListener("DOMContentLoaded", () => {
  const pageType = document.body ? document.body.dataset.page : "";
  if (pageType === "index") {
    initIndexPage();
  } else if (pageType === "login") {
    initLoginPage();
  } else if (pageType === "dashboard") {
    initDashboardPage();
  } else if (pageType === "verificar-licencia") {
    initVerificacionPublicaPage();
  } else if (pageType === "module-redirect") {
    const section = document.body.dataset.targetSection || "dashboard";
    const subview = document.body.dataset.subview;
    const searchParams = window.location.search ? window.location.search.replace(/^\?/, "") : "";
    const params = [];
    if (subview) params.push(`subview=${encodeURIComponent(subview)}`);
    if (searchParams) params.push(searchParams);
    const targetHash = params.length > 0 ? `${section}?${params.join("&")}` : encodeURIComponent(section);
    window.location.replace(`${APP_CONFIG.pages.dashboard}#${targetHash}`);
  }
});
