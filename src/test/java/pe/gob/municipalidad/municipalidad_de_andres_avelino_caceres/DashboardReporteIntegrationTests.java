package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.PresentarSolicitudRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.RegistrarPagoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.SolicitudRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.TipoTramiteRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.VerificarPagoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ActividadRecienteResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.DashboardEstadisticasResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.DashboardResumenResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse.FilaSolicitudReporte;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.SolicitudResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.TipoTramiteResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Ciudadano;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Establecimiento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Usuario;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoDocumento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.CiudadanoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.EstablecimientoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.RolRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.UsuarioRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.security.CustomUserDetails;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.DashboardService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.PagoService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.ReporteService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.ReporteService.ArchivoReporte;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.SolicitudService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.TipoTramiteService;

/**
 * Pruebas de integración contra PostgreSQL real para PROMPT 08 (Flujo F):
 * consulta de resumen ejecutivo, estadísticas, actividad reciente, reportes paginados JSON
 * y exportación de reportes en formato PDF y CSV.
 */
@SpringBootTest
@Transactional
class DashboardReporteIntegrationTests {

    @Autowired private DashboardService dashboardService;
    @Autowired private ReporteService reporteService;
    @Autowired private TipoTramiteService tipoTramiteService;
    @Autowired private SolicitudService solicitudService;
    @Autowired private PagoService pagoService;

    @Autowired private CiudadanoRepository ciudadanos;
    @Autowired private EstablecimientoRepository establecimientos;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private RolRepository roles;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(Usuario u) {
        CustomUserDetails cud = new CustomUserDetails(u);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(cud, null, cud.getAuthorities()));
    }

    @Test
    void flujoFIntegracionDashboardReportesPdfYExportacionCsvEnPostgreSql() {
        String sufijo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Usuario admin = new Usuario();
        admin.setUsername("admin_p08_" + sufijo.toLowerCase());
        admin.setPassword("$2a$10$abcdefghijklmnopqrstuuABCDEFGHIJKLMNOPQRSTUVWXYZ01234");
        admin.setNombres("Admin");
        admin.setApellidos("Dashboard");
        admin.setEmail("admin_p08_" + sufijo.toLowerCase() + "@municaceres.gob.pe");
        admin.setActivo(true);
        admin.setRoles(Set.of(roles.findByNombre("ADMINISTRADOR").orElseThrow()));
        admin = usuarios.save(admin);

        autenticarComo(admin);

        Ciudadano c = new Ciudadano();
        c.setTipoDocumento(TipoDocumento.DNI);
        c.setNumeroDocumento("75" + String.format("%06d", Math.abs(sufijo.hashCode()) % 1_000_000));
        c.setNombres("Lucía");
        c.setApellidos("Mendoza");
        c.setActivo(true);
        c = ciudadanos.save(c);

        Establecimiento est = new Establecimiento();
        est.setCiudadano(c);
        est.setNombreComercial("Librería Escolar " + sufijo);
        est.setRazonSocial("Inversiones Culturales " + sufijo + " S.A.C.");
        est.setRuc("20" + String.format("%09d", Math.abs(sufijo.hashCode()) % 1_000_000_000));
        est.setDireccion("Av. Andrés Avelino Cáceres 305");
        est.setGiroNegocio("Librería y Bazar");
        est.setAreaMetrosCuadrados(new BigDecimal("48.00"));
        est.setActivo(true);
        est = establecimientos.save(est);

        TipoTramiteResponse tt = tipoTramiteService.crear(new TipoTramiteRequest(
                "TT-P08-" + sufijo, "Licencia de Funcionamiento P08 " + sufijo,
                "Trámite de prueba P08", false));

        SolicitudResponse sol = solicitudService.crear(new SolicitudRequest(
                c.getId(), est.getId(), tt.id(), null, "Solicitud para prueba de Dashboard y Reportes"));
        solicitudService.presentar(sol.id(), new PresentarSolicitudRequest("Presentación en ventanilla"));

        var pago = pagoService.registrar(new RegistrarPagoRequest(
                sol.id(), "OP-P08-" + sufijo, "Derecho de trámite",
                new BigDecimal("130.00"), "PEN", LocalDateTime.now().minusMinutes(5),
                "CAJA_MUNICIPAL", "Recibo P08"));
        pagoService.verificar(pago.id(), new VerificarPagoRequest("Verificado para Dashboard"));

        DashboardResumenResponse resumen = dashboardService.obtenerResumen();
        assertThat(resumen.ciudadanos().total()).isGreaterThanOrEqualTo(1L);
        assertThat(resumen.establecimientos().total()).isGreaterThanOrEqualTo(1L);
        assertThat(resumen.solicitudes().total()).isGreaterThanOrEqualTo(1L);
        assertThat(resumen.pagos().verificados()).isGreaterThanOrEqualTo(1L);
        assertThat(resumen.pagos().montoVerificadoPEN()).isGreaterThanOrEqualTo(new BigDecimal("130.00"));

        DashboardEstadisticasResponse estadisticas = dashboardService.obtenerEstadisticasGenerales(
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        assertThat(estadisticas.solicitudes().total()).isGreaterThanOrEqualTo(1L);
        assertThat(estadisticas.pagos().montoVerificadoPEN()).isGreaterThanOrEqualTo(new BigDecimal("130.00"));

        List<ActividadRecienteResponse> actividad = dashboardService.obtenerActividadReciente(10);
        assertThat(actividad).isNotEmpty();

        ReporteResponse<FilaSolicitudReporte> repSol = reporteService.reporteSolicitudes(null, PageRequest.of(0, 10));
        assertThat(repSol.datos().totalElementos()).isGreaterThanOrEqualTo(1L);

        ArchivoReporte pdfSol = reporteService.exportarSolicitudesPdf(null);
        assertThat(new String(pdfSol.contenido(), 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");

        ArchivoReporte csvPagos = reporteService.exportarPagosCsv(null);
        String textoCsv = new String(csvPagos.contenido(), StandardCharsets.UTF_8);
        assertThat(textoCsv).contains("OP-P08-" + sufijo);
    }
}
