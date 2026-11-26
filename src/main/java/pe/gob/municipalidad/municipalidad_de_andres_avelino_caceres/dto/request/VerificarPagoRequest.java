package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud para verificar y confirmar administrativamente un pago registrado.
 */
public record VerificarPagoRequest(
        @Size(max = 50) String codigoOperacionConfirmado,
        @NotBlank(message = "Las observaciones o respaldo de verificación son obligatorios")
        @Size(max = 2000) String observaciones) {

    public VerificarPagoRequest(String observaciones) {
        this(null, observaciones);
    }
}
