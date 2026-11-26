/**
 * Capa de consumo REST para Tipos de Trámite Municipal (PROMPT 12 - Sección 10)
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
 * Lista tipos de trámite con filtros y paginación del backend (GET /api/tipos-tramite).
 */
export function listarTiposTramite({
  codigo = "",
  nombre = "",
  activo = "",
  page = 0,
  size = 20,
  sort = "id,asc"
} = {}) {
  const query = buildQuery({ codigo, nombre, activo, page, size, sort });
  return api.get(`${APP_CONFIG.endpoints.tiposTramite}${query}`);
}

/**
 * Obtiene un tipo de trámite por su identificador (GET /api/tipos-tramite/{id}).
 */
export function obtenerTipoTramite(id) {
  return api.get(`${APP_CONFIG.endpoints.tiposTramite}/${encodeURIComponent(id)}`);
}

/**
 * Registra un nuevo tipo de trámite municipal (POST /api/tipos-tramite). Solo ADMINISTRADOR.
 */
export function crearTipoTramite(payload) {
  return api.post(APP_CONFIG.endpoints.tiposTramite, payload);
}

/**
 * Actualiza un tipo de trámite municipal (PUT /api/tipos-tramite/{id}). Solo ADMINISTRADOR.
 */
export function actualizarTipoTramite(id, payload) {
  return api.put(`${APP_CONFIG.endpoints.tiposTramite}/${encodeURIComponent(id)}`, payload);
}

/**
 * Activa o desactiva un tipo de trámite (PATCH /api/tipos-tramite/{id}/estado). Solo ADMINISTRADOR.
 */
export function cambiarEstadoTipoTramite(id, activo) {
  return api.patch(`${APP_CONFIG.endpoints.tiposTramite}/${encodeURIComponent(id)}/estado`, {
    activo: Boolean(activo)
  });
}

/**
 * Consulta la lista ordenada de requisitos asociados a un tipo de trámite (GET /api/tipos-tramite/{id}/requisitos).
 */
export function listarRequisitosPorTipoTramite(tipoTramiteId) {
  return api.get(
    `${APP_CONFIG.endpoints.tiposTramite}/${encodeURIComponent(tipoTramiteId)}/requisitos`
  );
}

/**
 * Configura los requisitos aplicables a un tipo de trámite (PUT /api/tipos-tramite/{id}/requisitos). Solo ADMINISTRADOR.
 * Las solicitudes ya presentadas conservan su requisitosSnapshot histórico.
 */
export function configurarRequisitosDeTipoTramite(tipoTramiteId, asignaciones = []) {
  return api.put(
    `${APP_CONFIG.endpoints.tiposTramite}/${encodeURIComponent(tipoTramiteId)}/requisitos`,
    asignaciones
  );
}
