package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service;

import org.springframework.data.domain.Pageable;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.CrearNotificacionRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.NotificacionResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Usuario;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoNotificacion;

public interface NotificacionService {

    PaginaResponse<NotificacionResponse> listarMisNotificaciones(Pageable pageable);

    NotificacionResponse obtenerMiNotificacion(Long id);

    PaginaResponse<NotificacionResponse> listarMisNoLeidas(Pageable pageable);

    long contarMisNoLeidas();

    NotificacionResponse marcarComoLeida(Long id);

    int marcarTodasComoLeidas();

    NotificacionResponse crearNotificacionInterna(CrearNotificacionRequest request);

    void notificarUsuario(Usuario destinatario, String titulo, String mensaje, TipoNotificacion tipo);
}
