/**
 * Configuración institucional, endpoints REST y matriz de navegación por roles (PROMPT 09)
 */
export const APP_CONFIG = Object.freeze({
  municipalityName: "MUNICIPALIDAD DISTRITAL DE ANDRÉS AVELINO CÁCERES DORREGARAY",
  municipalityShortName: "MD Andrés Avelino Cáceres D.",
  systemName: "Sistema de Gestión de Licencias Municipales",
  regionLabel: "Ayacucho - Perú",
  emblemAcronym: "MDAACD",

  endpoints: Object.freeze({
    health: "/api/public/health",
    csrf: "/api/auth/csrf",
    login: "/api/auth/login",
    logout: "/api/auth/logout",
    me: "/api/auth/me",
    changePassword: "/api/auth/change-password",
    unreadNotificationsCount: "/api/notificaciones/contador",
    unreadNotificationsList: "/api/notificaciones/no-leidas",
    markAllNotificationsRead: "/api/notificaciones/leer-todas",
    dashboardResumen: "/api/dashboard/resumen",
    dashboardEstadisticas: "/api/dashboard/estadisticas",
    dashboardSolicitudesEstadisticas: "/api/dashboard/solicitudes/estadisticas",
    dashboardLicenciasEstadisticas: "/api/dashboard/licencias/estadisticas",
    dashboardInspeccionesEstadisticas: "/api/dashboard/inspecciones/estadisticas",
    dashboardPagosEstadisticas: "/api/dashboard/pagos/estadisticas",
    dashboardActividadReciente: "/api/dashboard/actividad-reciente",
    usuarios: "/api/usuarios",
    roles: "/api/roles",
    ciudadanos: "/api/ciudadanos",
    ciudadanosPorDocumento: "/api/ciudadanos/documento",
    representantes: "/api/representantes",
    establecimientos: "/api/establecimientos",
    solicitudes: "/api/solicitudes",
    solicitudesPorExpediente: "/api/solicitudes/expediente",
    tiposTramite: "/api/tipos-tramite",
    requisitos: "/api/requisitos",
    documentos: "/api/documentos",
    licencias: "/api/licencias",
    licenciasPorCodigo: "/api/licencias/codigo",
    licenciasPorSolicitud: "/api/licencias/solicitud",
    licenciaEmitir: "/api/licencias/emitir",
    verificarLicenciaPublica: "/api/public/licencias/verificar",
    inspecciones: "/api/inspecciones",
    inspeccionesPorSolicitud: "/api/inspecciones/solicitud",
    pagos: "/api/pagos",
    pagosResumen: "/api/pagos/resumen",
    pagosPorSolicitud: "/api/pagos/solicitud",
    notificaciones: "/api/notificaciones",
    auditorias: "/api/auditorias",
    reportes: "/api/reportes",
    reportesSolicitudes: "/api/reportes/solicitudes",
    reportesLicencias: "/api/reportes/licencias",
    reportesInspecciones: "/api/reportes/inspecciones",
    reportesPagos: "/api/reportes/pagos",
    reportesEstablecimientos: "/api/reportes/establecimientos",
    reportesCiudadanos: "/api/reportes/ciudadanos"
  }),

  pages: Object.freeze({
    index: "/index.html",
    login: "/login.html",
    dashboard: "/dashboard.html",
    usuarios: "/pages/usuarios.html",
    ciudadanos: "/pages/ciudadanos.html",
    representantes: "/pages/representantes.html",
    establecimientos: "/pages/establecimientos.html",
    solicitudes: "/pages/solicitudes.html",
    nuevaSolicitud: "/pages/nueva-solicitud.html",
    detalleSolicitud: "/pages/detalle-solicitud.html",
    tiposTramite: "/pages/tipos-tramite.html",
    requisitos: "/pages/requisitos.html",
    licencias: "/pages/licencias.html",
    detalleLicencia: "/pages/detalle-licencia.html",
    emitirLicencia: "/pages/emitir-licencia.html",
    verificarLicencia: "/pages/verificar-licencia.html",
    inspecciones: "/pages/inspecciones.html",
    detalleInspeccion: "/pages/detalle-inspeccion.html",
    pagos: "/pages/pagos.html",
    detallePago: "/pages/detalle-pago.html",
    reportes: "/pages/reportes.html",
    reporteDetalle: "/pages/reporte-detalle.html",
    notificaciones: "/pages/notificaciones.html",
    notificacionDetalle: "/pages/notificacion-detalle.html",
    auditoria: "/pages/auditoria.html",
    auditoriaDetalle: "/pages/auditoria-detalle.html"
  })
});

export const ROLES = Object.freeze({
  ADMINISTRADOR: "ADMINISTRADOR",
  FUNCIONARIO: "FUNCIONARIO",
  SUPERVISOR: "SUPERVISOR",
  CIUDADANO: "CIUDADANO"
});

export const ROLE_LABELS = Object.freeze({
  ADMINISTRADOR: "Administrador Municipal",
  FUNCIONARIO: "Funcionario Evaluador",
  SUPERVISOR: "Supervisor Municipal",
  CIUDADANO: "Ciudadano / Administrado"
});

/**
 * Definición de módulos del menú lateral y visibilidad según matriz de roles de Spring Security.
 */
