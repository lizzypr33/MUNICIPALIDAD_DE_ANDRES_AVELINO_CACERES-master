package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud para rechazar un pago inconsistente con motivo obligatorio.
 */
public record RechazarPagoRequest(
        @NotBlank(message = "El motivo de rechazo del pago es obligatorio") @Size(max = 2000) String motivo,
        @Size(max = 2000) String observaciones) {

    public RechazarPagoRequest(String motivo) {
        this(motivo, null);
    }
}
