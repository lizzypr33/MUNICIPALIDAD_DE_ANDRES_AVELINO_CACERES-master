package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

import java.time.LocalDateTime;

/**
 * Elemento de actividad reciente administrativa para el dashboard municipal.
 */
public record ActividadRecienteResponse(
        Long id,
        LocalDateTime fecha,
        String tipoOperacion,
        String modulo,
        String descripcionBreve,
        String usuarioResponsable) {
}
