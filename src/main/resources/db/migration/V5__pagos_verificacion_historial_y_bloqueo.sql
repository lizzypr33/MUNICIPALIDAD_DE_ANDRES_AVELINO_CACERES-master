-- V5: Verificación administrativa de pagos, bloqueo optimista, motivos de rechazo/anulación e historial de pagos.

ALTER TABLE pagos
    ADD COLUMN usuario_verificador_id   BIGINT,
    ADD COLUMN fecha_verificacion       DATETIME,
    ADD COLUMN motivo_rechazo_anulacion TEXT,
    ADD COLUMN fecha_registro           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN version                  BIGINT    NOT NULL DEFAULT 0,
    ADD CONSTRAINT fk_pagos_verificador FOREIGN KEY (usuario_verificador_id) REFERENCES usuarios (id);

CREATE INDEX idx_pagos_estado ON pagos (estado);
CREATE INDEX idx_pagos_fecha_pago ON pagos (fecha_pago DESC);
CREATE INDEX idx_pagos_verificador ON pagos (usuario_verificador_id);

CREATE TABLE historial_pagos (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    pago_id                BIGINT      NOT NULL,
    estado_anterior        VARCHAR(30),
    estado_nuevo           VARCHAR(30) NOT NULL,
    motivo                 TEXT,
    usuario_responsable_id BIGINT,
    fecha_cambio           DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_historial_pago FOREIGN KEY (pago_id) REFERENCES pagos (id),
    CONSTRAINT fk_historial_pago_usuario FOREIGN KEY (usuario_responsable_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_historial_pago ON historial_pagos (pago_id, fecha_cambio DESC);
