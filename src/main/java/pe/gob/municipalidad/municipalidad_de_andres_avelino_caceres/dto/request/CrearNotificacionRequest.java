package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoNotificacion;

/**
 * Solicitud interna para generar una notificación dirigida a un usuario del sistema.
 */
public record CrearNotificacionRequest(
        @NotNull(message = "El usuario destinatario es obligatorio") Long usuarioId,
        @NotBlank(message = "El título de la notificación es obligatorio") @Size(max = 150) String titulo,
        @NotBlank(message = "El mensaje de la notificación es obligatorio") @Size(max = 4000) String mensaje,
        TipoNotificacion tipo) {
}
