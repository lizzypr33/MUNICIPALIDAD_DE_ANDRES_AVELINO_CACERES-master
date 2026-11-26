/**
 * Capa de consumo REST para el Módulo de Inspecciones Municipales (PROMPT 13)
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
 * Lista inspecciones con filtros y paginación del backend (GET /api/inspecciones).
 */
export function listarInspecciones({
  solicitudId = "",
  inspectorId = "",
  estado = "",
  resultado = "",
  fechaDesde = "",
  fechaHasta = "",
  page = 0,
  size = 20,
  sort = "id,desc"
} = {}) {
  const query = buildQuery({
    solicitudId,
    inspectorId,
    estado,
    resultado,
    fechaDesde,
    fechaHasta,
    page,
    size,
    sort
  });
  return api.get(`${APP_CONFIG.endpoints.inspecciones}${query}`);
}

/**
 * Obtiene el detalle integral de una inspección por ID (GET /api/inspecciones/{id}).
 */
export function obtenerInspeccion(id) {
  return api.get(`${APP_CONFIG.endpoints.inspecciones}/${encodeURIComponent(id)}`);
}

/**
 * Lista inspecciones vinculadas a una solicitud (GET /api/inspecciones/solicitud/{solicitudId}).
 */
export function listarInspeccionesPorSolicitud(solicitudId) {
  return api.get(
    `${APP_CONFIG.endpoints.inspeccionesPorSolicitud}/${encodeURIComponent(solicitudId)}`
  );
}

/**
 * Programa una nueva inspección técnica (POST /api/inspecciones).
 */
export function programarInspeccion(payload) {
  return api.post(APP_CONFIG.endpoints.inspecciones, payload);
}

/**
 * Reprograma una inspección pendiente indicando nueva fecha y motivo (PUT /api/inspecciones/{id}/reprogramar).
 */
export function reprogramarInspeccion(id, payload) {
  return api.put(`${APP_CONFIG.endpoints.inspecciones}/${encodeURIComponent(id)}/reprogramar`, payload);
}

/**
 * Inicia la ejecución de una inspección técnica (PATCH /api/inspecciones/{id}/iniciar).
 */
export function iniciarInspeccion(id) {
  return api.patch(`${APP_CONFIG.endpoints.inspecciones}/${encodeURIComponent(id)}/iniciar`, {});
}

/**
 * Registra el resultado técnico de una inspección (PATCH /api/inspecciones/{id}/resultado).
 */
export function registrarResultadoInspeccion(id, payload) {
  return api.patch(
    `${APP_CONFIG.endpoints.inspecciones}/${encodeURIComponent(id)}/resultado`,
    payload
  );
}

/**
 * Cancela administrativamente una inspección con motivo obligatorio (PATCH /api/inspecciones/{id}/cancelar).
 */
export function cancelarInspeccion(id, { motivo, observaciones = "" }) {
  return api.patch(`${APP_CONFIG.endpoints.inspecciones}/${encodeURIComponent(id)}/cancelar`, {
    motivo: String(motivo || "").trim(),
    observaciones: observaciones ? String(observaciones).trim() : null
  });
}
