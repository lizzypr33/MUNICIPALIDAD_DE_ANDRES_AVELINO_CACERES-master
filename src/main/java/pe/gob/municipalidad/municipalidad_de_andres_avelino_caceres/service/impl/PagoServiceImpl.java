package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.dao.OptimisticLockingFailureException;
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
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.AnularPagoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.RechazarPagoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.RegistrarPagoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.VerificarPagoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PagoDetalleResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PagoDetalleResponse.HistorialPagoItem;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PagoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ResumenPagosResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Ciudadano;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.HistorialPago;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Notificacion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Pago;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Solicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Usuario;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoPago;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoNotificacion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ConflictException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ResourceNotFoundException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.PagoMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.CiudadanoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.HistorialPagoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.NotificacionRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.PagoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.SolicitudRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.UsuarioRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.security.CustomUserDetails;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.AuditoriaService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.PagoService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.validation.PagoValidator;

@Service
public class PagoServiceImpl implements PagoService {

    private static final Set<EstadoPago> ESTADOS_NO_BLOQUEANTES_DUPLICIDAD = EnumSet.of(
            EstadoPago.RECHAZADO,
            EstadoPago.ANULADO);

    private final PagoRepository pagos;
    private final HistorialPagoRepository historiales;
    private final SolicitudRepository solicitudes;
    private final CiudadanoRepository ciudadanos;
    private final UsuarioRepository usuarios;
    private final NotificacionRepository notificaciones;
    private final PagoValidator validator;
    private final PagoMapper mapper;
    private final AuditoriaService auditoria;

