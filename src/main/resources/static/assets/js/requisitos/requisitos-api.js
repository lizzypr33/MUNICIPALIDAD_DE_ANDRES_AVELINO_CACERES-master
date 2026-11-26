/**
 * Capa de consumo REST para el Catálogo de Requisitos Municipales (PROMPT 12 - Sección 11)
 * Reutiliza el cliente HTTP centralizado assets/js/api.js.
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
 * Lista los requisitos del catálogo municipal con filtros y paginación (GET /api/requisitos).
 */
export function listarRequisitos({
  codigo = "",
  nombre = "",
  activo = "",
  page = 0,
  size = 20,
  sort = "id,asc"
} = {}) {
  const query = buildQuery({ codigo, nombre, activo, page, size, sort });
  return api.get(`${APP_CONFIG.endpoints.requisitos}${query}`);
}

/**
 * Obtiene el detalle de un requisito por ID (GET /api/requisitos/{id}).
 */
export function obtenerRequisito(id) {
  return api.get(`${APP_CONFIG.endpoints.requisitos}/${encodeURIComponent(id)}`);
}

/**
 * Crea un nuevo requisito en el catálogo municipal (POST /api/requisitos). Solo ADMINISTRADOR.
 */
export function crearRequisito(payload) {
  return api.post(APP_CONFIG.endpoints.requisitos, payload);
}

/**
 * Actualiza un requisito existente (PUT /api/requisitos/{id}). Solo ADMINISTRADOR.
 */
export function actualizarRequisito(id, payload) {
  return api.put(`${APP_CONFIG.endpoints.requisitos}/${encodeURIComponent(id)}`, payload);
}

/**
 * Activa o desactiva un requisito (PATCH /api/requisitos/{id}/estado). Solo ADMINISTRADOR.
 */
export function cambiarEstadoRequisito(id, activo) {
  return api.patch(`${APP_CONFIG.endpoints.requisitos}/${encodeURIComponent(id)}/estado`, {
    activo: Boolean(activo)
  });
}
