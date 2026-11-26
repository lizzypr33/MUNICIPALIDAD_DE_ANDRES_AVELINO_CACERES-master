package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

import java.time.LocalDateTime;

/** Estructura uniforme de error devuelta al cliente. */
public record ErrorResponse(LocalDateTime timestamp, int status, String message, String path) {
}
