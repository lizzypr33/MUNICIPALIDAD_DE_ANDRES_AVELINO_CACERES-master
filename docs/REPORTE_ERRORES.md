# REPORTE DE AUDITORÍA, ERRORES Y CORRECCIONES TÉCNICAS
## Sistema Web de Gestión de Licencias Municipales
### Municipalidad Distrital de Andrés Avelino Cáceres Dorregaray — Ayacucho, Perú

**Fecha de Auditoría:** 13 de Noviembre de 2026  
**Fase:** PROMPT 15 DE 15 — Auditoría Técnica Integral y Preparación para Producción

---

## 1. RESUMEN DE HALLAZGOS Y ESTADO DE RESOLUCIÓN

Durante el proceso de auditoría técnica, inspección de código estático, revisión de seguridad OWASP y validación cruzada de contratos frontend-backend, se identificaron y subsanaron los siguientes eventos:

| ID | Categoría | Severidad | Archivo / Componente Afectado | Estado |
|:---:|:---:|:---:|:---|:---:|
| **ERR-SEC-01** | Seguridad / Autorización | **Alta** | `SecurityConfig.java` | **RESUELTO** |
| **ERR-INT-02** | Integración / Navegación | **Media** | `assets/js/main.js` | **RESUELTO** |
| **ERR-CON-03** | Coherencia de Contratos | **Media** | `assets/js/reportes/reportes.js` | **RESUELTO** |
| **ERR-ENV-04** | Entorno / Construcción | **Baja** | `pom.xml` / `mvnw.cmd` | **RESUELTO** |
| **OBS-DAT-05** | Persistencia Externa | **Informativa** | `PersistenciaTests.java` | **DOCUMENTADO** |

---

## 2. DETALLE DE INCIDENCIAS Y ACCIONES CORRECTIVAS

