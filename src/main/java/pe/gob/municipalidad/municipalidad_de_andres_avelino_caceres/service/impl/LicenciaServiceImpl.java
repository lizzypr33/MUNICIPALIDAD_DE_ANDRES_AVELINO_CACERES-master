package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
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
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.EmitirLicenciaRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.HistorialLicenciaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.LicenciaDetalleResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.LicenciaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Ciudadano;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Documento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.HistorialLicencia;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Inspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Licencia;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Notificacion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Solicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.TipoTramiteRequisito;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Usuario;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoDocumento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoLicencia;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoNotificacion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ConflictException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ResourceNotFoundException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.LicenciaMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.CiudadanoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.DocumentoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.HistorialLicenciaRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.InspeccionRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.LicenciaRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.NotificacionRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.SolicitudRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.TipoTramiteRequisitoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.UsuarioRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.security.CustomUserDetails;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.AuditoriaService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.LicenciaService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.util.CodigoLicenciaGenerator;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.validation.LicenciaValidator;

@Service
public class LicenciaServiceImpl implements LicenciaService {

    private final LicenciaRepository licencias;
    private final HistorialLicenciaRepository historiales;
    private final SolicitudRepository solicitudes;
    private final InspeccionRepository inspecciones;
    private final DocumentoRepository documentos;
    private final TipoTramiteRequisitoRepository tipoTramiteRequisitos;
    private final CiudadanoRepository ciudadanos;
    private final UsuarioRepository usuarios;
    private final NotificacionRepository notificaciones;
    private final LicenciaValidator validator;
    private final CodigoLicenciaGenerator codigoGenerator;
    private final LicenciaMapper mapper;
    private final AuditoriaService auditoria;

