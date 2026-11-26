/**
 * Capa de consumo REST para el Módulo de Licencias de Funcionamiento (PROMPT 13)
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
 * Lista licencias con filtros y paginación del backend (GET /api/licencias).
 */
export function listarLicencias({
  codigo = "",
  solicitudId = "",
  estado = "",
  establecimientoId = "",
  ciudadanoId = "",
  fechaDesde = "",
  fechaHasta = "",
  page = 0,
  size = 20,
  sort = "id,desc"
} = {}) {
  const query = buildQuery({
    codigo,
    solicitudId,
    estado,
    establecimientoId,
    ciudadanoId,
    fechaDesde,
    fechaHasta,
    page,
    size,
    sort
  });
  return api.get(`${APP_CONFIG.endpoints.licencias}${query}`);
}

/**
 * Obtiene el detalle integral de una licencia por su ID (GET /api/licencias/{id}).
 */
export function obtenerLicencia(id) {
  return api.get(`${APP_CONFIG.endpoints.licencias}/${encodeURIComponent(id)}`);
}

/**
 * Consulta una licencia por su código oficial (GET /api/licencias/codigo/{codigo}).
 */
export function obtenerLicenciaPorCodigo(codigo) {
  return api.get(
    `${APP_CONFIG.endpoints.licenciasPorCodigo}/${encodeURIComponent(String(codigo).trim())}`
  );
}

/**
 * Consulta la licencia asociada a una solicitud municipal (GET /api/licencias/solicitud/{solicitudId}).
 */
export function obtenerLicenciaPorSolicitud(solicitudId) {
  return api.get(
    `${APP_CONFIG.endpoints.licenciasPorSolicitud}/${encodeURIComponent(solicitudId)}`
  );
}

/**
 * Emite una licencia de funcionamiento a partir de una solicitud aprobada (POST /api/licencias/emitir).
 */
export function emitirLicencia(payload) {
  return api.post(APP_CONFIG.endpoints.licenciaEmitir, payload);
}

/**
 * Suspende administrativamente una licencia con motivo obligatorio (PATCH /api/licencias/{id}/suspender).
 */
export function suspenderLicencia(id, { motivo, observaciones = "" }) {
  return api.patch(`${APP_CONFIG.endpoints.licencias}/${encodeURIComponent(id)}/suspender`, {
    motivo: String(motivo || "").trim(),
    observaciones: observaciones ? String(observaciones).trim() : null
  });
}

/**
 * Reactiva una licencia suspendida con motivo obligatorio (PATCH /api/licencias/{id}/reactivar).
 */
export function reactivarLicencia(id, { motivo, observaciones = "" }) {
  return api.patch(`${APP_CONFIG.endpoints.licencias}/${encodeURIComponent(id)}/reactivar`, {
    motivo: String(motivo || "").trim(),
    observaciones: observaciones ? String(observaciones).trim() : null
  });
}

/**
 * Revoca definitivamente una licencia con motivo obligatorio (PATCH /api/licencias/{id}/revocar).
 */
export function revocarLicencia(id, { motivo, observaciones = "" }) {
  return api.patch(`${APP_CONFIG.endpoints.licencias}/${encodeURIComponent(id)}/revocar`, {
    motivo: String(motivo || "").trim(),
    observaciones: observaciones ? String(observaciones).trim() : null
  });
}

/**
 * Descarga el certificado oficial de la licencia en PDF generado por el backend (GET /api/licencias/{id}/certificado).
 */
export async function descargarCertificadoLicencia(id, nombreSugerido = "Certificado-Licencia") {
  const { blob, disposition } = await api.downloadBlob(
    `${APP_CONFIG.endpoints.licencias}/${encodeURIComponent(id)}/certificado`
  );

  let filename = `${nombreSugerido}.pdf`;
  if (disposition) {
    const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i);
    const plainMatch = disposition.match(/filename="?([^";]+)"?/i);
    if (utf8Match && utf8Match[1]) {
      try {
        filename = decodeURIComponent(utf8Match[1]);
      } catch (_e) {
        filename = utf8Match[1];
      }
    } else if (plainMatch && plainMatch[1]) {
      filename = plainMatch[1];
    }
  }

  const objectUrl = URL.createObjectURL(blob);
  try {
    const anchor = document.createElement("a");
    anchor.href = objectUrl;
    anchor.download = filename;
    anchor.rel = "noopener";
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
  } finally {
    setTimeout(() => URL.revokeObjectURL(objectUrl), 1500);
  }
}

/**
 * Obtiene el historial de estados de una licencia (GET /api/licencias/{id}/historial).
 */
export function listarHistorialLicencia(id) {
  return api.get(`${APP_CONFIG.endpoints.licencias}/${encodeURIComponent(id)}/historial`);
}

/**
 * Consulta pública sin autenticación de autenticidad de licencia por token QR (GET /api/public/licencias/verificar/{token}).
 */
export async function verificarLicenciaPublica(token) {
  const safeToken = encodeURIComponent(String(token || "").trim());
  const response = await fetch(`${APP_CONFIG.endpoints.verificarLicenciaPublica}/${safeToken}`, {
    method: "GET",
    headers: { Accept: "application/json" }
  });

  const payload = await response.json().catch(() => null);
  if (!response.ok) {
    const msg =
      payload && payload.message
        ? payload.message
        : "El token de verificación no fue encontrado o no corresponde a una licencia registrada.";
    throw new Error(msg);
  }
  return payload;
}
