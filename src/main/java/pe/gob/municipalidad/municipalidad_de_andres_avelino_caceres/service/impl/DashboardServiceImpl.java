package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ActividadRecienteResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.DashboardEstadisticasResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.DashboardResumenResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.DashboardResumenResponse.MetricasCiudadanos;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.DashboardResumenResponse.MetricasEstablecimientos;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.DashboardResumenResponse.MetricasInspecciones;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.DashboardResumenResponse.MetricasLicencias;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.DashboardResumenResponse.MetricasPagos;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.DashboardResumenResponse.MetricasSolicitudes;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.EstadisticaInspeccionResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.EstadisticaInspeccionResponse.ConteoPorInspector;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.EstadisticaInspeccionResponse.ConteoPorPeriodo;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.EstadisticaLicenciaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.EstadisticaLicenciaResponse.ConteoPorGiro;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.EstadisticaPagoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.EstadisticaPagoResponse.MontoMensual;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.EstadisticaPagoResponse.MontoPorConcepto;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.EstadisticaSolicitudResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.EstadisticaSolicitudResponse.ConteoPorTipoTramite;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Auditoria;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Inspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Licencia;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Pago;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Solicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.TipoTramite;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Usuario;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoInspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoLicencia;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoPago;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoSolicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.ResultadoInspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.AuditoriaRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.CiudadanoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.EstablecimientoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.InspeccionRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.LicenciaRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.PagoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.SolicitudRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.DashboardService;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final DateTimeFormatter FORMATO_PERIODO = DateTimeFormatter.ofPattern("yyyy-MM");

    private static final Set<EstadoSolicitud> ESTADOS_SOLICITUD_PENDIENTES = EnumSet.of(
            EstadoSolicitud.PRESENTADA,
            EstadoSolicitud.EN_REVISION,
            EstadoSolicitud.SUBSANADA);

    private static final Set<EstadoPago> ESTADOS_PAGO_PENDIENTES = EnumSet.of(
            EstadoPago.PENDIENTE,
            EstadoPago.PENDIENTE_VERIFICACION);

    private static final Set<EstadoPago> ESTADOS_PAGO_VERIFICADOS = EnumSet.of(
            EstadoPago.VERIFICADO,
            EstadoPago.PAGADO);

    private final CiudadanoRepository ciudadanos;
    private final EstablecimientoRepository establecimientos;
    private final SolicitudRepository solicitudes;
    private final LicenciaRepository licencias;
    private final InspeccionRepository inspecciones;
    private final PagoRepository pagos;
    private final AuditoriaRepository auditorias;
    private final int maxActividadReciente;

    @Autowired
    public DashboardServiceImpl(
            CiudadanoRepository ciudadanos,
            EstablecimientoRepository establecimientos,
            SolicitudRepository solicitudes,
            LicenciaRepository licencias,
            InspeccionRepository inspecciones,
            PagoRepository pagos,
            AuditoriaRepository auditorias,
            @Value("${app.dashboard.actividad-reciente.max-limit:100}") int maxActividadReciente) {
        this.ciudadanos = ciudadanos;
        this.establecimientos = establecimientos;
        this.solicitudes = solicitudes;
        this.licencias = licencias;
        this.inspecciones = inspecciones;
        this.pagos = pagos;
        this.auditorias = auditorias;
        this.maxActividadReciente = maxActividadReciente > 0 ? maxActividadReciente : 100;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResumenResponse obtenerResumen() {
        exigirRolPersonalMunicipal();

        long totalCiudadanos = ciudadanos.count();
        long totalEstablecimientos = establecimientos.count();

        long totalSolicitudes = solicitudes.count();
        long solPendientes = solicitudes.countByEstadoIn(ESTADOS_SOLICITUD_PENDIENTES);
        long solObservadas = solicitudes.countByEstado(EstadoSolicitud.OBSERVADA);
        long solAprobadas = solicitudes.countByEstado(EstadoSolicitud.APROBADA);
        long solRechazadas = solicitudes.countByEstado(EstadoSolicitud.RECHAZADA);

        long totalLicencias = licencias.count();
        long licVigentes = licencias.countByEstado(EstadoLicencia.VIGENTE);
        long licSuspendidas = licencias.countByEstado(EstadoLicencia.SUSPENDIDA);

        long inspProgramadas = inspecciones.countByEstado(EstadoInspeccion.PROGRAMADA);
        long inspFinalizadas = inspecciones.countByEstado(EstadoInspeccion.FINALIZADA);

        long pagosPendientes = pagos.countByEstadoIn(ESTADOS_PAGO_PENDIENTES);
        long pagosVerificados = pagos.countByEstadoIn(ESTADOS_PAGO_VERIFICADOS);
        BigDecimal montoVerificadoRaw = pagos.sumarMontoPorEstadosPen(ESTADOS_PAGO_VERIFICADOS);
        BigDecimal montoVerificadoPen = (montoVerificadoRaw != null ? montoVerificadoRaw : BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        return new DashboardResumenResponse(
                new MetricasCiudadanos(totalCiudadanos),
                new MetricasEstablecimientos(totalEstablecimientos),
                new MetricasSolicitudes(totalSolicitudes, solPendientes, solObservadas, solAprobadas, solRechazadas),
                new MetricasLicencias(totalLicencias, licVigentes, licSuspendidas),
                new MetricasInspecciones(inspProgramadas, inspFinalizadas),
                new MetricasPagos(pagosPendientes, pagosVerificados, montoVerificadoPen));
    }

    @Override
    @Transactional(readOnly = true)
    public EstadisticaSolicitudResponse obtenerEstadisticasSolicitudes(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Long tipoTramiteId,
            EstadoSolicitud estado) {
        exigirRolPersonalMunicipal();
        validarRangoFechas(fechaDesde, fechaHasta);

        Specification<Solicitud> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (tipoTramiteId != null) {
                p.add(cb.equal(root.get("tipoTramite").get("id"), tipoTramiteId));
            }
            if (estado != null) {
                p.add(cb.equal(root.get("estado"), estado));
            }
            if (fechaDesde != null) {
                p.add(cb.greaterThanOrEqualTo(
                        cb.coalesce(root.get("fechaPresentacion"), root.get("fechaActualizacion")),
                        fechaDesde.atStartOfDay()));
            }
            if (fechaHasta != null) {
                p.add(cb.lessThanOrEqualTo(
                        cb.coalesce(root.get("fechaPresentacion"), root.get("fechaActualizacion")),
                        fechaHasta.atTime(LocalTime.MAX)));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };

        List<Solicitud> lista = solicitudes.findAll(spec);

        Map<String, Long> porEstado = new LinkedHashMap<>();
        for (EstadoSolicitud est : EstadoSolicitud.values()) {
            porEstado.put(est.name(), 0L);
        }

        Map<String, Long> conteoMensualMap = new TreeMap<>();
        Map<Long, AcumuladorTramite> porTramiteMap = new LinkedHashMap<>();

        for (Solicitud s : lista) {
            if (s.getEstado() != null) {
                porEstado.merge(s.getEstado().name(), 1L, Long::sum);
            }
            LocalDateTime fechaRef = s.getFechaPresentacion() != null
                    ? s.getFechaPresentacion()
                    : s.getFechaActualizacion();
            if (fechaRef != null) {
                String periodo = fechaRef.format(FORMATO_PERIODO);
                conteoMensualMap.merge(periodo, 1L, Long::sum);
            }
            TipoTramite tt = s.getTipoTramite();
            if (tt != null && tt.getId() != null) {
                porTramiteMap.computeIfAbsent(tt.getId(),
                        id -> new AcumuladorTramite(id, tt.getCodigo(), tt.getNombre()))
                        .incrementar();
            }
        }

        List<EstadisticaSolicitudResponse.ConteoMensual> porMes = conteoMensualMap.entrySet().stream()
                .map(e -> new EstadisticaSolicitudResponse.ConteoMensual(e.getKey(), e.getValue()))
                .toList();

        List<ConteoPorTipoTramite> porTipoTramite = porTramiteMap.values().stream()
                .sorted(Comparator.comparingLong(AcumuladorTramite::cantidad).reversed())
                .map(a -> new ConteoPorTipoTramite(a.id, a.codigo, a.nombre, a.cantidad))
                .toList();

        return new EstadisticaSolicitudResponse(
                lista.size(),
                porEstado,
                porMes,
                porTipoTramite,
                fechaDesde,
                fechaHasta);
    }

    @Override
    @Transactional(readOnly = true)
    public EstadisticaLicenciaResponse obtenerEstadisticasLicencias(
            LocalDate fechaDesde,
            LocalDate fechaHasta) {
        exigirRolPersonalMunicipal();
        validarRangoFechas(fechaDesde, fechaHasta);

        Specification<Licencia> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (fechaDesde != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("fechaEmision"), fechaDesde));
            }
            if (fechaHasta != null) {
                p.add(cb.lessThanOrEqualTo(root.get("fechaEmision"), fechaHasta));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };

        List<Licencia> lista = licencias.findAll(spec);

        long vigentes = 0L;
        long suspendidas = 0L;
        long revocadas = 0L;
        Map<String, Long> porMesMap = new TreeMap<>();
        Map<String, Long> porGiroMap = new LinkedHashMap<>();

        for (Licencia l : lista) {
            if (l.getEstado() == EstadoLicencia.VIGENTE) {
                vigentes++;
            } else if (l.getEstado() == EstadoLicencia.SUSPENDIDA) {
                suspendidas++;
            } else if (l.getEstado() == EstadoLicencia.REVOCADA) {
                revocadas++;
            }

            if (l.getFechaEmision() != null) {
                porMesMap.merge(l.getFechaEmision().format(FORMATO_PERIODO), 1L, Long::sum);
            }

            String giro = "No especificado";
            if (l.getSolicitud() != null && l.getSolicitud().getEstablecimiento() != null
                    && StringUtils.hasText(l.getSolicitud().getEstablecimiento().getGiroNegocio())) {
                giro = l.getSolicitud().getEstablecimiento().getGiroNegocio().trim();
            }
            porGiroMap.merge(giro, 1L, Long::sum);
        }

        List<EstadisticaLicenciaResponse.ConteoMensual> porMes = porMesMap.entrySet().stream()
                .map(e -> new EstadisticaLicenciaResponse.ConteoMensual(e.getKey(), e.getValue()))
                .toList();

        List<ConteoPorGiro> porGiro = porGiroMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new ConteoPorGiro(e.getKey(), e.getValue()))
                .toList();

        return new EstadisticaLicenciaResponse(
                lista.size(),
                vigentes,
                suspendidas,
                revocadas,
                porMes,
                porGiro,
                fechaDesde,
                fechaHasta);
    }

    @Override
    @Transactional(readOnly = true)
    public EstadisticaInspeccionResponse obtenerEstadisticasInspecciones(
            LocalDate fechaDesde,
            LocalDate fechaHasta) {
        exigirRolPersonalMunicipal();
        validarRangoFechas(fechaDesde, fechaHasta);

        Specification<Inspeccion> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (fechaDesde != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("fechaProgramada"), fechaDesde.atStartOfDay()));
            }
            if (fechaHasta != null) {
                p.add(cb.lessThanOrEqualTo(root.get("fechaProgramada"), fechaHasta.atTime(LocalTime.MAX)));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };

        List<Inspeccion> lista = inspecciones.findAll(spec);

        long programadas = 0L;
        long enProceso = 0L;
        long finalizadas = 0L;
        long canceladas = 0L;
        long favorables = 0L;
        long desfavorables = 0L;
        long conObservaciones = 0L;

        Map<Long, AcumuladorInspector> porInspectorMap = new LinkedHashMap<>();
        Map<String, Long> porPeriodoMap = new TreeMap<>();

        for (Inspeccion i : lista) {
            if (i.getEstado() == EstadoInspeccion.PROGRAMADA) {
                programadas++;
            } else if (i.getEstado() == EstadoInspeccion.EN_PROCESO) {
                enProceso++;
            } else if (i.getEstado() == EstadoInspeccion.FINALIZADA) {
                finalizadas++;
            } else if (i.getEstado() == EstadoInspeccion.CANCELADA) {
                canceladas++;
            }

            if (i.getResultado() == ResultadoInspeccion.FAVORABLE) {
                favorables++;
            } else if (i.getResultado() == ResultadoInspeccion.DESFAVORABLE) {
                desfavorables++;
            } else if (i.getResultado() == ResultadoInspeccion.CON_OBSERVACIONES) {
                conObservaciones++;
            }

            Usuario insp = i.getInspector();
            if (insp != null && insp.getId() != null) {
                String nombre = (insp.getNombres() != null ? insp.getNombres() : "") + " "
                        + (insp.getApellidos() != null ? insp.getApellidos() : "");
                porInspectorMap.computeIfAbsent(insp.getId(),
                        id -> new AcumuladorInspector(id, nombre.trim()))
                        .incrementar();
            }

            if (i.getFechaProgramada() != null) {
                porPeriodoMap.merge(i.getFechaProgramada().format(FORMATO_PERIODO), 1L, Long::sum);
            }
        }

        List<ConteoPorInspector> porInspector = porInspectorMap.values().stream()
                .sorted(Comparator.comparingLong(AcumuladorInspector::cantidad).reversed())
                .map(a -> new ConteoPorInspector(a.id, a.nombre, a.cantidad))
                .toList();

        List<ConteoPorPeriodo> porPeriodo = porPeriodoMap.entrySet().stream()
                .map(e -> new ConteoPorPeriodo(e.getKey(), e.getValue()))
                .toList();

        return new EstadisticaInspeccionResponse(
                programadas,
                enProceso,
                finalizadas,
                canceladas,
                favorables,
                desfavorables,
                conObservaciones,
                porInspector,
                porPeriodo,
                fechaDesde,
                fechaHasta);
    }

    @Override
    @Transactional(readOnly = true)
    public EstadisticaPagoResponse obtenerEstadisticasPagos(
            LocalDate fechaDesde,
            LocalDate fechaHasta) {
        exigirRolPersonalMunicipal();
        validarRangoFechas(fechaDesde, fechaHasta);

        Specification<Pago> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (fechaDesde != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("fechaPago"), fechaDesde.atStartOfDay()));
            }
            if (fechaHasta != null) {
                p.add(cb.lessThanOrEqualTo(root.get("fechaPago"), fechaHasta.atTime(LocalTime.MAX)));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };

        List<Pago> lista = pagos.findAll(spec);

        long verificados = 0L;
        long pendientes = 0L;
        long rechazados = 0L;
        long anulados = 0L;
        BigDecimal montoVerificadoPen = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        Map<String, AcumuladorMonto> porMesMap = new TreeMap<>();
        Map<String, AcumuladorMonto> porConceptoMap = new LinkedHashMap<>();

        for (Pago p : lista) {
            EstadoPago est = p.getEstado();
            if (est == EstadoPago.VERIFICADO || est == EstadoPago.PAGADO) {
                verificados++;
                if ("PEN".equalsIgnoreCase(p.getMoneda()) && p.getMonto() != null) {
                    BigDecimal m = p.getMonto().setScale(2, RoundingMode.HALF_UP);
                    montoVerificadoPen = montoVerificadoPen.add(m);

                    if (p.getFechaPago() != null) {
                        String periodo = p.getFechaPago().format(FORMATO_PERIODO);
                        porMesMap.computeIfAbsent(periodo, AcumuladorMonto::new).agregar(m);
                    }
                    String concepto = StringUtils.hasText(p.getConcepto())
                            ? p.getConcepto().trim()
                            : "Sin concepto";
                    porConceptoMap.computeIfAbsent(concepto, AcumuladorMonto::new).agregar(m);
                }
            } else if (est == EstadoPago.PENDIENTE_VERIFICACION || est == EstadoPago.PENDIENTE) {
                pendientes++;
            } else if (est == EstadoPago.RECHAZADO) {
                rechazados++;
            } else if (est == EstadoPago.ANULADO || est == EstadoPago.REEMBOLSADO) {
                anulados++;
            }
        }

        List<MontoMensual> porMes = porMesMap.values().stream()
                .map(a -> new MontoMensual(a.clave, a.cantidad, a.monto))
                .toList();

        List<MontoPorConcepto> porConcepto = porConceptoMap.values().stream()
                .sorted(Comparator.comparing(AcumuladorMonto::monto).reversed())
                .map(a -> new MontoPorConcepto(a.clave, a.cantidad, a.monto))
                .toList();

        return new EstadisticaPagoResponse(
                lista.size(),
                verificados,
                pendientes,
                rechazados,
                anulados,
                montoVerificadoPen,
                "PEN",
                porMes,
                porConcepto,
                fechaDesde,
                fechaHasta);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardEstadisticasResponse obtenerEstadisticasGenerales(
            LocalDate fechaDesde,
            LocalDate fechaHasta) {
        exigirRolPersonalMunicipal();
        validarRangoFechas(fechaDesde, fechaHasta);

        return new DashboardEstadisticasResponse(
                obtenerEstadisticasSolicitudes(fechaDesde, fechaHasta, null, null),
                obtenerEstadisticasLicencias(fechaDesde, fechaHasta),
                obtenerEstadisticasInspecciones(fechaDesde, fechaHasta),
                obtenerEstadisticasPagos(fechaDesde, fechaHasta));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActividadRecienteResponse> obtenerActividadReciente(Integer limite) {
        exigirRolPersonalMunicipal();

        int limiteEfectivo = 20;
        if (limite != null) {
            if (limite < 1) {
                throw new BusinessException("El límite de actividad reciente debe ser al menos 1.");
            }
            limiteEfectivo = Math.min(limite, maxActividadReciente);
        }

        Page<Auditoria> pagina = auditorias.findAll(
                (root, query, cb) -> cb.conjunction(),
                PageRequest.of(0, limiteEfectivo, Sort.by(Sort.Direction.DESC, "fechaOperacion", "id")));

        boolean puedeVerIdentidadCompleta = tieneRol("ROLE_ADMINISTRADOR") || tieneRol("ROLE_SUPERVISOR");

        return pagina.getContent().stream()
                .map(a -> {
                    String responsable = "Sistema";
                    if (a.getUsuario() != null) {
                        if (puedeVerIdentidadCompleta) {
                            String nombreCompleto = ((a.getUsuario().getNombres() != null ? a.getUsuario().getNombres() : "")
                                    + " "
                                    + (a.getUsuario().getApellidos() != null ? a.getUsuario().getApellidos() : "")).trim();
                            responsable = StringUtils.hasText(nombreCompleto)
                                    ? nombreCompleto + " (" + a.getUsuario().getUsername() + ")"
                                    : a.getUsuario().getUsername();
                        } else {
                            responsable = "Personal municipal";
                        }
                    }
                    String desc = StringUtils.hasText(a.getDescripcion()) ? a.getDescripcion().trim() : "";
                    if (desc.length() > 160) {
                        desc = desc.substring(0, 157) + "...";
                    }
                    return new ActividadRecienteResponse(
                            a.getId(),
                            a.getFechaOperacion(),
                            a.getAccion(),
                            a.getEntidadAfectada(),
                            desc,
                            responsable);
                })
                .toList();
    }

    private void validarRangoFechas(LocalDate fechaDesde, LocalDate fechaHasta) {
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new BusinessException("La fecha inicial no puede ser posterior a la fecha final.");
        }
    }

    private void exigirRolPersonalMunicipal() {
        if (!tieneRol("ROLE_ADMINISTRADOR")
                && !tieneRol("ROLE_SUPERVISOR")
                && !tieneRol("ROLE_FUNCIONARIO")) {
            throw new AccessDeniedException(
                    "El acceso al dashboard administrativo y estadísticas generales está reservado al personal municipal autorizado.");
        }
    }

    private boolean tieneRol(String rolConPrefijo) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if (rolConPrefijo.equals(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private static final class AcumuladorTramite {
        private final Long id;
        private final String codigo;
        private final String nombre;
        private long cantidad;

        private AcumuladorTramite(Long id, String codigo, String nombre) {
            this.id = id;
            this.codigo = codigo;
            this.nombre = nombre;
        }

        private void incrementar() {
            this.cantidad++;
        }

        private long cantidad() {
            return cantidad;
        }
    }

    private static final class AcumuladorInspector {
        private final Long id;
        private final String nombre;
        private long cantidad;

        private AcumuladorInspector(Long id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        private void incrementar() {
            this.cantidad++;
        }

        private long cantidad() {
            return cantidad;
        }
    }

    private static final class AcumuladorMonto {
        private final String clave;
        private long cantidad;
        private BigDecimal monto = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        private AcumuladorMonto(String clave) {
            this.clave = clave;
        }

        private void agregar(BigDecimal valor) {
            this.cantidad++;
            this.monto = this.monto.add(valor);
        }

        private BigDecimal monto() {
            return monto;
        }
    }
}
