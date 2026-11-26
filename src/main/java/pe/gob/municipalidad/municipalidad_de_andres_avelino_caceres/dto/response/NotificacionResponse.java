package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

import java.time.LocalDateTime;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoNotificacion;

/**
 * Respuesta de una notificación interna del sistema.
 */
public record NotificacionResponse(
        Long id,
        Long usuarioId,
        String titulo,
        String mensaje,
        TipoNotificacion tipo,
        boolean leida,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaLectura) {
}
