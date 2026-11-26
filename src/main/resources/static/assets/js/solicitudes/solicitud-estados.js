/**
 * Módulo de Definición de Estados y Máquina de Transiciones de Solicitudes (PROMPT 12 - Secciones 11 y 12)
 */
import { createElement } from "../dom.js";

export const ESTADOS_SOLICITUD = Object.freeze({
  BORRADOR: {
    codigo: "BORRADOR",
    label: "Borrador",
    badgeClass: "badge--neutral",
    descripcion: "Solicitud en elaboración. Aún no cuenta con N° de Expediente."
  },
  PRESENTADA: {
    codigo: "PRESENTADA",
    label: "Presentada",
    badgeClass: "badge--info",
    descripcion: "Expediente formalmente presentado y registrado con N° oficial."
  },
  EN_REVISION: {
    codigo: "EN_REVISION",
    label: "En Revisión",
    badgeClass: "badge--warning",
    descripcion: "Bajo evaluación técnica y legal por el funcionario responsable."
  },
  OBSERVADA: {
    codigo: "OBSERVADA",
    label: "Observada",
    badgeClass: "badge--danger",
    descripcion: "Registra observaciones técnicas o documentales pendientes de subsanar."
  },
  SUBSANADA: {
    codigo: "SUBSANADA",
    label: "Subsanada",
    badgeClass: "badge--info",
    descripcion: "Subsanación ingresada por el administrado pendiente de reevaluación."
  },
  APROBADA: {
    codigo: "APROBADA",
    label: "Aprobada",
    badgeClass: "badge--success",
    descripcion: "Solicitud evaluada favorablemente. Habilitada para emisión de licencia."
  },
  RECHAZADA: {
    codigo: "RECHAZADA",
    label: "Rechazada",
    badgeClass: "badge--danger",
    descripcion: "Solicitud desestimada con resolución motivada."
  },
  ARCHIVADA: {
    codigo: "ARCHIVADA",
    label: "Archivada",
    badgeClass: "badge--neutral",
    descripcion: "Expediente archivado definitivamente por desistimiento o conclusión."
  }
});

/**
 * Retorna la etiqueta amigable del estado de una solicitud.
 */
export function getSolicitudEstadoLabel(estado) {
  const clean = String(estado || "").toUpperCase();
  return ESTADOS_SOLICITUD[clean]?.label || clean || "Desconocido";
}

/**
 * Genera un badge visual en elemento <span> con las clases institucionales.
 */
export function createSolicitudEstadoBadge(estado) {
  const clean = String(estado || "BORRADOR").toUpperCase();
  const meta = ESTADOS_SOLICITUD[clean] || {
    label: clean,
    badgeClass: "badge--neutral"
  };

  return createElement("span", {
    className: `badge ${meta.badgeClass}`,
    textContent: meta.label,
    attrs: { title: meta.descripcion || meta.label }
  });
}

/**
 * Determina las acciones de transición permitidas según el estado actual y los roles del usuario autenticado.
 */
export function getAvailableTransitions(estado, userRoles = []) {
  const clean = String(estado || "").toUpperCase();
  const roles = Array.isArray(userRoles) ? userRoles.map((r) => String(r).toUpperCase()) : [];

  const isAdmin = roles.includes("ROLE_ADMINISTRADOR") || roles.includes("ADMINISTRADOR");
  const isSupervisor = roles.includes("ROLE_SUPERVISOR") || roles.includes("SUPERVISOR");
  const isFuncionario = roles.includes("ROLE_FUNCIONARIO") || roles.includes("FUNCIONARIO");
  const isCiudadano = roles.includes("ROLE_CIUDADANO") || roles.includes("CIUDADANO");

  const canManageOfficials = isAdmin || isSupervisor;
  const canEvaluate = isAdmin || isSupervisor || isFuncionario;

  return {
    canEditDraft: clean === "BORRADOR" && (isAdmin || isCiudadano || isFuncionario),
    canUploadDocs: (clean === "BORRADOR" || clean === "OBSERVADA") && (isAdmin || isCiudadano || isFuncionario),
    canPresentar: clean === "BORRADOR" && (isAdmin || isCiudadano || isFuncionario),
    canAsignar: (clean === "PRESENTADA" || clean === "EN_REVISION" || clean === "SUBSANADA") && canManageOfficials,
    canIniciarRevision: (clean === "PRESENTADA" || clean === "SUBSANADA") && canEvaluate,
    canObservar: clean === "EN_REVISION" && canEvaluate,
    canSubsanar: clean === "OBSERVADA" && (isAdmin || isCiudadano || canEvaluate),
    canAprobar: clean === "EN_REVISION" && canEvaluate,
    canRechazar: clean === "EN_REVISION" && canEvaluate,
    canArchivar: ["BORRADOR", "PRESENTADA", "EN_REVISION", "OBSERVADA", "SUBSANADA"].includes(clean) && canEvaluate
  };
}
