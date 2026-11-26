/**
 * Capa de consumo REST para el Módulo de Ciudadanos (PROMPT 11 - Secciones 8, 9, 10 y 20)
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
 * Lista ciudadanos con filtros y paginación real del backend (GET /api/ciudadanos).
 */
export function listarCiudadanos({
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
  return api.get(`${APP_CONFIG.endpoints.ciudadanos}${query}`);
}

/**
 * Busca un ciudadano exacto por tipo y número de documento (GET /api/ciudadanos/documento).
 */
export function obtenerCiudadanoPorDocumento(tipoDocumento, numeroDocumento) {
  const query = buildQuery({ tipoDocumento, numeroDocumento });
  return api.get(`${APP_CONFIG.endpoints.ciudadanosPorDocumento}${query}`);
}

/**
 * Obtiene el detalle de un ciudadano por ID (GET /api/ciudadanos/{id}).
 */
export function obtenerCiudadano(id) {
  return api.get(`${APP_CONFIG.endpoints.ciudadanos}/${encodeURIComponent(id)}`);
}

/**
 * Obtiene la ficha de ciudadano asociada a la cuenta autenticada (GET /api/ciudadanos/mi-perfil).
 */
export function obtenerMiPerfilCiudadano() {
  return api.get(`${APP_CONFIG.endpoints.ciudadanos}/mi-perfil`);
}

/**
 * Registra un nuevo ciudadano (POST /api/ciudadanos).
 */
export function crearCiudadano(payload) {
  return api.post(APP_CONFIG.endpoints.ciudadanos, payload);
}

/**
 * Actualiza los datos de un ciudadano (PUT /api/ciudadanos/{id}).
 */
export function actualizarCiudadano(id, payload) {
  return api.put(`${APP_CONFIG.endpoints.ciudadanos}/${encodeURIComponent(id)}`, payload);
}

/**
 * Activa o desactiva un ciudadano (PATCH /api/ciudadanos/{id}/estado).
 */
export function cambiarEstadoCiudadano(id, activo) {
  return api.patch(`${APP_CONFIG.endpoints.ciudadanos}/${encodeURIComponent(id)}/estado`, {
    activo: Boolean(activo)
  });
}

/**
 * Consulta los establecimientos comerciales vinculados a un ciudadano (GET /api/establecimientos/ciudadano/{id}).
 */
export function listarEstablecimientosDeCiudadano(ciudadanoId) {
  return api.get(
    `${APP_CONFIG.endpoints.establecimientos}/ciudadano/${encodeURIComponent(ciudadanoId)}`
  );
}

/**
 * Consulta el resumen de solicitudes vinculadas a un ciudadano (GET /api/solicitudes?ciudadanoId=...).
 */
export function listarSolicitudesDeCiudadano(ciudadanoId, size = 10) {
  const query = buildQuery({ ciudadanoId, page: 0, size });
  return api.get(`${APP_CONFIG.endpoints.solicitudes}${query}`);
}

/**
 * Función auxiliar para selectores dinámicos remotos de ciudadanos activos (Sección 15).
 */
export async function buscarCiudadanosActivosSelector(queryText = "") {
  const q = (queryText || "").trim();
  const isDigitsOnly = /^\d{4,15}$/.test(q);
  const page = await listarCiudadanos({
    numeroDocumento: isDigitsOnly ? q : "",
    nombres: !isDigitsOnly ? q : "",
    activo: "true",
    page: 0,
    size: 10
  });
  const items = Array.isArray(page?.contenido) ? page.contenido : [];
  return items.map((c) => ({
    id: c.id,
    primaryText: `${c.nombres || ""} ${c.apellidos || ""}`.trim(),
    secondaryText: `${c.tipoDocumento} ${c.numeroDocumento}`
  }));
}
