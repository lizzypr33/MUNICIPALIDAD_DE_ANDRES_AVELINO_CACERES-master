/**
 * Cliente HTTP centralizado basado en Fetch API con soporte de sesiones HTTP y CSRF (PROMPT 09 - Secciones 9 y 10)
 */
import { APP_CONFIG } from "./config.js";

const MUTATING_METHODS = new Set(["POST", "PUT", "PATCH", "DELETE"]);

let cachedCsrfHeaderName = "X-XSRF-TOKEN";
let cachedCsrfToken = null;

export class ApiError extends Error {
  constructor(status, message, details = null) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.details = details;
  }
}

/**
 * Lee el valor de una cookie legible por documento (ej. XSRF-TOKEN).
 */
function readCookie(name) {
  const prefix = `${encodeURIComponent(name)}=`;
  const parts = document.cookie ? document.cookie.split("; ") : [];
  for (const part of parts) {
    if (part.startsWith(prefix)) {
      return decodeURIComponent(part.substring(prefix.length));
    }
  }
  return null;
}

/**
 * Valida que la ruta solicitada sea una ruta relativa segura del mismo origen.
 */
function normalizeSafePath(path) {
  if (typeof path !== "string" || !path.startsWith("/") || path.startsWith("//")) {
    throw new ApiError(400, "Ruta de solicitud inválida.");
  }
  return path;
}

/**
 * Obtiene o refresca el token CSRF desde el backend (/api/auth/csrf) o cookie XSRF-TOKEN.
 */
export async function ensureCsrfToken(forceRefresh = false) {
  if (!forceRefresh) {
    const cookieToken = readCookie("XSRF-TOKEN");
    if (cookieToken) {
      cachedCsrfToken = cookieToken;
      return { headerName: cachedCsrfHeaderName, token: cookieToken };
    }
    if (cachedCsrfToken) {
      return { headerName: cachedCsrfHeaderName, token: cachedCsrfToken };
    }
  }

  const response = await fetch(APP_CONFIG.endpoints.csrf, {
    method: "GET",
    credentials: "same-origin",
    headers: { Accept: "application/json" }
  });

  if (!response.ok) {
    throw new ApiError(response.status, "No fue posible inicializar el token de seguridad CSRF.");
  }

  const data = await response.json();
  cachedCsrfHeaderName = data.headerName || "X-XSRF-TOKEN";
  cachedCsrfToken = readCookie("XSRF-TOKEN") || data.token || null;

  return { headerName: cachedCsrfHeaderName, token: cachedCsrfToken };
}

/**
 * Traduce códigos de estado HTTP a mensajes comprensibles y seguros para el usuario.
 */
function resolveSafeErrorMessage(status, payload) {
  if (payload && typeof payload.message === "string" && payload.message.trim().length > 0) {
    return payload.message.trim();
  }
  switch (status) {
    case 400:
      return "Los datos enviados no son válidos. Revise los campos e intente nuevamente.";
    case 401:
      return "Su sesión ha expirado o las credenciales son incorrectas.";
    case 403:
      return "No cuenta con permisos para realizar esta operación.";
    case 404:
      return "El recurso solicitado no fue encontrado.";
    case 409:
      return "La operación entró en conflicto con el estado actual del registro.";
    case 422:
      return "No se cumplieron las validaciones de negocio requeridas.";
    case 500:
    default:
      return "Ocurrió un error en el servidor municipal. Intente nuevamente en unos momentos.";
  }
}

/**
 * Ejecuta una petición HTTP centralizada.
 */
async function request(path, options = {}, isRetry = false) {
  const safePath = normalizeSafePath(path);
  const method = (options.method || "GET").toUpperCase();
  const headers = new Headers(options.headers || {});

  if (!headers.has("Accept")) {
    headers.set("Accept", options.responseType === "blob" ? "*/*" : "application/json");
  }

  let body = options.body;
  const isFormData = typeof FormData !== "undefined" && body instanceof FormData;

  if (body !== undefined && body !== null && !isFormData && typeof body !== "string") {
    headers.set("Content-Type", "application/json");
    body = JSON.stringify(body);
  }

  if (MUTATING_METHODS.has(method)) {
    const csrf = await ensureCsrfToken(isRetry);
    if (csrf.token) {
      headers.set(csrf.headerName, csrf.token);
    }
  }

  let response;
  try {
    response = await fetch(safePath, {
      method,
      headers,
      body,
      credentials: "same-origin"
    });
  } catch (_networkErr) {
    throw new ApiError(0, "No se pudo conectar con el servidor municipal. Verifique su conexión de red.");
  }

  // Si falla por CSRF desincronizado en el primer intento, refrescamos token y reintentamos una sola vez
  if (response.status === 403 && MUTATING_METHODS.has(method) && !isRetry) {
    await ensureCsrfToken(true);
    return request(safePath, options, true);
  }

  if (response.status === 204) {
    return null;
  }

  if (options.responseType === "blob" && response.ok) {
    const blob = await response.blob();
    const disposition = response.headers.get("Content-Disposition") || "";
    return { blob, disposition, contentType: response.headers.get("Content-Type") || "" };
  }

  const contentType = response.headers.get("Content-Type") || "";
  let payload = null;
  if (contentType.includes("application/json")) {
    try {
      payload = await response.json();
    } catch (_parseErr) {
      payload = null;
    }
  }

  if (!response.ok) {
    const message = resolveSafeErrorMessage(response.status, payload);
    if (response.status === 401 && options.redirectOn401 !== false) {
      window.dispatchEvent(new CustomEvent("municipal:unauthorized", { detail: { path: safePath } }));
    }
    throw new ApiError(response.status, message, payload);
  }

  return payload;
}

export const api = Object.freeze({
  get(path, options = {}) {
    return request(path, { ...options, method: "GET" });
  },
  post(path, body, options = {}) {
    return request(path, { ...options, method: "POST", body });
  },
  put(path, body, options = {}) {
    return request(path, { ...options, method: "PUT", body });
  },
  patch(path, body, options = {}) {
    return request(path, { ...options, method: "PATCH", body });
  },
  delete(path, options = {}) {
    return request(path, { ...options, method: "DELETE" });
  },
  uploadFormData(path, formData, options = {}) {
    return request(path, { ...options, method: "POST", body: formData });
  },
  downloadBlob(path, options = {}) {
    return request(path, { ...options, method: "GET", responseType: "blob" });
  }
});
