package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.CrearNotificacionRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.NotificacionResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Notificacion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Usuario;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoNotificacion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ResourceNotFoundException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.NotificacionMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.NotificacionRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.UsuarioRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.security.CustomUserDetails;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.NotificacionService;

@Service
public class NotificacionServiceImpl implements NotificacionService {

    private final NotificacionRepository notificaciones;
    private final UsuarioRepository usuarios;
    private final NotificacionMapper mapper;

    public NotificacionServiceImpl(NotificacionRepository notificaciones,
            UsuarioRepository usuarios,
            NotificacionMapper mapper) {
        this.notificaciones = notificaciones;
        this.usuarios = usuarios;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<NotificacionResponse> listarMisNotificaciones(Pageable pageable) {
        Long usuarioId = exigirIdUsuarioAutenticado();
        Page<Notificacion> pagina = notificaciones.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId, pageable);
        return PaginaResponse.from(pagina.map(mapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public NotificacionResponse obtenerMiNotificacion(Long id) {
        Long usuarioId = exigirIdUsuarioAutenticado();
        Notificacion n = buscarYVerificarPertenencia(id, usuarioId);
        return mapper.toResponse(n);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<NotificacionResponse> listarMisNoLeidas(Pageable pageable) {
        Long usuarioId = exigirIdUsuarioAutenticado();
        Page<Notificacion> pagina = notificaciones.findByUsuarioIdAndLeidaFalseOrderByFechaCreacionDesc(
                usuarioId, pageable);
        return PaginaResponse.from(pagina.map(mapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public long contarMisNoLeidas() {
        Long usuarioId = exigirIdUsuarioAutenticado();
        return notificaciones.countByUsuarioIdAndLeidaFalse(usuarioId);
    }

    @Override
    @Transactional
    public NotificacionResponse marcarComoLeida(Long id) {
        Long usuarioId = exigirIdUsuarioAutenticado();
        Notificacion n = buscarYVerificarPertenencia(id, usuarioId);
        if (!n.isLeida()) {
            n.setLeida(true);
            n.setFechaLectura(LocalDateTime.now());
            notificaciones.save(n);
        }
        return mapper.toResponse(n);
    }

    @Override
    @Transactional
    public int marcarTodasComoLeidas() {
        Long usuarioId = exigirIdUsuarioAutenticado();
        List<Notificacion> pendientes = notificaciones.findByUsuarioIdAndLeidaFalse(usuarioId);
        if (pendientes.isEmpty()) {
            return 0;
        }
        LocalDateTime ahora = LocalDateTime.now();
        for (Notificacion n : pendientes) {
            n.setLeida(true);
            n.setFechaLectura(ahora);
        }
        notificaciones.saveAll(pendientes);
        return pendientes.size();
    }

    @Override
    @Transactional
    public NotificacionResponse crearNotificacionInterna(CrearNotificacionRequest request) {
        if (request == null || request.usuarioId() == null) {
            throw new BusinessException("El usuario destinatario de la notificación es obligatorio.");
        }
        if (!StringUtils.hasText(request.titulo()) || !StringUtils.hasText(request.mensaje())) {
            throw new BusinessException("El título y el mensaje de la notificación son obligatorios.");
        }
        Usuario destinatario = usuarios.findById(request.usuarioId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario destinatario no encontrado con id " + request.usuarioId()));

        Notificacion n = new Notificacion();
        n.setUsuario(destinatario);
        n.setTitulo(request.titulo().trim());
        n.setMensaje(request.mensaje().trim());
        n.setTipo(request.tipo() != null ? request.tipo() : TipoNotificacion.INFORMATIVA);
        n.setLeida(false);
        notificaciones.save(n);
        return mapper.toResponse(n);
    }

    @Override
    @Transactional
    public void notificarUsuario(Usuario destinatario, String titulo, String mensaje, TipoNotificacion tipo) {
        if (destinatario == null || destinatario.getId() == null
                || !StringUtils.hasText(titulo) || !StringUtils.hasText(mensaje)) {
            return;
        }
        Notificacion n = new Notificacion();
        n.setUsuario(destinatario);
        n.setTitulo(titulo.trim());
        n.setMensaje(mensaje.trim());
        n.setTipo(tipo != null ? tipo : TipoNotificacion.INFORMATIVA);
        n.setLeida(false);
        notificaciones.save(n);
    }

    private Notificacion buscarYVerificarPertenencia(Long id, Long usuarioIdAutenticado) {
        Notificacion n = notificaciones.findWithUsuarioById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada con id " + id));
        if (n.getUsuario() == null || !Objects.equals(n.getUsuario().getId(), usuarioIdAutenticado)) {
            throw new AccessDeniedException("No tiene permisos para acceder o modificar una notificación de otro usuario.");
        }
        return n;
    }

    private Long exigirIdUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Debe iniciar sesión para consultar sus notificaciones.");
        }
        if (auth.getPrincipal() instanceof CustomUserDetails cud && cud.getId() != null) {
            return cud.getId();
        }
        if (StringUtils.hasText(auth.getName())) {
            return usuarios.findByUsername(auth.getName())
                    .map(Usuario::getId)
                    .orElseThrow(() -> new AccessDeniedException("Usuario autenticado no encontrado."));
        }
        throw new AccessDeniedException("Sesión de usuario no válida.");
    }
}
