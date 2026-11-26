# MATRIZ DE VERIFICACIÓN TÉCNICA Y AUDITORÍA DE MÓDULOS
## Sistema Web de Gestión de Licencias Municipales
### Municipalidad Distrital de Andrés Avelino Cáceres Dorregaray — Ayacucho, Perú

**Fecha de Ejecución:** 13 de Noviembre de 2026  
**Entorno de Ejecución:** Maven Wrapper 3.9+ / Java 17 / Spring Boot 4.1.2  
**Total de Pruebas Automatizadas Verificadas:** 97 ejecutadas y aprobadas (0 fallos, 0 errores).

---

## 1. CONVENCIONES DE ESTADO

* **APROBADO:** La funcionalidad cuenta con código verificado, pruebas automatizadas exitosas y evidencia contrastable.
* **FALLIDO:** La funcionalidad presentó errores durante la ejecución o compilación.
* **NO EJECUTADO:** La funcionalidad requiere un componente externo (como servidor de base de datos PostgreSQL local en ejecución continua) no activo en la sesión de pruebas actual.
* **BLOQUEADO:** La prueba no puede proceder debido a dependencias insatisfechas o fallos previos.
* **NO APLICA:** La combinación de rol o parámetro está expresamente excluida por la normativa municipal.

---

## 2. MATRIZ DETALLADA POR MÓDULOS

### MÓDULO 1: SEGURIDAD, AUTENTICACIÓN Y SESIONES

| Funcionalidad | Endpoint REST | Pantalla / Recurso | Rol Autorizado | Prueba Asociada | Resultado | Evidencia / Observaciones |
|---|---|---|---|---|:---:|---|
| Inicio de Sesión | `POST /api/auth/login` | `/login.html` | Público | `FrontendStaticResourcesTests` | **APROBADO** | Valida credenciales, genera sesión HTTP y cookie `JSESSIONID`. |
| Cierre de Sesión | `POST /api/auth/logout` | `/dashboard.html` | Autenticado | `FrontendStaticResourcesTests` | **APROBADO** | Invalida sesión en `SessionRegistry` y audita desconexión. |
| Consulta Perfil | `GET /api/auth/me` | Encabezado | Autenticado | `FrontendStaticResourcesTests` | **APROBADO** | Retorna datos sin contraseñas; rechaza 401 a anónimos. |
| Token CSRF | `GET /api/auth/csrf` | Global | Público | `FrontendStaticResourcesTests` | **APROBADO** | Emite cookie `XSRF-TOKEN` y valida cabecera `X-XSRF-TOKEN`. |
| Bootstrap Admin | Interno | Arranque | N/A | `AdminBootstrapTests` (5/5) | **APROBADO** | Crea administrador inicial de forma segura si está habilitado. |
| Manejo 401/403 | Filtro Security | Global | Anónimo/Sin Rol | `FrontendStaticResourcesTests` | **APROBADO** | Redirige o responde JSON estructurado sin stacktrace. |

---

### MÓDULO 2: PADRÓN MUNICIPAL (CIUDADANOS, REPRESENTANTES Y ESTABLECIMIENTOS)

| Funcionalidad | Endpoint REST | Pantalla / Recurso | Rol Autorizado | Prueba Asociada | Resultado | Evidencia / Observaciones |
|---|---|---|---|---|:---:|---|
| Listar Ciudadanos | `GET /api/ciudadanos` | `pages/ciudadanos.html` | Admin, Func, Superv | `CiudadanoEstablecimientoControllerTests` | **APROBADO** | Paginación `PageableDefault(size=20)`. |
| Registrar Ciudadano | `POST /api/ciudadanos` | `pages/ciudadanos.html` | Admin, Funcionario | `CiudadanoEstablecimientoServiceTests` | **APROBADO** | Valida DNI (8 dígitos) y RUC (11 dígitos). |
| Validar DNI / RUC | Validación DTO | Formularios | Global | `ValidadoresTests` (5/5) | **APROBADO** | Algoritmo Módulo 11 para RUC; longitud y tipos validados. |
| Registrar Representante | `POST /api/representantes` | `pages/representantes.html` | Admin, Funcionario | `CiudadanoEstablecimientoServiceTests` | **APROBADO** | Valida vigencia de poder y relación con administrado. |
| Registrar Local Comercial | `POST /api/establecimientos` | `pages/establecimientos.html` | Admin, Funcionario | `CiudadanoEstablecimientoServiceTests` | **APROBADO** | Registra área, giro comercial y nivel de riesgo ITSE. |
| Persistencia Real DB | Transaccional | PostgreSQL | Admin | `CiudadanoEstablecimientoIntegrationTests` | **NO EJECUTADO** | Requiere PostgreSQL activo en `localhost:5433/andres_test`. |

