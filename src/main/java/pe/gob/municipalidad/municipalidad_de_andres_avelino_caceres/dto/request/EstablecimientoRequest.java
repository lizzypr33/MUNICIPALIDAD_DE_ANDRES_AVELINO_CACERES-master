package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de registro de establecimiento comercial.
 * El RUC y el representante son opcionales según el tipo de titular y trámite.
 */
public record EstablecimientoRequest(
        @NotBlank(message = "El nombre comercial es obligatorio") @Size(max = 200) String nombreComercial,
        @Size(max = 200) String razonSocial,
        @Size(max = 11) String ruc,
        @NotBlank(message = "La dirección es obligatoria") @Size(max = 255) String direccion,
        @Size(max = 255) String referencia,
        @NotBlank(message = "El giro de negocio es obligatorio") @Size(max = 150) String giroNegocio,
        @NotNull(message = "El área en metros cuadrados es obligatoria")
        @DecimalMin(value = "0.01", message = "El área debe ser mayor que cero") BigDecimal areaMetrosCuadrados,
        @NotNull(message = "El número de pisos es obligatorio")
        @Min(value = 1, message = "El número de pisos debe ser mayor que cero") Integer numeroPisos,
        @Size(max = 20) String telefono,
        @Email(message = "El correo electrónico no tiene un formato válido") @Size(max = 150) String email,
        @NotNull(message = "El ciudadano titular es obligatorio") Long ciudadanoId,
        Long representanteId) {
}
