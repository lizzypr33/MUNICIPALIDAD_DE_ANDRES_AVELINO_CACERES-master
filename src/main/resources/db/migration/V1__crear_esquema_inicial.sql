-- V1: esquema inicial del sistema de licencias municipales.
-- Solo crea objetos; no elimina nada ni inserta datos.

CREATE TABLE roles (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(50)  NOT NULL,
    descripcion VARCHAR(255),
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_roles_nombre UNIQUE (nombre)
);

CREATE TABLE usuarios (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    username            VARCHAR(50)  NOT NULL,
    password            VARCHAR(100) NOT NULL,
    nombres             VARCHAR(100) NOT NULL,
    apellidos           VARCHAR(100) NOT NULL,
    email               VARCHAR(150) NOT NULL,
    telefono            VARCHAR(20),
    activo              BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_acceso       DATETIME,
    CONSTRAINT uk_usuarios_username UNIQUE (username),
    CONSTRAINT uk_usuarios_email UNIQUE (email)
);

CREATE TABLE usuarios_roles (
    usuario_id BIGINT NOT NULL,
    rol_id     BIGINT NOT NULL,
    PRIMARY KEY (usuario_id, rol_id),
    CONSTRAINT fk_usuarios_roles_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_usuarios_roles_rol FOREIGN KEY (rol_id) REFERENCES roles (id)
);
CREATE INDEX idx_usuarios_roles_rol ON usuarios_roles (rol_id);

CREATE TABLE ciudadanos (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    tipo_documento   VARCHAR(30)  NOT NULL,
    numero_documento VARCHAR(20)  NOT NULL,
    nombres          VARCHAR(100) NOT NULL,
    apellidos        VARCHAR(100) NOT NULL,
    email            VARCHAR(150),
    telefono         VARCHAR(20),
    direccion        VARCHAR(255),
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_registro   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_id       BIGINT,
    CONSTRAINT uk_ciudadanos_documento UNIQUE (tipo_documento, numero_documento),
    CONSTRAINT uk_ciudadanos_usuario UNIQUE (usuario_id),
    CONSTRAINT fk_ciudadanos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);
CREATE INDEX idx_ciudadanos_apellidos ON ciudadanos (apellidos, nombres);

CREATE TABLE representantes (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    tipo_documento   VARCHAR(30)  NOT NULL,
    numero_documento VARCHAR(20)  NOT NULL,
    nombres          VARCHAR(100) NOT NULL,
    apellidos        VARCHAR(100) NOT NULL,
    cargo            VARCHAR(100),
    telefono         VARCHAR(20),
    email            VARCHAR(150),
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_representantes_documento UNIQUE (tipo_documento, numero_documento)
);

CREATE TABLE establecimientos (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre_comercial      VARCHAR(200) NOT NULL,
    razon_social          VARCHAR(200),
    ruc                   VARCHAR(11),
    direccion             VARCHAR(255) NOT NULL,
    referencia            VARCHAR(255),
    giro_negocio          VARCHAR(150),
    area_metros_cuadrados NUMERIC(10, 2),
    numero_pisos          INTEGER,
    telefono              VARCHAR(20),
    email                 VARCHAR(150),
    activo                BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_registro        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ciudadano_id          BIGINT,
    representante_id      BIGINT,
    CONSTRAINT ck_establecimientos_area CHECK (area_metros_cuadrados IS NULL OR area_metros_cuadrados >= 0),
    CONSTRAINT fk_establecimientos_ciudadano FOREIGN KEY (ciudadano_id) REFERENCES ciudadanos (id),
    CONSTRAINT fk_establecimientos_representante FOREIGN KEY (representante_id) REFERENCES representantes (id)
);
CREATE INDEX idx_establecimientos_ruc ON establecimientos (ruc);
CREATE INDEX idx_establecimientos_ciudadano ON establecimientos (ciudadano_id);
CREATE INDEX idx_establecimientos_representante ON establecimientos (representante_id);

