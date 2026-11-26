# MANUAL TÉCNICO DE ARQUITECTURA Y DESPLIEGUE
## Sistema Web de Gestión de Licencias Municipales
### Municipalidad Distrital de Andrés Avelino Cáceres Dorregaray — Ayacucho, Perú

---

## 1. ESPECIFICACIÓN TÉCNICA GENERAL

* **Proyecto:** `MUNICIPALIDAD_DE_ANDRES_AVELINO_CACERES`
* **Paquete Base:** `pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres`
* **Clase Principal:** `MunicipalidadDeAndresAvelinoCaceresApplication`
* **Lenguaje:** Java 17 (LTS)
* **Framework Backend:** Spring Boot 4.1.2 (Spring Data JPA, Spring Security 7, Spring WebMVC, Bean Validation)
* **Gestor de Construcción:** Apache Maven 3.9+ (utilizando Maven Wrapper `mvnw.cmd` / `./mvnw`)
* **Motor de Base de Datos:** PostgreSQL 14+
* **Gestor de Migraciones:** Flyway Community Edition
* **Generación Documental:** OpenPDF (LibrePDF 1.3.40) y ZXing 3.5.3 (código QR)
* **Frontend:** Vanilla JavaScript ES6+ Modular, HTML5 semántico, CSS3 nativo (Grid, Flexbox, Variables CSS), Fetch API. Cero dependencias o frameworks externos (sin React, Angular, Vue, Bootstrap, Tailwind, jQuery ni Thymeleaf).

---

## 2. ARQUITECTURA DE SOFTWARE (CAPAS EN SPRING BOOT)

El backend implementa una arquitectura en capas desacopladas con estricta inversión de dependencias:

```
[ Navegador Web / Cliente HTTP ]
              │ (HTTPS / Fetch API + CSRF Token)
              ▼
[ Spring Security Filter Chain ] ──▶ CookieCsrfTokenRepository / SessionManagement
              │
              ▼
[ REST Controllers (@RestController) ] ──▶ DTO Requests / Responses
              │
              ▼
[ Service Layer (@Service) ] ──▶ Lógica de Negocio, Transacciones (@Transactional), Validaciones
              │
              ▼
[ Repository Layer (@Repository) ] ──▶ Spring Data JPA / JpaSpecificationExecutor
              │
              ▼
[ PostgreSQL Database ] ──▶ Tablas administradas exclusivamente por Flyway
```

### 2.1 Estructura del Árbol de Paquetes
```text
pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres/
├── config/              # Configuración de Seguridad, Beans, Web y Sesiones
├── controller/          # Controladores REST organizados por agregados de dominio
├── dto/
│   ├── request/         # Objetos de transferencia de entrada con Bean Validation
│   └── response/        # Proyecciones seguras de salida (Records inmutables)
├── entity/              # Entidades JPA mapeadas a las tablas de PostgreSQL
├── enums/               # Enumeraciones de estado, tipo de trámite y roles
├── exception/           # Excepciones de negocio y @RestControllerAdvice global
├── mapper/              # Mapeadores de entidad a DTO
├── repository/          # Interfaces JPA y consultas dinámicas Specification
├── security/            # UserDetails, UserDetailsService, AdminBootstrap, Handlers
├── service/             # Contratos de servicios de aplicación
│   └── impl/            # Implementaciones de servicios con lógica transaccional
├── storage/             # Servicio de almacenamiento de archivos fuera del classpath
├── util/                # Generadores de códigos, tokens QR y creadores de PDF
└── validation/          # Validadores de negocio (DNI, RUC, requisitos, transiciones)
```

---

## 3. MODELO DE DATOS Y MIGRACIONES FLYWAY

La base de datos relacional PostgreSQL está gobernada por scripts SQL versionados en `src/main/resources/db/migration`:

