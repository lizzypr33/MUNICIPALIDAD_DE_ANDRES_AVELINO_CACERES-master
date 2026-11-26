package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.AuthenticatedUserResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.UsuarioResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Rol;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Usuario;

/** Convierte entidades en DTO. Nunca copia la contraseña. */
@Component
public class UsuarioMapper {

    public UsuarioResponse toResponse(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getUsername(), u.getNombres(), u.getApellidos(),
                u.getEmail(), u.getTelefono(), u.isActivo(), nombresRoles(u),
                u.getFechaCreacion(), u.getUltimoAcceso());
    }

    public AuthenticatedUserResponse toAuthenticated(Usuario u) {
        return new AuthenticatedUserResponse(u.getId(), u.getUsername(), u.getNombres(), u.getApellidos(),
                u.getEmail(), nombresRoles(u), u.isActivo());
    }

    private List<String> nombresRoles(Usuario u) {
        return u.getRoles().stream().map(Rol::getNombre).sorted(Comparator.naturalOrder()).toList();
    }
}
