# API REST del Backend Municipal — Documentación Técnica e Integración para el Frontend

Sistema Web de Gestión de Licencias Municipales — **Municipalidad Distrital de Andrés Avelino Cáceres Dorregaray** (Ayacucho, Perú).

- **Proyecto Maven**: `MUNICIPALIDAD_DE_ANDRES_AVELINO_CACERES`
- **Paquete base**: `pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres`
- **Clase principal**: `MunicipalidadDeAndresAvelinoCaceresApplication`
- **Versión del Backend**: Completado hasta **PROMPT 08** (100 % de los módulos backend).

---

## 1. Módulos Implementados en el Backend

| Prompt | Módulo / Dominio | Controladores Principales |
|---|---|---|
| **01 – 02** | Salud del sistema, modelo de datos relacional y migraciones Flyway (`V1`–`V2`) | `HealthController` |
| **03** | Seguridad con sesiones HTTP, protección CSRF, autenticación, gestión de usuarios, roles y auditoría base | `AuthController`, `UsuarioController`, `RolController` |
| **04** | Gestión de ciudadanos, representantes legales y establecimientos comerciales | `CiudadanoController`, `RepresentanteController`, `EstablecimientoController` |
| **05** | Gestión de tipos de trámite, catálogo de requisitos, solicitudes municipales (expedientes) y documentos adjuntos (`V3`) | `TipoTramiteController`, `RequisitoController`, `SolicitudController`, `DocumentoController` |
| **06** | Inspecciones técnicas municipales, emisión/estados de licencias de funcionamiento, certificados PDF con código QR y verificación pública (`V4`) | `InspeccionController`, `LicenciaController`, `VerificacionLicenciaController` |
| **07** | Gestión de pagos municipales, constancia PDF de pago, notificaciones internas y consulta de auditoría municipal (`V5`) | `PagoController`, `NotificacionController`, `AuditoriaController` |
| **08** | Dashboard administrativo, estadísticas por módulo, actividad reciente y reportes municipales en JSON paginado, PDF y CSV | `DashboardController`, `ReporteController` |

---

## 2. Convenciones Transversales para el Frontend

### 2.1 Autenticación por Sesiones HTTP y Protección CSRF
1. El sistema utiliza sesiones HTTP administradas por Spring Security (`JSESSIONID` con `HttpOnly=true` y `SameSite=Lax`).
2. Antes de invocar cualquier endpoint mutador (`POST`, `PUT`, `PATCH`, `DELETE`), el cliente web debe obtener la cookie `XSRF-TOKEN` llamando a `GET /api/auth/csrf` y enviar su valor decodificado en la cabecera HTTP `X-XSRF-TOKEN`.
3. Todas las peticiones desde el navegador deben incluir `credentials: 'same-origin'`.

### 2.2 Formato Estándar de Paginación (`PaginaResponse<T>`)
Todos los listados paginados aceptan los parámetros de consulta `page` (base `0`, por defecto `0`), `size` (por defecto `20`, máximo `100`) y `sort` (ej. `id,desc`). La respuesta JSON tiene la estructura uniforme:

```json
{
  "contenido": [],
  "pagina": 0,
  "tamanio": 20,
  "totalElementos": 0,
  "totalPaginas": 0
}
```

### 2.3 Formato Estándar de Errores (`ErrorResponse`)
Todos los errores de negocio, validación, seguridad y recursos no encontrados devuelven JSON con la estructura:

```json
{
  "timestamp": "2026-11-12T22:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Descripción clara del error en español",
  "path": "/api/ruta-solicitada",
  "validationErrors": {
    "campo": "Mensaje de validación del campo"
  }
}
```

### 2.4 Códigos de Estado HTTP
- `200 OK`: Consulta, actualización, cambio de estado o descarga exitosa.
- `201 Created`: Creación exitosa de un recurso.
- `204 No Content`: Eliminación lógica exitosa (ej. retiro de documento en borrador).
- `400 Bad Request`: Error de validación (`@Valid`) o regla de negocio (`BusinessException`).
- `401 Unauthorized`: Sin sesión activa, credenciales inválidas o sesión expirada.
- `403 Forbidden`: Sin permisos por rol (`AccessDeniedException`) o token CSRF ausente/inválido.
- `404 Not Found`: Recurso inexistente (`ResourceNotFoundException`).
- `409 Conflict`: Duplicidad, restricción de integridad o estado incompatible (`ConflictException`).