CREATE TABLE tipos_tramite (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo         VARCHAR(50)  NOT NULL,
    nombre         VARCHAR(150) NOT NULL,
    descripcion    TEXT,
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tipos_tramite_codigo UNIQUE (codigo)
);

CREATE TABLE requisitos (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo      VARCHAR(50)  NOT NULL,
    nombre      VARCHAR(200) NOT NULL,
    descripcion TEXT,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_requisitos_codigo UNIQUE (codigo)
);

CREATE TABLE tipos_tramite_requisitos (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    tipo_tramite_id     BIGINT  NOT NULL,
    requisito_id        BIGINT  NOT NULL,
    obligatorio         BOOLEAN NOT NULL DEFAULT TRUE,
    orden_visualizacion INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_tipo_tramite_requisito UNIQUE (tipo_tramite_id, requisito_id),
    CONSTRAINT fk_ttr_tipo_tramite FOREIGN KEY (tipo_tramite_id) REFERENCES tipos_tramite (id),
    CONSTRAINT fk_ttr_requisito FOREIGN KEY (requisito_id) REFERENCES requisitos (id)
);
CREATE INDEX idx_ttr_requisito ON tipos_tramite_requisitos (requisito_id);

CREATE TABLE solicitudes (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero_expediente      VARCHAR(30) NOT NULL,
    ciudadano_id           BIGINT      NOT NULL,
    establecimiento_id     BIGINT      NOT NULL,
    tipo_tramite_id        BIGINT      NOT NULL,
    representante_id       BIGINT,
    fecha_presentacion     DATETIME,
    fecha_actualizacion    DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado                 VARCHAR(30) NOT NULL DEFAULT 'BORRADOR',
    observaciones          TEXT,
    motivo_rechazo         TEXT,
    usuario_responsable_id BIGINT,
    CONSTRAINT uk_solicitudes_expediente UNIQUE (numero_expediente),
    CONSTRAINT fk_solicitudes_ciudadano FOREIGN KEY (ciudadano_id) REFERENCES ciudadanos (id),
    CONSTRAINT fk_solicitudes_establecimiento FOREIGN KEY (establecimiento_id) REFERENCES establecimientos (id),
    CONSTRAINT fk_solicitudes_tipo_tramite FOREIGN KEY (tipo_tramite_id) REFERENCES tipos_tramite (id),
    CONSTRAINT fk_solicitudes_representante FOREIGN KEY (representante_id) REFERENCES representantes (id),
    CONSTRAINT fk_solicitudes_responsable FOREIGN KEY (usuario_responsable_id) REFERENCES usuarios (id)
);
CREATE INDEX idx_solicitudes_ciudadano ON solicitudes (ciudadano_id);
CREATE INDEX idx_solicitudes_establecimiento ON solicitudes (establecimiento_id);
CREATE INDEX idx_solicitudes_tipo_tramite ON solicitudes (tipo_tramite_id);
CREATE INDEX idx_solicitudes_representante ON solicitudes (representante_id);
CREATE INDEX idx_solicitudes_responsable ON solicitudes (usuario_responsable_id);
CREATE INDEX idx_solicitudes_estado ON solicitudes (estado);

CREATE TABLE documentos (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    solicitud_id         BIGINT       NOT NULL,
    requisito_id         BIGINT,
    nombre_original      VARCHAR(255) NOT NULL,
    nombre_almacenado    VARCHAR(255) NOT NULL,
    ruta_almacenamiento  VARCHAR(500) NOT NULL,
    tipo_mime            VARCHAR(100) NOT NULL,
    tamanio_bytes        BIGINT       NOT NULL,
    fecha_subida         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado               VARCHAR(30)  NOT NULL DEFAULT 'PENDIENTE',
    observaciones        TEXT,
    CONSTRAINT ck_documentos_tamanio CHECK (tamanio_bytes >= 0),
    CONSTRAINT fk_documentos_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitudes (id),
    CONSTRAINT fk_documentos_requisito FOREIGN KEY (requisito_id) REFERENCES requisitos (id)
);
CREATE INDEX idx_documentos_solicitud ON documentos (solicitud_id);
CREATE INDEX idx_documentos_requisito ON documentos (requisito_id);

