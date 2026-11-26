package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/** Vista administrativa de un usuario. Nunca incluye la contraseña ni su hash. */
public record UsuarioResponse(
        Long id,
        String username,
        String nombres,
        String apellidos,
        String email,
        String telefono,
        boolean activo,
        List<String> roles,
        LocalDateTime fechaCreacion,
        LocalDateTime ultimoAcceso) {
}
