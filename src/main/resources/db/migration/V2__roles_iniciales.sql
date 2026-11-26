-- V2: roles base del sistema (idempotente). No se crean usuarios.
INSERT IGNORE INTO roles (nombre, descripcion) VALUES
    ('ADMINISTRADOR', 'Administración general del sistema'),
    ('FUNCIONARIO', 'Personal municipal que gestiona solicitudes y licencias'),
    ('SUPERVISOR', 'Supervisión e inspección de expedientes'),
    ('CIUDADANO', 'Usuario del portal que realiza trámites');
