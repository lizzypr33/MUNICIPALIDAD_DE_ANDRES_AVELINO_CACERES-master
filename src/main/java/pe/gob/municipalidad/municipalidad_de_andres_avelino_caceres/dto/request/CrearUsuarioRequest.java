package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Campos desconocidos en el JSON (por ejemplo "activo" o "roles") se ignoran: nunca se mapean. */
public record CrearUsuarioRequest(
        @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[A-Za-z0-9._-]+$") String username,
        @NotBlank @Size(min = 12, max = 72) String password,
        @NotBlank @Size(max = 100) String nombres,
        @NotBlank @Size(max = 100) String apellidos,
        @NotBlank @Email @Size(max = 150) String email,
        @Size(max = 20) String telefono,
        @NotEmpty Set<@NotNull Long> rolIds) {
}
