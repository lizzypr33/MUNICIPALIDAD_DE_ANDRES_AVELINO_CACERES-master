package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud para registrar la declaración de un pago municipal asociado a un expediente.
 */
public record RegistrarPagoRequest(
        @NotNull(message = "El identificador de la solicitud es obligatorio") Long solicitudId,
        @NotBlank(message = "El código de operación o referencia es obligatorio") @Size(max = 50) String codigoOperacion,
        @NotBlank(message = "El concepto del pago es obligatorio") @Size(max = 200) String concepto,
        @NotNull(message = "El monto del pago es obligatorio")
        @DecimalMin(value = "0.01", inclusive = true, message = "El monto del pago debe ser mayor que cero") BigDecimal monto,
        @Size(max = 3) String moneda,
        @NotNull(message = "La fecha declarada del pago es obligatoria") LocalDateTime fechaPago,
        @NotBlank(message = "El medio de pago es obligatorio") @Size(max = 50) String medioPago,
        @Size(max = 2000) String observaciones) {
}
