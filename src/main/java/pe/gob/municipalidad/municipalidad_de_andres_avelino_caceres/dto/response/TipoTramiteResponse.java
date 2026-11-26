package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

import java.time.LocalDateTime;

/** Respuesta con la información de un tipo de trámite municipal. */
public record TipoTramiteResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        boolean activo,
        LocalDateTime fechaCreacion) {
}
