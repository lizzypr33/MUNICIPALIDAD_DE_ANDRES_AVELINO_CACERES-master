package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.HistorialLicenciaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.InspeccionDetalleResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.InspeccionResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.LicenciaDetalleResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.LicenciaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.VerificacionLicenciaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoInspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoLicencia;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.ResultadoInspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ResourceNotFoundException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.AuditoriaRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.CiudadanoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.DocumentoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.EstablecimientoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.HistorialLicenciaRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.HistorialSolicitudRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.InspeccionRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.LicenciaRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.NotificacionRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.RepresentanteRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.RequisitoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.RolRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.SolicitudRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.TipoTramiteRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.TipoTramiteRequisitoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.UsuarioRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.CertificadoService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.CertificadoService.CertificadoDescarga;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.InspeccionService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.LicenciaService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.VerificacionLicenciaService;

/**
 * Pruebas MockMvc de InspeccionController, LicenciaController y VerificacionLicenciaController:
 * seguridad por roles, CSRF, descarga de certificado PDF y verificación pública por QR.
 */
@SpringBootTest(properties = "spring.autoconfigure.exclude="
        + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
        + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
        + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,"
        + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration")
@AutoConfigureMockMvc
class LicenciaInspeccionControllerTests {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    private MockMvc mvc;

    @MockitoBean private InspeccionService inspeccionService;
    @MockitoBean private LicenciaService licenciaService;
    @MockitoBean private CertificadoService certificadoService;
    @MockitoBean private VerificacionLicenciaService verificacionLicenciaService;

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
    @MockitoBean private pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.PagoRepository pagoRepository;
    @MockitoBean private pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.HistorialPagoRepository historialPagoRepository;

    private static InspeccionDetalleResponse inspeccionDetalle(EstadoInspeccion estado, ResultadoInspeccion resultado) {
        return new InspeccionDetalleResponse(
                15L, 100L, "EXP-2026-000015", 10L, "Carlos Mendoza",
                20L, "Botica San Andrés", "Av. Mariscal Cáceres 450",
                5L, "Inspector Municipal", LocalDateTime.now().plusDays(1),
                resultado != null ? LocalDateTime.now() : null,
                estado, resultado, "Observaciones técnicas", "Recomendaciones", null,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private static LicenciaDetalleResponse licenciaDetalle(EstadoLicencia estado) {
        return new LicenciaDetalleResponse(
                70L, "LIC-2026-000001", "a1b2c3d4e5f60718293a4b5c6d7e8f9012345678",
                "/api/public/licencias/verificar/a1b2c3d4e5f60718293a4b5c6d7e8f9012345678",
                100L, "EXP-2026-000015", 10L, "Carlos Mendoza", "44556677",
                20L, "Botica San Andrés", "Inversiones San Andrés SAC", "20601234561",
                "Av. Mariscal Cáceres 450", "Botica y Farmacia", new BigDecimal("68.50"),
                30L, "LIC-FUNC-IND", "Licencia Indeterminada",
                LocalDate.now(), null, estado, "Observaciones",
                1L, "Administrador Municipal", List.of());
    }

    @Test
    void endpointsDeInspeccionesCumplenPermisosYTransiciones() throws Exception {
        when(inspeccionService.programar(any()))
                .thenReturn(inspeccionDetalle(EstadoInspeccion.PROGRAMADA, null));
        when(inspeccionService.reprogramar(eq(15L), any()))
                .thenReturn(inspeccionDetalle(EstadoInspeccion.PROGRAMADA, null));
        when(inspeccionService.iniciar(15L))
                .thenReturn(inspeccionDetalle(EstadoInspeccion.EN_PROCESO, null));
        when(inspeccionService.registrarResultado(eq(15L), any()))
                .thenReturn(inspeccionDetalle(EstadoInspeccion.FINALIZADA, ResultadoInspeccion.FAVORABLE));
        when(inspeccionService.listar(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PaginaResponse<>(List.of(new InspeccionResponse(
                        15L, 100L, "EXP-2026-000015", "Botica San Andrés",
                        5L, "Inspector Municipal", LocalDateTime.now(), null,
                        EstadoInspeccion.PROGRAMADA, null)), 0, 20, 1L, 1));

        String jsonProgramar = """
                {"solicitudId":100,"inspectorId":5,"fechaProgramada":"2026-11-20T10:00:00","observaciones":"Visita"}
                """;

        // Sin CSRF -> 403
        mvc.perform(post("/api/inspecciones")
                .with(user("func").roles("FUNCIONARIO"))
                .contentType(JSON)
                .content(jsonProgramar))
                .andExpect(status().isForbidden());

        // Ciudadano NO puede programar ni registrar resultados -> 403
        mvc.perform(post("/api/inspecciones")
                .with(user("ciud").roles("CIUDADANO"))
                .with(csrf())
                .contentType(JSON)
                .content(jsonProgramar))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/inspecciones/15/resultado")
                .with(user("ciud").roles("CIUDADANO"))
                .with(csrf())
                .contentType(JSON)
                .content("""
                        {"resultado":"FAVORABLE","observaciones":"Intento ciudadano"}
                        """))
                .andExpect(status().isForbidden());

        // Funcionario programa -> 201
        mvc.perform(post("/api/inspecciones")
                .with(user("func").roles("FUNCIONARIO"))
                .with(csrf())
                .contentType(JSON)
                .content(jsonProgramar))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PROGRAMADA"));

        // Reprogramar -> 200
        mvc.perform(put("/api/inspecciones/15/reprogramar")
                .with(user("func").roles("FUNCIONARIO"))
                .with(csrf())
                .contentType(JSON)
                .content("""
                        {"fechaProgramada":"2026-11-22T15:00:00","inspectorId":5,"motivo":"Coordinación municipal"}
                        """))
                .andExpect(status().isOk());

        // Iniciar -> 200
        mvc.perform(patch("/api/inspecciones/15/iniciar")
                .with(user("func").roles("FUNCIONARIO"))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_PROCESO"));

        // Registrar resultado -> 200
        mvc.perform(patch("/api/inspecciones/15/resultado")
                .with(user("func").roles("FUNCIONARIO"))
                .with(csrf())
                .contentType(JSON)
                .content("""
                        {"resultado":"FAVORABLE","observaciones":"Cumple normas de seguridad","recomendaciones":"Ninguna"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADA"))
                .andExpect(jsonPath("$.resultado").value("FAVORABLE"));
    }

    @Test
    void endpointsDeLicenciasYCertificadoPdfCumplenMatrizDeRoles() throws Exception {
        when(licenciaService.emitir(any())).thenReturn(licenciaDetalle(EstadoLicencia.VIGENTE));
        when(licenciaService.obtener(70L)).thenReturn(licenciaDetalle(EstadoLicencia.VIGENTE));
        when(licenciaService.obtenerPorCodigo("LIC-2026-000001")).thenReturn(licenciaDetalle(EstadoLicencia.VIGENTE));
        when(licenciaService.suspender(eq(70L), any())).thenReturn(licenciaDetalle(EstadoLicencia.SUSPENDIDA));
        when(licenciaService.reactivar(eq(70L), any())).thenReturn(licenciaDetalle(EstadoLicencia.VIGENTE));
        when(licenciaService.revocar(eq(70L), any())).thenReturn(licenciaDetalle(EstadoLicencia.REVOCADA));
        when(licenciaService.listarHistorial(70L)).thenReturn(List.of(
                new HistorialLicenciaResponse(1L, 70L, null, EstadoLicencia.VIGENTE,
                        "Emisión inicial", 1L, "Administrador Municipal", LocalDateTime.now())));

        byte[] pdfBytes = "%PDF-1.4 Certificado Municipal".getBytes(StandardCharsets.UTF_8);
        when(certificadoService.generarCertificadoLicencia(70L))
                .thenReturn(new CertificadoDescarga("certificado-LIC-2026-000001.pdf", "application/pdf", pdfBytes));

        // Ciudadano NO puede emitir, suspender ni revocar licencias -> 403
        mvc.perform(post("/api/licencias/emitir")
                .with(user("ciud").roles("CIUDADANO"))
                .with(csrf())
                .contentType(JSON)
                .content("""
                        {"solicitudId":100,"observaciones":"Intento ciudadano"}
                        """))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/licencias/70/suspender")
                .with(user("ciud").roles("CIUDADANO"))
                .with(csrf())
                .contentType(JSON)
                .content("""
                        {"motivo":"Intento ciudadano"}
                        """))
                .andExpect(status().isForbidden());

        // Funcionario NO puede revocar ni suspender (reservado a ADMINISTRADOR / SUPERVISOR) -> 403
        mvc.perform(patch("/api/licencias/70/revocar")
                .with(user("func").roles("FUNCIONARIO"))
                .with(csrf())
                .contentType(JSON)
                .content("""
                        {"motivo":"Intento funcionario"}
                        """))
                .andExpect(status().isForbidden());

        // Supervisor o Administrador emite -> 201
        mvc.perform(post("/api/licencias/emitir")
                .with(user("sup").roles("SUPERVISOR"))
                .with(csrf())
                .contentType(JSON)
                .content("""
                        {"solicitudId":100,"observaciones":"Emisión autorizada"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoLicencia").value("LIC-2026-000001"))
                .andExpect(jsonPath("$.estado").value("VIGENTE"));

        // Suspender, reactivar y revocar con rol SUPERVISOR / ADMINISTRADOR -> 200
        mvc.perform(patch("/api/licencias/70/suspender")
                .with(user("sup").roles("SUPERVISOR"))
                .with(csrf())
                .contentType(JSON)
                .content("""
                        {"motivo":"Suspensión preventiva por clausura temporal"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("SUSPENDIDA"));

        mvc.perform(patch("/api/licencias/70/reactivar")
                .with(user("admin").roles("ADMINISTRADOR"))
                .with(csrf())
                .contentType(JSON)
                .content("""
                        {"motivo":"Levantamiento de observaciones"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VIGENTE"));

        mvc.perform(patch("/api/licencias/70/revocar")
                .with(user("admin").roles("ADMINISTRADOR"))
                .with(csrf())
                .contentType(JSON)
                .content("""
                        {"motivo":"Resolución de Alcaldía N° 099-2026"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("REVOCADA"));

        // Descarga de certificado PDF -> 200 con cabeceras seguras
        mvc.perform(get("/api/licencias/70/certificado")
                .with(user("ciud").roles("CIUDADANO")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        Matchers.containsString("certificado-LIC-2026-000001.pdf")));
    }

    @Test
    void endpointPublicoDeVerificacionFuncionaSinAutenticacionYNoExponeDatosSensibles() throws Exception {
        String tokenValido = "a1b2c3d4e5f60718293a4b5c6d7e8f9012345678";
        when(verificacionLicenciaService.verificarPorToken(tokenValido))
                .thenReturn(new VerificacionLicenciaResponse(
                        true, true, "LIC-2026-000001", EstadoLicencia.VIGENTE,
                        "Botica San Andrés", "Av. Mariscal Cáceres 450", "Botica y Farmacia",
                        LocalDate.now(), null, "INDETERMINADA",
                        "Licencia de funcionamiento auténtica y VIGENTE.", LocalDateTime.now()));
        when(verificacionLicenciaService.verificarPorToken("tokeninexistente1234567890"))
                .thenThrow(new ResourceNotFoundException("No se encontró ninguna licencia asociada al token."));

        // Acceso público sin sesión
        mvc.perform(get("/api/public/licencias/verificar/" + tokenValido))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autentica").value(true))
                .andExpect(jsonPath("$.vigente").value(true))
                .andExpect(jsonPath("$.codigoLicencia").value("LIC-2026-000001"))
                .andExpect(jsonPath("$.nombreComercial").value("Botica San Andrés"))
                .andExpect(jsonPath("$.numeroDocumentoTitular").doesNotExist())
                .andExpect(jsonPath("$.dni").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.telefono").doesNotExist());

        // Token inexistente -> 404
        mvc.perform(get("/api/public/licencias/verificar/tokeninexistente1234567890"))
                .andExpect(status().isNotFound());
    }
}