1. `V1__crear_esquema_inicial.sql`:
   * Tablas fundamentales: `usuarios`, `roles`, `usuarios_roles`, `ciudadanos`, `representantes`, `establecimientos`, `tipos_tramite`, `requisitos`, `tipos_tramite_requisitos`, `solicitudes`, `documentos`, `inspecciones`, `licencias`, `pagos`, `notificaciones`, `auditoria`.
2. `V2__roles_iniciales.sql`:
   * Inserción de los cuatro roles institucionales: `ROLE_ADMINISTRADOR`, `ROLE_FUNCIONARIO`, `ROLE_SUPERVISOR`, `ROLE_CIUDADANO`.
3. `V3__solicitudes_historial_y_documentos.sql`:
   * Creación de la tabla `historial_solicitudes` para auditoría temporal de transiciones de estado de expedientes y campos de almacenamiento de archivos.
4. `V4__licencias_inspecciones_historial_y_verificacion.sql`:
   * Creación de `historial_licencias` y adición del campo `token_verificacion` único e indexado para la consulta pública por código QR.
5. `V5__pagos_verificacion_historial_y_bloqueo.sql`:
   * Creación de `historial_pagos`, control de concurrencia y validaciones de caja.

### 3.1 Entidades Clave y Relaciones
* **Usuario (1) ──── (N) Rol:** Mapeado con tabla intermedia `usuarios_roles`.
* **Ciudadano (1) ──── (N) Establecimiento:** Mapea la titularidad del local.
* **Ciudadano (1) ──── (N) Solicitud:** Administrado que inicia el trámite.
* **Solicitud (1) ──── (1) Licencia:** Relación uno a uno. Una solicitud aprobada genera exactamente una licencia oficial.
* **Solicitud (1) ──── (N) Documento:** Documentos probatorios adjuntos en disco.
* **Solicitud (1) ──── (N) Inspección:** Inspecciones técnicas ITSE programadas.
* **Solicitud (1) ──── (N) Pago:** Declaraciones y comprobantes de recaudación.

---

## 4. SEGURIDAD Y PROTECCIÓN OWASP

### 4.1 Autenticación y Gestión de Sesiones
* **Algoritmo de Hash:** BCrypt con salt dinámico por defecto (costo 10).
* **Sesiones HTTP:** Almacenadas en servidor con política de fijación de sesión `changeSessionId()`.
* **Cookies de Sesión:** `JSESSIONID` con atributos `HttpOnly=true`, `SameSite=Lax` y `Secure` configurable por variable de entorno `SESSION_COOKIE_SECURE`.
* **Expiración:** Configurada a 30 minutos de inactividad.

### 4.2 Protección CSRF
* Implementada mediante `CookieCsrfTokenRepository.withHttpOnlyFalse()`.
* El frontend lee el token desde la cookie `XSRF-TOKEN` o mediante el endpoint `/api/auth/csrf` y lo envía en la cabecera HTTP `X-XSRF-TOKEN` en cada llamada `POST`, `PUT`, `PATCH` y `DELETE`.

### 4.3 Control de Acceso por Roles (RBAC)
Configurado de forma centralizada en `SecurityConfig`:
```java
// Ejemplo de autorización en SecurityConfig
.requestMatchers(HttpMethod.GET, "/pages/auditoria.html").hasAnyRole("ADMINISTRADOR", "SUPERVISOR")
.requestMatchers(HttpMethod.GET, "/pages/reportes.html").hasAnyRole("ADMINISTRADOR", "FUNCIONARIO", "SUPERVISOR")
.requestMatchers("/api/auditorias/**").hasAnyRole("ADMINISTRADOR", "SUPERVISOR")
.requestMatchers(HttpMethod.POST, "/api/licencias/emitir").hasAnyRole("ADMINISTRADOR", "FUNCIONARIO", "SUPERVISOR")
```

---

## 5. CATÁLOGO DE ENDPOINTS REST

### 5.1 Autenticación y Perfil (`/api/auth`)
* `POST /api/auth/login`: Autentica credenciales y genera sesión HTTP.
* `POST /api/auth/logout`: Invalida la sesión actual y audita el cierre.
* `GET /api/auth/me`: Retorna el perfil del usuario autenticado.
* `PATCH /api/auth/change-password`: Cambio de contraseña con verificación de clave anterior.
* `GET /api/auth/csrf`: Provee el token de seguridad CSRF.

