-- V3: Ajustes para gestión de solicitudes, correlativo de expedientes, historial de estados y retiro lógico de documentos.

-- MySQL no tiene secuencias: se emulan con una tabla AUTO_INCREMENT.
CREATE TABLE IF NOT EXISTS seq_numero_expediente (id BIGINT AUTO_INCREMENT PRIMARY KEY);

ALTER TABLE solicitudes
    MODIFY COLUMN numero_expediente VARCHAR(30) NULL;

ALTER TABLE solicitudes
    ADD COLUMN requisitos_snapshot TEXT;

CREATE INDEX idx_solicitudes_fecha_presentacion ON solicitudes (fecha_presentacion DESC);

ALTER TABLE documentos
    ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_documentos_solicitud_activo ON documentos (solicitud_id, activo);

CREATE TABLE historial_solicitudes (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    solicitud_id           BIGINT      NOT NULL,
    estado_anterior        VARCHAR(30),
    estado_nuevo           VARCHAR(30) NOT NULL,
    usuario_responsable_id BIGINT,
    observaciones          TEXT,
    fecha_cambio           DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_historial_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitudes (id),
    CONSTRAINT fk_historial_usuario FOREIGN KEY (usuario_responsable_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_historial_solicitud ON historial_solicitudes (solicitud_id, fecha_cambio DESC);
