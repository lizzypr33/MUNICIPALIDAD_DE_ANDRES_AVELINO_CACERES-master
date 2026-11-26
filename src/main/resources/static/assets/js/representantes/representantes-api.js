/**
 * Capa de consumo REST para el Módulo de Representantes (PROMPT 11 - Secciones 11, 12 y 20)
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
 * Lista representantes con filtros y paginación real del backend (GET /api/representantes).
 */
export function listarRepresentantes({
  tipoDocumento = "",
  numeroDocumento = "",
  nombres = "",
  apellidos = "",
  activo = "",
  page = 0,
  size = 10,
  sort = "id,asc"
} = {}) {
  const query = buildQuery({
    tipoDocumento,
    numeroDocumento,
    nombres,
    apellidos,
    activo,
    page,
    size,
    sort
  });
  return api.get(`${APP_CONFIG.endpoints.representantes}${query}`);
}

/**
 * Obtiene el detalle de un representante por ID (GET /api/representantes/{id}).
 */
export function obtenerRepresentante(id) {
  return api.get(`${APP_CONFIG.endpoints.representantes}/${encodeURIComponent(id)}`);
}

/**
 * Registra un nuevo representante (POST /api/representantes).
 */
export function crearRepresentante(payload) {
  return api.post(APP_CONFIG.endpoints.representantes, payload);
}

/**
 * Actualiza los datos de un representante (PUT /api/representantes/{id}).
 */
export function actualizarRepresentante(id, payload) {
  return api.put(`${APP_CONFIG.endpoints.representantes}/${encodeURIComponent(id)}`, payload);
}

/**
 * Activa o desactiva un representante (PATCH /api/representantes/{id}/estado).
 */
export function cambiarEstadoRepresentante(id, activo) {
  return api.patch(`${APP_CONFIG.endpoints.representantes}/${encodeURIComponent(id)}/estado`, {
    activo: Boolean(activo)
  });
}

/**
 * Vincula un representante activo a un establecimiento comercial existente (PUT /api/establecimientos/{id}).
 */
export async function vincularRepresentanteAEstablecimiento(establecimientoId, representanteId) {
  const est = await api.get(
    `${APP_CONFIG.endpoints.establecimientos}/${encodeURIComponent(establecimientoId)}`
  );
  return api.put(
    `${APP_CONFIG.endpoints.establecimientos}/${encodeURIComponent(establecimientoId)}`,
    {
      nombreComercial: est.nombreComercial,
      razonSocial: est.razonSocial || null,
      ruc: est.ruc || null,
      direccion: est.direccion,
      referencia: est.referencia || null,
      giroNegocio: est.giroNegocio,
      areaMetrosCuadrados: est.areaMetrosCuadrados,
      numeroPisos: est.numeroPisos,
      telefono: est.telefono || null,
      email: est.email || null,
      ciudadanoId: est.ciudadanoId,
      representanteId: Number(representanteId)
    }
  );
}

/**
 * Función auxiliar para selectores dinámicos remotos de representantes activos (Sección 15).
 */
export async function buscarRepresentantesActivosSelector(queryText = "") {
  const q = (queryText || "").trim();
  const isDigitsOnly = /^\d{4,15}$/.test(q);
  const page = await listarRepresentantes({
    numeroDocumento: isDigitsOnly ? q : "",
    nombres: !isDigitsOnly ? q : "",
    activo: "true",
    page: 0,
    size: 10
  });
  const items = Array.isArray(page?.contenido) ? page.contenido : [];
  return items.map((r) => ({
    id: r.id,
    primaryText: `${r.nombres || ""} ${r.apellidos || ""}`.trim(),
    secondaryText: `${r.tipoDocumento} ${r.numeroDocumento}${r.cargo ? ` · ${r.cargo}` : ""}`
  }));
}
