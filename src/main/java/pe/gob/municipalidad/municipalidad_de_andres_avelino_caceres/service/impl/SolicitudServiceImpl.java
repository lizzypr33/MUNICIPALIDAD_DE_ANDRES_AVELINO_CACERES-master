package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ActualizarSolicitudRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.AsignarFuncionarioRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ObservarSolicitudRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.PresentarSolicitudRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ResolverSolicitudRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.SolicitudRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.SubsanarSolicitudRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.DocumentoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ExpedienteResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.HistorialSolicitudResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.SolicitudDetalleResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.SolicitudResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Ciudadano;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Documento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Establecimiento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.HistorialSolicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Notificacion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Representante;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Requisito;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Solicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.TipoTramite;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.TipoTramiteRequisito;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Usuario;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoDocumento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoSolicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoNotificacion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ConflictException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ResourceNotFoundException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.DocumentoMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.SolicitudMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.CiudadanoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.DocumentoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.EstablecimientoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.HistorialSolicitudRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.NotificacionRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.RepresentanteRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.RequisitoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.SolicitudRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.TipoTramiteRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.TipoTramiteRequisitoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.UsuarioRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.security.CustomUserDetails;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.AuditoriaService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.ExpedienteService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.SolicitudService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.validation.SolicitudValidator;

@Service
public class SolicitudServiceImpl implements SolicitudService {

    private final SolicitudRepository solicitudes;
    private final CiudadanoRepository ciudadanos;
    private final EstablecimientoRepository establecimientos;
    private final TipoTramiteRepository tiposTramite;
    private final TipoTramiteRequisitoRepository tipoTramiteRequisitos;
    private final RequisitoRepository requisitos;
    private final RepresentanteRepository representantes;
    private final DocumentoRepository documentos;
    private final HistorialSolicitudRepository historialRepository;
    private final UsuarioRepository usuarios;
    private final NotificacionRepository notificaciones;
    private final ExpedienteService expedienteService;
    private final SolicitudValidator solicitudValidator;
    private final SolicitudMapper mapper;
    private final DocumentoMapper documentoMapper;
    private final AuditoriaService auditoria;