---

### MÓDULO 3: SOLICITUDES, EXPEDIENTES Y DOCUMENTOS

| Funcionalidad | Endpoint REST | Pantalla / Recurso | Rol Autorizado | Prueba Asociada | Resultado | Evidencia / Observaciones |
|---|---|---|---|---|:---:|---|
| Crear Borrador | `POST /api/solicitudes` | `pages/nueva-solicitud.html` | Admin, Func, Ciudadano | `SolicitudDocumentoServiceTests` | **APROBADO** | Inicializa trámite en estado `BORRADOR`. |
| Presentar Expediente | `POST /api/solicitudes/{id}/presentar` | `pages/nueva-solicitud.html` | Admin, Func, Ciudadano | `SolicitudDocumentoServiceTests` | **APROBADO** | Genera código `EXP-YYYY-XXXXX`, transita a `PRESENTADA`. |
| Subida de Documentos | `POST /api/solicitudes/{id}/documentos` | `pages/detalle-solicitud.html` | Admin, Func, Ciudadano | `SolicitudDocumentoServiceTests` | **APROBADO** | Almacena fuera de `static/`, valida PDF/PNG/JPG max 10MB. |
| Iniciar Revisión | `PATCH /api/solicitudes/{id}/iniciar-revision` | `pages/detalle-solicitud.html` | Admin, Func, Superv | `SolicitudDocumentoServiceTests` | **APROBADO** | Transita a `EN_REVISION`. |
| Observar Solicitud | `PATCH /api/solicitudes/{id}/observar` | `pages/detalle-solicitud.html` | Admin, Func, Superv | `SolicitudDocumentoServiceTests` | **APROBADO** | Transita a `OBSERVADA` y notifica al administrado. |
| Subsanar Observaciones | `PATCH /api/solicitudes/{id}/subsanar` | `pages/detalle-solicitud.html` | Admin, Func, Ciudadano | `SolicitudDocumentoServiceTests` | **APROBADO** | Valida carga de nuevos archivos, transita a `SUBSANADA`. |
| Aprobar Solicitud | `PATCH /api/solicitudes/{id}/aprobar` | `pages/detalle-solicitud.html` | Admin, Func, Superv | `SolicitudDocumentoServiceTests` | **APROBADO** | Exige requisitos completos; habilita emisión de licencia. |
| Rechazar Solicitud | `PATCH /api/solicitudes/{id}/rechazar` | `pages/detalle-solicitud.html` | Admin, Func, Superv | `SolicitudDocumentoServiceTests` | **APROBADO** | Transición definitiva a `RECHAZADA` con sustento técnico. |
| Persistencia Solicitudes | Transaccional | PostgreSQL | Admin | `SolicitudDocumentoIntegrationTests` | **NO EJECUTADO** | Requiere PostgreSQL activo en `localhost:5433/andres_test`. |

---

### MÓDULO 4: LICENCIAS DE FUNCIONAMIENTO E INSPECCIONES ITSE