    public PagoServiceImpl(PagoRepository pagos,
            HistorialPagoRepository historiales,
            SolicitudRepository solicitudes,
            CiudadanoRepository ciudadanos,
            UsuarioRepository usuarios,
            NotificacionRepository notificaciones,
            PagoValidator validator,
            PagoMapper mapper,
            AuditoriaService auditoria) {
        this.pagos = pagos;
        this.historiales = historiales;
        this.solicitudes = solicitudes;
        this.ciudadanos = ciudadanos;
        this.usuarios = usuarios;
        this.notificaciones = notificaciones;
        this.validator = validator;
        this.mapper = mapper;
        this.auditoria = auditoria;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<PagoResponse> listar(
            Long solicitudId,
            EstadoPago estado,
            Long tipoTramiteId,
            String concepto,
            String codigoOperacion,
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
                    throw new AccessDeniedException("No tiene permisos para consultar los pagos de otro ciudadano.");
                }
            }
        }

        Page<Pago> pagina = pagos.findAll(
                filtros(solicitudId, ciudadanoFiltroId, estado, tipoTramiteId, concepto, codigoOperacion, fechaDesde,
                        fechaHasta),
                pageable);
        return PaginaResponse.from(pagina.map(mapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PagoDetalleResponse obtener(Long id) {
        Pago p = buscarConRelaciones(id);
        verificarAccesoCiudadanoSiCorresponde(p.getSolicitud());
        return construirDetalle(p);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoResponse> listarPorSolicitud(Long solicitudId) {
        Solicitud solicitud = buscarSolicitud(solicitudId);
        verificarAccesoCiudadanoSiCorresponde(solicitud);
        return pagos.findBySolicitudIdOrderByIdDesc(solicitudId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public PagoDetalleResponse registrar(RegistrarPagoRequest request) {
        Solicitud solicitud = buscarSolicitud(request.solicitudId());
        verificarAccesoCiudadanoSiCorresponde(solicitud);
        validator.validarSolicitudCompatible(solicitud);

        String codigoOperacion = normalizarObligatorio(
                request.codigoOperacion(), "El código de operación o referencia es obligatorio.");
        String concepto = normalizarObligatorio(
                request.concepto(), "El concepto del pago es obligatorio.");
        String medioPago = normalizarObligatorio(
                request.medioPago(), "El medio de pago es obligatorio.");
        String moneda = validator.validarYNormalizarMoneda(request.moneda());
        String observaciones = normalizarOpcional(request.observaciones());

        validator.validarDatosPago(
                request.monto(), codigoOperacion, concepto, medioPago, request.fechaPago(), observaciones);

        if (pagos.existsByCodigoOperacionIgnoreCaseAndMedioPagoIgnoreCaseAndEstadoNotIn(
                codigoOperacion, medioPago, ESTADOS_NO_BLOQUEANTES_DUPLICIDAD)) {
            throw new ConflictException(
                    "Ya existe un pago registrado activo con el código de operación '" + codigoOperacion
                            + "' para el medio de pago '" + medioPago + "'.");
        }

        BigDecimal montoNormalizado = request.monto().setScale(2, RoundingMode.HALF_UP);

        Pago p = new Pago();
        p.setSolicitud(solicitud);
        p.setCodigoOperacion(codigoOperacion);
        p.setConcepto(concepto);
        p.setMonto(montoNormalizado);
        p.setMoneda(moneda);
        p.setFechaPago(request.fechaPago());
        p.setMedioPago(medioPago);
        p.setEstado(EstadoPago.PENDIENTE_VERIFICACION);
        p.setObservaciones(observaciones);
        // @Version con valor 0L hace que JPA haga merge: hay que usar la instancia devuelta para obtener el id
        p = pagos.saveAndFlush(p);

        Usuario responsable = obtenerUsuarioAutenticadoEntidad();
        registrarHistorial(p, null, EstadoPago.PENDIENTE_VERIFICACION,
                "Registro inicial de declaración de pago (pendiente de verificación).", responsable);

        auditoria.registrar("REGISTRAR_PAGO", "Pago", p.getId(),
                "Pago declarado registrado (PENDIENTE_VERIFICACION) para solicitud id=" + solicitud.getId()
                        + ", operación=" + codigoOperacion + ", monto=" + montoNormalizado + " " + moneda);

        notificarEventosPago(solicitud,
                "Pago declarado registrado",
                "Se ha registrado la declaración de pago con referencia " + codigoOperacion
                        + " por " + montoNormalizado + " " + moneda
                        + " para el expediente " + valorExpediente(solicitud)
                        + ". Estado: PENDIENTE_VERIFICACION.");

        return construirDetalle(p);
    }

    @Override
    @Transactional
    public PagoDetalleResponse verificar(Long id, VerificarPagoRequest request) {
        if (esSoloCiudadano()) {
            throw new AccessDeniedException("Un ciudadano no está autorizado para verificar pagos municipales.");
        }

        Pago p = buscarConRelaciones(id);
        String observacionesVerificacion = normalizarObligatorio(
                request != null ? request.observaciones() : null,
                "El respaldo u observaciones de verificación son obligatorios.");
        validator.validarSinDatosDeTarjeta(observacionesVerificacion, "observaciones de verificación");

        if (request != null && StringUtils.hasText(request.codigoOperacionConfirmado())) {
            String confirmado = request.codigoOperacionConfirmado().trim();
            if (!confirmado.equalsIgnoreCase(p.getCodigoOperacion())) {
                throw new ConflictException(
                        "El código de operación confirmado no coincide con la referencia registrada en el pago.");
            }
        }

        boolean esAdminOSupervisor = tieneRol("ROLE_ADMINISTRADOR") || tieneRol("ROLE_SUPERVISOR");
        validator.validarTransicion(p, EstadoPago.VERIFICADO, observacionesVerificacion, esAdminOSupervisor);

        EstadoPago estadoAnterior = p.getEstado();
        Usuario verificador = obtenerUsuarioAutenticadoEntidad();
        LocalDateTime ahora = LocalDateTime.now();

        p.setEstado(EstadoPago.VERIFICADO);
        p.setUsuarioVerificador(verificador);
        p.setFechaVerificacion(ahora);
        p.setObservaciones(observacionesVerificacion);

        try {
            pagos.saveAndFlush(p);
        } catch (OptimisticLockingFailureException ex) {
            throw new ConflictException(
                    "El pago fue modificado simultáneamente por otro funcionario. Recargue el registro e intente nuevamente.");
        }

        registrarHistorial(p, estadoAnterior, EstadoPago.VERIFICADO, observacionesVerificacion, verificador);

        auditoria.registrar("VERIFICAR_PAGO", "Pago", p.getId(),
                "Pago verificado: operación=" + p.getCodigoOperacion() + ", monto=" + p.getMonto() + " "
                        + p.getMoneda());

        notificarTitularSiCorresponde(p.getSolicitud(),
                "Pago municipal verificado",
                "Su pago con referencia " + p.getCodigoOperacion() + " por " + p.getMonto() + " " + p.getMoneda()
                        + " del expediente " + valorExpediente(p.getSolicitud()) + " ha sido VERIFICADO.");

        return construirDetalle(p);
    }

    @Override
    @Transactional
    public PagoDetalleResponse rechazar(Long id, RechazarPagoRequest request) {
        if (esSoloCiudadano()) {
            throw new AccessDeniedException("Un ciudadano no está autorizado para rechazar pagos municipales.");
        }

        Pago p = buscarConRelaciones(id);
        String motivo = normalizarObligatorio(
                request != null ? request.motivo() : null,
                "El motivo de rechazo del pago es obligatorio.");
        validator.validarSinDatosDeTarjeta(motivo, "motivo de rechazo");

        boolean esAdminOSupervisor = tieneRol("ROLE_ADMINISTRADOR") || tieneRol("ROLE_SUPERVISOR");
        validator.validarTransicion(p, EstadoPago.RECHAZADO, motivo, esAdminOSupervisor);

        EstadoPago estadoAnterior = p.getEstado();
        Usuario responsable = obtenerUsuarioAutenticadoEntidad();

        p.setEstado(EstadoPago.RECHAZADO);
        p.setMotivoRechazoAnulacion(motivo);
        if (request != null && StringUtils.hasText(request.observaciones())) {
            p.setObservaciones(normalizarOpcional(request.observaciones()));
        }

        try {
            pagos.saveAndFlush(p);
        } catch (OptimisticLockingFailureException ex) {
            throw new ConflictException("El pago fue modificado simultáneamente por otro usuario.");
        }

        registrarHistorial(p, estadoAnterior, EstadoPago.RECHAZADO, motivo, responsable);

        auditoria.registrar("RECHAZAR_PAGO", "Pago", p.getId(),
                "Pago rechazado: operación=" + p.getCodigoOperacion() + ". Motivo: " + motivo);

        notificarTitularSiCorresponde(p.getSolicitud(),
                "Pago municipal rechazado",
                "El registro de pago con referencia " + p.getCodigoOperacion()
                        + " fue RECHAZADO. Motivo: " + motivo);

        return construirDetalle(p);
    }

    @Override
    @Transactional
    public PagoDetalleResponse anular(Long id, AnularPagoRequest request) {
        if (esSoloCiudadano()) {
            throw new AccessDeniedException("Un ciudadano no está autorizado para anular pagos municipales.");
        }

        Pago p = buscarConRelaciones(id);
        String motivo = normalizarObligatorio(
                request != null ? request.motivo() : null,
                "El motivo administrativo de anulación del pago es obligatorio.");
        validator.validarSinDatosDeTarjeta(motivo, "motivo de anulación");

        boolean esAdminOSupervisor = tieneRol("ROLE_ADMINISTRADOR") || tieneRol("ROLE_SUPERVISOR");
        validator.validarTransicion(p, EstadoPago.ANULADO, motivo, esAdminOSupervisor);

        EstadoPago estadoAnterior = p.getEstado();
        Usuario responsable = obtenerUsuarioAutenticadoEntidad();

        p.setEstado(EstadoPago.ANULADO);
        p.setMotivoRechazoAnulacion(motivo);
        if (request != null && StringUtils.hasText(request.observaciones())) {
            p.setObservaciones(normalizarOpcional(request.observaciones()));
        }

        try {
            pagos.saveAndFlush(p);
        } catch (OptimisticLockingFailureException ex) {
            throw new ConflictException("El pago fue modificado simultáneamente por otro usuario.");
        }

        registrarHistorial(p, estadoAnterior, EstadoPago.ANULADO, motivo, responsable);

        auditoria.registrar("ANULAR_PAGO", "Pago", p.getId(),
                "Pago anulado (" + estadoAnterior + " -> ANULADO): operación=" + p.getCodigoOperacion()
                        + ". Motivo: " + motivo);

        notificarTitularSiCorresponde(p.getSolicitud(),
                "Registro de pago anulado",
                "El registro de pago con referencia " + p.getCodigoOperacion()
                        + " ha sido ANULADO administrativamente. Motivo: " + motivo);

        return construirDetalle(p);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenPagosResponse obtenerResumen(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            EstadoPago estado,
            Long tipoTramiteId,
            String concepto) {

        if (esSoloCiudadano()) {
            throw new AccessDeniedException("Un ciudadano no está autorizado para consultar el resumen consolidado de pagos.");
        }

        List<Pago> lista = pagos.findAll(
                filtros(null, null, estado, tipoTramiteId, concepto, null, fechaDesde, fechaHasta));

        long total = lista.size();
        long verificados = 0L;
        long pendientes = 0L;
        long rechazados = 0L;
        long anulados = 0L;
        BigDecimal sumaVerificadaPen = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (Pago p : lista) {
            EstadoPago est = p.getEstado();
            if (est == EstadoPago.VERIFICADO || est == EstadoPago.PAGADO) {
                verificados++;
                if ("PEN".equalsIgnoreCase(p.getMoneda()) && p.getMonto() != null) {
                    sumaVerificadaPen = sumaVerificadaPen.add(p.getMonto());
                }
            } else if (est == EstadoPago.PENDIENTE_VERIFICACION || est == EstadoPago.PENDIENTE) {
                pendientes++;
            } else if (est == EstadoPago.RECHAZADO) {
                rechazados++;
            } else if (est == EstadoPago.ANULADO || est == EstadoPago.REEMBOLSADO) {
                anulados++;
            }
        }

        return new ResumenPagosResponse(
                total,
                verificados,
                sumaVerificadaPen,
                "PEN",
                pendientes,
                rechazados,
                anulados,
                fechaDesde,
                fechaHasta);
    }

    private void registrarHistorial(Pago pago, EstadoPago anterior, EstadoPago nuevo, String motivo,
            Usuario responsable) {
        HistorialPago h = new HistorialPago();
        h.setPago(pago);
        h.setEstadoAnterior(anterior);
        h.setEstadoNuevo(nuevo);
        h.setMotivo(motivo);
        h.setUsuarioResponsable(responsable);
        historiales.save(h);
    }

    private PagoDetalleResponse construirDetalle(Pago p) {
        List<HistorialPagoItem> items = historiales.findByPagoIdOrderByFechaCambioAscIdAsc(p.getId()).stream()
                .map(mapper::toHistorialItem)
                .toList();
        return mapper.toDetalleResponse(p, items);
    }

    private Pago buscarConRelaciones(Long id) {
        return pagos.findWithRelacionesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado con id " + id));
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
            throw new AccessDeniedException("No tiene permisos para acceder a pagos vinculados a otro ciudadano.");
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

    private boolean tieneRol(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }

    private void notificarEventosPago(Solicitud solicitud, String titulo, String mensaje) {
        notificarTitularSiCorresponde(solicitud, titulo, mensaje);
        if (solicitud != null && solicitud.getUsuarioResponsable() != null) {
            Usuario func = solicitud.getUsuarioResponsable();
            Usuario titularUsuario = solicitud.getCiudadano() != null ? solicitud.getCiudadano().getUsuario() : null;
            if (titularUsuario == null || !Objects.equals(func.getId(), titularUsuario.getId())) {
                crearNotificacion(func, titulo, mensaje);
            }
        }
    }

    private void notificarTitularSiCorresponde(Solicitud solicitud, String titulo, String mensaje) {
        if (solicitud != null && solicitud.getCiudadano() != null && solicitud.getCiudadano().getUsuario() != null) {
            crearNotificacion(solicitud.getCiudadano().getUsuario(), titulo, mensaje);
        }
    }

    private void crearNotificacion(Usuario destinatario, String titulo, String mensaje) {
        if (destinatario == null || destinatario.getId() == null) {
            return;
        }
        Notificacion n = new Notificacion();
        n.setUsuario(destinatario);
        n.setTitulo(titulo);
        n.setMensaje(mensaje);
        n.setTipo(TipoNotificacion.PAGO);
        n.setLeida(false);
        notificaciones.save(n);
    }

    private static String valorExpediente(Solicitud s) {
        if (s == null) {
            return "N/A";
        }
        return StringUtils.hasText(s.getNumeroExpediente()) ? s.getNumeroExpediente() : "ID-" + s.getId();
    }

    private Specification<Pago> filtros(
            Long solicitudId,
            Long ciudadanoId,
            EstadoPago estado,
            Long tipoTramiteId,
            String concepto,
            String codigoOperacion,
            LocalDate fechaDesde,
            LocalDate fechaHasta) {

        return (Root<Pago> root, jakarta.persistence.criteria.CriteriaQuery<?> query,
                jakarta.persistence.criteria.CriteriaBuilder cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (solicitudId != null) {
                predicados.add(cb.equal(root.get("solicitud").get("id"), solicitudId));
            }
            if (ciudadanoId != null) {
                predicados.add(cb.equal(root.get("solicitud").get("ciudadano").get("id"), ciudadanoId));
            }
            if (estado != null) {
                predicados.add(cb.equal(root.get("estado"), estado));
            }
            if (tipoTramiteId != null) {
                predicados.add(cb.equal(root.get("solicitud").get("tipoTramite").get("id"), tipoTramiteId));
            }
            if (StringUtils.hasText(concepto)) {
                predicados.add(cb.like(cb.lower(root.get("concepto")), "%" + concepto.trim().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(codigoOperacion)) {
                predicados.add(cb.like(cb.lower(root.get("codigoOperacion")),
                        "%" + codigoOperacion.trim().toLowerCase() + "%"));
            }
            if (fechaDesde != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("fechaPago"), fechaDesde.atStartOfDay()));
            }
            if (fechaHasta != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("fechaPago"), fechaHasta.atTime(LocalTime.MAX)));
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
