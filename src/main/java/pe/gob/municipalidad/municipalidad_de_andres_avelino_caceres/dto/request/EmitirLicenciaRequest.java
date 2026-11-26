package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud para emitir una licencia de funcionamiento a partir de una solicitud aprobada.
 * El código de licencia siempre es generado en el backend.
 */
public record EmitirLicenciaRequest(
        @NotNull(message = "El identificador de la solicitud aprobada es obligatorio") Long solicitudId,
        LocalDate fechaVencimiento,
        @Size(max = 2000) String observaciones) {

    public EmitirLicenciaRequest(Long solicitudId, String observaciones) {
        this(solicitudId, null, observaciones);
    }
}