---

## 3. Catálogo Completo de Endpoints por Dominio

### 3.1 Salud y Verificación Pública (`permitAll`)
| Método | Ruta | Roles | Descripción |
|---|---|---|---|
| `GET` | `/api/public/health` | Público | Estado operativo del backend |
| `GET` | `/api/public/licencias/verificar/{token}` | Público | Verificación pública de autenticidad y vigencia de licencia mediante token QR (sin exponer DNI, teléfono ni correo) |

### 3.2 Autenticación y Perfil (`/api/auth`)
| Método | Ruta | Roles | Descripción |
|---|---|---|---|
| `GET` | `/api/auth/csrf` | Público | Emite cookie `XSRF-TOKEN` y devuelve metadatos del token CSRF |
| `POST` | `/api/auth/login` | Público | Autentica usuario (`username`, `password`) y crea sesión HTTP |
| `POST` | `/api/auth/logout` | Autenticado | Registra auditoría, invalida la sesión y limpia cookies |
| `GET` | `/api/auth/me` | Autenticado | Devuelve el perfil del usuario autenticado (`AuthUserResponse`) |
| `POST` | `/api/auth/change-password` | Autenticado | Cambia contraseña (`passwordActual`, `passwordNueva`) e invalida otras sesiones activas |

### 3.3 Usuarios y Roles (`/api/usuarios`, `/api/roles`)
| Método | Ruta | Roles | Descripción |
|---|---|---|---|
| `GET` | `/api/usuarios` | `ADMINISTRADOR` | Listado paginado (`username`, `nombre`, `activo`, `rol`) |
| `GET` | `/api/usuarios/{id}` | `ADMINISTRADOR` | Detalle de usuario |
| `POST` | `/api/usuarios` | `ADMINISTRADOR` | Crea usuario (**201**) |
| `PUT` | `/api/usuarios/{id}` | `ADMINISTRADOR` | Actualiza datos personales de usuario |
| `PATCH` | `/api/usuarios/{id}/estado` | `ADMINISTRADOR` | Activa/desactiva usuario y cierra sus sesiones activas |
| `PUT` | `/api/usuarios/{id}/roles` | `ADMINISTRADOR` | Reemplaza roles de un usuario y cierra sus sesiones |
| `GET` | `/api/roles` | `ADMINISTRADOR` | Lista todos los roles del sistema |
| `GET` | `/api/roles/{id}` | `ADMINISTRADOR` | Detalle de rol por ID |

