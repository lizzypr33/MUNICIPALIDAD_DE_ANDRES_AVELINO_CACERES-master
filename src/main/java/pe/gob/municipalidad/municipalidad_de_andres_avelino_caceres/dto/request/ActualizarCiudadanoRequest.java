package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoDocumento;

/**
 * Solicitud de actualización de ciudadano.
 * No incluye id. Si se envía tipoDocumento o numeroDocumento, el servicio valida que no tenga expedientes vinculados.
 */
public record ActualizarCiudadanoRequest(
        TipoDocumento tipoDocumento,
        @Size(max = 20) String numeroDocumento,
        @NotBlank(message = "Los nombres son obligatorios") @Size(max = 100) String nombres,
        @NotBlank(message = "Los apellidos son obligatorios") @Size(max = 100) String apellidos,
        @Email(message = "El correo electrónico no tiene un formato válido") @Size(max = 150) String email,
        @Size(max = 20) String telefono,
        @Size(max = 255) String direccion) {
}
