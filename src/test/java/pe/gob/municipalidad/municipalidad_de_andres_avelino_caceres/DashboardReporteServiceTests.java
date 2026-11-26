package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ReporteFiltroRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ActividadRecienteResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.DashboardEstadisticasResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.DashboardResumenResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.EstadisticaInspeccionResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.EstadisticaLicenciaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.EstadisticaPagoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.EstadisticaSolicitudResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse.FilaCiudadanoReporte;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse.FilaEstablecimientoReporte;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse.FilaInspeccionReporte;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse.FilaLicenciaReporte;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse.FilaPagoReporte;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse.FilaSolicitudReporte;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Auditoria;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Ciudadano;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Establecimiento;
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
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoDocumento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.AuditoriaRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.CiudadanoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.EstablecimientoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.InspeccionRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.LicenciaRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.PagoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.SolicitudRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.AuditoriaService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.ReporteService.ArchivoReporte;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl.DashboardServiceImpl;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl.ReporteServiceImpl;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.util.PdfGenerator;

/**
 * Pruebas unitarias de DashboardServiceImpl y ReporteServiceImpl (PROMPT 08):
 * resumen ejecutivo, estadísticas por módulo, actividad reciente, reportes paginados JSON,
 * exportación PDF (incluyendo paginación de 50+ filas), exportación CSV con escape y
 * protección contra CSV injection, límite de exportación y control de roles.
 */
@ExtendWith(MockitoExtension.class)
class DashboardReporteServiceTests {

    @Mock private CiudadanoRepository ciudadanoRepository;
    @Mock private EstablecimientoRepository establecimientoRepository;
    @Mock private SolicitudRepository solicitudRepository;
    @Mock private LicenciaRepository licenciaRepository;
    @Mock private InspeccionRepository inspeccionRepository;
    @Mock private PagoRepository pagoRepository;
    @Mock private AuditoriaRepository auditoriaRepository;
    @Mock private AuditoriaService auditoriaService;

