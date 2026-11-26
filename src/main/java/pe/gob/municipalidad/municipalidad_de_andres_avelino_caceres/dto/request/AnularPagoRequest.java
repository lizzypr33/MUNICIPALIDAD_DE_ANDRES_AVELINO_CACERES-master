package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud para anular administrativamente un registro de pago con motivo obligatorio.
 */
public record AnularPagoRequest(
        @NotBlank(message = "El motivo de anulación del pago es obligatorio") @Size(max = 2000) String motivo,
        @Size(max = 2000) String observaciones) {

    public AnularPagoRequest(String motivo) {
        this(motivo, null);
    }
}
