package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de creación o actualización de un tipo de trámite municipal.
 */
public record TipoTramiteRequest(
        @NotBlank(message = "El código del tipo de trámite es obligatorio") @Size(max = 50) String codigo,
        @NotBlank(message = "El nombre del tipo de trámite es obligatorio") @Size(max = 150) String nombre,
        @Size(max = 2000) String descripcion,
        Boolean requiereInspeccion) {

    public TipoTramiteRequest(String codigo, String nombre, String descripcion) {
        this(codigo, nombre, descripcion, true);
    }
}
