package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de subsanación de observaciones por parte del ciudadano titular o representante autorizado.
 */
public record SubsanarSolicitudRequest(
        @NotBlank(message = "El detalle de la subsanación es obligatorio")
        @Size(max = 2000, message = "El detalle de la subsanación no debe superar los 2000 caracteres") String observaciones) {
}
