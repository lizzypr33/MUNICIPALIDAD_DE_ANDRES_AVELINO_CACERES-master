package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.ResultadoInspeccion;

/**
 * Solicitud para registrar el resultado técnico de una inspección municipal.
 */
public record ResultadoInspeccionRequest(
        @NotNull(message = "El resultado de la inspección es obligatorio") ResultadoInspeccion resultado,
        LocalDateTime fechaRealizada,
        @NotBlank(message = "Las observaciones de la inspección son obligatorias") @Size(max = 4000) String observaciones,
        @Size(max = 4000) String recomendaciones,
        @Size(max = 2000) String justificacionRectificacion) {

    public ResultadoInspeccionRequest(ResultadoInspeccion resultado,
            LocalDateTime fechaRealizada,
            String observaciones,
            String recomendaciones) {
        this(resultado, fechaRealizada, observaciones, recomendaciones, null);
    }
}