    private DashboardServiceImpl dashboardService;
    private ReporteServiceImpl reporteService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardServiceImpl(
                ciudadanoRepository,
                establecimientoRepository,
                solicitudRepository,
                licenciaRepository,
                inspeccionRepository,
                pagoRepository,
                auditoriaRepository,
                100);
        reporteService = new ReporteServiceImpl(
                solicitudRepository,
                licenciaRepository,
                inspeccionRepository,
                pagoRepository,
                establecimientoRepository,
                ciudadanoRepository,
                new PdfGenerator(),
                auditoriaService,
                1000);
        autenticarRol("ROLE_ADMINISTRADOR", "admin_muni");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarRol(String rol, String username) {
        var auth = new UsernamePasswordAuthenticationToken(
                username, "N/A", List.of(new SimpleGrantedAuthority(rol)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // =========================================================================
    // SECCIÓN 20: PRUEBAS DEL DASHBOARD
    // =========================================================================

    @Test
    void resumenAdministrativoConDatosSeparaMontosVerificadosDePendientes() {
        when(ciudadanoRepository.count()).thenReturn(45L);
        when(establecimientoRepository.count()).thenReturn(30L);

        when(solicitudRepository.count()).thenReturn(20L);
        when(solicitudRepository.countByEstadoIn(any())).thenReturn(8L);
        when(solicitudRepository.countByEstado(EstadoSolicitud.OBSERVADA)).thenReturn(2L);
        when(solicitudRepository.countByEstado(EstadoSolicitud.APROBADA)).thenReturn(6L);
        when(solicitudRepository.countByEstado(EstadoSolicitud.RECHAZADA)).thenReturn(2L);

        when(licenciaRepository.count()).thenReturn(10L);
        when(licenciaRepository.countByEstado(EstadoLicencia.VIGENTE)).thenReturn(7L);
        when(licenciaRepository.countByEstado(EstadoLicencia.SUSPENDIDA)).thenReturn(1L);

        when(inspeccionRepository.countByEstado(EstadoInspeccion.PROGRAMADA)).thenReturn(3L);
        when(inspeccionRepository.countByEstado(EstadoInspeccion.FINALIZADA)).thenReturn(6L);

        when(pagoRepository.countByEstadoIn(any())).thenReturn(4L, 9L);
        when(pagoRepository.sumarMontoPorEstadosPen(any()))
                .thenReturn(new BigDecimal("1850.50"));

        DashboardResumenResponse res = dashboardService.obtenerResumen();

        assertThat(res.ciudadanos().total()).isEqualTo(45L);
        assertThat(res.establecimientos().total()).isEqualTo(30L);
        assertThat(res.solicitudes().total()).isEqualTo(20L);
        assertThat(res.solicitudes().pendientes()).isEqualTo(8L);
        assertThat(res.licencias().vigentes()).isEqualTo(7L);
        assertThat(res.inspecciones().finalizadas()).isEqualTo(6L);
        assertThat(res.pagos().pendientes()).isEqualTo(4L);
        assertThat(res.pagos().verificados()).isEqualTo(9L);
        assertThat(res.pagos().montoVerificadoPEN()).isEqualByComparingTo("1850.50");
    }

    @Test
    void resumenAdministrativoConCeroRegistrosDevuelveCerosYEscalaMonetariaDosDecimales() {
        DashboardResumenResponse res = dashboardService.obtenerResumen();

        assertThat(res.ciudadanos().total()).isZero();
        assertThat(res.establecimientos().total()).isZero();
        assertThat(res.solicitudes().total()).isZero();
        assertThat(res.licencias().total()).isZero();
        assertThat(res.inspecciones().programadas()).isZero();
        assertThat(res.pagos().verificados()).isZero();
        assertThat(res.pagos().montoVerificadoPEN()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void estadisticasGlobalesSinFiltrosConsolidaCuatroModulos() {
        when(solicitudRepository.findAll(any(Specification.class))).thenReturn(List.of(crearSolicitudMuestra()));
        when(licenciaRepository.findAll(any(Specification.class))).thenReturn(List.of(crearLicenciaMuestra()));
        when(inspeccionRepository.findAll(any(Specification.class))).thenReturn(List.of(crearInspeccionMuestra()));
        when(pagoRepository.findAll(any(Specification.class))).thenReturn(List.of(crearPagoMuestra(EstadoPago.VERIFICADO, "120.00")));

        DashboardEstadisticasResponse global = dashboardService.obtenerEstadisticasGenerales(null, null);

        assertThat(global.solicitudes().total()).isEqualTo(1L);
        assertThat(global.licencias().totalEmitidas()).isEqualTo(1L);
        assertThat(global.inspecciones().totalFinalizadas()).isEqualTo(1L);
        assertThat(global.pagos().cantidadRegistrados()).isEqualTo(1L);
        assertThat(global.pagos().montoVerificadoPEN()).isEqualByComparingTo("120.00");
    }

    @Test
    @SuppressWarnings("unchecked")
    void estadisticasFiltradasPorRangoDeFechasValido() {
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 12, 31);
        when(solicitudRepository.findAll(any(Specification.class))).thenReturn(List.of(crearSolicitudMuestra()));

        EstadisticaSolicitudResponse est = dashboardService.obtenerEstadisticasSolicitudes(desde, hasta, null, null);

        assertThat(est.fechaDesde()).isEqualTo(desde);
        assertThat(est.fechaHasta()).isEqualTo(hasta);
        assertThat(est.total()).isEqualTo(1L);
    }

    @Test
    void estadisticasRechazaFechaDesdeMayorQueFechaHasta() {
        LocalDate desde = LocalDate.of(2026, 6, 15);
        LocalDate hasta = LocalDate.of(2026, 6, 1);

        assertThatThrownBy(() -> dashboardService.obtenerEstadisticasGenerales(desde, hasta))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("posterior");
    }

    @Test
    @SuppressWarnings("unchecked")
    void estadisticasDeSolicitudesPorEstadoTipoTramiteYMes() {
        Solicitud s1 = crearSolicitudMuestra();
        s1.setEstado(EstadoSolicitud.APROBADA);
        s1.setFechaPresentacion(LocalDateTime.of(2026, 3, 10, 9, 30));

        Solicitud s2 = crearSolicitudMuestra();
        s2.setId(101L);
        s2.setEstado(EstadoSolicitud.EN_REVISION);
        s2.setFechaPresentacion(LocalDateTime.of(2026, 3, 22, 11, 0));

        when(solicitudRepository.findAll(any(Specification.class))).thenReturn(List.of(s1, s2));

        EstadisticaSolicitudResponse est = dashboardService.obtenerEstadisticasSolicitudes(null, null, null, null);

        assertThat(est.total()).isEqualTo(2L);
        assertThat(est.porEstado().get("APROBADA")).isEqualTo(1L);
        assertThat(est.porEstado().get("EN_REVISION")).isEqualTo(1L);
        assertThat(est.porTipoTramite()).extracting("nombreTramite").contains("Licencia de Funcionamiento Indeterminada");
        assertThat(est.porMes()).extracting("periodo").contains("2026-03");
        assertThat(est.porMes().get(0).cantidad()).isEqualTo(2L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void estadisticasDeLicenciasPorEstadoYGiroComercial() {
        Licencia l1 = crearLicenciaMuestra();
        when(licenciaRepository.findAll(any(Specification.class))).thenReturn(List.of(l1));

        EstadisticaLicenciaResponse est = dashboardService.obtenerEstadisticasLicencias(null, null);

        assertThat(est.totalEmitidas()).isEqualTo(1L);
        assertThat(est.totalVigentes()).isEqualTo(1L);
        assertThat(est.porGiroNegocio()).extracting("giroNegocio").contains("Botica y Farmacia");
        assertThat(est.porMesEmision()).isNotEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void estadisticasDeInspeccionesPorEstadoYResultado() {
        Inspeccion i1 = crearInspeccionMuestra();
        when(inspeccionRepository.findAll(any(Specification.class))).thenReturn(List.of(i1));

        EstadisticaInspeccionResponse est = dashboardService.obtenerEstadisticasInspecciones(null, null);

        assertThat(est.totalFinalizadas()).isEqualTo(1L);
        assertThat(est.totalFavorables()).isEqualTo(1L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void estadisticasDePagosDiferenciaMontosPendientesVerificadosRechazadosYAnulados() {
        Pago pVerificado = crearPagoMuestra(EstadoPago.VERIFICADO, "250.00");
        Pago pPendiente = crearPagoMuestra(EstadoPago.PENDIENTE, "90.00");
        Pago pPendVerif = crearPagoMuestra(EstadoPago.PENDIENTE_VERIFICACION, "60.00");
        Pago pRechazado = crearPagoMuestra(EstadoPago.RECHAZADO, "40.00");
        Pago pAnulado = crearPagoMuestra(EstadoPago.ANULADO, "30.00");

        when(pagoRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(pVerificado, pPendiente, pPendVerif, pRechazado, pAnulado));

        EstadisticaPagoResponse est = dashboardService.obtenerEstadisticasPagos(null, null);

        assertThat(est.cantidadRegistrados()).isEqualTo(5L);
        assertThat(est.cantidadVerificados()).isEqualTo(1L);
        assertThat(est.cantidadPendientes()).isEqualTo(2L);
        assertThat(est.cantidadRechazados()).isEqualTo(1L);
        assertThat(est.cantidadAnulados()).isEqualTo(1L);
        assertThat(est.montoVerificadoPEN()).isEqualByComparingTo("250.00");
    }

    @Test
    @SuppressWarnings("unchecked")
    void actividadRecienteRespetaLimiteMaximoConfigurado() {
        Auditoria a = new Auditoria();
        a.setId(99L);
        a.setEntidadAfectada("LICENCIAS");
        a.setAccion("EMISION_LICENCIA");
        a.setDescripcion("Licencia emitida LIC-2026-000001");
        a.setFechaOperacion(LocalDateTime.now());

        when(auditoriaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(a)));

        List<ActividadRecienteResponse> lista = dashboardService.obtenerActividadReciente(500);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).modulo()).isEqualTo("LICENCIAS");
        assertThat(lista.get(0).usuarioResponsable()).isEqualTo("Sistema");
    }

    @Test
    void dashboardPermiteFuncionarioSupervisorYDeniegaCiudadano() {
        autenticarRol("ROLE_FUNCIONARIO", "func_muni");
        assertThat(dashboardService.obtenerResumen()).isNotNull();

        autenticarRol("ROLE_SUPERVISOR", "sup_muni");
        assertThat(dashboardService.obtenerResumen()).isNotNull();

        autenticarRol("ROLE_CIUDADANO", "ciudadano_01");
        assertThatThrownBy(() -> dashboardService.obtenerResumen())
                .isInstanceOf(AccessDeniedException.class);
    }

    // =========================================================================
    // SECCIÓN 21: PRUEBAS DE REPORTES Y EXPORTACIÓN PDF / CSV
    // =========================================================================

    @Test
    @SuppressWarnings("unchecked")
    void reportesJsonPaginadosParaSeisModulosConMinimizacionDeDatos() {
        Pageable pageable = PageRequest.of(0, 10);
        ReporteFiltroRequest filtro = new ReporteFiltroRequest(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        when(solicitudRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(crearSolicitudMuestra()), pageable, 1));
        when(licenciaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(crearLicenciaMuestra()), pageable, 1));
        when(inspeccionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(crearInspeccionMuestra()), pageable, 1));
        when(pagoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(crearPagoMuestra(EstadoPago.VERIFICADO, "120.00")), pageable, 1));
        when(establecimientoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(crearEstablecimientoMuestra()), pageable, 1));
        when(ciudadanoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(crearCiudadanoMuestra()), pageable, 1));

        ReporteResponse<FilaSolicitudReporte> repSol = reporteService.reporteSolicitudes(filtro, pageable);
        ReporteResponse<FilaLicenciaReporte> repLic = reporteService.reporteLicencias(filtro, pageable);
        ReporteResponse<FilaInspeccionReporte> repInsp = reporteService.reporteInspecciones(filtro, pageable);
        ReporteResponse<FilaPagoReporte> repPag = reporteService.reportePagos(filtro, pageable);
        ReporteResponse<FilaEstablecimientoReporte> repEstAdmin = reporteService.reporteEstablecimientos(filtro, pageable);
        ReporteResponse<FilaCiudadanoReporte> repCiu = reporteService.reporteCiudadanos(filtro, pageable);

        assertThat(repSol.datos().contenido()).hasSize(1);
        assertThat(repLic.datos().contenido()).hasSize(1);
        assertThat(repInsp.datos().contenido()).hasSize(1);
        assertThat(repPag.datos().contenido()).hasSize(1);
        // Administrador ve RUC completo
        assertThat(repEstAdmin.datos().contenido().get(0).ruc()).isEqualTo("20601234567");
        // Documento de ciudadano siempre enmascarado en reporte general
        assertThat(repCiu.datos().contenido().get(0).documentoEnmascarado()).isEqualTo("****5678");

        // Funcionario ve RUC parcialmente enmascarado
        autenticarRol("ROLE_FUNCIONARIO", "func_muni");
        ReporteResponse<FilaEstablecimientoReporte> repEstFunc = reporteService.reporteEstablecimientos(filtro, pageable);
        assertThat(repEstFunc.datos().contenido().get(0).ruc()).isEqualTo("20*******67");
    }

    @Test
    @SuppressWarnings("unchecked")
    void exportacionPdfDeCincoModulosGeneraPdfValidoYAudita() {
        when(solicitudRepository.findAll(any(Specification.class))).thenReturn(List.of(crearSolicitudMuestra()));
        when(licenciaRepository.findAll(any(Specification.class))).thenReturn(List.of(crearLicenciaMuestra()));
        when(inspeccionRepository.findAll(any(Specification.class))).thenReturn(List.of(crearInspeccionMuestra()));
        when(pagoRepository.findAll(any(Specification.class))).thenReturn(List.of(crearPagoMuestra(EstadoPago.VERIFICADO, "150.00")));
        when(establecimientoRepository.findAll(any(Specification.class))).thenReturn(List.of(crearEstablecimientoMuestra()));

        ArchivoReporte pdfSol = reporteService.exportarSolicitudesPdf(null);
        ArchivoReporte pdfLic = reporteService.exportarLicenciasPdf(null);
        ArchivoReporte pdfInsp = reporteService.exportarInspeccionesPdf(null);
        ArchivoReporte pdfPag = reporteService.exportarPagosPdf(null);
        ArchivoReporte pdfEst = reporteService.exportarEstablecimientosPdf(null);

        for (ArchivoReporte pdf : List.of(pdfSol, pdfLic, pdfInsp, pdfPag, pdfEst)) {
            assertThat(pdf.tipoMime()).isEqualTo("application/pdf");
            assertThat(pdf.contenido()).isNotEmpty();
            String header = new String(pdf.contenido(), 0, 4, StandardCharsets.ISO_8859_1);
            assertThat(header).isEqualTo("%PDF");
        }
        verify(auditoriaService, atLeastOnce()).registrar(anyString(), anyString(), any(), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void exportacionPdfCon55FilasPaginaCorrectamenteSinExcepcion() {
        List<Solicitud> cincuentaYCinco = new ArrayList<>();
        for (int i = 1; i <= 55; i++) {
            Solicitud s = crearSolicitudMuestra();
            s.setId((long) i);
            s.setNumeroExpediente(String.format("EXP-2026-%06d", i));
            cincuentaYCinco.add(s);
        }
        when(solicitudRepository.findAll(any(Specification.class))).thenReturn(cincuentaYCinco);

        ArchivoReporte pdf = reporteService.exportarSolicitudesPdf(null);

        assertThat(pdf.contenido()).hasSizeGreaterThan(2000);
        assertThat(new String(pdf.contenido(), 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    @Test
    @SuppressWarnings("unchecked")
    void exportacionCsvConEncabezadosEscapeDeCaracteresYProteccionFormulaInjection() {
        Establecimiento e = crearEstablecimientoMuestra();
        e.setNombreComercial("=CMD|' /C calc'!A0, \"Local Especial\"");
        e.setRazonSocial("+Inversiones @Andinas; S.A.C.");
        e.setDireccion("-Av. Los Libertadores\nNro 123");

        when(establecimientoRepository.findAll(any(Specification.class))).thenReturn(List.of(e));

        ArchivoReporte csv = reporteService.exportarEstablecimientosCsv(null);
        String textoCsv = new String(csv.contenido(), StandardCharsets.UTF_8);

        assertThat(csv.tipoMime()).isEqualTo("text/csv;charset=UTF-8");
        assertThat(textoCsv).startsWith("Nombre Comercial,Razón Social,RUC,Dirección,Giro Comercial,Estado\r\n");
        // Verifica neutralización de '=' con apóstrofe inicial y escape de comillas dobles
        assertThat(textoCsv).contains("\"'=CMD|' /C calc'!A0, \"\"Local Especial\"\"\"");
        // Verifica neutralización de '+' y ';' entrecomillado
        assertThat(textoCsv).contains("\"'+Inversiones @Andinas; S.A.C.\"");
        // Verifica neutralización de '-' y salto de línea entrecomillado
        assertThat(textoCsv).contains("\"'-Av. Los Libertadores\nNro 123\"");
        verify(auditoriaService, atLeastOnce()).registrar(anyString(), anyString(), any(), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void exportacionRechazaCuandoSuperaLimiteMaximoConfigurado() {
        ReporteServiceImpl servicioConLimiteBajo = new ReporteServiceImpl(
                solicitudRepository,
                licenciaRepository,
                inspeccionRepository,
                pagoRepository,
                establecimientoRepository,
                ciudadanoRepository,
                new PdfGenerator(),
                auditoriaService,
                2);

        when(solicitudRepository.findAll(any(Specification.class))).thenReturn(
                List.of(crearSolicitudMuestra(), crearSolicitudMuestra(), crearSolicitudMuestra()));

        assertThatThrownBy(() -> servicioConLimiteBajo.exportarSolicitudesCsv(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("límite máximo permitido");
    }

    @Test
    void reportesDeniegaAccesoACiudadano() {
        autenticarRol("ROLE_CIUDADANO", "ciudadano_01");
        assertThatThrownBy(() -> reporteService.reporteSolicitudes(null, PageRequest.of(0, 10)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> reporteService.exportarSolicitudesPdf(null))
                .isInstanceOf(AccessDeniedException.class);
    }

    // =========================================================================
    // DATOS DE MUESTRA
    // =========================================================================

    private Ciudadano crearCiudadanoMuestra() {
        Ciudadano c = new Ciudadano();
        c.setId(10L);
        c.setTipoDocumento(TipoDocumento.DNI);
        c.setNumeroDocumento("12345678");
        c.setNombres("María Elena");
        c.setApellidos("Quispe Huamán");
        c.setActivo(true);
        c.setFechaRegistro(LocalDateTime.of(2026, 2, 10, 8, 30));
        return c;
    }

    private Establecimiento crearEstablecimientoMuestra() {
        Establecimiento e = new Establecimiento();
        e.setId(20L);
        e.setCiudadano(crearCiudadanoMuestra());
        e.setNombreComercial("Botica Virgen del Carmen");
        e.setRazonSocial("Inversiones Farmacéuticas Ayacucho S.A.C.");
        e.setRuc("20601234567");
        e.setDireccion("Av. Mariscal Cáceres 450");
        e.setGiroNegocio("Botica y Farmacia");
        e.setAreaMetrosCuadrados(new BigDecimal("68.50"));
        e.setActivo(true);
        return e;
    }

    private Solicitud crearSolicitudMuestra() {
        TipoTramite tt = new TipoTramite();
        tt.setId(1L);
        tt.setCodigo("TT-LIC-IND");
        tt.setNombre("Licencia de Funcionamiento Indeterminada");

        Usuario func = new Usuario();
        func.setId(5L);
        func.setNombres("Jorge");
        func.setApellidos("Gómez");

        Solicitud s = new Solicitud();
        s.setId(100L);
        s.setNumeroExpediente("EXP-2026-000001");
        s.setCiudadano(crearCiudadanoMuestra());
        s.setEstablecimiento(crearEstablecimientoMuestra());
        s.setTipoTramite(tt);
        s.setUsuarioResponsable(func);
        s.setEstado(EstadoSolicitud.APROBADA);
        s.setFechaPresentacion(LocalDateTime.of(2026, 3, 15, 10, 0));
        s.setFechaActualizacion(LocalDateTime.of(2026, 3, 16, 15, 0));
        return s;
    }

    private Licencia crearLicenciaMuestra() {
        Licencia l = new Licencia();
        l.setId(300L);
        l.setCodigoLicencia("LIC-2026-000001");
        l.setTokenVerificacion("VERIF-2026-ABCD");
        l.setSolicitud(crearSolicitudMuestra());
        l.setFechaEmision(LocalDate.of(2026, 3, 20));
        l.setEstado(EstadoLicencia.VIGENTE);
        return l;
    }

    private Inspeccion crearInspeccionMuestra() {
        Usuario inspector = new Usuario();
        inspector.setId(7L);
        inspector.setNombres("Luis");
        inspector.setApellidos("Paredes");

        Inspeccion i = new Inspeccion();
        i.setId(400L);
        i.setSolicitud(crearSolicitudMuestra());
        i.setInspector(inspector);
        i.setFechaProgramada(LocalDateTime.of(2026, 3, 18, 9, 0));
        i.setFechaRealizada(LocalDateTime.of(2026, 3, 18, 11, 30));
        i.setEstado(EstadoInspeccion.FINALIZADA);
        i.setResultado(ResultadoInspeccion.FAVORABLE);
        return i;
    }

    private Pago crearPagoMuestra(EstadoPago estado, String monto) {
        Pago p = new Pago();
        p.setId(500L);
        p.setSolicitud(crearSolicitudMuestra());
        p.setCodigoOperacion("OP-2026-00100");
        p.setConcepto("Derecho de trámite de Licencia de Funcionamiento");
        p.setMonto(new BigDecimal(monto));
        p.setMoneda("PEN");
        p.setFechaPago(LocalDateTime.of(2026, 3, 15, 14, 0));
        p.setEstado(estado);
        return p;
    }
}
