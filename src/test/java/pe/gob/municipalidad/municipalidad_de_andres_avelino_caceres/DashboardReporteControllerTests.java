package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.EstadisticaLicenciaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.EstadisticaPagoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.EstadisticaSolicitudResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse.FilaCiudadanoReporte;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse.FilaEstablecimientoReporte;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse.FilaInspeccionReporte;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse.FilaLicenciaReporte;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse.FilaPagoReporte;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse.FilaSolicitudReporte;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoInspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoLicencia;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoPago;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoSolicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.ResultadoInspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoDocumento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.AuditoriaRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.CiudadanoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.DocumentoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.EstablecimientoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.HistorialLicenciaRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.HistorialPagoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.HistorialSolicitudRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.InspeccionRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.LicenciaRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.NotificacionRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.PagoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.RepresentanteRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.RequisitoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.RolRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.SolicitudRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.TipoTramiteRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.TipoTramiteRequisitoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.UsuarioRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.DashboardService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.ReporteService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.ReporteService.ArchivoReporte;

/**
 * Pruebas MockMvc de DashboardController y ReporteController (PROMPT 08):
 * control de acceso por roles (ADMINISTRADOR, SUPERVISOR, FUNCIONARIO vs CIUDADANO 403 y anónimo 401),
 * contratos JSON de dashboard/reportes y cabeceras de descarga PDF/CSV.
 */