### INCIDENCIA ERR-SEC-01: Control de Acceso por Rol en Páginas HTML Administrativas Privadas
* **Descripción:** Se detectó que las rutas estáticas de páginas privadas recientemente incorporadas (`/pages/auditoria.html`, `/pages/reportes.html`) caían bajo la regla genérica `.requestMatchers(HttpMethod.GET, "/pages/**").authenticated()`, permitiendo que usuarios con rol `CIUDADANO` pudieran descargar el markup HTML del módulo de auditoría, aunque los endpoints REST estuviesen protegidos.
* **Severidad:** **Alta** (Violación de principio de defensa en profundidad).
* **Causa Raíz:** Ausencia de matchers explícitos por rol para las vistas HTML antes de la regla comodín `.authenticated()`.
* **Corrección Implementada:** Se modificó [SecurityConfig.java](file:///C:/Users/ASUS/Desktop/MUNICASERES/MUNICIPALIDAD_DE_ANDRES_AVELINO_CACERES/src/main/java/pe/gob/municipalidad/municipalidad_de_andres_avelino_caceres/config/SecurityConfig.java) incorporando reglas granulares:
  * `/pages/auditoria.html` y `/pages/auditoria-detalle.html`: restringidos exclusivamente a `ADMINISTRADOR` y `SUPERVISOR`.
  * `/pages/reportes.html` y `/pages/reporte-detalle.html`: restringidos a `ADMINISTRADOR`, `FUNCIONARIO` y `SUPERVISOR`.
* **Prueba de Verificación:** En `FrontendStaticResourcesTests.java`, se agregaron pruebas MockMvc con credenciales de `CIUDADANO` y `FUNCIONARIO` verificando la respuesta `403 FORBIDDEN`.
* **Estado:** **RESUELTO Y VERIFICADO.**

---

### INCIDENCIA ERR-INT-02: Preservación de Parámetros de Consulta en Despachador de Módulos
* **Descripción:** Al navegar directamente a una URL de detalle mediante query params (ej. `/pages/detalle-licencia.html?id=5` o `/pages/reportes.html?tipo=licencias`), el despachador `data-page="module-redirect"` de `main.js` únicamente tomaba el atributo `data-subview`, descartando la cadena de búsqueda (`window.location.search`).
* **Severidad:** **Media** (Pérdida de contexto en enlaces directos y marcadores del navegador).
* **Causa Raíz:** Construcción rígida del hash de redirección sin concatenar los parámetros GET.
* **Corrección Implementada:** En [assets/js/main.js](file:///C:/Users/ASUS/Desktop/MUNICASERES/MUNICIPALIDAD_DE_ANDRES_AVELINO_CACERES/src/main/resources/static/assets/js/main.js), se implementó la extracción y combinación dinámica de `subview` con `window.location.search`, generando hashes completos (ej. `#licencias?subview=detalle&id=5`).
* **Prueba de Verificación:** Verificación funcional en navegación y pruebas unitarias de enrutamiento.
* **Estado:** **RESUELTO Y VERIFICADO.**

---

### INCIDENCIA ERR-CON-03: Botones de Exportación PDF/CSV en Reportes de Ciudadanos
* **Descripción:** En la vista de reportes, al seleccionar la pestaña "Padrón de Ciudadanos", la interfaz presentaba botones de exportación PDF y CSV que provocaban un error 404/405 en el servidor, debido a que `ReporteController.java` omite intencionalmente dichos endpoints para evitar la descarga masiva de datos personales conforme a la Ley N.º 29733.
* **Severidad:** **Media** (Inconsistencia de contrato y mala experiencia de usuario).
* **Causa Raíz:** Asunción genérica de que los 6 tipos de reportes contaban con endpoints binarios de exportación.
* **Corrección Implementada:** En [assets/js/reportes/reportes.js](file:///C:/Users/ASUS/Desktop/MUNICASERES/MUNICIPALIDAD_DE_ANDRES_AVELINO_CACERES/src/main/resources/static/assets/js/reportes/reportes.js), se acondicionó la función `mountFilters()` para ocultar automáticamente los botones de exportación cuando `activeTab === "ciudadanos"`.
* **Prueba de Verificación:** Prueba manual e inspección estática del DOM.
* **Estado:** **RESUELTO Y VERIFICADO.**

---

### INCIDENCIA ERR-ENV-04: Bloqueo de Recurso `application.properties` en Entornos Windows
* **Descripción:** Durante la ejecución de `mvn resources:resources` en modo sandbox de Windows, el plugin de recursos fallaba con `AccessDeniedException` al intentar sobrescribir `target\classes\application.properties` mientras existía un bloqueo de archivo por el sistema operativo.
* **Severidad:** **Baja** (Específica del entorno de compilación de pruebas de Windows).
* **Causa Raíz:** Bloqueo de archivos por subprocesos Java en el entorno de desarrollo local.
* **Corrección Implementada:** Ejecución estricta con parámetros limpios y manejo de permisos de ejecución sin alterar el código fuente ni relajar la configuración del proyecto.
* **Prueba de Verificación:** Ejecución de `mvnw.cmd package` finalizando con `BUILD SUCCESS`.
* **Estado:** **RESUELTO Y VERIFICADO.**

---

### OBSERVACIÓN OBS-DAT-05: Estado de Conectividad con Instancia PostgreSQL Viva (Puerto 5433)
* **Descripción:** La suite de pruebas de integración transaccionales completas (`PersistenciaTests`, `CiudadanoEstablecimientoIntegrationTests`, `SolicitudDocumentoIntegrationTests`, etc.) requiere una base de datos PostgreSQL real escuchando en el puerto local `5433` con la base `andres_test`. Al sondear el puerto en el entorno actual de desarrollo, la conexión TCP no tuvo éxito (`TcpTestSucceeded: False`).
* **Severidad:** **Informativa / Condición de Entorno**.
* **Tratamiento Técnico:** Siguiendo las directrices expresas de la Sección 7 y 25 de PROMPT 15 (*"Si PostgreSQL no está disponible, registrar la prueba como NO EJECUTADA y explicar la causa. No afirmar que la base de datos funciona sin conectarse"*), dichas 5 pruebas de persistencia se clasificaron objetivamente como **NO EJECUTADAS**.
* **Estado:** **DOCUMENTADO Y CONFORME.**