### 5.2 Trámites y Expedientes (`/api/solicitudes`, `/api/documentos`)
* `GET /api/solicitudes`: Listado paginado con filtros.
* `POST /api/solicitudes`: Creación de borrador de solicitud.
* `POST /api/solicitudes/{id}/presentar`: Presentación formal y generación de expediente `EXP-YYYY-XXXXX`.
* `PATCH /api/solicitudes/{id}/iniciar-revision`: Transición a `EN_REVISION`.
* `PATCH /api/solicitudes/{id}/observar`: Emisión de observaciones formales.
* `PATCH /api/solicitudes/{id}/subsanar`: Subsanación por el administrado.
* `PATCH /api/solicitudes/{id}/aprobar`: Aprobación técnica definitiva.
* `POST /api/solicitudes/{id}/documentos`: Carga segura de archivos adjuntos (Multipart).
* `GET /api/documentos/{id}/descargar`: Descarga de archivo mediante streaming binario seguro.

### 5.3 Licencias e Inspecciones (`/api/licencias`, `/api/inspecciones`, `/api/public/licencias`)
* `POST /api/licencias/emitir`: Emisión formal de licencia con generación de token QR.
* `GET /api/licencias/{id}/certificado`: Descarga del certificado oficial en PDF generado con OpenPDF.
* `GET /api/public/licencias/verificar/{token}`: **Endpoint público** (sin sesión) para consulta ciudadana por código QR.
* `POST /api/inspecciones`: Programación de inspección ITSE.
* `PATCH /api/inspecciones/{id}/resultado`: Registro de dictamen técnico (`FAVORABLE`, `NO_FAVORABLE`, `CON_OBSERVACIONES`).

### 5.4 Pagos, Notificaciones y Auditoría (`/api/pagos`, `/api/notificaciones`, `/api/auditorias`)
* `POST /api/pagos`: Declaración de pago de tasa administrativa.
* `PATCH /api/pagos/{id}/verificar`: Confirmación de ingreso por caja municipal.
* `GET /api/pagos/{id}/constancia`: Descarga de constancia oficial de pago en PDF.
* `GET /api/notificaciones/contador`: Conteo reactivo de avisos no leídos.
* `PATCH /api/notificaciones/leer-todas`: Marcado masivo de lectura.
* `GET /api/auditorias`: Consulta inmutable de la bitácora del sistema (solo roles directivos).

### 5.5 Reportes y Estadísticas (`/api/dashboard`, `/api/reportes`)
* `GET /api/dashboard/resumen`: Indicadores ejecutivos para el dashboard.
* `GET /api/reportes/{solicitudes|licencias|inspecciones|pagos|establecimientos|ciudadanos}`: Reportes paginados con filtros dinámicos.
* `GET /api/reportes/{tipo}/pdf`: Generación y exportación de reporte consolidado en PDF.
* `GET /api/reportes/{tipo}/csv`: Exportación tabular en CSV delimitado con codificación UTF-8.

---

## 6. CONFIGURACIÓN DEL ENTORNO Y VARIABLES

El archivo `application.properties` se alimenta dinámicamente de variables de entorno del sistema operativo:

