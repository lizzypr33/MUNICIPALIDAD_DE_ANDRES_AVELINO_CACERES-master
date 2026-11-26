/**
 * Cliente API REST de Solo Lectura para Auditoría del Sistema (PROMPT 14)
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
 * Consulta la bitácora paginada de eventos de auditoría (GET /api/auditorias).
 * Protegido por Spring Security: Solo accesible para roles autorizados (ADMINISTRADOR y SUPERVISOR).
 */
export function getAuditorias(params = {}) {
  return api.get(`${APP_CONFIG.endpoints.auditorias}${buildQuery(params)}`);
}

/**
 * Consulta el detalle inmutable de un evento de auditoría por su ID (GET /api/auditorias/{id}).
 */
export function getAuditoria(id) {
  return api.get(`${APP_CONFIG.endpoints.auditorias}/${encodeURIComponent(id)}`);
}