    public LicenciaServiceImpl(LicenciaRepository licencias,
            HistorialLicenciaRepository historiales,
            SolicitudRepository solicitudes,
            InspeccionRepository inspecciones,
            DocumentoRepository documentos,
            TipoTramiteRequisitoRepository tipoTramiteRequisitos,
            CiudadanoRepository ciudadanos,
            UsuarioRepository usuarios,
            NotificacionRepository notificaciones,
            LicenciaValidator validator,
            CodigoLicenciaGenerator codigoGenerator,
            LicenciaMapper mapper,
            AuditoriaService auditoria) {
        this.licencias = licencias;
        this.historiales = historiales;
        this.solicitudes = solicitudes;
        this.inspecciones = inspecciones;
        this.documentos = documentos;
        this.tipoTramiteRequisitos = tipoTramiteRequisitos;
        this.ciudadanos = ciudadanos;
        this.usuarios = usuarios;
        this.notificaciones = notificaciones;
        this.validator = validator;
        this.codigoGenerator = codigoGenerator;
        this.mapper = mapper;
        this.auditoria = auditoria;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<LicenciaResponse> listar(
            String codigo,
            Long solicitudId,
            EstadoLicencia estado,
            Long establecimientoId,
            Long ciudadanoId,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Pageable pageable) {

        Long ciudadanoFiltroId = ciudadanoId;
        if (esSoloCiudadano()) {
            Ciudadano ciudadanoActual = obtenerCiudadanoAutenticado();
            if (ciudadanoId != null && !Objects.equals(ciudadanoId, ciudadanoActual.getId())) {
                throw new AccessDeniedException("No tiene permisos para consultar las licencias de otro ciudadano.");
            }
            ciudadanoFiltroId = ciudadanoActual.getId();
        }

        Page<Licencia> pagina = licencias.findAll(
                filtros(codigo, solicitudId, estado, establecimientoId, ciudadanoFiltroId, fechaDesde, fechaHasta),
                pageable);
        return PaginaResponse.from(pagina.map(mapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public LicenciaDetalleResponse obtener(Long id) {
        Licencia l = buscarConRelaciones(id);
        verificarAccesoCiudadanoSiCorresponde(l);
        return construirDetalle(l);
    }

    @Override
    @Transactional(readOnly = true)
    public LicenciaDetalleResponse obtenerPorCodigo(String codigo) {
        String normalizado = codigoGenerator.normalizarCodigoLicencia(codigo);
        Licencia l = licencias.findByCodigoLicencia(normalizado)
                .orElseThrow(() -> new ResourceNotFoundException("Licencia no encontrada con código " + normalizado));
        verificarAccesoCiudadanoSiCorresponde(l);
        return construirDetalle(l);
    }

    @Override
    @Transactional(readOnly = true)
    public LicenciaDetalleResponse obtenerPorSolicitud(Long solicitudId) {
        Licencia l = licencias.findBySolicitudId(solicitudId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe licencia emitida para la solicitud con id " + solicitudId));
        verificarAccesoCiudadanoSiCorresponde(l);
        return construirDetalle(l);
    }

    @Override
    @Transactional
    public LicenciaDetalleResponse emitir(EmitirLicenciaRequest request) {
        if (esSoloCiudadano()) {
            throw new AccessDeniedException("Un ciudadano no está autorizado para emitir licencias de funcionamiento.");
        }

        Solicitud solicitud = solicitudes.findWithRelacionesById(request.solicitudId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Solicitud no encontrada con id " + request.solicitudId()));

        boolean yaEmitida = licencias.existsBySolicitudId(solicitud.getId());
        List<Inspeccion> inspeccionesSolicitud = inspecciones.findBySolicitudId(solicitud.getId());

        validator.validarCondicionesPreviasEmision(solicitud, inspeccionesSolicitud, yaEmitida);
        validarDocumentosObligatoriosSolicitud(solicitud);

        LocalDate fechaEmision = LocalDate.now();
        validator.validarFechasVigencia(fechaEmision, request.fechaVencimiento());

        Long correlativo = licencias.obtenerSiguienteCorrelativoLicencia();
        if (correlativo == null || correlativo <= 0) {
            throw new IllegalStateException("No se pudo obtener el correlativo de la secuencia seq_codigo_licencia.");
        }
        String codigoLicencia = codigoGenerator.formatearCodigoLicencia(correlativo);
        String tokenVerificacion = codigoGenerator.generarTokenVerificacion();
        Usuario usuarioEmisor = obtenerUsuarioAutenticadoEntidad();

        Licencia l = new Licencia();
        l.setCodigoLicencia(codigoLicencia);
        l.setTokenVerificacion(tokenVerificacion);
        l.setSolicitud(solicitud);
        l.setFechaEmision(fechaEmision);
        l.setFechaVencimiento(request.fechaVencimiento());
        l.setEstado(EstadoLicencia.VIGENTE);
        l.setObservaciones(normalizarOpcional(request.observaciones()));
        l.setUsuarioEmisor(usuarioEmisor);

        try {
            licencias.saveAndFlush(l);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(
                    "Ya existe una licencia emitida para la solicitud o se detectó una colisión concurrente de emisión.");
        }

        registrarHistorial(l, null, EstadoLicencia.VIGENTE,
                StringUtils.hasText(request.observaciones())
                        ? "Emisión de licencia: " + request.observaciones().trim()
                        : "Emisión inicial de licencia de funcionamiento.",
                usuarioEmisor);

        auditoria.registrar("EMITIR_LICENCIA", "Licencia", l.getId(),
                "Licencia emitida: " + l.getCodigoLicencia() + " para solicitud id=" + solicitud.getId());

        notificarTitularSiCorresponde(solicitud,
                "Licencia de funcionamiento emitida",
                "Se ha emitido su licencia de funcionamiento N° " + l.getCodigoLicencia()
                        + " correspondiente al expediente " + valorExpediente(solicitud) + ".");

        return construirDetalle(l);
    }

    @Override
    @Transactional
    public LicenciaDetalleResponse suspender(Long id, CambiarEstadoLicenciaRequest request) {
        return cambiarEstadoAutorizado(id, EstadoLicencia.SUSPENDIDA, request, "SUSPENDER_LICENCIA",
                "Licencia de funcionamiento suspendida");
    }

    @Override
    @Transactional
    public LicenciaDetalleResponse reactivar(Long id, CambiarEstadoLicenciaRequest request) {
        return cambiarEstadoAutorizado(id, EstadoLicencia.VIGENTE, request, "REACTIVAR_LICENCIA",
                "Licencia de funcionamiento reactivada");
    }

    @Override
    @Transactional
    public LicenciaDetalleResponse revocar(Long id, CambiarEstadoLicenciaRequest request) {
        return cambiarEstadoAutorizado(id, EstadoLicencia.REVOCADA, request, "REVOCAR_LICENCIA",
                "Licencia de funcionamiento revocada");
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistorialLicenciaResponse> listarHistorial(Long id) {
        Licencia l = buscarConRelaciones(id);
        verificarAccesoCiudadanoSiCorresponde(l);
        return historiales.findByLicenciaIdOrderByFechaCambioAscIdAsc(id).stream()
                .map(mapper::toHistorialResponse)
                .toList();
    }

    private LicenciaDetalleResponse cambiarEstadoAutorizado(
            Long id,
            EstadoLicencia estadoNuevo,
            CambiarEstadoLicenciaRequest request,
            String accionAuditoria,
            String tituloNotificacion) {

        if (esSoloCiudadano()) {
            throw new AccessDeniedException(
                    "Un ciudadano no está autorizado para modificar el estado de una licencia de funcionamiento.");
        }

        Licencia l = buscarConRelaciones(id);
        String motivo = normalizarObligatorio(
                request != null ? request.motivo() : null,
                "El motivo administrativo para cambiar el estado de la licencia es obligatorio.");

        validator.validarCambioEstado(l, estadoNuevo, motivo);

        EstadoLicencia estadoAnterior = l.getEstado();
        l.setEstado(estadoNuevo);
        if (request != null && StringUtils.hasText(request.observaciones())) {
            l.setObservaciones(normalizarOpcional(request.observaciones()));
        }

        Usuario responsable = obtenerUsuarioAutenticadoEntidad();
        registrarHistorial(l, estadoAnterior, estadoNuevo, motivo, responsable);

        auditoria.registrar(accionAuditoria, "Licencia", l.getId(),
                "Licencia " + l.getCodigoLicencia() + ": " + estadoAnterior + " -> " + estadoNuevo
                        + ". Motivo: " + motivo);

        notificarTitularSiCorresponde(l.getSolicitud(),
                tituloNotificacion,
                "Su licencia N° " + l.getCodigoLicencia() + " cambió al estado " + estadoNuevo
                        + ". Motivo administrativo: " + motivo);

        return construirDetalle(l);
    }

    private void validarDocumentosObligatoriosSolicitud(Solicitud solicitud) {
        List<Documento> docsActivos = documentos
                .findBySolicitudIdAndActivoTrueOrderByFechaSubidaAscIdAsc(solicitud.getId());
        boolean tieneRechazado = docsActivos.stream()
                .anyMatch(d -> d.getEstado() == EstadoDocumento.RECHAZADO);
        if (tieneRechazado) {
            throw new ConflictException(
                    "No se puede emitir la licencia porque la solicitud contiene documentos observados o rechazados.");
        }

        Set<Long> requisitosSubidos = docsActivos.stream()
                .filter(d -> d.getRequisito() != null && d.getRequisito().getId() != null)
                .map(d -> d.getRequisito().getId())
                .collect(Collectors.toSet());

        if (StringUtils.hasText(solicitud.getRequisitosSnapshot())) {
            Set<Long> obligatoriosSnapshot = Arrays.stream(solicitud.getRequisitosSnapshot().split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(Long::valueOf)
                    .collect(Collectors.toSet());
            if (!requisitosSubidos.containsAll(obligatoriosSnapshot)) {
                throw new ConflictException(
                        "No se puede emitir la licencia porque faltan documentos obligatorios exigidos para la solicitud.");
            }
            return;
        }

        if (solicitud.getTipoTramite() != null && solicitud.getTipoTramite().getId() != null) {
            List<TipoTramiteRequisito> configurados = tipoTramiteRequisitos
                    .findByTipoTramiteIdOrderByOrdenVisualizacionAsc(solicitud.getTipoTramite().getId());
            for (TipoTramiteRequisito ttr : configurados) {
                if (ttr.isObligatorio() && ttr.getRequisito() != null
                        && !requisitosSubidos.contains(ttr.getRequisito().getId())) {
                    throw new ConflictException(
                            "No se puede emitir la licencia porque falta el requisito obligatorio: "
                                    + ttr.getRequisito().getNombre());
                }
            }
        }
    }

    private void registrarHistorial(Licencia licencia, EstadoLicencia anterior, EstadoLicencia nuevo,
            String motivo, Usuario responsable) {
        HistorialLicencia h = new HistorialLicencia();
        h.setLicencia(licencia);
        h.setEstadoAnterior(anterior);
        h.setEstadoNuevo(nuevo);
        h.setMotivo(motivo);
        h.setUsuarioResponsable(responsable);
        historiales.save(h);
    }

    private LicenciaDetalleResponse construirDetalle(Licencia l) {
        List<HistorialLicenciaResponse> listaHistorial = historiales
                .findByLicenciaIdOrderByFechaCambioAscIdAsc(l.getId()).stream()
                .map(mapper::toHistorialResponse)
                .toList();
        return mapper.toDetalleResponse(l, listaHistorial);
    }

    private Licencia buscarConRelaciones(Long id) {
        return licencias.findWithRelacionesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Licencia no encontrada con id " + id));
    }

    private void verificarAccesoCiudadanoSiCorresponde(Licencia licencia) {
        if (!esSoloCiudadano()) {
            return;
        }
        Ciudadano ciudadanoActual = obtenerCiudadanoAutenticado();
        Solicitud s = licencia.getSolicitud();
        if (s == null || s.getCiudadano() == null
                || !Objects.equals(s.getCiudadano().getId(), ciudadanoActual.getId())) {
            throw new AccessDeniedException(
                    "No tiene permisos para acceder a licencias vinculadas a trámites de otro ciudadano.");
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

    private Usuario obtenerUsuarioAutenticadoEntidad() {
        Long usuarioId = obtenerIdUsuarioAutenticado();
        if (usuarioId == null) {
            return null;
        }
        return usuarios.findById(usuarioId).orElse(null);
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

    private void notificarTitularSiCorresponde(Solicitud solicitud, String titulo, String mensaje) {
        if (solicitud != null && solicitud.getCiudadano() != null && solicitud.getCiudadano().getUsuario() != null) {
            Notificacion n = new Notificacion();
            n.setUsuario(solicitud.getCiudadano().getUsuario());
            n.setTitulo(titulo);
            n.setMensaje(mensaje);
            n.setTipo(TipoNotificacion.LICENCIA);
            n.setLeida(false);
            notificaciones.save(n);
        }
    }

    private static String valorExpediente(Solicitud s) {
        if (s == null) {
            return "N/A";
        }
        return StringUtils.hasText(s.getNumeroExpediente()) ? s.getNumeroExpediente() : "ID-" + s.getId();
    }

    private Specification<Licencia> filtros(
            String codigo,
            Long solicitudId,
            EstadoLicencia estado,
            Long establecimientoId,
            Long ciudadanoId,
            LocalDate fechaDesde,
            LocalDate fechaHasta) {

        return (Root<Licencia> root, jakarta.persistence.criteria.CriteriaQuery<?> query,
                jakarta.persistence.criteria.CriteriaBuilder cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (StringUtils.hasText(codigo)) {
                predicados.add(cb.like(cb.upper(root.get("codigoLicencia")),
                        "%" + codigo.trim().toUpperCase() + "%"));
            }
            if (solicitudId != null) {
                predicados.add(cb.equal(root.get("solicitud").get("id"), solicitudId));
            }
            if (estado != null) {
                predicados.add(cb.equal(root.get("estado"), estado));
            }
            if (establecimientoId != null) {
                predicados.add(cb.equal(root.get("solicitud").get("establecimiento").get("id"), establecimientoId));
            }
            if (ciudadanoId != null) {
                predicados.add(cb.equal(root.get("solicitud").get("ciudadano").get("id"), ciudadanoId));
            }
            if (fechaDesde != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("fechaEmision"), fechaDesde));
            }
            if (fechaHasta != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("fechaEmision"), fechaHasta));
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
