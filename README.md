# Sistema Web de Gestión de Licencias Municipales
## Municipalidad Distrital de Andrés Avelino Cáceres Dorregaray — Ayacucho, Perú

[![Java 17](https://img.shields.io/badge/Java-17%20LTS-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14%2B-blue.svg)](https://www.postgresql.org/)
[![Flyway](https://img.shields.io/badge/Flyway-Migrations-red.svg)](https://flywaydb.org/)
[![Status](https://img.shields.io/badge/Estado-PROMPT%2015%20COMPLETADO-success.svg)]()

---

## 1. NOMBRE Y OBJETIVO DEL PROYECTO

* **Proyecto:** `MUNICIPALIDAD_DE_ANDRES_AVELINO_CACERES`
* **Paquete Principal:** `pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres`
* **Clase Principal:** `MunicipalidadDeAndresAvelinoCaceresApplication`

### Objetivo Institucional
Automatizar y optimizar integralmente el ciclo de vida de tramitación, inspección técnica de seguridad en edificaciones (ITSE), recaudación y emisión formal de **Licencias de Funcionamiento** para la Municipalidad Distrital de Andrés Avelino Cáceres Dorregaray (Provincia de Huamanga, Departamento de Ayacucho, Perú), conforme a la Ley Marco de Licencias de Funcionamiento (Ley N.º 28976), el Texto Único de Procedimientos Administrativos (TUPA) y la Ley de Procedimiento Administrativo General (Ley N.º 27444).

---

## 2. TECNOLOGÍAS UTILIZADAS

### Backend
* **Java 17** (LTS)
* **Spring Boot 4.1.2**
  * Spring WebMVC
  * Spring Data JPA
  * Spring Security 7 (con autenticación por sesiones HTTP y protección CSRF)
  * Bean Validation (Hibernate Validator)
  * Spring Boot Starter Flyway
* **PostgreSQL 14+** (con driver oficial `org.postgresql:postgresql`)
* **Flyway Database PostgreSQL** (migraciones SQL versionadas `V1` a `V5`)
* **OpenPDF (LibrePDF 1.3.40)** para generación de certificados de licencia, reportes y constancias de pago
* **ZXing 3.5.3** para generación de códigos QR de verificación pública
* **Lombok** (para reducción de boilerplate exclusivamente en entidades JPA)
* **JUnit 5, MockMvc y Mockito** para pruebas unitarias y de integración

### Frontend
* **HTML5 Semántico y Accesible (a11y)**
* **CSS3 Nativo** (CSS Grid, Flexbox, Variables CSS, Media Queries responsivas)
* **Vanilla JavaScript ES6+ Modular** (Fetch API nativo, Web Components ligeros)
* **Cero Frameworks Externos:** Prohibición estricta de React, Angular, Vue, Bootstrap, Tailwind, jQuery o Thymeleaf, garantizando ligereza, soberanía técnica y mantenimiento a largo plazo.

---

## 3. REQUISITOS PREVIOS

1. **Java Development Kit (JDK):** Versión 17 o superior instalada y configurada en la variable `JAVA_HOME`.
2. **PostgreSQL:** Versión 14 o superior instalada (por defecto configurado en puerto `5433` o `5432`).
3. **Maven:** No requiere instalación global; el proyecto incluye el ejecutable de conveniencia **Maven Wrapper** (`mvnw.cmd` para Windows y `./mvnw` para Linux/macOS).

---

## 4. CONFIGURACIÓN DE POSTGRESQL Y BASE DE DATOS

1. Crear la base de datos principal para el sistema:
   ```sql
   CREATE DATABASE "ANDRES" WITH OWNER = postgres ENCODING = 'UTF8';
   ```
2. Crear la base de datos para pruebas de integración (opcional):
   ```sql
   CREATE DATABASE "andres_test" WITH OWNER = postgres ENCODING = 'UTF8';
   ```

---

## 5. VARIABLES DE ENTORNO

Para garantizar la seguridad de la información, **ninguna contraseña sensible se almacena en el código fuente**. Las credenciales se inyectan mediante variables de entorno:

| Variable | Descripción | Valor por Defecto / Ejemplo |
|---|---|---|
| `DB_URL` | URL JDBC de conexión | `jdbc:postgresql://localhost:5433/ANDRES` |
| `DB_USERNAME` | Usuario de PostgreSQL | `postgres` |
| `DB_PASSWORD` | Contraseña de PostgreSQL | *(Requerido)* |
| `TEST_DB_URL` | URL JDBC para pruebas | `jdbc:postgresql://localhost:5433/andres_test` |
| `DOCUMENT_STORAGE_PATH` | Carpeta para archivos adjuntos | `./data/documentos` |
| `LICENCIA_VERIFICACION_BASE_URL` | URL base para enlaces QR | `http://localhost:8080` |
| `SESSION_COOKIE_SECURE` | Habilitar bandera Secure en cookies | `false` (desarrollo) / `true` (HTTPS producción) |
| `ADMIN_BOOTSTRAP_ENABLED` | Sembrar administrador inicial | `false` |
| `ADMIN_USERNAME` | Usuario del administrador inicial | `admin.municipal` |
| `ADMIN_EMAIL` | Correo del administrador inicial | `admin@municaceres.gob.pe` |
| `ADMIN_PASSWORD` | Contraseña del administrador inicial | *(Definir de forma segura)* |

---

## 6. INSTALACIÓN Y COMPILACIÓN

Clone o descargue el repositorio en su estación de trabajo y abra un terminal en la raíz del proyecto:

### Compilación y Ejecución de Pruebas:
```bash
# En Windows:
.\mvnw.cmd test

# En Linux / macOS:
./mvnw test
```

### Empaquetado del JAR de Producción:
```bash
# En Windows:
.\mvnw.cmd clean package

# En Linux / macOS:
./mvnw clean package
```
El archivo ejecutable se genera en:  
`target/MUNICIPALIDAD_DE_ANDRES_AVELINO_CACERES-0.0.1-SNAPSHOT.jar`

---

## 7. EJECUCIÓN DEL SERVIDOR

### En Entorno de Desarrollo (mediante Maven Wrapper):
```powershell
$env:DB_PASSWORD="SuPasswordPostgreSQL"
.\mvnw.cmd spring-boot:run
```

### En Entorno de Producción (mediante el archivo JAR empaquetado):
```bash
export DB_URL="jdbc:postgresql://localhost:5432/db_municipalidad_licencias"
export DB_USERNAME="postgres"
export DB_PASSWORD="SuPasswordPostgreSQL"
export DOCUMENT_STORAGE_PATH="/opt/municaceres/licencias/data/documentos"
export SESSION_COOKIE_SECURE="true"

java -jar target/MUNICIPALIDAD_DE_ANDRES_AVELINO_CACERES-0.0.1-SNAPSHOT.jar
```

Una vez iniciado, ingrese en su navegador web a:  
👉 **`http://localhost:8080`**

---

## 8. MIGRACIONES FLYWAY

Flyway se ejecuta automáticamente en el arranque de la aplicación y administra las siguientes migraciones:
* `V1__crear_esquema_inicial.sql`: Estructura completa de tablas e índices del sistema.
* `V2__roles_iniciales.sql`: Inserción de roles (`ADMINISTRADOR`, `FUNCIONARIO`, `SUPERVISOR`, `CIUDADANO`).
* `V3__solicitudes_historial_y_documentos.sql`: Auditoría de solicitudes y almacenamiento documental.
* `V4__licencias_inspecciones_historial_y_verificacion.sql`: Trazabilidad de licencias y token QR público.
* `V5__pagos_verificacion_historial_y_bloqueo.sql`: Auditoría de pagos, validaciones financieras y caja.

---

## 9. SEGURIDAD INSTITUCIONAL (OWASP)

* **Autenticación por Sesiones HTTP:** Almacenamiento seguro en servidor; cookies `HttpOnly`, `SameSite=Lax`.
* **Protección CSRF:** Activada en todos los verbos mutantes (`POST`, `PUT`, `PATCH`, `DELETE`) vía `X-XSRF-TOKEN`.
* **Cifrado de Contraseñas:** Algoritmo BCrypt.
* **Minimización de Datos (Ley N.º 29733):** Números de documento enmascarados en consultas y protección de identidad en consultas públicas.
* **Control de Acceso Basado en Roles (RBAC):** Restricciones granulares en backend (`SecurityConfig`) y frontend (`router.js` y `layout.js`).

---

## 10. MÓDULOS DEL SISTEMA

1. **Seguridad y Usuarios:** Autenticación, gestión de cuentas y cambio de contraseña.
2. **Padrón Municipal:** Gestión de ciudadanos administrados, representantes legales y establecimientos comerciales.
3. **Expedientes y Solicitudes:** Trámite guiado con código `EXP-YYYY-XXXXX`, evaluación documental, observaciones, subsanación y aprobación.
4. **Inspecciones Técnicas (ITSE):** Programación de diligencias, inicio in situ y dictamen técnico de seguridad en edificaciones.
5. **Licencias de Funcionamiento:** Emisión formal, certificado oficial en PDF, código QR y verificación pública descentralizada.
6. **Caja y Pagos Municipales:** Declaración de comprobantes, verificación administrativa en caja, resumen de recaudación y constancia oficial en PDF.
7. **Dashboard y Reportes:** Indicadores en tiempo real, gráficos circulares tipo dona en SVG nativo, filtros dinámicos y exportación PDF/CSV.
8. **Centro de Notificaciones:** Notificaciones internas vinculadas a expedientes y sincronización reactiva de campana.
9. **Bitácora de Auditoría:** Registro inmutable de eventos con IP de origen, usuario ejecutor y trazabilidad forense.

---

## 11. PRUEBAS AUTOMATIZADAS Y CALIDAD (QA)

El proyecto cuenta con una amplia suite de pruebas que garantiza la solidez del sistema:

* `ValidadoresTests`: Validación de documentos de identidad peruanos (DNI, RUC con algoritmo Módulo 11).
* `CiudadanoEstablecimientoServiceTests` & `ControllerTests`: Reglas de negocio del padrón municipal.
* `SolicitudDocumentoServiceTests` & `ControllerTests`: Flujo integral de expedientes, estados y archivos adjuntos.
* `LicenciaInspeccionServiceTests` & `ControllerTests`: Emisión de licencias, certificados PDF, códigos QR y dictámenes ITSE.
* `PagoNotificacionAuditoriaServiceTests` & `ControllerTests`: Verificación de cobros, notificaciones y bitácora.
* `DashboardReporteServiceTests` & `ControllerTests`: Reportes dinámicos, métricas y exportaciones.
* `FrontendStaticResourcesTests`: Verificación estática de todos los archivos HTML5, CSS3, módulos JS ES6 y políticas de seguridad RBAC.
* `HealthControllerTests` & `AdminBootstrapTests`: Monitoreo y siembra inicial.

**Total verificado en compilación:** **97 pruebas ejecutadas, 0 fallos, 0 errores (BUILD SUCCESS).**

---

## 12. CONSIDERACIONES PARA EL DESPLIEGUE EN PRODUCCIÓN

1. Configurar un proxy inverso NGINX o Apache con certificado SSL/TLS (HTTPS obligatorio).
2. Habilitar la variable `SESSION_COOKIE_SECURE=true`.
3. Crear un usuario de sistema dedicado sin privilegios de superusuario (`app_licencias`) para ejecutar el servicio Java.
4. Asegurar permisos de lectura y escritura en la carpeta configurada en `DOCUMENT_STORAGE_PATH`.
5. Mantener copias de seguridad periódicas automatizadas mediante `pg_dump` y respaldos del directorio de documentos.

---

## 13. PROBLEMAS CONOCIDOS Y CONDICIONES DE ENTORNO

* **Persistencia Transaccional Viva en Entornos de CI/Sandbox:** Las pruebas que requieren una base de datos PostgreSQL viva en el puerto `5433` (`PersistenciaTests`, etc.) no deben ejecutarse en entornos sandbox desprovistos de un demonio PostgreSQL en ejecución continua, encontrándose documentadas objetivamente como `NO EJECUTADAS` en la [Matriz de Verificación](docs/MATRIZ_VERIFICACION.md).
* **Bloqueos de Archivos en Windows:** Durante pruebas intensivas en Windows, el plugin de recursos de Maven puede presentar bloqueos temporales en `application.properties`; se recomienda ejecutar `mvnw.cmd clean package` fuera de editores que mantengan descriptores de archivo abiertos.

---

## 14. DOCUMENTACIÓN ADICIONAL

* [Manual de Usuario](docs/MANUAL_USUARIO.md) — Guía operacional ilustrada para Administradores, Funcionarios y Ciudadanos.
* [Manual Técnico](docs/MANUAL_TECNICO.md) — Arquitectura interna, servicios systemd, NGINX y respaldos.
* [Matriz de Verificación](docs/MATRIZ_VERIFICACION.md) — Auditoría detallada de los 42 requerimientos del sistema.
* [Reporte de Errores y Auditoría](docs/REPORTE_ERRORES.md) — Bitácora de incidencias resueltas y trazabilidad técnica.
