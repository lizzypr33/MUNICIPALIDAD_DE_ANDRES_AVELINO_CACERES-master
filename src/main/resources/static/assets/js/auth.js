/**
 * Gestión de autenticación y autorización respaldada por Spring Security (PROMPT 09 - Secciones 8, 11 y 12)
 */
import { APP_CONFIG, MENU_ITEMS } from "./config.js";
import { api, ensureCsrfToken } from "./api.js";

let currentUserCache = null;

/**
 * Normaliza un nombre de rol eliminando el prefijo ROLE_ si estuviera presente.
 */
export function normalizeRole(role) {
  if (typeof role !== "string") {
    return "";
  }
  return role.trim().toUpperCase().replace(/^ROLE_/, "");
}

/**
 * Consulta el perfil del usuario autenticado desde GET /api/auth/me.
 * Nunca almacena contraseñas ni confía en roles guardados en localStorage.
 */
export async function getCurrentUser(forceRefresh = false) {
  if (currentUserCache && !forceRefresh) {
    return currentUserCache;
  }
  try {
    const user = await api.get(APP_CONFIG.endpoints.me, { redirectOn401: false });
    if (user && user.username) {
      const normalizedRoles = Array.isArray(user.roles)
        ? user.roles.map(normalizeRole).filter(Boolean)
        : [];
      currentUserCache = Object.freeze({
        id: user.id,
        username: user.username,
        nombres: user.nombres || "",
        apellidos: user.apellidos || "",
        nombreCompleto: `${user.nombres || ""} ${user.apellidos || ""}`.trim() || user.username,
        email: user.email || "",
        roles: Object.freeze(normalizedRoles),
        activo: Boolean(user.activo)
      });
      return currentUserCache;
    }
    currentUserCache = null;
    return null;
  } catch (_err) {
    currentUserCache = null;
    return null;
  }
}

/**
 * Indica si existe una sesión autenticada válida en el servidor.
 */
export async function isAuthenticated(forceRefresh = false) {
  const user = await getCurrentUser(forceRefresh);
  return Boolean(user && user.activo);
}

/**
 * Inicia sesión contra POST /api/auth/login previa obtención del token CSRF.
 */
export async function login(username, password) {
  const cleanUsername = typeof username === "string" ? username.trim() : "";
  await ensureCsrfToken(true);

  await api.post(
    APP_CONFIG.endpoints.login,
    { username: cleanUsername, password },
    { redirectOn401: false }
  );

  // Al iniciar sesión, Spring Security rota el ID de sesión y el token CSRF
  await ensureCsrfToken(true);
  const user = await getCurrentUser(true);
  if (!user) {
    throw new Error("No fue posible verificar la sesión iniciada.");
  }
  return user;
}

/**
 * Cierra la sesión activa enviando POST /api/auth/logout con protección CSRF.
 */
export async function logout() {
  try {
    await ensureCsrfToken();
    await api.post(APP_CONFIG.endpoints.logout, null, { redirectOn401: false });
  } finally {
    currentUserCache = null;
    sessionStorage.removeItem("municipal_ui_notice");
  }
}

/**
 * Protege páginas privadas verificando la sesión contra el backend.
 * Si no hay sesión válida, redirige a login.html conservando el hash solicitado.
 */
export async function requireAuth() {
  const user = await getCurrentUser(true);
  if (!user || !user.activo) {
    if (window.location.pathname.endsWith("/login.html") || window.location.pathname === "/login.html") {
      return null;
    }
    const requestedHash = window.location.hash && /^#[a-z0-9_-]+$/i.test(window.location.hash)
      ? window.location.hash.substring(1)
      : "";
    const targetUrl = requestedHash
      ? `${APP_CONFIG.pages.login}?expired=1&section=${encodeURIComponent(requestedHash)}`
      : `${APP_CONFIG.pages.login}`;
    window.location.replace(targetUrl);
    return null;
  }
  return user;
}

/**
 * Verifica si el usuario autenticado posee al menos uno de los roles indicados.
 */
export function hasRole(user, ...allowedRoles) {
  if (!user || !Array.isArray(user.roles)) {
    return false;
  }
  const flatAllowed = allowedRoles.flat().map(normalizeRole).filter(Boolean);
  if (flatAllowed.length === 0) {
    return true;
  }
  return user.roles.some((r) => flatAllowed.includes(r));
}

/**
 * Verifica si el usuario autenticado tiene permiso para acceder a un módulo del menú.
 */
export function hasPermission(user, moduleKey) {
  const item = MENU_ITEMS.find((m) => m.key === moduleKey);
  if (!item) {
    return false;
  }
  return hasRole(user, item.roles);
}

/**
 * Cambia la contraseña del usuario autenticado (POST /api/auth/change-password).
 */
export async function changePassword(currentPassword, newPassword) {
  return api.post(APP_CONFIG.endpoints.changePassword, {
    currentPassword,
    newPassword
  });
}

export function clearAuthCache() {
  currentUserCache = null;
}
