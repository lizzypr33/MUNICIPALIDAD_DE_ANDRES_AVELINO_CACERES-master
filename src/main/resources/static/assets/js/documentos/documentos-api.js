/**
 * Capa de consumo REST para Documentos Adjuntos Municipales (PROMPT 12 - Secciones 10, 14, 15)
 * Reutiliza el cliente HTTP centralizado assets/js/api.js con soporte FormData y Blob.
 */
import { APP_CONFIG } from "../config.js";
import { api } from "../api.js";

/**
 * Lista los documentos adjuntos de una solicitud municipal (GET /api/solicitudes/{id}/documentos).
 */
export function listarDocumentosPorSolicitud(solicitudId) {
  return api.get(
    `${APP_CONFIG.endpoints.solicitudes}/${encodeURIComponent(solicitudId)}/documentos`
  );
}

/**
 * Sube un documento adjunto a una solicitud (POST /api/solicitudes/{id}/documentos).
 * Construye FormData con el archivo ("archivo"), requisitoId opcional y observaciones opcionales.
 */
export function subirDocumentoSolicitud(
  solicitudId,
  { archivo, requisitoId = null, observaciones = "" } = {}
) {
  const formData = new FormData();
  formData.append("archivo", archivo);
  if (requisitoId !== undefined && requisitoId !== null && String(requisitoId).trim() !== "") {
    formData.append("requisitoId", String(requisitoId).trim());
  }
  if (observaciones && String(observaciones).trim().length > 0) {
    formData.append("observaciones", String(observaciones).trim());
  }

  return api.uploadFormData(
    `${APP_CONFIG.endpoints.solicitudes}/${encodeURIComponent(solicitudId)}/documentos`,
    formData
  );
}

/**
 * Descarga el archivo binario de un documento (GET /api/documentos/{id}/descargar).
 */
export async function descargarDocumento(documentoId, nombreSugerido = "documento-municipal") {
  const { blob, disposition } = await api.downloadBlob(
    `${APP_CONFIG.endpoints.documentos}/${encodeURIComponent(documentoId)}/descargar`
  );

  let filename = nombreSugerido;
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
    anchor.download = filename || "documento-municipal";
    anchor.rel = "noopener";
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
  } finally {
    setTimeout(() => URL.revokeObjectURL(objectUrl), 1500);
  }
}

/**
 * Retira lógicamente un documento adjunto cuando la solicitud está en BORRADOR u OBSERVADA (DELETE /api/documentos/{id}).
 */
export function retirarDocumento(documentoId) {
  return api.delete(`${APP_CONFIG.endpoints.documentos}/${encodeURIComponent(documentoId)}`);
}
