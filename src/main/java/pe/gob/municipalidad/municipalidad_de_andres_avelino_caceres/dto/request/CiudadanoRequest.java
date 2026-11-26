package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoDocumento;

/**
 * Solicitud de registro de ciudadano.
 * No incluye id ni estado activo para evitar asignación masiva no autorizada.
 */
public record CiudadanoRequest(
        @NotNull(message = "El tipo de documento es obligatorio") TipoDocumento tipoDocumento,
        @NotBlank(message = "El número de documento es obligatorio") @Size(max = 20) String numeroDocumento,
        @NotBlank(message = "Los nombres son obligatorios") @Size(max = 100) String nombres,
        @NotBlank(message = "Los apellidos son obligatorios") @Size(max = 100) String apellidos,
        @Email(message = "El correo electrónico no tiene un formato válido") @Size(max = 150) String email,
        @Size(max = 20) String telefono,
        @Size(max = 255) String direccion) {
}
