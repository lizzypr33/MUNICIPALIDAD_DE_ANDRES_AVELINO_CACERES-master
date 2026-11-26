package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud para programar una inspección técnica municipal asociada a una solicitud.
 */
public record ProgramarInspeccionRequest(
        @NotNull(message = "El identificador de la solicitud es obligatorio") Long solicitudId,
        @NotNull(message = "El identificador del inspector es obligatorio") Long inspectorId,
        @NotNull(message = "La fecha programada de la inspección es obligatoria") LocalDateTime fechaProgramada,
        @Size(max = 2000) String observaciones) {
}
