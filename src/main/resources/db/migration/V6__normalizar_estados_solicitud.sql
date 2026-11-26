-- V6: Normaliza los estados de solicitud guardados en forma masculina (APROBADO, OBSERVADO, ...)
-- a la forma femenina que usa el enum EstadoSolicitud (APROBADA, OBSERVADA, ...).
-- Es idempotente: las filas ya normalizadas no coinciden con el WHERE y no se modifican.

UPDATE solicitudes SET estado = CASE estado
    WHEN 'APROBADO'   THEN 'APROBADA'
    WHEN 'OBSERVADO'  THEN 'OBSERVADA'
    WHEN 'PRESENTADO' THEN 'PRESENTADA'
    WHEN 'SUBSANADO'  THEN 'SUBSANADA'
    WHEN 'RECHAZADO'  THEN 'RECHAZADA'
    WHEN 'ARCHIVADO'  THEN 'ARCHIVADA'
    ELSE estado END
WHERE estado IN ('APROBADO', 'OBSERVADO', 'PRESENTADO', 'SUBSANADO', 'RECHAZADO', 'ARCHIVADO');

UPDATE historial_solicitudes SET estado_anterior = CASE estado_anterior
    WHEN 'APROBADO'   THEN 'APROBADA'
    WHEN 'OBSERVADO'  THEN 'OBSERVADA'
    WHEN 'PRESENTADO' THEN 'PRESENTADA'
    WHEN 'SUBSANADO'  THEN 'SUBSANADA'
    WHEN 'RECHAZADO'  THEN 'RECHAZADA'
    WHEN 'ARCHIVADO'  THEN 'ARCHIVADA'
    ELSE estado_anterior END
WHERE estado_anterior IN ('APROBADO', 'OBSERVADO', 'PRESENTADO', 'SUBSANADO', 'RECHAZADO', 'ARCHIVADO');

UPDATE historial_solicitudes SET estado_nuevo = CASE estado_nuevo
    WHEN 'APROBADO'   THEN 'APROBADA'
    WHEN 'OBSERVADO'  THEN 'OBSERVADA'
    WHEN 'PRESENTADO' THEN 'PRESENTADA'
    WHEN 'SUBSANADO'  THEN 'SUBSANADA'
    WHEN 'RECHAZADO'  THEN 'RECHAZADA'
    WHEN 'ARCHIVADO'  THEN 'ARCHIVADA'
    ELSE estado_nuevo END
WHERE estado_nuevo IN ('APROBADO', 'OBSERVADO', 'PRESENTADO', 'SUBSANADO', 'RECHAZADO', 'ARCHIVADO');
