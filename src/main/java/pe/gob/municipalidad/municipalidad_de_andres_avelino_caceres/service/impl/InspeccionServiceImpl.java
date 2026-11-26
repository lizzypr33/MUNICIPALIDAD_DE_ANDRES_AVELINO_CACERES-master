package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.CambiarEstadoLicenciaRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ProgramarInspeccionRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ReprogramarInspeccionRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ResultadoInspeccionRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.InspeccionDetalleResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.InspeccionResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Ciudadano;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Inspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Notificacion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Solicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Usuario;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoInspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.ResultadoInspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoNotificacion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ConflictException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ResourceNotFoundException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.InspeccionMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.CiudadanoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.InspeccionRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.LicenciaRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.NotificacionRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.SolicitudRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.UsuarioRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.security.CustomUserDetails;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.AuditoriaService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.InspeccionService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.validation.InspeccionValidator;

@Service
public class InspeccionServiceImpl implements InspeccionService {

    private static final Set<EstadoInspeccion> ESTADOS_ACTIVOS = EnumSet.of(
            EstadoInspeccion.PROGRAMADA,
            EstadoInspeccion.REPROGRAMADA,
            EstadoInspeccion.EN_PROCESO);

    private final InspeccionRepository inspecciones;
    private final SolicitudRepository solicitudes;
    private final UsuarioRepository usuarios;
    private final CiudadanoRepository ciudadanos;
    private final LicenciaRepository licencias;
    private final NotificacionRepository notificaciones;
    private final InspeccionValidator validator;
    private final InspeccionMapper mapper;
    private final AuditoriaService auditoria;

