package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de creación o actualización de un requisito municipal, y elemento de asignación a un tipo de trámite.
 */
public record RequisitoRequest(
        @NotBlank(message = "El código del requisito es obligatorio") @Size(max = 50) String codigo,
        @NotBlank(message = "El nombre del requisito es obligatorio") @Size(max = 200) String nombre,
        @Size(max = 2000) String descripcion) {

    /**
     * Elemento de vinculación entre un tipo de trámite y un requisito ({@code PUT /api/tipos-tramite/{id}/requisitos}).
     */
    public record AsignacionTramiteItem(
            @NotNull(message = "El identificador del requisito es obligatorio") Long requisitoId,
            Boolean obligatorio,
            Integer ordenVisualizacion) {
    }
}
