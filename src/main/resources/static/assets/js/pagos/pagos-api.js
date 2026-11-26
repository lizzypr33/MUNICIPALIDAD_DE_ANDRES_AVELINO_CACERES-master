/**
 * Capa de consumo REST para el Módulo de Pagos Municipales (PROMPT 13)
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
 * Lista pagos con filtros y paginación del backend (GET /api/pagos).
 */
export function listarPagos({
  solicitudId = "",
  estado = "",
  tipoTramiteId = "",
  concepto = "",
  codigoOperacion = "",
  fechaDesde = "",
  fechaHasta = "",
  page = 0,
  size = 20,
  sort = "id,desc"
} = {}) {
  const query = buildQuery({
    solicitudId,
    estado,
    tipoTramiteId,
    concepto,
    codigoOperacion,
    fechaDesde,
    fechaHasta,
    page,
    size,
    sort
  });
  return api.get(`${APP_CONFIG.endpoints.pagos}${query}`);
}

/**
 * Obtiene el resumen financiero consolidado de pagos (GET /api/pagos/resumen).
 */
export function obtenerResumenPagos({
  fechaDesde = "",
  fechaHasta = "",
  estado = "",
  tipoTramiteId = "",
  concepto = ""
} = {}) {
  const query = buildQuery({ fechaDesde, fechaHasta, estado, tipoTramiteId, concepto });
  return api.get(`${APP_CONFIG.endpoints.pagosResumen}${query}`);
}

/**
 * Obtiene el detalle integral de un pago por su ID (GET /api/pagos/{id}).
 */
export function obtenerPago(id) {
  return api.get(`${APP_CONFIG.endpoints.pagos}/${encodeURIComponent(id)}`);
}

/**
 * Consulta la lista de pagos vinculados a una solicitud (GET /api/pagos/solicitud/{solicitudId}).
 */
export function listarPagosPorSolicitud(solicitudId) {
  return api.get(
    `${APP_CONFIG.endpoints.pagosPorSolicitud}/${encodeURIComponent(solicitudId)}`
  );
}

/**
 * Registra una declaración de pago municipal asociada a un expediente (POST /api/pagos).
 */
export function registrarPago(payload) {
  return api.post(APP_CONFIG.endpoints.pagos, payload);
}

/**
 * Verifica y confirma administrativamente un pago (PATCH /api/pagos/{id}/verificar).
 */
export function verificarPago(id, { codigoOperacionConfirmado = null, observaciones = "" } = {}) {
  return api.patch(`${APP_CONFIG.endpoints.pagos}/${encodeURIComponent(id)}/verificar`, {
    codigoOperacionConfirmado: codigoOperacionConfirmado ? String(codigoOperacionConfirmado).trim() : null,
    observaciones: String(observaciones || "").trim()
  });
}

/**
 * Rechaza un pago con inconsistencias con motivo obligatorio (PATCH /api/pagos/{id}/rechazar).
 */
export function rechazarPago(id, { motivo, observaciones = "" }) {
  return api.patch(`${APP_CONFIG.endpoints.pagos}/${encodeURIComponent(id)}/rechazar`, {
    motivo: String(motivo || "").trim(),
    observaciones: observaciones ? String(observaciones).trim() : null
  });
}

/**
 * Anula administrativamente un pago con motivo obligatorio (PATCH /api/pagos/{id}/anular).
 */
export function anularPago(id, { motivo, observaciones = "" }) {
  return api.patch(`${APP_CONFIG.endpoints.pagos}/${encodeURIComponent(id)}/anular`, {
    motivo: String(motivo || "").trim(),
    observaciones: observaciones ? String(observaciones).trim() : null
  });
}

/**
 * Descarga la constancia oficial de pago en PDF (GET /api/pagos/{id}/constancia).
 */
export async function descargarConstanciaPago(id, codigoOperacion = "Constancia-Pago") {
  const { blob, disposition } = await api.downloadBlob(
    `${APP_CONFIG.endpoints.pagos}/${encodeURIComponent(id)}/constancia`
  );

  let filename = `Constancia-Pago-${codigoOperacion}.pdf`;
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