### 3.4 Ciudadanos, Representantes y Establecimientos (`/api/ciudadanos`, `/api/representantes`, `/api/establecimientos`)
| Método | Ruta | Roles | Descripción |
|---|---|---|---|
| `GET` | `/api/ciudadanos` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Listado paginado (`tipoDocumento`, `numeroDocumento`, `nombres`, `apellidos`, `activo`) |
| `GET` | `/api/ciudadanos/{id}` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR`, `CIUDADANO` (propio) | Detalle de ciudadano |
| `GET` | `/api/ciudadanos/documento` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Búsqueda exacta por `tipoDocumento` y `numeroDocumento` |
| `POST` | `/api/ciudadanos` | `ADMINISTRADOR`, `FUNCIONARIO` | Registra ciudadano (**201**) |
| `PUT` | `/api/ciudadanos/{id}` | `ADMINISTRADOR`, `FUNCIONARIO`, `CIUDADANO` (propio) | Actualiza ciudadano |
| `PATCH` | `/api/ciudadanos/{id}/estado` | `ADMINISTRADOR` | Activación/desactivación lógica |
| `GET` | `/api/representantes` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Listado paginado de representantes |
| `GET` | `/api/representantes/{id}` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Detalle de representante |
| `POST` | `/api/representantes` | `ADMINISTRADOR`, `FUNCIONARIO` | Registra representante (**201**) |
| `PUT` | `/api/representantes/{id}` | `ADMINISTRADOR`, `FUNCIONARIO` | Actualiza representante |
| `PATCH` | `/api/representantes/{id}/estado` | `ADMINISTRADOR` | Activación/desactivación lógica |
| `GET` | `/api/establecimientos` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Listado paginado (`nombreComercial`, `ruc`, `ciudadanoId`, `activo`) |
| `GET` | `/api/establecimientos/{id}` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR`, `CIUDADANO` (propio) | Detalle de establecimiento |
| `GET` | `/api/establecimientos/ruc/{ruc}` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Establecimientos por RUC válido (módulo 11) |
| `GET` | `/api/establecimientos/ciudadano/{ciudadanoId}` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR`, `CIUDADANO` (propio) | Establecimientos de un titular |
| `POST` | `/api/establecimientos` | `ADMINISTRADOR`, `FUNCIONARIO` | Registra establecimiento (**201**) |
| `PUT` | `/api/establecimientos/{id}` | `ADMINISTRADOR`, `FUNCIONARIO`, `CIUDADANO` (propio) | Actualiza establecimiento |
| `PATCH` | `/api/establecimientos/{id}/estado` | `ADMINISTRADOR` | Activación/desactivación lógica |

### 3.5 Tipos de Trámite, Requisitos, Solicitudes y Documentos (`/api/tipos-tramite`, `/api/requisitos`, `/api/solicitudes`, `/api/documentos`)
| Método | Ruta | Roles | Descripción |
|---|---|---|---|
| `GET` | `/api/tipos-tramite` | Todos los autenticados | Listado paginado (`codigo`, `nombre`, `activo`) |
| `GET` | `/api/tipos-tramite/{id}` | Todos los autenticados | Detalle con requisitos asociados |
| `POST` | `/api/tipos-tramite` | `ADMINISTRADOR` | Crea tipo de trámite (**201**) |
| `PUT` | `/api/tipos-tramite/{id}` | `ADMINISTRADOR` | Actualiza tipo de trámite |
| `PATCH` | `/api/tipos-tramite/{id}/estado` | `ADMINISTRADOR` | Activa/desactiva tipo de trámite |
| `GET` | `/api/tipos-tramite/{id}/requisitos` | Todos los autenticados | Requisitos ordenados del trámite |
| `PUT` | `/api/tipos-tramite/{id}/requisitos` | `ADMINISTRADOR` | Asigna/reordena requisitos del trámite |
| `GET` | `/api/requisitos` | Todos los autenticados | Catálogo paginado de requisitos |
| `GET` | `/api/requisitos/{id}` | Todos los autenticados | Detalle de requisito |
| `POST` | `/api/requisitos` | `ADMINISTRADOR` | Crea requisito (**201**) |
| `PUT` | `/api/requisitos/{id}` | `ADMINISTRADOR` | Actualiza requisito |
| `PATCH` | `/api/requisitos/{id}/estado` | `ADMINISTRADOR` | Activa/desactiva requisito |
| `GET` | `/api/solicitudes` | Todos los autenticados (`CIUDADANO` solo propias) | Listado paginado con filtros múltiples |
| `GET` | `/api/solicitudes/{id}` | Todos los autenticados (`CIUDADANO` solo propia) | Detalle completo del expediente |
| `GET` | `/api/solicitudes/expediente/{numero}` | Todos los autenticados (`CIUDADANO` solo propio) | Consulta por número `EXP-YYYY-000001` |
| `POST` | `/api/solicitudes` | `ADMINISTRADOR`, `FUNCIONARIO`, `CIUDADANO` | Crea solicitud en `BORRADOR` (**201**) |
| `PUT` | `/api/solicitudes/{id}` | `ADMINISTRADOR`, `FUNCIONARIO`, `CIUDADANO` (propia) | Edita solicitud en `BORRADOR` u `OBSERVADA` |
| `POST` | `/api/solicitudes/{id}/presentar` | `ADMINISTRADOR`, `FUNCIONARIO`, `CIUDADANO` (propia) | Genera expediente y pasa a `PRESENTADA` |
| `PATCH` | `/api/solicitudes/{id}/asignar` | `ADMINISTRADOR`, `SUPERVISOR` | Asigna funcionario responsable |
| `PATCH` | `/api/solicitudes/{id}/iniciar-revision` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Pasa a `EN_REVISION` |
| `PATCH` | `/api/solicitudes/{id}/observar` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Pasa a `OBSERVADA` con observaciones |
| `PATCH` | `/api/solicitudes/{id}/subsanar` | `ADMINISTRADOR`, `FUNCIONARIO`, `CIUDADANO` (propia) | Retorna de `OBSERVADA` a `EN_REVISION` |
| `PATCH` | `/api/solicitudes/{id}/aprobar` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Pasa a `APROBADA` |
| `PATCH` | `/api/solicitudes/{id}/rechazar` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Pasa a `RECHAZADA` con motivo |
| `PATCH` | `/api/solicitudes/{id}/archivar` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Pasa a `ARCHIVADA` con motivo |
| `GET` | `/api/solicitudes/{id}/historial` | Todos los autenticados (`CIUDADANO` solo propia) | Historial cronológico del expediente |
| `POST` | `/api/solicitudes/{solicitudId}/documentos` | `ADMINISTRADOR`, `FUNCIONARIO`, `CIUDADANO` (propia) | Carga archivo `multipart/form-data` (**201**) |
| `GET` | `/api/solicitudes/{solicitudId}/documentos` | Todos los autenticados (`CIUDADANO` solo propia) | Lista documentos activos del expediente |
| `GET` | `/api/documentos/{id}` | Todos los autenticados (`CIUDADANO` solo propio) | Metadatos del documento |
| `GET` | `/api/documentos/{id}/descargar` | Todos los autenticados (`CIUDADANO` solo propio) | Descarga binaria del archivo |
| `DELETE` | `/api/documentos/{id}` | `ADMINISTRADOR`, `FUNCIONARIO`, `CIUDADANO` (propio) | Retiro lógico en estado `BORRADOR` (**204**) |

### 3.6 Inspecciones y Licencias (`/api/inspecciones`, `/api/licencias`)
| Método | Ruta | Roles | Descripción |
|---|---|---|---|
| `GET` | `/api/inspecciones` | Todos los autenticados (`CIUDADANO` solo propias) | Listado paginado con filtros |
| `GET` | `/api/inspecciones/{id}` | Todos los autenticados (`CIUDADANO` solo propia) | Detalle de inspección |
| `GET` | `/api/inspecciones/solicitud/{solicitudId}` | Todos los autenticados (`CIUDADANO` solo propia) | Inspecciones de un expediente |
| `POST` | `/api/inspecciones` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Programa inspección (**201**) |
| `PUT` | `/api/inspecciones/{id}/reprogramar` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Reprograma inspección `PROGRAMADA` |
| `PATCH` | `/api/inspecciones/{id}/iniciar` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Pasa a `EN_PROCESO` |
| `PATCH` | `/api/inspecciones/{id}/resultado` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Registra resultado y pasa a `FINALIZADA` |
| `PATCH` | `/api/inspecciones/{id}/cancelar` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Cancela inspección con motivo |
| `GET` | `/api/licencias` | Todos los autenticados (`CIUDADANO` solo propias) | Listado paginado de licencias |
| `GET` | `/api/licencias/{id}` | Todos los autenticados (`CIUDADANO` solo propia) | Detalle de licencia |
| `GET` | `/api/licencias/codigo/{codigo}` | Todos los autenticados (`CIUDADANO` solo propia) | Búsqueda por `LIC-YYYY-000001` |
| `GET` | `/api/licencias/solicitud/{solicitudId}` | Todos los autenticados (`CIUDADANO` solo propia) | Licencia asociada a un expediente |
| `POST` | `/api/licencias/emitir` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Emite licencia (**201**) |
| `PATCH` | `/api/licencias/{id}/suspender` | `ADMINISTRADOR`, `SUPERVISOR` | Suspende licencia `VIGENTE` |
| `PATCH` | `/api/licencias/{id}/reactivar` | `ADMINISTRADOR`, `SUPERVISOR` | Reactiva licencia `SUSPENDIDA` |
| `PATCH` | `/api/licencias/{id}/revocar` | `ADMINISTRADOR`, `SUPERVISOR` | Revoca licencia |
| `GET` | `/api/licencias/{id}/certificado` | Todos los autenticados (`CIUDADANO` solo propia) | Descarga certificado PDF con QR |
| `GET` | `/api/licencias/{id}/historial` | Todos los autenticados (`CIUDADANO` solo propia) | Historial de estados de la licencia |

### 3.7 Pagos, Notificaciones y Auditoría (`/api/pagos`, `/api/notificaciones`, `/api/auditorias`)
| Método | Ruta | Roles | Descripción |
|---|---|---|---|
| `GET` | `/api/pagos` | Todos los autenticados (`CIUDADANO` solo propios) | Listado paginado de pagos |
| `GET` | `/api/pagos/{id}` | Todos los autenticados (`CIUDADANO` solo propio) | Detalle con historial de estados |
| `GET` | `/api/pagos/solicitud/{solicitudId}` | Todos los autenticados (`CIUDADANO` solo propia) | Pagos de un expediente |
| `POST` | `/api/pagos` | `ADMINISTRADOR`, `FUNCIONARIO`, `CIUDADANO` (propia) | Registra pago en `PENDIENTE_VERIFICACION` (**201**) |
| `PATCH` | `/api/pagos/{id}/verificar` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Verifica pago (`VERIFICADO`) |
| `PATCH` | `/api/pagos/{id}/rechazar` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Rechaza pago (`RECHAZADO`) |
| `PATCH` | `/api/pagos/{id}/anular` | `ADMINISTRADOR`, `SUPERVISOR`, `FUNCIONARIO` (solo pendientes) | Anula pago (`ANULADO`) |
| `GET` | `/api/pagos/{id}/constancia` | Todos los autenticados (`CIUDADANO` solo propio) | Descarga constancia PDF de pago |
| `GET` | `/api/pagos/resumen` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Resumen financiero consolidado |
| `GET` | `/api/notificaciones` | Todos los autenticados (propias) | Notificaciones paginadas del usuario |
| `GET` | `/api/notificaciones/no-leidas` | Todos los autenticados (propias) | Notificaciones no leídas |
| `GET` | `/api/notificaciones/contador` | Todos los autenticados (propio) | Contador `{"noLeidas": N}` |
| `GET` | `/api/notificaciones/{id}` | Todos los autenticados (propia) | Detalle de notificación |
| `PATCH` | `/api/notificaciones/{id}/leer` | Todos los autenticados (propia) | Marca notificación como leída |
| `PATCH` | `/api/notificaciones/leer-todas` | Todos los autenticados (propias) | Marca todas como leídas |
| `POST` | `/api/notificaciones` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Crea notificación administrativa (**201**) |
| `GET` | `/api/auditorias` | `ADMINISTRADOR`, `SUPERVISOR` | Consulta paginada inmutable de auditorías |
| `GET` | `/api/auditorias/{id}` | `ADMINISTRADOR`, `SUPERVISOR` | Detalle de evento de auditoría |

### 3.8 Dashboard y Reportes Municipales (`/api/dashboard`, `/api/reportes`)
| Método | Ruta | Roles | Descripción |
|---|---|---|---|
| `GET` | `/api/dashboard/resumen` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Indicadores globales (`ciudadanos`, `establecimientos`, `solicitudes`, `licencias`, `inspecciones`, `pagos.montoVerificadoPEN`) |
| `GET` | `/api/dashboard/estadisticas` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Estadísticas consolidadas (`fechaDesde`, `fechaHasta`) |
| `GET` | `/api/dashboard/solicitudes/estadisticas` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Estadísticas de solicitudes (`fechaDesde`, `fechaHasta`, `tipoTramiteId`, `estado`) |
| `GET` | `/api/dashboard/licencias/estadisticas` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Estadísticas de licencias (`fechaDesde`, `fechaHasta`) |
| `GET` | `/api/dashboard/inspecciones/estadisticas` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Estadísticas de inspecciones (`fechaDesde`, `fechaHasta`) |
| `GET` | `/api/dashboard/pagos/estadisticas` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Estadísticas financieras en `PEN` (`BigDecimal`) |
| `GET` | `/api/dashboard/actividad-reciente` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Últimos eventos (`limite`, máx. `100`) |
| `GET` | `/api/reportes/solicitudes` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Reporte paginado JSON de solicitudes |
| `GET` | `/api/reportes/licencias` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Reporte paginado JSON de licencias |
| `GET` | `/api/reportes/inspecciones` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Reporte paginado JSON de inspecciones |
| `GET` | `/api/reportes/pagos` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Reporte paginado JSON de pagos |
| `GET` | `/api/reportes/establecimientos` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Reporte paginado JSON de establecimientos |
| `GET` | `/api/reportes/ciudadanos` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Reporte paginado JSON de ciudadanos |
| `GET` | `/api/reportes/{modulo}/pdf` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Descarga PDF horizontal A4 (`solicitudes`, `licencias`, `inspecciones`, `pagos`, `establecimientos`) |
| `GET` | `/api/reportes/{modulo}/csv` | `ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR` | Descarga CSV UTF-8 seguro (`solicitudes`, `licencias`, `inspecciones`, `pagos`, `establecimientos`) |

---

## 4. Flujo Administrativo Completo del Sistema

1. **Registro de Administrados y Locales**: El funcionario o administrador registra al `Ciudadano`, opcionalmente a su `Representante`, y su `Establecimiento` comercial (`POST /api/ciudadanos`, `POST /api/establecimientos`).
2. **Configuración de Trámites**: El administrador define el `TipoTramite` (indicando si `requiereInspeccion = true/false`) y su lista de `Requisito` (`POST /api/tipos-tramite`, `PUT /api/tipos-tramite/{id}/requisitos`).
3. **Apertura y Carga Documental del Expediente**: Se crea la `Solicitud` en `BORRADOR` (`POST /api/solicitudes`), se cargan los documentos obligatorios (`POST /api/solicitudes/{id}/documentos`) y se presenta (`POST /api/solicitudes/{id}/presentar`), generando el número único `EXP-YYYY-000001` y congelando la instantánea de requisitos.
4. **Registro y Verificación del Pago Municipal**: Se registra el pago de la tasa administrativa (`POST /api/pagos`), un funcionario o supervisor lo verifica (`PATCH /api/pagos/{id}/verificar`) y se habilita la descarga de la constancia PDF (`GET /api/pagos/{id}/constancia`).
5. **Revisión e Inspección Técnica**: Se asigna el expediente (`PATCH /api/solicitudes/{id}/asignar`), se inicia la revisión (`PATCH /api/solicitudes/{id}/iniciar-revision`) y, cuando el trámite lo exige, se programa, inicia y registra el resultado `FAVORABLE` de la inspección (`POST /api/inspecciones`, `PATCH /api/inspecciones/{id}/iniciar`, `PATCH /api/inspecciones/{id}/resultado`), tras lo cual se aprueba el expediente (`PATCH /api/solicitudes/{id}/aprobar`).
6. **Emisión de Licencia y Verificación Pública QR**: Se emite la licencia (`POST /api/licencias/emitir`) con código `LIC-YYYY-000001`, se descarga el certificado PDF institucional (`GET /api/licencias/{id}/certificado`) y cualquier ciudadano o fiscalizador puede verificar su autenticidad y vigencia mediante el QR en `GET /api/public/licencias/verificar/{token}`.
7. **Supervisión Ejecutiva y Reportes**: El personal autorizado consulta las métricas en tiempo real en `/api/dashboard/**` y exporta reportes tabulares en PDF o CSV desde `/api/reportes/**`.
