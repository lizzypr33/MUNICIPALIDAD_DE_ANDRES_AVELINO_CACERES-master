package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

import java.time.LocalDateTime;

/**
 * Respuesta de solo lectura de un evento de auditoría municipal.
 */
public record AuditoriaResponse(
        Long id,
        Long usuarioId,
        String usernameUsuario,
        String nombreCompletoUsuario,
        String accion,
        String entidadAfectada,
        Long entidadId,
        String descripcion,
        LocalDateTime fechaOperacion,
        String direccionIp) {
}
