-- V4: Secuencia de códigos de licencia, indicador de requerimiento de inspección por trámite,
--     token público de verificación QR, metadatos de reprogramación e historial de licencias.

-- MySQL no tiene secuencias: se emulan con una tabla AUTO_INCREMENT.
CREATE TABLE IF NOT EXISTS seq_codigo_licencia (id BIGINT AUTO_INCREMENT PRIMARY KEY);

ALTER TABLE tipos_tramite
    ADD COLUMN requiere_inspeccion BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE inspecciones
    ADD COLUMN motivo_reprogramacion TEXT,
    ADD COLUMN fecha_creacion        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN fecha_actualizacion   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX idx_inspecciones_estado ON inspecciones (estado);
CREATE INDEX idx_inspecciones_fecha_programada ON inspecciones (fecha_programada);

ALTER TABLE licencias
    ADD COLUMN token_verificacion VARCHAR(64);

UPDATE licencias
SET token_verificacion = MD5(CONCAT(RAND(), NOW(6), id))
WHERE token_verificacion IS NULL;

ALTER TABLE licencias
    MODIFY COLUMN token_verificacion VARCHAR(64) NOT NULL,
    ADD CONSTRAINT uk_licencias_token_verificacion UNIQUE (token_verificacion);

CREATE INDEX idx_licencias_fecha_emision ON licencias (fecha_emision DESC);

CREATE TABLE historial_licencias (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    licencia_id            BIGINT      NOT NULL,
    estado_anterior        VARCHAR(30),
    estado_nuevo           VARCHAR(30) NOT NULL,
    motivo                 TEXT,
    usuario_responsable_id BIGINT,
    fecha_cambio           DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_historial_licencia FOREIGN KEY (licencia_id) REFERENCES licencias (id),
    CONSTRAINT fk_historial_licencia_usuario FOREIGN KEY (usuario_responsable_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_historial_licencia ON historial_licencias (licencia_id, fecha_cambio DESC);
