/**
 * Cliente API REST para el Centro de Notificaciones Municipales (PROMPT 14)
 * Municipalidad Distrital de Andrés Avelino Cáceres Dorregaray
 */
import { APP_CONFIG } from "../config.js";
import { api } from "../api.js";

function buildQuery(params = {}) {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && String(value).trim() !== "") {
      search.set(key, String(value).trim());
    }
  }
  const qs = search.toString();
  return qs ? `?${qs}` : "";
}

/**
 * Consulta la lista paginada de todas las notificaciones del usuario (GET /api/notificaciones).
 */
export function getNotificaciones(params = {}) {
  return api.get(`${APP_CONFIG.endpoints.notificaciones}${buildQuery(params)}`);
}

/**
 * Consulta la lista paginada de notificaciones no leídas (GET /api/notificaciones/no-leidas).
 */
export function getNotificacionesNoLeidas(params = {}) {
  return api.get(`${APP_CONFIG.endpoints.unreadNotificationsList}${buildQuery(params)}`);
}

/**
 * Consulta el contador en tiempo real de notificaciones no leídas (GET /api/notificaciones/contador).
 */
export function getNotificacionesContador() {
  return api.get(APP_CONFIG.endpoints.unreadNotificationsCount, { redirectOn401: false });
}

/**
 * Obtiene el detalle de una notificación por ID (GET /api/notificaciones/{id}).
 */
export function getNotificacion(id) {
  return api.get(`${APP_CONFIG.endpoints.notificaciones}/${encodeURIComponent(id)}`);
}

/**
 * Marca una notificación específica como leída (PATCH /api/notificaciones/{id}/leer).
 */
export function marcarNotificacionLeida(id) {
  return api.patch(`${APP_CONFIG.endpoints.notificaciones}/${encodeURIComponent(id)}/leer`, {});
}

/**
 * Marca todas las notificaciones del usuario como leídas (PATCH /api/notificaciones/leer-todas).
 */
export function marcarTodasNotificacionesLeidas() {
  return api.patch(APP_CONFIG.endpoints.markAllNotificationsRead, {});
}
