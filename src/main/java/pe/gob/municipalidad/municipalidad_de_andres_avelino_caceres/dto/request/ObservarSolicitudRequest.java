package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de observación de un expediente en revisión. Exige motivo obligatorio.
 */
public record ObservarSolicitudRequest(
        @NotBlank(message = "El motivo de observación es obligatorio")
        @Size(max = 2000, message = "El motivo de observación no debe superar los 2000 caracteres") String observaciones) {
}