| Variable de Entorno | Descripción | Valor por Defecto / Ejemplo |
|---|---|---|
| `DB_URL` | Cadena de conexión JDBC a PostgreSQL | `jdbc:postgresql://localhost:5433/ANDRES` |
| `DB_USERNAME` | Usuario de la base de datos | `postgres` |
| `DB_PASSWORD` | Contraseña del usuario PostgreSQL | *(Obligatorio en producción)* |
| `TEST_DB_URL` | Base de datos para pruebas de integración | `jdbc:postgresql://localhost:5433/andres_test` |
| `DOCUMENT_STORAGE_PATH` | Directorio físico para archivos adjuntos | `./data/documentos` |
| `LICENCIA_VERIFICACION_BASE_URL` | URL base para la generación de enlaces QR | `http://localhost:8080` (o HTTPS oficial) |
| `SESSION_COOKIE_SECURE` | Habilitar bandera Secure en cookies (HTTPS) | `false` en dev / `true` en prod |
| `ADMIN_BOOTSTRAP_ENABLED` | Sembrado automático del primer administrador | `false` |
| `ADMIN_USERNAME` | Nombre de usuario del primer administrador | `admin.municipal` |
| `ADMIN_PASSWORD` | Contraseña inicial segura del administrador | *(Asignar de forma temporal)* |

---

## 7. PROCEDIMIENTO DE COMPILACIÓN Y EMPAQUETADO

Para compilar el proyecto y generar el archivo ejecutable (`fat-JAR`):

```bash
# En Windows:
.\mvnw.cmd clean package

# En Linux / macOS:
./mvnw clean package
```

El artefacto generado se ubicará en:
`target/MUNICIPALIDAD_DE_ANDRES_AVELINO_CACERES-0.0.1-SNAPSHOT.jar`

---

## 8. GUÍA DE DESPLIEGUE EN SERVIDOR DE PRODUCCIÓN (LINUX / SYSTEMD)

### 8.1 Preparación del Directorio
```bash
sudo mkdir -p /opt/municaceres/licencias/data/documentos
sudo useradd -r -s /bin/false app_licencias
sudo chown -R app_licencias:app_licencias /opt/municaceres
```

### 8.2 Archivo de Servicio Systemd (`/etc/systemd/system/licencias-municipales.service`)
```ini
[Unit]
Description=Sistema Web de Licencias Municipales - MDAACD
After=network.target postgresql.service

[Service]
Type=simple
User=app_licencias
Group=app_licencias
WorkingDirectory=/opt/municaceres/licencias
Environment="DB_URL=jdbc:postgresql://localhost:5432/db_municipalidad_licencias"
Environment="DB_USERNAME=postgres"
Environment="DB_PASSWORD=ContraseñaSegura123!"
Environment="DOCUMENT_STORAGE_PATH=/opt/municaceres/licencias/data/documentos"
Environment="LICENCIA_VERIFICACION_BASE_URL=https://licencias.municaceres.gob.pe"
Environment="SESSION_COOKIE_SECURE=true"
Environment="ADMIN_BOOTSTRAP_ENABLED=false"
ExecStart=/usr/bin/java -Xms512m -Xmx2048m -jar /opt/municaceres/licencias/MUNICIPALIDAD_DE_ANDRES_AVELINO_CACERES-0.0.1-SNAPSHOT.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### 8.3 Configuración de Proxy Inverso NGINX con Certificado SSL
```nginx
server {
    listen 80;
    server_name licencias.municaceres.gob.pe;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name licencias.municaceres.gob.pe;

    ssl_certificate /etc/letsencrypt/live/licencias.municaceres.gob.pe/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/licencias.municaceres.gob.pe/privkey.pem;

    client_max_body_size 15M;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 90;
    }
}
```

---

## 9. POLÍTICA DE COPIAS DE SEGURIDAD Y RECUPERACIÓN

1. **Base de Datos PostgreSQL:**
   ```bash
   pg_dump -U postgres -d db_municipalidad_licencias -F c -b -v -f "/backup/db_licencias_$(date +%Y%m%d_%H%M%S).dump"
   ```
2. **Archivos Adjuntos:**
   ```bash
   tar -czf "/backup/documentos_$(date +%Y%m%d_%H%M%S).tar.gz" /opt/municaceres/licencias/data/documentos
   ```
3. **Restauración:**
   ```bash
   pg_restore -U postgres -d db_municipalidad_licencias -v "/backup/db_licencias_archivo.dump"
   tar -xzf "/backup/documentos_archivo.tar.gz" -C /opt/municaceres/licencias/data/
   ```
