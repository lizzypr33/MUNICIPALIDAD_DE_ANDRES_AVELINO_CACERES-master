/**
 * Capa de consumo REST para el Módulo de Usuarios y Roles (PROMPT 11 - Secciones 5, 6, 7 y 20)
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
 * Lista usuarios con filtros y paginación real del backend (GET /api/usuarios).
 */
export function listarUsuarios({
  username = "",
  nombre = "",
  activo = "",
  rol = "",
  page = 0,
  size = 10,
  sort = "id,asc"
} = {}) {
  const query = buildQuery({ username, nombre, activo, rol, page, size, sort });
  return api.get(`${APP_CONFIG.endpoints.usuarios}${query}`);
}

/**
 * Obtiene el detalle de un usuario por ID (GET /api/usuarios/{id}).
 */
export function obtenerUsuario(id) {
  return api.get(`${APP_CONFIG.endpoints.usuarios}/${encodeURIComponent(id)}`);
}

/**
 * Crea un nuevo usuario municipal (POST /api/usuarios).
 */
export function crearUsuario(payload) {
  return api.post(APP_CONFIG.endpoints.usuarios, payload);
}

/**
 * Actualiza datos personales de un usuario (PUT /api/usuarios/{id}).
 */
export function actualizarUsuario(id, payload) {
  return api.put(`${APP_CONFIG.endpoints.usuarios}/${encodeURIComponent(id)}`, payload);
}

/**
 * Activa o desactiva la cuenta de un usuario (PATCH /api/usuarios/{id}/estado).
 */
export function cambiarEstadoUsuario(id, activo) {
  return api.patch(`${APP_CONFIG.endpoints.usuarios}/${encodeURIComponent(id)}/estado`, {
    activo: Boolean(activo)
  });
}

/**
 * Asigna roles autorizados a un usuario (PUT /api/usuarios/{id}/roles).
 */
export function asignarRolesUsuario(id, rolIds) {
  return api.put(`${APP_CONFIG.endpoints.usuarios}/${encodeURIComponent(id)}/roles`, {
    rolIds: Array.from(rolIds).map(Number)
  });
}

/**
 * Obtiene el catálogo de roles disponibles en el sistema (GET /api/roles).
 */
export function listarRolesSistema() {
  return api.get(APP_CONFIG.endpoints.roles);
}
