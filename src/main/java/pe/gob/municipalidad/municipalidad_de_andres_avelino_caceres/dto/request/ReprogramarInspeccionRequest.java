package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud para reprogramar una inspección técnica pendiente.
 */
public record ReprogramarInspeccionRequest(
        @NotNull(message = "La nueva fecha programada es obligatoria") LocalDateTime fechaProgramada,
        Long inspectorId,
        @NotBlank(message = "El motivo de reprogramación es obligatorio") @Size(max = 2000) String motivo) {
}