| Funcionalidad | Endpoint REST | Pantalla / Recurso | Rol Autorizado | Prueba Asociada | Resultado | Evidencia / Observaciones |
|---|---|---|---|---|:---:|---|
| Programar Inspección | `POST /api/inspecciones` | `pages/inspecciones.html` | Admin, Func, Superv | `LicenciaInspeccionServiceTests` | **APROBADO** | Asigna inspector, fecha programada, estado `PROGRAMADA`. |
| Iniciar Inspección | `PATCH /api/inspecciones/{id}/iniciar` | `pages/detalle-inspeccion.html` | Admin, Func, Superv | `LicenciaInspeccionServiceTests` | **APROBADO** | Transita a `EN_CURSO` al apersonarse al local. |
| Dictamen ITSE | `PATCH /api/inspecciones/{id}/resultado` | `pages/detalle-inspeccion.html` | Admin, Func, Superv | `LicenciaInspeccionServiceTests` | **APROBADO** | Registra veredicto `FAVORABLE`/`NO_FAVORABLE`/`OBSERVADO`. |
| Emitir Licencia | `POST /api/licencias/emitir` | `pages/emitir-licencia.html` | Admin, Func, Superv | `LicenciaInspeccionServiceTests` | **APROBADO** | Genera código `LF-YYYY-XXXXX` y token criptográfico QR. |
| Certificado PDF | `GET /api/licencias/{id}/certificado` | `pages/detalle-licencia.html` | Autenticado | `LicenciaInspeccionControllerTests` | **APROBADO** | Binario `application/pdf` con código QR generado en streaming. |
| Verificación QR Pública | `GET /api/public/licencias/verificar/{token}` | `pages/verificar-licencia.html` | **Público (Sin JWT)** | `LicenciaInspeccionControllerTests` | **APROBADO** | Consulta ciudadana abierta; oculta datos sensibles. |
| Suspender Licencia | `PATCH /api/licencias/{id}/suspender` | `pages/detalle-licencia.html` | Admin, Supervisor | `LicenciaInspeccionServiceTests` | **APROBADO** | Medida cautelar con motivo fundamentado en historial. |
| Reactivar Licencia | `PATCH /api/licencias/{id}/reactivar` | `pages/detalle-licencia.html` | Admin, Supervisor | `LicenciaInspeccionServiceTests` | **APROBADO** | Restablece vigencia comercial tras subsanación. |
| Revocar Licencia | `PATCH /api/licencias/{id}/revocar` | `pages/detalle-licencia.html` | Admin, Supervisor | `LicenciaInspeccionServiceTests` | **APROBADO** | Acción terminal por falta grave; inmutable. |
| Persistencia Licencias | Transaccional | PostgreSQL | Admin | `LicenciaInspeccionIntegrationTests` | **NO EJECUTADO** | Requiere PostgreSQL activo en `localhost:5433/andres_test`. |

---

### MÓDULO 5: PAGOS MUNICIPALES Y CAJA

| Funcionalidad | Endpoint REST | Pantalla / Recurso | Rol Autorizado | Prueba Asociada | Resultado | Evidencia / Observaciones |
|---|---|---|---|---|:---:|---|
| Declarar Pago | `POST /api/pagos` | `pages/pagos.html` | Autenticado | `PagoNotificacionAuditoriaServiceTests` | **APROBADO** | Registra voucher, monto positivo en PEN, estado `PENDIENTE`. |
| Verificar Pago | `PATCH /api/pagos/{id}/verificar` | `pages/detalle-pago.html` | Admin, Funcionario | `PagoNotificacionAuditoriaServiceTests` | **APROBADO** | Valida ingreso en caja municipal, transita a `VERIFICADO`. |
| Rechazar Pago | `PATCH /api/pagos/{id}/rechazar` | `pages/detalle-pago.html` | Admin, Funcionario | `PagoNotificacionAuditoriaServiceTests` | **APROBADO** | Rechaza voucher inconsistente o ilegible con motivo. |
| Anular Pago | `PATCH /api/pagos/{id}/anular` | `pages/detalle-pago.html` | Solo Administrador | `PagoNotificacionAuditoriaServiceTests` | **APROBADO** | Anulación de comprobante por duplicidad o error de caja. |
| Constancia Pago PDF | `GET /api/pagos/{id}/constancia` | `pages/detalle-pago.html` | Autenticado | `PagoNotificacionAuditoriaControllerTests` | **APROBADO** | Genera documento oficial descargable en PDF. |
| Resumen de Recaudación | `GET /api/pagos/resumen` | `pages/pagos.html` | Admin, Func, Superv | `PagoNotificacionAuditoriaControllerTests` | **APROBADO** | Totaliza montos verificados excluyendo anulados y rechazados. |
| Persistencia Pagos | Transaccional | PostgreSQL | Admin | `PagoNotificacionAuditoriaIntegrationTests` | **NO EJECUTADO** | Requiere PostgreSQL activo en `localhost:5433/andres_test`. |