CREATE TABLE inspecciones (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    solicitud_id     BIGINT      NOT NULL,
    inspector_id     BIGINT,
    fecha_programada DATETIME,
    fecha_realizada  DATETIME,
    estado           VARCHAR(30) NOT NULL DEFAULT 'PROGRAMADA',
    resultado        VARCHAR(40),
    observaciones    TEXT,
    recomendaciones  TEXT,
    CONSTRAINT fk_inspecciones_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitudes (id),
    CONSTRAINT fk_inspecciones_inspector FOREIGN KEY (inspector_id) REFERENCES usuarios (id)
);
CREATE INDEX idx_inspecciones_solicitud ON inspecciones (solicitud_id);
CREATE INDEX idx_inspecciones_inspector ON inspecciones (inspector_id);

CREATE TABLE licencias (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo_licencia   VARCHAR(30) NOT NULL,
    solicitud_id      BIGINT      NOT NULL,
    fecha_emision     DATE        NOT NULL,
    fecha_vencimiento DATE,
    estado            VARCHAR(30) NOT NULL DEFAULT 'VIGENTE',
    observaciones     TEXT,
    usuario_emisor_id BIGINT,
    CONSTRAINT uk_licencias_codigo UNIQUE (codigo_licencia),
    CONSTRAINT uk_licencias_solicitud UNIQUE (solicitud_id),
    CONSTRAINT ck_licencias_fechas CHECK (fecha_vencimiento IS NULL OR fecha_vencimiento >= fecha_emision),
    CONSTRAINT fk_licencias_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitudes (id),
    CONSTRAINT fk_licencias_emisor FOREIGN KEY (usuario_emisor_id) REFERENCES usuarios (id)
);
CREATE INDEX idx_licencias_emisor ON licencias (usuario_emisor_id);
CREATE INDEX idx_licencias_estado ON licencias (estado);

CREATE TABLE pagos (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    solicitud_id      BIGINT         NOT NULL,
    codigo_operacion  VARCHAR(50),
    concepto          VARCHAR(200)   NOT NULL,
    monto             NUMERIC(12, 2) NOT NULL,
    moneda            VARCHAR(3)     NOT NULL DEFAULT 'PEN',
    fecha_pago        DATETIME,
    medio_pago        VARCHAR(50),
    estado            VARCHAR(30)    NOT NULL DEFAULT 'PENDIENTE',
    observaciones     TEXT,
    CONSTRAINT ck_pagos_monto CHECK (monto >= 0),
    CONSTRAINT fk_pagos_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitudes (id)
);
CREATE INDEX idx_pagos_solicitud ON pagos (solicitud_id);
CREATE INDEX idx_pagos_codigo_operacion ON pagos (codigo_operacion);

CREATE TABLE notificaciones (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id     BIGINT       NOT NULL,
    titulo         VARCHAR(150) NOT NULL,
    mensaje        TEXT         NOT NULL,
    tipo           VARCHAR(30)  NOT NULL DEFAULT 'INFORMATIVA',
    leida          BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_creacion DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_lectura  DATETIME,
    CONSTRAINT fk_notificaciones_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);
CREATE INDEX idx_notificaciones_usuario ON notificaciones (usuario_id, leida);

CREATE TABLE auditorias (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id        BIGINT,
    accion            VARCHAR(100) NOT NULL,
    entidad_afectada  VARCHAR(100) NOT NULL,
    entidad_id        BIGINT,
    descripcion       TEXT,
    fecha_operacion   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    direccion_ip      VARCHAR(45),
    CONSTRAINT fk_auditorias_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);
CREATE INDEX idx_auditorias_usuario ON auditorias (usuario_id);
CREATE INDEX idx_auditorias_entidad ON auditorias (entidad_afectada, entidad_id);
CREATE INDEX idx_auditorias_fecha ON auditorias (fecha_operacion);
