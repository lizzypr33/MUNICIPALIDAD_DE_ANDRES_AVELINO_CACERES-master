/**
 * Cliente API REST para el Módulo de Reportes Municipales y Exportaciones (PROMPT 14)
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
 * Consulta el reporte paginado de solicitudes (GET /api/reportes/solicitudes).
 */
export function getReporteSolicitudes(params = {}) {
  return api.get(`${APP_CONFIG.endpoints.reportesSolicitudes}${buildQuery(params)}`);
}

/**
 * Consulta el reporte paginado de licencias (GET /api/reportes/licencias).
 */
export function getReporteLicencias(params = {}) {
  return api.get(`${APP_CONFIG.endpoints.reportesLicencias}${buildQuery(params)}`);
}

/**
 * Consulta el reporte paginado de inspecciones (GET /api/reportes/inspecciones).
 */
export function getReporteInspecciones(params = {}) {
  return api.get(`${APP_CONFIG.endpoints.reportesInspecciones}${buildQuery(params)}`);
}

/**
 * Consulta el reporte paginado de pagos (GET /api/reportes/pagos).
 */
export function getReportePagos(params = {}) {
  return api.get(`${APP_CONFIG.endpoints.reportesPagos}${buildQuery(params)}`);
}

/**
 * Consulta el reporte paginado de establecimientos (GET /api/reportes/establecimientos).
 */
export function getReporteEstablecimientos(params = {}) {
  return api.get(`${APP_CONFIG.endpoints.reportesEstablecimientos}${buildQuery(params)}`);
}

/**
 * Consulta el reporte paginado de ciudadanos (GET /api/reportes/ciudadanos).
 */
export function getReporteCiudadanos(params = {}) {
  return api.get(`${APP_CONFIG.endpoints.reportesCiudadanos}${buildQuery(params)}`);
}

/**
 * Descarga el archivo de reporte en PDF desde el backend (GET /api/reportes/{tipo}/pdf).
 */
export async function downloadReportePdf(tipo, params = {}) {
  const safeTipo = encodeURIComponent(tipo);
  const endpoint = `${APP_CONFIG.endpoints.reportes}/${safeTipo}/pdf${buildQuery(params)}`;
  return api.downloadBlob(endpoint);
}

/**
 * Descarga el archivo de reporte en CSV desde el backend (GET /api/reportes/{tipo}/csv).
 */
export async function downloadReporteCsv(tipo, params = {}) {
  const safeTipo = encodeURIComponent(tipo);
  const endpoint = `${APP_CONFIG.endpoints.reportes}/${safeTipo}/csv${buildQuery(params)}`;
  return api.downloadBlob(endpoint);
}