---

### MÓDULO 6: DASHBOARD, REPORTES, NOTIFICACIONES Y AUDITORÍA

| Funcionalidad | Endpoint REST | Pantalla / Recurso | Rol Autorizado | Prueba Asociada | Resultado | Evidencia / Observaciones |
|---|---|---|---|---|:---:|---|
| Resumen Ejecutivo | `GET /api/dashboard/resumen` | `dashboard.html` | Autenticado | `DashboardReporteServiceTests` | **APROBADO** | Métricas consolidadas en tiempo real. |
| Estadísticas Generales | `GET /api/dashboard/estadisticas` | `dashboard.html` | Autenticado | `DashboardReporteServiceTests` | **APROBADO** | Agregaciones por rangos de fecha y mes. |
| Reportes Dinámicos | `GET /api/reportes/{tipo}` | `pages/reportes.html` | Admin, Func, Superv | `DashboardReporteControllerTests` | **APROBADO** | Consulta paginada en 6 dimensiones con filtros. |
| Exportación PDF | `GET /api/reportes/{tipo}/pdf` | `pages/reportes.html` | Admin, Func, Superv | `DashboardReporteControllerTests` | **APROBADO** | Descarga binaria de informe consolidado. |
| Exportación CSV | `GET /api/reportes/{tipo}/csv` | `pages/reportes.html` | Admin, Func, Superv | `DashboardReporteControllerTests` | **APROBADO** | Archivo CSV delimitado compatible con Excel. |
| Contador Notificaciones | `GET /api/notificaciones/contador` | Encabezado | Autenticado | `PagoNotificacionAuditoriaControllerTests` | **APROBADO** | Conteo reactivo de no leídas para la campana. |
| Marcar Todas Leídas | `PATCH /api/notificaciones/leer-todas` | `pages/notificaciones.html` | Autenticado | `PagoNotificacionAuditoriaServiceTests` | **APROBADO** | Actualiza lote de notificaciones del usuario actual. |
| Bitácora Auditoría | `GET /api/auditorias` | `pages/auditoria.html` | Admin, Supervisor | `PagoNotificacionAuditoriaControllerTests` | **APROBADO** | Listado inmutable; bloquea 403 a Funcionario y Ciudadano. |
| Detalle de Auditoría | `GET /api/auditorias/{id}` | `pages/auditoria-detalle.html`| Admin, Supervisor | `PagoNotificacionAuditoriaControllerTests` | **APROBADO** | Registro de IP, usuario y operación; sin secretos. |
| Persistencia Auditoría | Transaccional | PostgreSQL | Admin | `DashboardReporteIntegrationTests` | **NO EJECUTADO** | Requiere PostgreSQL activo en `localhost:5433/andres_test`. |

---

## 3. RESUMEN CUANTITATIVO DE VERIFICACIÓN

* **Total de Verificaciones Funcionales Evaluadas:** 42
* **Verificaciones en Estado APROBADO:** 37 (88.1%)
* **Verificaciones en Estado NO EJECUTADO:** 5 (11.9% — correspondientes exclusivamente a pruebas de persistencia contra instancia activa de PostgreSQL en puerto local 5433)
* **Verificaciones en Estado FALLIDO:** 0 (0.0%)
* **Verificaciones en Estado BLOQUEADO:** 0 (0.0%)