export const MENU_ITEMS = Object.freeze([
  {
    key: "dashboard",
    label: "Dashboard",
    section: "Principal",
    icon: "dashboard",
    roles: [ROLES.ADMINISTRADOR, ROLES.FUNCIONARIO, ROLES.SUPERVISOR, ROLES.CIUDADANO],
    description: "Panel principal de bienvenida y accesos rápidos según su perfil institucional.",
    promptTarget: "PROMPT 10"
  },
  {
    key: "ciudadanos",
    label: "Ciudadanos",
    section: "Padrón Municipal",
    icon: "users",
    roles: [ROLES.ADMINISTRADOR, ROLES.FUNCIONARIO, ROLES.SUPERVISOR, ROLES.CIUDADANO],
    description: "Registro, consulta y actualización de ciudadanos titulares y administrados.",
    promptTarget: "PROMPT 11"
  },
  {
    key: "representantes",
    label: "Representantes",
    section: "Padrón Municipal",
    icon: "badge",
    roles: [ROLES.ADMINISTRADOR, ROLES.FUNCIONARIO, ROLES.SUPERVISOR],
    description: "Gestión de representantes legales acreditados ante la municipalidad.",
    promptTarget: "PROMPT 11"
  },
  {
    key: "establecimientos",
    label: "Establecimientos",
    section: "Padrón Municipal",
    icon: "store",
    roles: [ROLES.ADMINISTRADOR, ROLES.FUNCIONARIO, ROLES.SUPERVISOR, ROLES.CIUDADANO],
    description: "Administración de locales comerciales, giros de negocio, área y ubicación.",
    promptTarget: "PROMPT 11"
  },
  {
    key: "solicitudes",
    label: "Solicitudes",
    section: "Gestión de Expedientes",
    icon: "folder",
    roles: [ROLES.ADMINISTRADOR, ROLES.FUNCIONARIO, ROLES.SUPERVISOR, ROLES.CIUDADANO],
    description: "Trámite de expedientes municipales, requisitos, documentos adjuntos y evaluación.",
    promptTarget: "PROMPT 12"
  },
  {
    key: "inspecciones",
    label: "Inspecciones",
    section: "Gestión de Expedientes",
    icon: "clipboard",
    roles: [ROLES.ADMINISTRADOR, ROLES.FUNCIONARIO, ROLES.SUPERVISOR, ROLES.CIUDADANO],
    description: "Programación, ejecución y registro de resultados de inspecciones técnicas.",
    promptTarget: "PROMPT 13"
  },
  {
    key: "licencias",
    label: "Licencias",
    section: "Gestión de Expedientes",
    icon: "certificate",
    roles: [ROLES.ADMINISTRADOR, ROLES.FUNCIONARIO, ROLES.SUPERVISOR, ROLES.CIUDADANO],
    description: "Emisión de licencias de funcionamiento, descarga de certificados PDF y código QR.",
    promptTarget: "PROMPT 13"
  },
  {
    key: "pagos",
    label: "Pagos",
    section: "Recaudación y Control",
    icon: "payment",
    roles: [ROLES.ADMINISTRADOR, ROLES.FUNCIONARIO, ROLES.SUPERVISOR, ROLES.CIUDADANO],
    description: "Registro de pagos municipales, verificación financiera y constancias PDF.",
    promptTarget: "PROMPT 13"
  },
  {
    key: "notificaciones",
    label: "Notificaciones",
    section: "Recaudación y Control",
    icon: "bell",
    roles: [ROLES.ADMINISTRADOR, ROLES.FUNCIONARIO, ROLES.SUPERVISOR, ROLES.CIUDADANO],
    description: "Bandeja de avisos administrativos sobre el estado de expedientes y pagos.",
    promptTarget: "PROMPT 14"
  },
  {
    key: "reportes",
    label: "Reportes",
    section: "Supervisión y Analítica",
    icon: "chart",
    roles: [ROLES.ADMINISTRADOR, ROLES.FUNCIONARIO, ROLES.SUPERVISOR],
    description: "Generación de reportes municipales filtrados y exportación en PDF y CSV.",
    promptTarget: "PROMPT 14"
  },
  {
    key: "auditoria",
    label: "Auditoría",
    section: "Supervisión y Analítica",
    icon: "shield",
    roles: [ROLES.ADMINISTRADOR, ROLES.SUPERVISOR],
    description: "Bitácora inmutable de eventos y trazabilidad de operaciones del sistema.",
    promptTarget: "PROMPT 14"
  },
  {
    key: "usuarios",
    label: "Usuarios",
    section: "Administración",
    icon: "userCog",
    roles: [ROLES.ADMINISTRADOR],
    description: "Administración de cuentas de personal municipal, asignación de roles y estados.",
    promptTarget: "PROMPT 15"
  },
  {
    key: "configuracion",
    label: "Configuración",
    section: "Administración",
    icon: "settings",
    roles: [ROLES.ADMINISTRADOR, ROLES.FUNCIONARIO, ROLES.SUPERVISOR, ROLES.CIUDADANO],
    description: "Seguridad de la cuenta, actualización de contraseña y parámetros de sesión.",
    promptTarget: "PROMPT 09"
  }
]);