    public InspeccionServiceImpl(InspeccionRepository inspecciones,
            SolicitudRepository solicitudes,
            UsuarioRepository usuarios,
            CiudadanoRepository ciudadanos,
            LicenciaRepository licencias,
            NotificacionRepository notificaciones,
            InspeccionValidator validator,
            InspeccionMapper mapper,
            AuditoriaService auditoria) {
        this.inspecciones = inspecciones;
        this.solicitudes = solicitudes;
        this.usuarios = usuarios;
        this.ciudadanos = ciudadanos;
        this.licencias = licencias;
        this.notificaciones = notificaciones;
        this.validator = validator;
        this.mapper = mapper;
        this.auditoria = auditoria;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<InspeccionResponse> listar(
            Long solicitudId,
            Long inspectorId,
            EstadoInspeccion estado,
            ResultadoInspeccion resultado,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Pageable pageable) {

        Long ciudadanoFiltroId = null;
        if (esSoloCiudadano()) {
            Ciudadano ciudadanoActual = obtenerCiudadanoAutenticado();
            ciudadanoFiltroId = ciudadanoActual.getId();
            if (solicitudId != null) {
                Solicitud s = buscarSolicitud(solicitudId);
                if (!Objects.equals(s.getCiudadano().getId(), ciudadanoFiltroId)) {
                    throw new AccessDeniedException("No tiene permisos para consultar las inspecciones de otro ciudadano.");
                }
            }
        }

        Page<Inspeccion> pagina = inspecciones.findAll(
                filtros(solicitudId, inspectorId, ciudadanoFiltroId, estado, resultado, fechaDesde, fechaHasta),
                pageable);
        return PaginaResponse.from(pagina.map(mapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public InspeccionDetalleResponse obtener(Long id) {
        Inspeccion i = buscarConRelaciones(id);
        verificarAccesoCiudadanoSiCorresponde(i.getSolicitud());
        return mapper.toDetalleResponse(i);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InspeccionResponse> listarPorSolicitud(Long solicitudId) {
        Solicitud solicitud = buscarSolicitud(solicitudId);
        verificarAccesoCiudadanoSiCorresponde(solicitud);
        return inspecciones.findBySolicitudIdOrderByIdDesc(solicitudId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public InspeccionDetalleResponse programar(ProgramarInspeccionRequest request) {
        if (esSoloCiudadano()) {
            throw new AccessDeniedException("Un ciudadano no está autorizado para programar inspecciones municipales.");
        }

        Solicitud solicitud = buscarSolicitud(request.solicitudId());
        validator.validarSolicitudParaProgramacion(solicitud);
        validator.validarFechaProgramada(request.fechaProgramada());

        Usuario inspector = usuarios.findWithRolesById(request.inspectorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario inspector no encontrado con id " + request.inspectorId()));
        validator.validarInspector(inspector);

        if (inspecciones.existsBySolicitudIdAndEstadoIn(solicitud.getId(), ESTADOS_ACTIVOS)) {
            throw new ConflictException(
                    "La solicitud ya cuenta con una inspección técnica programada o en proceso.");
        }

        if (inspecciones.existsByInspectorIdAndFechaProgramadaAndEstadoIn(
                inspector.getId(), request.fechaProgramada(), ESTADOS_ACTIVOS)) {
            throw new ConflictException(
                    "El inspector seleccionado ya tiene otra inspección programada en la misma fecha y hora.");
        }

        Inspeccion i = new Inspeccion();
        i.setSolicitud(solicitud);
        i.setInspector(inspector);
        i.setFechaProgramada(request.fechaProgramada());
        i.setEstado(EstadoInspeccion.PROGRAMADA);
        i.setObservaciones(normalizarOpcional(request.observaciones()));
        inspecciones.save(i);

        auditoria.registrar("PROGRAMAR_INSPECCION", "Inspeccion", i.getId(),
                "Inspección programada para solicitud id=" + solicitud.getId()
                        + " con inspector " + inspector.getUsername()
                        + " en fecha " + request.fechaProgramada());

        crearNotificacionInterna(inspector,
                "Nueva inspección técnica asignada",
                "Se le ha programado una inspección técnica para el expediente "
                        + valorExpediente(solicitud) + " en fecha " + request.fechaProgramada() + ".",
                TipoNotificacion.INSPECCION);
        notificarCiudadanoSiTieneUsuario(solicitud,
                "Inspección municipal programada",
                "Se ha programado una inspección técnica para su solicitud (" + valorExpediente(solicitud)
                        + ") el día " + request.fechaProgramada() + ".",
                TipoNotificacion.INSPECCION);

        return mapper.toDetalleResponse(i);
    }

    @Override
    @Transactional
    public InspeccionDetalleResponse reprogramar(Long id, ReprogramarInspeccionRequest request) {
        if (esSoloCiudadano()) {
            throw new AccessDeniedException("Un ciudadano no está autorizado para reprogramar inspecciones.");
        }

        Inspeccion i = buscarConRelaciones(id);
        String motivo = normalizarObligatorio(request.motivo(), "El motivo de reprogramación es obligatorio.");
        validator.validarReprogramable(i, motivo);
        validator.validarFechaProgramada(request.fechaProgramada());

        Usuario inspectorDestino = i.getInspector();
        if (request.inspectorId() != null
                && (inspectorDestino == null || !Objects.equals(inspectorDestino.getId(), request.inspectorId()))) {
            inspectorDestino = usuarios.findWithRolesById(request.inspectorId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Usuario inspector no encontrado con id " + request.inspectorId()));
            validator.validarInspector(inspectorDestino);
        }

        if (inspectorDestino != null
                && inspecciones.existsByInspectorIdAndFechaProgramadaAndEstadoInAndIdNot(
                        inspectorDestino.getId(), request.fechaProgramada(), ESTADOS_ACTIVOS, i.getId())) {
            throw new ConflictException(
                    "El inspector asignado ya tiene otra inspección programada en la fecha y hora indicadas.");
        }

        LocalDateTime fechaAnterior = i.getFechaProgramada();
        i.setFechaProgramada(request.fechaProgramada());
        i.setInspector(inspectorDestino);
        i.setMotivoReprogramacion(motivo);
        i.setEstado(EstadoInspeccion.PROGRAMADA);

        auditoria.registrar("REPROGRAMAR_INSPECCION", "Inspeccion", i.getId(),
                "Inspección reprogramada de " + fechaAnterior + " a " + request.fechaProgramada()
                        + ". Motivo: " + motivo);

        if (inspectorDestino != null) {
            crearNotificacionInterna(inspectorDestino,
                    "Inspección técnica reprogramada",
                    "La inspección del expediente " + valorExpediente(i.getSolicitud())
                            + " ha sido reprogramada para " + request.fechaProgramada() + ". Motivo: " + motivo,
                    TipoNotificacion.INSPECCION);
        }

        return mapper.toDetalleResponse(i);
    }

    @Override
    @Transactional
    public InspeccionDetalleResponse iniciar(Long id) {
        if (esSoloCiudadano()) {
            throw new AccessDeniedException("Un ciudadano no está autorizado para iniciar inspecciones.");
        }

        Inspeccion i = buscarConRelaciones(id);
        validator.validarTransicion(i.getEstado(), EstadoInspeccion.EN_PROCESO);
        i.setEstado(EstadoInspeccion.EN_PROCESO);

        auditoria.registrar("INICIAR_INSPECCION", "Inspeccion", i.getId(),
                "Inspección iniciada (EN_PROCESO) para solicitud id=" + i.getSolicitud().getId());

        return mapper.toDetalleResponse(i);
    }

    @Override
    @Transactional
    public InspeccionDetalleResponse registrarResultado(Long id, ResultadoInspeccionRequest request) {
        if (esSoloCiudadano()) {
            throw new AccessDeniedException(
                    "Un ciudadano no está autorizado para registrar resultados de inspección municipal.");
        }

        Inspeccion i = buscarConRelaciones(id);
        String observaciones = normalizarObligatorio(
                request.observaciones(), "Las observaciones del resultado de inspección son obligatorias.");
        String recomendaciones = normalizarOpcional(request.recomendaciones());
        String justificacion = normalizarOpcional(request.justificacionRectificacion());

        boolean solicitudTieneLicencia = licencias.existsBySolicitudId(i.getSolicitud().getId());
        boolean tieneAutoridadSupervision = tieneRol("ROLE_ADMINISTRADOR") || tieneRol("ROLE_SUPERVISOR");

        validator.validarRegistroResultado(
                i, request.resultado(), observaciones, justificacion,
                tieneAutoridadSupervision, solicitudTieneLicencia);

        boolean esRectificacion = i.getEstado() == EstadoInspeccion.FINALIZADA
                || i.getEstado() == EstadoInspeccion.REALIZADA;

        if (esRectificacion) {
            ResultadoInspeccion resultadoAnterior = i.getResultado();
            String observacionPrevia = i.getObservaciones();
            String trazaRectificacion = "[Rectificación autorizada de resultado previo (" + resultadoAnterior
                    + ") - Justificación: " + justificacion
                    + (StringUtils.hasText(observacionPrevia) ? " | Obs. previa: " + observacionPrevia : "")
                    + "] " + observaciones;
            i.setObservaciones(trazaRectificacion);
        } else {
            validator.validarTransicion(i.getEstado(), EstadoInspeccion.FINALIZADA);
            i.setObservaciones(observaciones);
        }

        i.setEstado(EstadoInspeccion.FINALIZADA);
        i.setResultado(request.resultado());
        i.setFechaRealizada(request.fechaRealizada() != null ? request.fechaRealizada() : LocalDateTime.now());
        i.setRecomendaciones(recomendaciones);

        auditoria.registrar(
                esRectificacion ? "RECTIFICAR_RESULTADO_INSPECCION" : "REGISTRAR_RESULTADO_INSPECCION",
                "Inspeccion",
                i.getId(),
                "Resultado de inspección registrado: " + request.resultado()
                        + " para solicitud id=" + i.getSolicitud().getId());

        notificarCiudadanoSiTieneUsuario(i.getSolicitud(),
                "Resultado de inspección municipal registrado",
                "La inspección técnica de su solicitud (" + valorExpediente(i.getSolicitud())
                        + ") concluyó con resultado: " + request.resultado() + ".",
                TipoNotificacion.INSPECCION);

        return mapper.toDetalleResponse(i);
    }

    @Override
    @Transactional
    public InspeccionDetalleResponse cancelar(Long id, CambiarEstadoLicenciaRequest request) {
        if (esSoloCiudadano()) {
            throw new AccessDeniedException("Un ciudadano no está autorizado para cancelar inspecciones municipales.");
        }

        Inspeccion i = buscarConRelaciones(id);
        String motivo = normalizarObligatorio(
                request != null ? request.motivo() : null, "El motivo de cancelación de la inspección es obligatorio.");

        validator.validarTransicion(i.getEstado(), EstadoInspeccion.CANCELADA);
        i.setEstado(EstadoInspeccion.CANCELADA);
        i.setMotivoReprogramacion(motivo);

        auditoria.registrar("CANCELAR_INSPECCION", "Inspeccion", i.getId(),
                "Inspección cancelada. Motivo: " + motivo);

        return mapper.toDetalleResponse(i);
    }

    private Inspeccion buscarConRelaciones(Long id) {
        return inspecciones.findWithRelacionesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspección no encontrada con id " + id));
    }

    private Solicitud buscarSolicitud(Long solicitudId) {
        return solicitudes.findWithRelacionesById(solicitudId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con id " + solicitudId));
    }

    private void verificarAccesoCiudadanoSiCorresponde(Solicitud solicitud) {
        if (!esSoloCiudadano()) {
            return;
        }
        Ciudadano ciudadanoActual = obtenerCiudadanoAutenticado();
        if (solicitud == null || solicitud.getCiudadano() == null
                || !Objects.equals(solicitud.getCiudadano().getId(), ciudadanoActual.getId())) {
            throw new AccessDeniedException("No tiene permisos para acceder a las inspecciones de otro ciudadano.");
        }
    }

    private Ciudadano obtenerCiudadanoAutenticado() {
        Long usuarioId = obtenerIdUsuarioAutenticado();
        if (usuarioId == null) {
            throw new AccessDeniedException("Sesión de ciudadano no válida.");
        }
        return ciudadanos.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new AccessDeniedException(
                        "El usuario autenticado no tiene un registro de ciudadano asociado."));
    }

    private Long obtenerIdUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails cud) {
            return cud.getId();
        }
        return null;
    }

    private boolean esSoloCiudadano() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        boolean tieneCiudadano = false;
        for (GrantedAuthority ga : auth.getAuthorities()) {
            String rol = ga.getAuthority();
            if ("ROLE_ADMINISTRADOR".equals(rol)
                    || "ROLE_FUNCIONARIO".equals(rol)
                    || "ROLE_SUPERVISOR".equals(rol)) {
                return false;
            }
            if ("ROLE_CIUDADANO".equals(rol)) {
                tieneCiudadano = true;
            }
        }
        return tieneCiudadano;
    }

    private boolean tieneRol(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }

    private void crearNotificacionInterna(Usuario destinatario, String titulo, String mensaje, TipoNotificacion tipo) {
        if (destinatario == null || destinatario.getId() == null) {
            return;
        }
        Notificacion n = new Notificacion();
        n.setUsuario(destinatario);
        n.setTitulo(titulo);
        n.setMensaje(mensaje);
        n.setTipo(tipo);
        n.setLeida(false);
        notificaciones.save(n);
    }

    private void notificarCiudadanoSiTieneUsuario(Solicitud solicitud, String titulo, String mensaje,
            TipoNotificacion tipo) {
        if (solicitud != null && solicitud.getCiudadano() != null && solicitud.getCiudadano().getUsuario() != null) {
            crearNotificacionInterna(solicitud.getCiudadano().getUsuario(), titulo, mensaje, tipo);
        }
    }

    private static String valorExpediente(Solicitud s) {
        if (s == null) {
            return "N/A";
        }
        return StringUtils.hasText(s.getNumeroExpediente()) ? s.getNumeroExpediente() : "ID-" + s.getId();
    }

    private Specification<Inspeccion> filtros(
            Long solicitudId,
            Long inspectorId,
            Long ciudadanoId,
            EstadoInspeccion estado,
            ResultadoInspeccion resultado,
            LocalDate fechaDesde,
            LocalDate fechaHasta) {

        return (Root<Inspeccion> root, jakarta.persistence.criteria.CriteriaQuery<?> query,
                jakarta.persistence.criteria.CriteriaBuilder cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (solicitudId != null) {
                predicados.add(cb.equal(root.get("solicitud").get("id"), solicitudId));
            }
            if (inspectorId != null) {
                predicados.add(cb.equal(root.get("inspector").get("id"), inspectorId));
            }
            if (ciudadanoId != null) {
                predicados.add(cb.equal(root.get("solicitud").get("ciudadano").get("id"), ciudadanoId));
            }
            if (estado != null) {
                predicados.add(cb.equal(root.get("estado"), estado));
            }
            if (resultado != null) {
                predicados.add(cb.equal(root.get("resultado"), resultado));
            }
            if (fechaDesde != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("fechaProgramada"), fechaDesde.atStartOfDay()));
            }
            if (fechaHasta != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("fechaProgramada"), fechaHasta.atTime(LocalTime.MAX)));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }

    private static String normalizarObligatorio(String valor, String mensajeError) {
        if (!StringUtils.hasText(valor)) {
            throw new BusinessException(mensajeError);
        }
        return valor.trim().replaceAll("\\s+", " ");
    }

    private static String normalizarOpcional(String valor) {
        if (!StringUtils.hasText(valor)) {
            return null;
        }
        return valor.trim().replaceAll("\\s+", " ");
    }
}
