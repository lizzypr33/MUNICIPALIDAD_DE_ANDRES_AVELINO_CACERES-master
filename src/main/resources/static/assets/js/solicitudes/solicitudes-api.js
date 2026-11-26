/**
 * Capa de consumo REST para Solicitudes y Expedientes Municipales (PROMPT 12 - Sección 10)
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
 * Lista solicitudes municipales con filtros y paginación del backend (GET /api/solicitudes).
 */
export function listarSolicitudes({
  numeroExpediente = "",
  ciudadanoId = "",
  establecimientoId = "",
  tipoTramiteId = "",
  estado = "",
  fechaInicial = "",
  fechaFinal = "",
  usuarioResponsableId = "",
  page = 0,
  size = 15,
  sort = "fechaPresentacion,desc"
} = {}) {
  const query = buildQuery({
    numeroExpediente,
    ciudadanoId,
    establecimientoId,
    tipoTramiteId,
    estado,
    fechaInicial,
    fechaFinal,
    usuarioResponsableId,
    page,
    size,
    sort
  });
  return api.get(`${APP_CONFIG.endpoints.solicitudes}${query}`);
}

/**
 * Obtiene el detalle integral de una solicitud y su expediente (GET /api/solicitudes/{id}).
 */
export function obtenerSolicitud(id) {
  return api.get(`${APP_CONFIG.endpoints.solicitudes}/${encodeURIComponent(id)}`);
}

/**
 * Consulta una solicitud por su número único de expediente (GET /api/solicitudes/expediente/{numero}).
 */
export function consultarPorExpediente(numeroExpediente) {
  return api.get(
    `${APP_CONFIG.endpoints.solicitudesPorExpediente}/${encodeURIComponent(
      String(numeroExpediente || "").trim()
    )}`
  );
}

/**
 * Registra una solicitud municipal en estado BORRADOR (POST /api/solicitudes).
 */
export function crearSolicitud(payload) {
  return api.post(APP_CONFIG.endpoints.solicitudes, payload);
}

/**
 * Actualiza los datos de una solicitud en estado BORRADOR (PUT /api/solicitudes/{id}).
 */
export function actualizarSolicitud(id, payload) {
  return api.put(`${APP_CONFIG.endpoints.solicitudes}/${encodeURIComponent(id)}`, payload);
}

/**
 * Presenta formalmente la solicitud verificando requisitos obligatorios y generando N° de expediente (POST /api/solicitudes/{id}/presentar).
 */
export function presentarSolicitud(id, observaciones = "") {
  return api.post(`${APP_CONFIG.endpoints.solicitudes}/${encodeURIComponent(id)}/presentar`, {
    observaciones: observaciones ? String(observaciones).trim() : null
  });
}

/**
 * Asigna un funcionario responsable al expediente (PATCH /api/solicitudes/{id}/asignar).
 */
export function asignarFuncionario(id, { usuarioResponsableId, observaciones = "" }) {
  return api.patch(`${APP_CONFIG.endpoints.solicitudes}/${encodeURIComponent(id)}/asignar`, {
    usuarioResponsableId: Number(usuarioResponsableId),
    observaciones: observaciones ? String(observaciones).trim() : null
  });
}

/**
 * Inicia la revisión técnica del expediente pasando de PRESENTADA o SUBSANADA a EN_REVISION (PATCH /api/solicitudes/{id}/iniciar-revision).
 */
export function iniciarRevisionSolicitud(id, observaciones = "") {
  return api.patch(
    `${APP_CONFIG.endpoints.solicitudes}/${encodeURIComponent(id)}/iniciar-revision`,
    {
      observaciones: observaciones ? String(observaciones).trim() : null
    }
  );
}

/**
 * Registra observaciones sobre la solicitud cambiando su estado a OBSERVADA (PATCH /api/solicitudes/{id}/observar).
 */
export function observarSolicitud(id, observaciones) {
  return api.patch(`${APP_CONFIG.endpoints.solicitudes}/${encodeURIComponent(id)}/observar`, {
    observaciones: String(observaciones || "").trim()
  });
}

/**
 * Registra la subsanación ciudadana cambiando el estado de OBSERVADA a SUBSANADA (PATCH /api/solicitudes/{id}/subsanar).
 */
export function subsanarSolicitud(id, observaciones) {
  return api.patch(`${APP_CONFIG.endpoints.solicitudes}/${encodeURIComponent(id)}/subsanar`, {
    observaciones: String(observaciones || "").trim()
  });
}

/**
 * Aprueba técnicamente la solicitud pasando de EN_REVISION a APROBADA (PATCH /api/solicitudes/{id}/aprobar).
 */
export function aprobarSolicitud(id, observaciones = "") {
  return api.patch(`${APP_CONFIG.endpoints.solicitudes}/${encodeURIComponent(id)}/aprobar`, {
    observaciones: observaciones ? String(observaciones).trim() : null
  });
}

/**
 * Rechaza motivadamente la solicitud pasando de EN_REVISION a RECHAZADA (PATCH /api/solicitudes/{id}/rechazar).
 */
export function rechazarSolicitud(id, { motivoRechazo, observaciones = "" }) {
  return api.patch(`${APP_CONFIG.endpoints.solicitudes}/${encodeURIComponent(id)}/rechazar`, {
    motivoRechazo: String(motivoRechazo || "").trim(),
    observaciones: observaciones ? String(observaciones).trim() : null
  });
}

/**
 * Archiva formalmente la solicitud indicando el motivo (PATCH /api/solicitudes/{id}/archivar).
 */
export function archivarSolicitud(id, { motivoRechazo, observaciones = "" }) {
  return api.patch(`${APP_CONFIG.endpoints.solicitudes}/${encodeURIComponent(id)}/archivar`, {
    motivoRechazo: String(motivoRechazo || "").trim(),
    observaciones: observaciones ? String(observaciones).trim() : null
  });
}

/**
 * Obtiene el historial completo de estados de la solicitud (GET /api/solicitudes/{id}/historial).
 */
export function listarHistorialSolicitud(id) {
  return api.get(
    `${APP_CONFIG.endpoints.solicitudes}/${encodeURIComponent(id)}/historial`
  );
}