    public SolicitudServiceImpl(SolicitudRepository solicitudes,
            CiudadanoRepository ciudadanos,
            EstablecimientoRepository establecimientos,
            TipoTramiteRepository tiposTramite,
            TipoTramiteRequisitoRepository tipoTramiteRequisitos,
            RequisitoRepository requisitos,
            RepresentanteRepository representantes,
            DocumentoRepository documentos,
            HistorialSolicitudRepository historialRepository,
            UsuarioRepository usuarios,
            NotificacionRepository notificaciones,
            ExpedienteService expedienteService,
            SolicitudValidator solicitudValidator,
            SolicitudMapper mapper,
            DocumentoMapper documentoMapper,
            AuditoriaService auditoria) {
        this.solicitudes = solicitudes;
        this.ciudadanos = ciudadanos;
        this.establecimientos = establecimientos;
        this.tiposTramite = tiposTramite;
        this.tipoTramiteRequisitos = tipoTramiteRequisitos;
        this.requisitos = requisitos;
        this.representantes = representantes;
        this.documentos = documentos;
        this.historialRepository = historialRepository;
        this.usuarios = usuarios;
        this.notificaciones = notificaciones;
        this.expedienteService = expedienteService;
        this.solicitudValidator = solicitudValidator;
        this.mapper = mapper;
        this.documentoMapper = documentoMapper;
        this.auditoria = auditoria;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<SolicitudResponse> listar(String numeroExpediente, Long ciudadanoId, Long establecimientoId,
            Long tipoTramiteId, EstadoSolicitud estado, LocalDate fechaInicial, LocalDate fechaFinal,
            Long usuarioResponsableId, Pageable pageable) {

        Long ciudadanoFiltroEfectivo = ciudadanoId;
        if (esSoloRolCiudadano()) {
            Long usuarioActualId = obtenerUsuarioActualId();
            Ciudadano propio = usuarioActualId != null
                    ? ciudadanos.findByUsuarioId(usuarioActualId).orElse(null)
                    : null;
            if (propio == null) {
                throw new AccessDeniedException(
                        "Su cuenta de usuario no se encuentra vinculada a un registro de ciudadano.");
            }
            if (ciudadanoId != null && !ciudadanoId.equals(propio.getId())) {
                throw new AccessDeniedException("No tiene permisos para consultar solicitudes de otros ciudadanos.");
            }
            ciudadanoFiltroEfectivo = propio.getId();
        }

        if (fechaInicial != null && fechaFinal != null && fechaFinal.isBefore(fechaInicial)) {
            throw new BusinessException("La fecha final no puede ser anterior a la fecha inicial.");
        }

        Page<Solicitud> pagina = solicitudes.findAll(
                filtros(numeroExpediente, ciudadanoFiltroEfectivo, establecimientoId, tipoTramiteId, estado,
                        fechaInicial, fechaFinal, usuarioResponsableId),
                pageable);
        return PaginaResponse.from(pagina.map(mapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public SolicitudDetalleResponse obtener(Long id) {
        Solicitud s = buscar(id);
        verificarAccesoSolicitud(s);
        List<DocumentoResponse> docs = documentos.findBySolicitudIdAndActivoTrueOrderByFechaSubidaAscIdAsc(id).stream()
                .map(documentoMapper::toResponse)
                .toList();
        List<HistorialSolicitudResponse> hist = historialRepository
                .findBySolicitudIdOrderByFechaCambioAscIdAsc(id).stream()
                .map(mapper::toHistorialResponse)
                .toList();
        return mapper.toDetalleResponse(s, docs, hist);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpedienteResponse obtenerPorExpediente(String numeroExpediente) {
        return expedienteService.obtenerPorNumero(numeroExpediente);
    }

    @Override
    @Transactional
    public SolicitudResponse crear(SolicitudRequest request) {
        if (request.ciudadanoId() == null) {
            throw new BusinessException("El ciudadano titular es obligatorio.");
        }
        if (request.establecimientoId() == null) {
            throw new BusinessException("El establecimiento comercial es obligatorio.");
        }
        if (request.tipoTramiteId() == null) {
            throw new BusinessException("El tipo de trámite es obligatorio.");
        }

        Ciudadano ciudadano = buscarCiudadano(request.ciudadanoId());
        verificarAccesoTitularidadCiudadano(ciudadano);

        Establecimiento establecimiento = buscarEstablecimiento(request.establecimientoId());
        TipoTramite tipoTramite = buscarTipoTramite(request.tipoTramiteId());
        Representante representante = buscarRepresentanteOpcional(request.representanteId());

        solicitudValidator.validarEntidadesRelacionadas(ciudadano, establecimiento, tipoTramite, representante);

        Solicitud s = new Solicitud();
        s.setCiudadano(ciudadano);
        s.setEstablecimiento(establecimiento);
        s.setTipoTramite(tipoTramite);
        s.setRepresentante(representante);
        s.setEstado(EstadoSolicitud.BORRADOR);
        s.setNumeroExpediente(null);
        s.setFechaPresentacion(null);
        s.setObservaciones(normalizarOpcional(request.observaciones()));
        solicitudes.save(s);

        registrarHistorial(s, null, EstadoSolicitud.BORRADOR, "Solicitud registrada en estado BORRADOR.");
        auditoria.registrar("CREAR_BORRADOR_SOLICITUD", "Solicitud", s.getId(),
                "Borrador de solicitud creado para ciudadano ID " + ciudadano.getId()
                        + " y establecimiento ID " + establecimiento.getId());

        return mapper.toResponse(s);
    }

    @Override
    @Transactional
    public SolicitudResponse actualizar(Long id, ActualizarSolicitudRequest request) {
        Solicitud s = buscar(id);
        verificarAccesoSolicitud(s);
        solicitudValidator.validarModificableEnBorrador(s);

        Establecimiento nuevoEst = request.establecimientoId() != null
                ? buscarEstablecimiento(request.establecimientoId())
                : s.getEstablecimiento();
        TipoTramite nuevoTramite = request.tipoTramiteId() != null
                ? buscarTipoTramite(request.tipoTramiteId())
                : s.getTipoTramite();
        Representante nuevoRep = request.representanteId() != null
                ? buscarRepresentanteOpcional(request.representanteId())
                : s.getRepresentante();

        solicitudValidator.validarEntidadesRelacionadas(s.getCiudadano(), nuevoEst, nuevoTramite, nuevoRep);

        s.setEstablecimiento(nuevoEst);
        s.setTipoTramite(nuevoTramite);
        s.setRepresentante(nuevoRep);
        s.setObservaciones(normalizarOpcional(request.observaciones()));

        auditoria.registrar("ACTUALIZAR_SOLICITUD", "Solicitud", s.getId(),
                "Solicitud en borrador actualizada (ID " + s.getId() + ").");
        return mapper.toResponse(s);
    }

    @Override
    @Transactional
    public SolicitudResponse presentar(Long id, PresentarSolicitudRequest request) {
        Solicitud s = buscar(id);
        verificarAccesoSolicitud(s);

        solicitudValidator.validarTransicion(s.getEstado(), EstadoSolicitud.PRESENTADA);
        solicitudValidator.validarEntidadesRelacionadas(
                s.getCiudadano(), s.getEstablecimiento(), s.getTipoTramite(), s.getRepresentante());

        List<Requisito> obligatorios = obtenerRequisitosObligatoriosVigentes(s.getTipoTramite().getId());
        validarDocumentosObligatoriosParaPresentar(s.getId(), obligatorios);

        String snapshotIds = obligatorios.stream()
                .map(r -> String.valueOf(r.getId()))
                .collect(Collectors.joining(","));
        s.setRequisitosSnapshot(snapshotIds);

        String numeroExpediente = expedienteService.generarNumeroExpediente();
        EstadoSolicitud anterior = s.getEstado();
        s.setNumeroExpediente(numeroExpediente);
        s.setFechaPresentacion(LocalDateTime.now());
        s.setEstado(EstadoSolicitud.PRESENTADA);
        if (request != null && StringUtils.hasText(request.observaciones())) {
            s.setObservaciones(normalizarOpcional(request.observaciones()));
        }

        String obsHistorial = (request != null && StringUtils.hasText(request.observaciones()))
                ? normalizarOpcional(request.observaciones())
                : "Solicitud presentada con expediente " + numeroExpediente;
        registrarHistorial(s, anterior, EstadoSolicitud.PRESENTADA, obsHistorial);

        auditoria.registrar("PRESENTAR_SOLICITUD", "Solicitud", s.getId(),
                "Solicitud presentada con expediente " + numeroExpediente);
        notificarCiudadano(s, "Solicitud presentada",
                "Su solicitud ha sido presentada con el expediente " + numeroExpediente + ".",
                TipoNotificacion.SOLICITUD);

        return mapper.toResponse(s);
    }

    @Override
    @Transactional
    public SolicitudResponse asignarFuncionario(Long id, AsignarFuncionarioRequest request) {
        verificarSoloPersonalMunicipal();
        Solicitud s = buscar(id);
        solicitudValidator.validarEstadoCompatibleConAsignacion(s);

        if (request == null || request.usuarioResponsableId() == null) {
            throw new BusinessException("El identificador del funcionario responsable es obligatorio.");
        }
        Usuario funcionario = usuarios.findWithRolesById(request.usuarioResponsableId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Funcionario no encontrado con id " + request.usuarioResponsableId()));
        if (!funcionario.isActivo()) {
            throw new BusinessException("El funcionario seleccionado se encuentra inactivo.");
        }
        boolean tieneRolOperativo = funcionario.getRoles().stream()
                .anyMatch(r -> "FUNCIONARIO".equals(r.getNombre())
                        || "SUPERVISOR".equals(r.getNombre())
                        || "ADMINISTRADOR".equals(r.getNombre()));
        if (!tieneRolOperativo) {
            throw new BusinessException(
                    "El usuario seleccionado no posee un rol autorizado para revisar solicitudes.");
        }

        s.setUsuarioResponsable(funcionario);
        String detalle = StringUtils.hasText(request.observaciones())
                ? normalizarOpcional(request.observaciones())
                : "Expediente asignado al funcionario " + funcionario.getUsername();

        registrarHistorial(s, s.getEstado(), s.getEstado(), detalle);
        auditoria.registrar("ASIGNAR_FUNCIONARIO_SOLICITUD", "Solicitud", s.getId(),
                "Solicitud " + s.getId() + " asignada a " + funcionario.getUsername());
        notificarUsuario(funcionario, "Expediente asignado",
                "Se le ha asignado la revisión del expediente "
                        + (s.getNumeroExpediente() != null ? s.getNumeroExpediente() : "#" + s.getId()) + ".",
                TipoNotificacion.SOLICITUD);

        return mapper.toResponse(s);
    }

    @Override
    @Transactional
    public SolicitudResponse iniciarRevision(Long id, String observaciones) {
        verificarSoloPersonalMunicipal();
        Solicitud s = buscar(id);
        EstadoSolicitud anterior = s.getEstado();
        solicitudValidator.validarTransicion(anterior, EstadoSolicitud.EN_REVISION);

        if (s.getUsuarioResponsable() == null) {
            obtenerUsuarioEntidadActual().ifPresent(s::setUsuarioResponsable);
        }
        s.setEstado(EstadoSolicitud.EN_REVISION);
        if (StringUtils.hasText(observaciones)) {
            s.setObservaciones(normalizarOpcional(observaciones));
        }

        String obs = StringUtils.hasText(observaciones)
                ? normalizarOpcional(observaciones)
                : "Inicio de revisión administrativa del expediente.";
        registrarHistorial(s, anterior, EstadoSolicitud.EN_REVISION, obs);
        auditoria.registrar("INICIAR_REVISION_SOLICITUD", "Solicitud", s.getId(),
                "Iniciada revisión de solicitud " + s.getId());
        notificarCiudadano(s, "Expediente en revisión",
                "Su expediente " + s.getNumeroExpediente() + " ha pasado a estado EN_REVISION.",
                TipoNotificacion.SOLICITUD);

        return mapper.toResponse(s);
    }

    @Override
    @Transactional
    public SolicitudResponse observar(Long id, ObservarSolicitudRequest request) {
        verificarSoloPersonalMunicipal();
        Solicitud s = buscar(id);
        EstadoSolicitud anterior = s.getEstado();
        solicitudValidator.validarTransicion(anterior, EstadoSolicitud.OBSERVADA);

        String motivo = request != null
                ? normalizarTextoObligatorio(request.observaciones(), "El motivo de observación es obligatorio.")
                : null;
        if (!StringUtils.hasText(motivo)) {
            throw new BusinessException("El motivo de observación es obligatorio.");
        }

        if (s.getUsuarioResponsable() == null) {
            obtenerUsuarioEntidadActual().ifPresent(s::setUsuarioResponsable);
        }
        s.setEstado(EstadoSolicitud.OBSERVADA);
        s.setObservaciones(motivo);

        registrarHistorial(s, anterior, EstadoSolicitud.OBSERVADA, motivo);
        auditoria.registrar("OBSERVAR_SOLICITUD", "Solicitud", s.getId(),
                "Solicitud " + s.getId() + " observada.");
        notificarCiudadano(s, "Expediente observado",
                "Su expediente " + s.getNumeroExpediente() + " registra observaciones que requieren subsanación.",
                TipoNotificacion.ALERTA);

        return mapper.toResponse(s);
    }

    @Override
    @Transactional
    public SolicitudResponse subsanar(Long id, SubsanarSolicitudRequest request) {
        Solicitud s = buscar(id);
        verificarAccesoSolicitud(s);

        EstadoSolicitud anterior = s.getEstado();
        solicitudValidator.validarTransicion(anterior, EstadoSolicitud.SUBSANADA);

        String detalle = request != null
                ? normalizarTextoObligatorio(request.observaciones(), "El detalle de la subsanación es obligatorio.")
                : null;
        if (!StringUtils.hasText(detalle)) {
            throw new BusinessException("El detalle de la subsanación es obligatorio.");
        }

        List<Requisito> obligatorios = resolverRequisitosObligatoriosDeSolicitud(s);
        validarDocumentosObligatoriosParaPresentar(s.getId(), obligatorios);

        s.setEstado(EstadoSolicitud.SUBSANADA);
        s.setObservaciones(detalle);

        registrarHistorial(s, anterior, EstadoSolicitud.SUBSANADA, detalle);
        auditoria.registrar("SUBSANAR_SOLICITUD", "Solicitud", s.getId(),
                "Subsanación registrada para solicitud " + s.getId());
        if (s.getUsuarioResponsable() != null) {
            notificarUsuario(s.getUsuarioResponsable(), "Subsanación recibida",
                    "El ciudadano ha registrado la subsanación del expediente " + s.getNumeroExpediente() + ".",
                    TipoNotificacion.SOLICITUD);
        }

        return mapper.toResponse(s);
    }

    @Override
    @Transactional
    public SolicitudResponse aprobar(Long id, ResolverSolicitudRequest request) {
        verificarSoloPersonalMunicipal();
        Solicitud s = buscar(id);
        EstadoSolicitud anterior = s.getEstado();
        solicitudValidator.validarTransicion(anterior, EstadoSolicitud.APROBADA);

        List<Requisito> obligatorios = resolverRequisitosObligatoriosDeSolicitud(s);
        validarYMarcarDocumentosParaAprobacion(s.getId(), obligatorios);

        if (s.getUsuarioResponsable() == null) {
            obtenerUsuarioEntidadActual().ifPresent(s::setUsuarioResponsable);
        }
        s.setEstado(EstadoSolicitud.APROBADA);
        if (request != null && StringUtils.hasText(request.observaciones())) {
            s.setObservaciones(normalizarOpcional(request.observaciones()));
        }

        String obs = (request != null && StringUtils.hasText(request.observaciones()))
                ? normalizarOpcional(request.observaciones())
                : "Solicitud aprobada administrativamente.";
        registrarHistorial(s, anterior, EstadoSolicitud.APROBADA, obs);
        auditoria.registrar("APROBAR_SOLICITUD", "Solicitud", s.getId(),
                "Solicitud aprobada con expediente " + s.getNumeroExpediente());
        notificarCiudadano(s, "Solicitud aprobada",
                "Su expediente " + s.getNumeroExpediente() + " ha sido aprobado.",
                TipoNotificacion.SOLICITUD);

        return mapper.toResponse(s);
    }

    @Override
    @Transactional
    public SolicitudResponse rechazar(Long id, ResolverSolicitudRequest request) {
        verificarSoloPersonalMunicipal();
        Solicitud s = buscar(id);
        EstadoSolicitud anterior = s.getEstado();
        solicitudValidator.validarTransicion(anterior, EstadoSolicitud.RECHAZADA);

        String motivoRaw = request != null
                ? (StringUtils.hasText(request.motivoRechazo()) ? request.motivoRechazo() : request.observaciones())
                : null;
        String motivo = normalizarTextoObligatorio(motivoRaw, "El motivo de rechazo es obligatorio.");

        if (s.getUsuarioResponsable() == null) {
            obtenerUsuarioEntidadActual().ifPresent(s::setUsuarioResponsable);
        }
        s.setEstado(EstadoSolicitud.RECHAZADA);
        s.setMotivoRechazo(motivo);
        if (request != null && StringUtils.hasText(request.observaciones())) {
            s.setObservaciones(normalizarOpcional(request.observaciones()));
        }

        registrarHistorial(s, anterior, EstadoSolicitud.RECHAZADA, motivo);
        auditoria.registrar("RECHAZAR_SOLICITUD", "Solicitud", s.getId(),
                "Solicitud rechazada con expediente " + s.getNumeroExpediente());
        notificarCiudadano(s, "Solicitud rechazada",
                "Su expediente " + s.getNumeroExpediente() + " ha sido rechazado. Revise el motivo en su bandeja.",
                TipoNotificacion.ALERTA);

        return mapper.toResponse(s);
    }

    @Override
    @Transactional
    public SolicitudResponse archivar(Long id, ResolverSolicitudRequest request) {
        verificarSoloPersonalMunicipal();
        Solicitud s = buscar(id);
        EstadoSolicitud anterior = s.getEstado();
        solicitudValidator.validarTransicion(anterior, EstadoSolicitud.ARCHIVADA);

        String motivoRaw = request != null
                ? (StringUtils.hasText(request.observaciones()) ? request.observaciones() : request.motivoRechazo())
                : null;
        String motivo = normalizarTextoObligatorio(motivoRaw, "El motivo para archivar la solicitud es obligatorio.");

        s.setEstado(EstadoSolicitud.ARCHIVADA);
        s.setObservaciones(motivo);

        registrarHistorial(s, anterior, EstadoSolicitud.ARCHIVADA, motivo);
        auditoria.registrar("ARCHIVAR_SOLICITUD", "Solicitud", s.getId(),
                "Solicitud " + s.getId() + " archivada.");
        notificarCiudadano(s, "Expediente archivado",
                "Su solicitud ha pasado a estado ARCHIVADA.",
                TipoNotificacion.INFORMATIVA);

        return mapper.toResponse(s);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistorialSolicitudResponse> listarHistorial(Long id) {
        Solicitud s = buscar(id);
        verificarAccesoSolicitud(s);
        return historialRepository.findBySolicitudIdOrderByFechaCambioAscIdAsc(id).stream()
                .map(mapper::toHistorialResponse)
                .toList();
    }

    private List<Requisito> obtenerRequisitosObligatoriosVigentes(Long tipoTramiteId) {
        return tipoTramiteRequisitos.findByTipoTramiteIdOrderByOrdenVisualizacionAsc(tipoTramiteId).stream()
                .filter(TipoTramiteRequisito::isObligatorio)
                .map(TipoTramiteRequisito::getRequisito)
                .filter(r -> r != null && r.isActivo())
                .toList();
    }

    private List<Requisito> resolverRequisitosObligatoriosDeSolicitud(Solicitud s) {
        if (StringUtils.hasText(s.getRequisitosSnapshot())) {
            List<Long> ids = Arrays.stream(s.getRequisitosSnapshot().split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(Long::valueOf)
                    .toList();
            return requisitos.findAllById(ids);
        }
        return obtenerRequisitosObligatoriosVigentes(s.getTipoTramite().getId());
    }

    private void validarDocumentosObligatoriosParaPresentar(Long solicitudId, List<Requisito> obligatorios) {
        if (obligatorios.isEmpty()) {
            return;
        }
        List<Documento> adjuntosActivos = documentos.findBySolicitudIdAndActivoTrueOrderByFechaSubidaAscIdAsc(solicitudId);
        Set<Long> requisitosCubiertos = adjuntosActivos.stream()
                .filter(d -> d.getRequisito() != null && d.getEstado() != EstadoDocumento.RECHAZADO)
                .map(d -> d.getRequisito().getId())
                .collect(Collectors.toSet());

        List<String> faltantes = obligatorios.stream()
                .filter(r -> !requisitosCubiertos.contains(r.getId()))
                .map(r -> r.getCodigo() + " (" + r.getNombre() + ")")
                .toList();

        if (!faltantes.isEmpty()) {
            throw new BusinessException(
                    "Faltan documentos obligatorios para el trámite: " + String.join(", ", faltantes));
        }
    }

    private void validarYMarcarDocumentosParaAprobacion(Long solicitudId, List<Requisito> obligatorios) {
        List<Documento> adjuntosActivos = documentos.findBySolicitudIdAndActivoTrueOrderByFechaSubidaAscIdAsc(solicitudId);
        boolean tieneObservadosORechazados = adjuntosActivos.stream()
                .anyMatch(d -> d.getEstado() == EstadoDocumento.OBSERVADO
                        || d.getEstado() == EstadoDocumento.RECHAZADO);
        if (tieneObservadosORechazados) {
            throw new BusinessException(
                    "No se puede aprobar la solicitud mientras existan documentos activos observados o rechazados.");
        }

        if (!obligatorios.isEmpty()) {
            Set<Long> requisitosCubiertos = adjuntosActivos.stream()
                    .filter(d -> d.getRequisito() != null)
                    .map(d -> d.getRequisito().getId())
                    .collect(Collectors.toSet());
            List<String> faltantes = obligatorios.stream()
                    .filter(r -> !requisitosCubiertos.contains(r.getId()))
                    .map(r -> r.getCodigo() + " (" + r.getNombre() + ")")
                    .toList();
            if (!faltantes.isEmpty()) {
                throw new BusinessException(
                        "No se puede aprobar la solicitud porque faltan documentos obligatorios: "
                                + String.join(", ", faltantes));
            }
        }

        for (Documento doc : adjuntosActivos) {
            if (doc.getEstado() == EstadoDocumento.PENDIENTE) {
                doc.setEstado(EstadoDocumento.VALIDADO);
            }
        }
    }

    private void registrarHistorial(Solicitud solicitud, EstadoSolicitud estadoAnterior,
            EstadoSolicitud estadoNuevo, String observaciones) {
        HistorialSolicitud h = new HistorialSolicitud();
        h.setSolicitud(solicitud);
        h.setEstadoAnterior(estadoAnterior);
        h.setEstadoNuevo(estadoNuevo);
        h.setUsuarioResponsable(obtenerUsuarioEntidadActual().orElse(solicitud.getUsuarioResponsable()));
        h.setObservaciones(observaciones);
        historialRepository.save(h);
    }

    private void notificarCiudadano(Solicitud s, String titulo, String mensaje, TipoNotificacion tipo) {
        if (s.getCiudadano() != null && s.getCiudadano().getUsuario() != null) {
            notificarUsuario(s.getCiudadano().getUsuario(), titulo, mensaje, tipo);
        }
    }

    private void notificarUsuario(Usuario destinatario, String titulo, String mensaje, TipoNotificacion tipo) {
        if (destinatario == null) {
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

    private Solicitud buscar(Long id) {
        return solicitudes.findWithRelacionesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con id " + id));
    }

    private Ciudadano buscarCiudadano(Long id) {
        return ciudadanos.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ciudadano no encontrado con id " + id));
    }

    private Establecimiento buscarEstablecimiento(Long id) {
        return establecimientos.findWithRelacionesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Establecimiento no encontrado con id " + id));
    }

    private TipoTramite buscarTipoTramite(Long id) {
        return tiposTramite.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de trámite no encontrado con id " + id));
    }

    private Representante buscarRepresentanteOpcional(Long id) {
        if (id == null) {
            return null;
        }
        return representantes.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Representante no encontrado con id " + id));
    }

    private void verificarAccesoTitularidadCiudadano(Ciudadano ciudadano) {
        if (!esSoloRolCiudadano()) {
            return;
        }
        Long usuarioActualId = obtenerUsuarioActualId();
        if (usuarioActualId == null || ciudadano.getUsuario() == null
                || !usuarioActualId.equals(ciudadano.getUsuario().getId())) {
            throw new AccessDeniedException(
                    "No tiene permisos para registrar o gestionar solicitudes en nombre de otro ciudadano.");
        }
    }

    private void verificarAccesoSolicitud(Solicitud s) {
        if (!esSoloRolCiudadano()) {
            return;
        }
        Ciudadano titular = s.getCiudadano();
        Long usuarioActualId = obtenerUsuarioActualId();
        if (usuarioActualId == null || titular == null || titular.getUsuario() == null
                || !usuarioActualId.equals(titular.getUsuario().getId())) {
            throw new AccessDeniedException("No tiene permisos para acceder a expedientes de otros ciudadanos.");
        }
    }

    private void verificarSoloPersonalMunicipal() {
        if (esSoloRolCiudadano()) {
            throw new AccessDeniedException("No tiene permisos para ejecutar esta operación administrativa.");
        }
    }

    private boolean esSoloRolCiudadano() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return false;
        }
        List<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        boolean esPersonalMunicipal = authorities.contains("ROLE_ADMINISTRADOR")
                || authorities.contains("ROLE_FUNCIONARIO")
                || authorities.contains("ROLE_SUPERVISOR");
        return !esPersonalMunicipal && authorities.contains("ROLE_CIUDADANO");
    }

    private Long obtenerUsuarioActualId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails details) {
            return details.getId();
        }
        return null;
    }

    private Optional<Usuario> obtenerUsuarioEntidadActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        if (auth.getPrincipal() instanceof CustomUserDetails details && details.getId() != null) {
            return usuarios.findById(details.getId());
        }
        return usuarios.findByUsername(auth.getName());
    }

    private Specification<Solicitud> filtros(String numeroExpediente, Long ciudadanoId, Long establecimientoId,
            Long tipoTramiteId, EstadoSolicitud estado, LocalDate fechaInicial, LocalDate fechaFinal,
            Long usuarioResponsableId) {
        return (Root<Solicitud> root, jakarta.persistence.criteria.CriteriaQuery<?> query,
                jakarta.persistence.criteria.CriteriaBuilder cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (StringUtils.hasText(numeroExpediente)) {
                predicados.add(cb.like(cb.lower(root.get("numeroExpediente")),
                        "%" + numeroExpediente.trim().toLowerCase() + "%"));
            }
            if (ciudadanoId != null) {
                predicados.add(cb.equal(root.get("ciudadano").get("id"), ciudadanoId));
            }
            if (establecimientoId != null) {
                predicados.add(cb.equal(root.get("establecimiento").get("id"), establecimientoId));
            }
            if (tipoTramiteId != null) {
                predicados.add(cb.equal(root.get("tipoTramite").get("id"), tipoTramiteId));
            }
            if (estado != null) {
                predicados.add(cb.equal(root.get("estado"), estado));
            }
            if (fechaInicial != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("fechaPresentacion"), fechaInicial.atStartOfDay()));
            }
            if (fechaFinal != null) {
                predicados.add(cb.lessThan(root.get("fechaPresentacion"),
                        fechaFinal.plusDays(1).atStartOfDay()));
            }
            if (usuarioResponsableId != null) {
                predicados.add(cb.equal(root.get("usuarioResponsable").get("id"), usuarioResponsableId));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }

    private static String normalizarTextoObligatorio(String valor, String mensajeError) {
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
