/**
 * Capa de consumo REST para el Módulo de Establecimientos Comerciales (PROMPT 11 - Secciones 13, 14 y 20)
 * Reutiliza el cliente HTTP centralizado assets/js/api.js con soporte de sesión y CSRF.
 */
import { APP_CONFIG } from "../config.js";
import { api } from "../api.js";

function buildQuery(params = {}) {
  const qs = new URLSearchParams();
  for (const [key, val] of Object.entries(params)) {
    if (val !== undefined && val !== null && String(val).trim() !== "") {
      qs.set(key, String(val).trim());
    }
  }
  const str = qs.toString();
  return str ? `?${str}` : "";
}

/**
 * Lista establecimientos con filtros y paginación real del backend (GET /api/establecimientos).
 */
export function listarEstablecimientos({
  nombreComercial = "",
  ruc = "",
  ciudadanoId = "",
  activo = "",
  page = 0,
  size = 10,
  sort = "id,asc"
} = {}) {
  const query = buildQuery({
    nombreComercial,
    ruc,
    ciudadanoId,
    activo,
    page,
    size,
    sort
  });
  return api.get(`${APP_CONFIG.endpoints.establecimientos}${query}`);
}

/**
 * Busca establecimientos por número de RUC (GET /api/establecimientos/ruc/{ruc}).
 */
export function listarEstablecimientosPorRuc(ruc) {
  return api.get(`${APP_CONFIG.endpoints.establecimientos}/ruc/${encodeURIComponent(ruc)}`);
}

/**
 * Lista los establecimientos pertenecientes a un ciudadano titular (GET /api/establecimientos/ciudadano/{ciudadanoId}).
 */
export function listarEstablecimientosPorCiudadano(ciudadanoId) {
  return api.get(
    `${APP_CONFIG.endpoints.establecimientos}/ciudadano/${encodeURIComponent(ciudadanoId)}`
  );
}

/**
 * Obtiene el detalle completo de un establecimiento comercial (GET /api/establecimientos/{id}).
 */
export function obtenerEstablecimiento(id) {
  return api.get(`${APP_CONFIG.endpoints.establecimientos}/${encodeURIComponent(id)}`);
}

/**
 * Registra un nuevo establecimiento comercial (POST /api/establecimientos).
 */
export function crearEstablecimiento(payload) {
  return api.post(APP_CONFIG.endpoints.establecimientos, payload);
}

/**
 * Actualiza un establecimiento comercial existente (PUT /api/establecimientos/{id}).
 */
export function actualizarEstablecimiento(id, payload) {
  return api.put(`${APP_CONFIG.endpoints.establecimientos}/${encodeURIComponent(id)}`, payload);
}

/**
 * Activa o desactiva un establecimiento comercial (PATCH /api/establecimientos/{id}/estado).
 */
export function cambiarEstadoEstablecimiento(id, activo) {
  return api.patch(`${APP_CONFIG.endpoints.establecimientos}/${encodeURIComponent(id)}/estado`, {
    activo: Boolean(activo)
  });
}

/**
 * Consulta las solicitudes vinculadas a un establecimiento (GET /api/solicitudes?establecimientoId=...).
 */
export function listarSolicitudesDeEstablecimiento(establecimientoId, size = 10) {
  const query = buildQuery({ establecimientoId, page: 0, size });
  return api.get(`${APP_CONFIG.endpoints.solicitudes}${query}`);
}