@SpringBootTest(properties = "spring.autoconfigure.exclude="
        + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
        + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
        + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,"
        + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration")
@AutoConfigureMockMvc
class DashboardReporteControllerTests {

    @Autowired
    private MockMvc mvc;

    @MockitoBean private DashboardService dashboardService;
    @MockitoBean private ReporteService reporteService;

    @MockitoBean private UsuarioRepository usuarioRepository;
    @MockitoBean private RolRepository rolRepository;
    @MockitoBean private AuditoriaRepository auditoriaRepository;
    @MockitoBean private CiudadanoRepository ciudadanoRepository;
    @MockitoBean private RepresentanteRepository representanteRepository;
    @MockitoBean private EstablecimientoRepository establecimientoRepository;
    @MockitoBean private SolicitudRepository solicitudRepository;
    @MockitoBean private TipoTramiteRepository tipoTramiteRepository;
    @MockitoBean private RequisitoRepository requisitoRepository;
    @MockitoBean private TipoTramiteRequisitoRepository tipoTramiteRequisitoRepository;
    @MockitoBean private DocumentoRepository documentoRepository;
    @MockitoBean private HistorialSolicitudRepository historialSolicitudRepository;
    @MockitoBean private NotificacionRepository notificacionRepository;
    @MockitoBean private InspeccionRepository inspeccionRepository;
    @MockitoBean private LicenciaRepository licenciaRepository;
    @MockitoBean private HistorialLicenciaRepository historialLicenciaRepository;
    @MockitoBean private PagoRepository pagoRepository;
    @MockitoBean private HistorialPagoRepository historialPagoRepository;

    private static DashboardResumenResponse resumenMuestra() {
        return new DashboardResumenResponse(
                new MetricasCiudadanos(50),
                new MetricasEstablecimientos(35),
                new MetricasSolicitudes(25, 11, 1, 9, 2),
                new MetricasLicencias(12, 9, 1),
                new MetricasInspecciones(3, 8),
                new MetricasPagos(4, 12, new BigDecimal("2450.00")));
    }

    @Test
    void dashboardResumenPermiteAdministradorSupervisorYFuncionario() throws Exception {
        when(dashboardService.obtenerResumen()).thenReturn(resumenMuestra());

        for (String rol : List.of("ADMINISTRADOR", "SUPERVISOR", "FUNCIONARIO")) {
            mvc.perform(get("/api/dashboard/resumen").with(user("personal").roles(rol)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ciudadanos.total").value(50))
                    .andExpect(jsonPath("$.pagos.montoVerificadoPEN").value(2450.00));
        }
    }

    @Test
    void dashboardDeniegaCiudadano403YSinSesion401() throws Exception {
        mvc.perform(get("/api/dashboard/resumen").with(user("ciudadano").roles("CIUDADANO")))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/dashboard/resumen"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dashboardEndpointsDeEstadisticasYActividadRecienteResponden200() throws Exception {
        var estSol = new EstadisticaSolicitudResponse(10, Map.of("APROBADA", 6L), List.of(), List.of(), null, null);
        var estLic = new EstadisticaLicenciaResponse(8, 7, 1, 0, List.of(), List.of(), null, null);
        var estInsp = new EstadisticaInspeccionResponse(2, 0, 7, 0, 6, 1, 0, List.of(), List.of(), null, null);
        var estPag = new EstadisticaPagoResponse(
                12, 10, 2, 0, 0, new BigDecimal("1500.00"), "PEN", List.of(), List.of(), null, null);
        var global = new DashboardEstadisticasResponse(estSol, estLic, estInsp, estPag);

        when(dashboardService.obtenerEstadisticasGenerales(any(), any())).thenReturn(global);
        when(dashboardService.obtenerEstadisticasSolicitudes(any(), any(), any(), any())).thenReturn(estSol);
        when(dashboardService.obtenerEstadisticasLicencias(any(), any())).thenReturn(estLic);
        when(dashboardService.obtenerEstadisticasInspecciones(any(), any())).thenReturn(estInsp);
        when(dashboardService.obtenerEstadisticasPagos(any(), any())).thenReturn(estPag);
        when(dashboardService.obtenerActividadReciente(any())).thenReturn(List.of(
                new ActividadRecienteResponse(1L, LocalDateTime.now(), "EMITIR", "LICENCIAS", "Emitida LIC-01", "admin")));

        var auth = user("supervisor").roles("SUPERVISOR");

        mvc.perform(get("/api/dashboard/estadisticas").with(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solicitudes.total").value(10));

        mvc.perform(get("/api/dashboard/solicitudes/estadisticas").with(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porEstado.APROBADA").value(6));

        mvc.perform(get("/api/dashboard/licencias/estadisticas").with(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVigentes").value(7));

        mvc.perform(get("/api/dashboard/inspecciones/estadisticas").with(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFavorables").value(6));

        mvc.perform(get("/api/dashboard/pagos/estadisticas").with(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoVerificadoPEN").value(1500.00));

        mvc.perform(get("/api/dashboard/actividad-reciente").param("limite", "15").with(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].modulo").value("LICENCIAS"));
    }

    @Test
    void dashboardDevuelve400CuandoFechaDesdeEsMayorQueFechaHasta() throws Exception {
        when(dashboardService.obtenerEstadisticasGenerales(any(), any()))
                .thenThrow(new BusinessException("La fecha inicial no puede ser posterior a la fecha final."));

        mvc.perform(get("/api/dashboard/estadisticas")
                        .param("fechaDesde", "2026-08-20")
                        .param("fechaHasta", "2026-08-01")
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reportesJsonPaginadosParaSeisModulosResponden200() throws Exception {
        when(reporteService.reporteSolicitudes(any(), any())).thenReturn(new ReporteResponse<>(
                "REPORTE DE SOLICITUDES", LocalDateTime.now(), Map.of(),
                new PaginaResponse<>(List.of(new FilaSolicitudReporte(
                        1L, "EXP-2026-000001", LocalDateTime.now(), "Licencia", "Botica",
                        EstadoSolicitud.APROBADA, "Jorge")), 0, 20, 1, 1)));

        when(reporteService.reporteLicencias(any(), any())).thenReturn(new ReporteResponse<>(
                "REPORTE DE LICENCIAS", LocalDateTime.now(), Map.of(),
                new PaginaResponse<>(List.of(new FilaLicenciaReporte(
                        2L, "LIC-2026-000001", "EXP-2026-000001", "Botica", "Farmacia",
                        LocalDate.now(), EstadoLicencia.VIGENTE)), 0, 20, 1, 1)));

        when(reporteService.reporteInspecciones(any(), any())).thenReturn(new ReporteResponse<>(
                "REPORTE DE INSPECCIONES", LocalDateTime.now(), Map.of(),
                new PaginaResponse<>(List.of(new FilaInspeccionReporte(
                        3L, "EXP-2026-000001", "Inspector", LocalDateTime.now(), LocalDateTime.now(),
                        EstadoInspeccion.FINALIZADA, ResultadoInspeccion.FAVORABLE)), 0, 20, 1, 1)));

        when(reporteService.reportePagos(any(), any())).thenReturn(new ReporteResponse<>(
                "REPORTE DE PAGOS", LocalDateTime.now(), Map.of(),
                new PaginaResponse<>(List.of(new FilaPagoReporte(
                        4L, "OP-01", "EXP-2026-000001", "Tasa", new BigDecimal("120.00"), "PEN",
                        LocalDateTime.now(), EstadoPago.VERIFICADO)), 0, 20, 1, 1)));

        when(reporteService.reporteEstablecimientos(any(), any())).thenReturn(new ReporteResponse<>(
                "REPORTE DE ESTABLECIMIENTOS", LocalDateTime.now(), Map.of(),
                new PaginaResponse<>(List.of(new FilaEstablecimientoReporte(
                        5L, "Botica", "Farmacia SAC", "20601234567", "Av. Cáceres", "Farmacia", "ACTIVO")), 0, 20, 1, 1)));

        when(reporteService.reporteCiudadanos(any(), any())).thenReturn(new ReporteResponse<>(
                "REPORTE DE CIUDADANOS", LocalDateTime.now(), Map.of(),
                new PaginaResponse<>(List.of(new FilaCiudadanoReporte(
                        6L, "María Quispe", TipoDocumento.DNI, "****5678", "ACTIVO", LocalDateTime.now())), 0, 20, 1, 1)));

        var auth = user("func").roles("FUNCIONARIO");

        for (String path : List.of(
                "/api/reportes/solicitudes",
                "/api/reportes/licencias",
                "/api/reportes/inspecciones",
                "/api/reportes/pagos",
                "/api/reportes/establecimientos",
                "/api/reportes/ciudadanos")) {
            mvc.perform(get(path).with(auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.datos.totalElementos").value(1));
        }
    }

    @Test
    void exportacionesPdfYCsvDevuelvenContentTypeYAttachmentCorrectos() throws Exception {
        byte[] pdfBytes = "%PDF-1.4-reporte".getBytes(StandardCharsets.ISO_8859_1);
        ArchivoReporte archivoPdf = new ArchivoReporte("reporte.pdf", "application/pdf", pdfBytes);

        when(reporteService.exportarSolicitudesPdf(any())).thenReturn(archivoPdf);
        when(reporteService.exportarLicenciasPdf(any())).thenReturn(archivoPdf);
        when(reporteService.exportarInspeccionesPdf(any())).thenReturn(archivoPdf);
        when(reporteService.exportarPagosPdf(any())).thenReturn(archivoPdf);
        when(reporteService.exportarEstablecimientosPdf(any())).thenReturn(archivoPdf);

        byte[] csvBytes = "ID,Estado\r\n1,ACTIVO\r\n".getBytes(StandardCharsets.UTF_8);
        ArchivoReporte archivoCsv = new ArchivoReporte("reporte.csv", "text/csv;charset=UTF-8", csvBytes);

        when(reporteService.exportarSolicitudesCsv(any())).thenReturn(archivoCsv);
        when(reporteService.exportarLicenciasCsv(any())).thenReturn(archivoCsv);
        when(reporteService.exportarInspeccionesCsv(any())).thenReturn(archivoCsv);
        when(reporteService.exportarPagosCsv(any())).thenReturn(archivoCsv);
        when(reporteService.exportarEstablecimientosCsv(any())).thenReturn(archivoCsv);

        var auth = user("admin").roles("ADMINISTRADOR");

        for (String modulo : List.of("solicitudes", "licencias", "inspecciones", "pagos", "establecimientos")) {
            mvc.perform(get("/api/reportes/" + modulo + "/pdf").with(auth))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", Matchers.containsString("application/pdf")))
                    .andExpect(header().string("Content-Disposition", Matchers.containsString("attachment")))
                    .andExpect(content().bytes(pdfBytes));

            mvc.perform(get("/api/reportes/" + modulo + "/csv").with(auth))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", Matchers.containsString("text/csv")))
                    .andExpect(header().string("Content-Disposition", Matchers.containsString("attachment")))
                    .andExpect(content().bytes(csvBytes));
        }
    }

    @Test
    void reportesDeniegaCiudadano403YSinSesion401() throws Exception {
        mvc.perform(get("/api/reportes/solicitudes").with(user("ciudadano").roles("CIUDADANO")))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/reportes/solicitudes/pdf").with(user("ciudadano").roles("CIUDADANO")))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/reportes/solicitudes/csv").with(user("ciudadano").roles("CIUDADANO")))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/reportes/solicitudes"))
                .andExpect(status().isUnauthorized());
    }
}
