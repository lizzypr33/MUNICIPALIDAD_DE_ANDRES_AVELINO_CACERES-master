package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

import java.util.List;

/** Perfil del usuario autenticado. Nunca incluye la contraseña ni su hash. */
public record AuthenticatedUserResponse(
        Long id,
        String username,
        String nombres,
        String apellidos,
        String email,
        List<String> roles,
        boolean activo) {
}
