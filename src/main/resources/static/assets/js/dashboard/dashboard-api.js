/**
 * Capa de consumo de endpoints REST del Dashboard y Notificaciones (PROMPT 10 - Sección 8 y 14)
 * Reutiliza el cliente HTTP centralizado de api.js respetando sesiones HTTP y CSRF.
 */
import { APP_CONFIG } from "../config.js";
import { api } from "../api.js";

/**
 * Construye una cadena de consulta URL segura con parámetros opcionales.
 */
function buildQueryString(params = {}) {
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
 * Obtiene el resumen global ejecutivo del dashboard (GET /api/dashboard/resumen).
 */
export function getDashboardResumen() {
  return api.get(APP_CONFIG.endpoints.dashboardResumen);
}

/**
 * Obtiene las estadísticas consolidadas por rango de fechas (GET /api/dashboard/estadisticas).
 */
export function getDashboardEstadisticas(filtros = {}) {
  const qs = buildQueryString({
    fechaDesde: filtros.fechaDesde,
    fechaHasta: filtros.fechaHasta
  });
  return api.get(`${APP_CONFIG.endpoints.dashboardEstadisticas}${qs}`);
}

/**
 * Obtiene las estadísticas de solicitudes por mes y estado (GET /api/dashboard/solicitudes/estadisticas).
 */
export function getSolicitudesEstadisticas(filtros = {}) {
  const qs = buildQueryString({
    fechaDesde: filtros.fechaDesde,
    fechaHasta: filtros.fechaHasta,
    tipoTramiteId: filtros.tipoTramiteId,
    estado: filtros.estado
  });
  return api.get(`${APP_CONFIG.endpoints.dashboardSolicitudesEstadisticas}${qs}`);
}

/**
 * Obtiene las estadísticas de inspecciones (GET /api/dashboard/inspecciones/estadisticas).
 */
export function getInspeccionesEstadisticas(filtros = {}) {
  const qs = buildQueryString({
    fechaDesde: filtros.fechaDesde,
    fechaHasta: filtros.fechaHasta
  });
  return api.get(`${APP_CONFIG.endpoints.dashboardInspeccionesEstadisticas}${qs}`);
}

/**
 * Obtiene la lista de actividad reciente administrativa (GET /api/dashboard/actividad-reciente).
 */
export function getActividadReciente(limite = 8) {
  const qs = buildQueryString({ limite });
  return api.get(`${APP_CONFIG.endpoints.dashboardActividadReciente}${qs}`);
}

/**
 * Consulta el estado de disponibilidad del servicio backend (GET /api/public/health).
 */
export function getBackendHealth() {
  return api.get(APP_CONFIG.endpoints.health, { redirectOn401: false });
}

/**
 * Consulta el contador de notificaciones no leídas del usuario actual (GET /api/notificaciones/contador).
 */
export function getUnreadNotificationsCount() {
  return api.get(APP_CONFIG.endpoints.unreadNotificationsCount, { redirectOn401: false });
}

/**
 * Consulta las últimas notificaciones no leídas del usuario actual (GET /api/notificaciones/no-leidas).
 */
export function getUnreadNotificationsList(size = 5) {
  const qs = buildQueryString({ page: 0, size });
  return api.get(`${APP_CONFIG.endpoints.unreadNotificationsList}${qs}`, { redirectOn401: false });
}

/**
 * Marca todas las notificaciones del usuario autenticado como leídas (PATCH /api/notificaciones/leer-todas).
 */
export function markAllNotificationsAsRead() {
  return api.patch(APP_CONFIG.endpoints.markAllNotificationsRead, {});
}
