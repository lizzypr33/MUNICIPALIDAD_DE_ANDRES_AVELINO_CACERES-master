package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

/**
 * Respuesta con la información de un requisito municipal.
 * Cuando se consulta dentro de un tipo de trámite incluye {@code obligatorio} y {@code ordenVisualizacion}.
 */
public record RequisitoResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        boolean activo,
        Boolean obligatorio,
        Integer ordenVisualizacion) {
}
