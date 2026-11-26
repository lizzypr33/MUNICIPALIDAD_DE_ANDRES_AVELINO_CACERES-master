package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper;

import org.springframework.stereotype.Component;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.NotificacionResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Notificacion;

/**
 * Convierte entidades {@link Notificacion} en DTO de respuesta.
 */
@Component
public class NotificacionMapper {

    public NotificacionResponse toResponse(Notificacion n) {
        if (n == null) {
            return null;
        }
        return new NotificacionResponse(
                n.getId(),
                n.getUsuario() != null ? n.getUsuario().getId() : null,
                n.getTitulo(),
                n.getMensaje(),
                n.getTipo(),
                n.isLeida(),
                n.getFechaCreacion(),
                n.getFechaLectura());
    }
}
