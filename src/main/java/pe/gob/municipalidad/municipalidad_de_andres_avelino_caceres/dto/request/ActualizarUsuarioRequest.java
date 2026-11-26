package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** No incluye contraseña ni roles: se cambian con sus endpoints específicos. */
public record ActualizarUsuarioRequest(
        @NotBlank @Size(max = 100) String nombres,
        @NotBlank @Size(max = 100) String apellidos,
        @NotBlank @Email @Size(max = 150) String email,
        @Size(max = 20) String telefono) {
}
