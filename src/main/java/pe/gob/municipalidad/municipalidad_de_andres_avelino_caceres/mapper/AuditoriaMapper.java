package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper;

import org.springframework.stereotype.Component;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.AuditoriaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Auditoria;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Usuario;

/**
 * Convierte entidades {@link Auditoria} en DTO de respuesta sin exponer contraseñas ni credenciales.
 */
@Component
public class AuditoriaMapper {

    public AuditoriaResponse toResponse(Auditoria a) {
        if (a == null) {
            return null;
        }
        Usuario u = a.getUsuario();
        return new AuditoriaResponse(
                a.getId(),
                u != null ? u.getId() : null,
                u != null ? u.getUsername() : null,
                u != null ? u.getNombres() + " " + u.getApellidos() : null,
                a.getAccion(),
                a.getEntidadAfectada(),
                a.getEntidadId(),
                a.getDescripcion(),
                a.getFechaOperacion(),
                a.getDireccionIp());
    }
}
