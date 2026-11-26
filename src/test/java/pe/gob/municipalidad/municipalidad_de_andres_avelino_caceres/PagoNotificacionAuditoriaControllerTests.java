package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.AuditoriaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.NotificacionResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PagoDetalleResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PagoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ResumenPagosResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoPago;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoNotificacion;
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
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.AuditoriaService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.ConstanciaPagoService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.ConstanciaPagoService.ConstanciaPagoDescarga;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.NotificacionService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.PagoService;

/**
 * Pruebas MockMvc de PagoController, NotificacionController y AuditoriaController:
 * seguridad por roles, protección CSRF, descarga de constancia PDF y consulta de auditoría.
 */
@SpringBootTest(properties = "spring.autoconfigure.exclude="
        + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
        + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
        + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,"
        + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration")
@AutoConfigureMockMvc
class PagoNotificacionAuditoriaControllerTests {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    private MockMvc mvc;

    @MockitoBean private PagoService pagoService;
    @MockitoBean private ConstanciaPagoService constanciaPagoService;
    @MockitoBean private NotificacionService notificacionService;
    @MockitoBean private AuditoriaService auditoriaService;

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

    private static PagoDetalleResponse pagoDetalle(EstadoPago estado) {
        return new PagoDetalleResponse(
                500L, 100L, "EXP-2026-000015", 10L, "Carlos Mendoza",
                20L, "Botica San Andrés", "OP-2026-00991",
                "Derecho de trámite de Licencia de Funcionamiento",
                new BigDecimal("125.50"), "PEN", LocalDateTime.now().minusHours(2),
                "CAJA_MUNICIPAL", estado, "Recibo de caja", null,
                estado == EstadoPago.VERIFICADO ? 6L : null,
                estado == EstadoPago.VERIFICADO ? "Funcionario Verificador" : null,
                estado == EstadoPago.VERIFICADO ? LocalDateTime.now() : null,
                LocalDateTime.now().minusHours(2),
                List.of());
    }

    private static PagoResponse pagoResumen(EstadoPago estado) {
        return new PagoResponse(
                500L, 100L, "EXP-2026-000015", 10L, "Carlos Mendoza",
                "OP-2026-00991", "Derecho de trámite de Licencia de Funcionamiento",
                new BigDecimal("125.50"), "PEN", LocalDateTime.now().minusHours(2),
                "CAJA_MUNICIPAL", estado, null, null, null);
    }

    @Test
    void endpointsProtegidosDevuelven401SinSesionY403SinCsrf() throws Exception {
        mvc.perform(get("/api/pagos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/notificaciones")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auditorias")).andExpect(status().isUnauthorized());

        String payload = """
                {
                  "solicitudId": 100,
                  "codigoOperacion": "OP-2026-00991",
                  "concepto": "Derecho de trámite",
                  "monto": 125.50,
                  "moneda": "PEN",
                  "fechaPago": "2026-06-10T10:30:00",
                  "medioPago": "CAJA_MUNICIPAL"
                }
                """;

        // POST sin CSRF -> 403
        mvc.perform(post("/api/pagos")
                        .with(user("ciudadano1").roles("CIUDADANO"))
                        .contentType(JSON)
                        .content(payload))
                .andExpect(status().isForbidden());

        // PATCH sin CSRF -> 403
        mvc.perform(patch("/api/pagos/500/verificar")
                        .with(user("funcionario1").roles("FUNCIONARIO"))
                        .contentType(JSON)
                        .content("{\"observaciones\":\"OK\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/notificaciones/10/leer")
                        .with(user("ciudadano1").roles("CIUDADANO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void registrarConsultarVerificarRechazarYAnularPagosSegunPermisosDeRol() throws Exception {
        when(pagoService.registrar(any())).thenReturn(pagoDetalle(EstadoPago.PENDIENTE_VERIFICACION));
        when(pagoService.obtener(500L)).thenReturn(pagoDetalle(EstadoPago.PENDIENTE_VERIFICACION));
        when(pagoService.listar(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PaginaResponse<>(List.of(pagoResumen(EstadoPago.PENDIENTE_VERIFICACION)), 0, 20, 1L, 1));
        when(pagoService.listarPorSolicitud(100L)).thenReturn(List.of(pagoResumen(EstadoPago.PENDIENTE_VERIFICACION)));
        when(pagoService.verificar(eq(500L), any())).thenReturn(pagoDetalle(EstadoPago.VERIFICADO));
        when(pagoService.rechazar(eq(500L), any())).thenReturn(pagoDetalle(EstadoPago.RECHAZADO));
        when(pagoService.anular(eq(500L), any())).thenReturn(pagoDetalle(EstadoPago.ANULADO));
        when(pagoService.obtenerResumen(any(), any(), any(), any(), any()))
                .thenReturn(new ResumenPagosResponse(2L, 1L, new BigDecimal("125.50"), "PEN",
                        1L, 0L, 0L, LocalDate.now().minusDays(7), LocalDate.now()));

        String payloadRegistro = """
                {
                  "solicitudId": 100,
                  "codigoOperacion": "OP-2026-00991",
                  "concepto": "Derecho de trámite de Licencia de Funcionamiento",
                  "monto": 125.50,
                  "moneda": "PEN",
                  "fechaPago": "2026-06-10T10:30:00",
                  "medioPago": "CAJA_MUNICIPAL"
                }
                """;

        // CIUDADANO puede registrar su pago
        mvc.perform(post("/api/pagos")
                        .with(user("ciudadano1").roles("CIUDADANO"))
                        .with(csrf())
                        .contentType(JSON)
                        .content(payloadRegistro))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(500))
                .andExpect(jsonPath("$.estado").value("PENDIENTE_VERIFICACION"));

        // CIUDADANO NO puede verificar, rechazar, anular ni consultar resumen general
        mvc.perform(patch("/api/pagos/500/verificar")
                        .with(user("ciudadano1").roles("CIUDADANO"))
                        .with(csrf())
                        .contentType(JSON)
                        .content("{\"observaciones\":\"Intento\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/pagos/500/rechazar")
                        .with(user("ciudadano1").roles("CIUDADANO"))
                        .with(csrf())
                        .contentType(JSON)
                        .content("{\"motivo\":\"Intento\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/pagos/500/anular")
                        .with(user("ciudadano1").roles("CIUDADANO"))
                        .with(csrf())
                        .contentType(JSON)
                        .content("{\"motivo\":\"Intento\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/pagos/resumen")
                        .with(user("ciudadano1").roles("CIUDADANO")))
                .andExpect(status().isForbidden());

        // FUNCIONARIO puede verificar y rechazar pagos
        mvc.perform(patch("/api/pagos/500/verificar")
                        .with(user("funcionario1").roles("FUNCIONARIO"))
                        .with(csrf())
                        .contentType(JSON)
                        .content("{\"observaciones\":\"Voucher conforme\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VERIFICADO"));

        // Rechazar sin motivo falla con 400 Bad Request por Bean Validation
        mvc.perform(patch("/api/pagos/500/rechazar")
                        .with(user("funcionario1").roles("FUNCIONARIO"))
                        .with(csrf())
                        .contentType(JSON)
                        .content("{\"motivo\":\"   \"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(patch("/api/pagos/500/rechazar")
                        .with(user("funcionario1").roles("FUNCIONARIO"))
                        .with(csrf())
                        .contentType(JSON)
                        .content("{\"motivo\":\"Operación inexistente\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADO"));

        // SUPERVISOR puede anular y consultar resumen
        mvc.perform(patch("/api/pagos/500/anular")
                        .with(user("supervisor1").roles("SUPERVISOR"))
                        .with(csrf())
                        .contentType(JSON)
                        .content("{\"motivo\":\"Duplicado administrativo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADO"));

        mvc.perform(get("/api/pagos/resumen")
                        .with(user("supervisor1").roles("SUPERVISOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadTotalPagos").value(2))
                .andExpect(jsonPath("$.montoTotalVerificado").value(125.50));
    }

    @Test
    void descargarConstanciaPdfDevuelveContentTypeApplicationPdf() throws Exception {
        byte[] pdfSimulado = "%PDF-1.4 Constancia Interna de Pago Municipal".getBytes(StandardCharsets.UTF_8);
        when(constanciaPagoService.generarConstanciaPago(500L))
                .thenReturn(new ConstanciaPagoDescarga("constancia-pago-500.pdf", "application/pdf", pdfSimulado));

        mvc.perform(get("/api/pagos/500/constancia")
                        .with(user("ciudadano1").roles("CIUDADANO")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", Matchers.containsString("constancia-pago-500.pdf")))
                .andExpect(content().bytes(pdfSimulado));
    }

    @Test
    void endpointsDeNotificacionesPermitenConsultarContarYMarcarComoLeidas() throws Exception {
        NotificacionResponse n = new NotificacionResponse(
                10L, 50L, "Pago verificado",
                "Su pago OP-2026-00991 fue verificado.", TipoNotificacion.PAGO,
                false, LocalDateTime.now(), null);
        NotificacionResponse nLeida = new NotificacionResponse(
                10L, 50L, "Pago verificado",
                "Su pago OP-2026-00991 fue verificado.", TipoNotificacion.PAGO,
                true, LocalDateTime.now().minusMinutes(5), LocalDateTime.now());

        when(notificacionService.listarMisNotificaciones(any()))
                .thenReturn(new PaginaResponse<>(List.of(n), 0, 20, 1L, 1));
        when(notificacionService.listarMisNoLeidas(any()))
                .thenReturn(new PaginaResponse<>(List.of(n), 0, 20, 1L, 1));
        when(notificacionService.contarMisNoLeidas()).thenReturn(3L);
        when(notificacionService.obtenerMiNotificacion(10L)).thenReturn(n);
        when(notificacionService.marcarComoLeida(10L)).thenReturn(nLeida);
        when(notificacionService.marcarTodasComoLeidas()).thenReturn(3);
        when(notificacionService.obtenerMiNotificacion(99L))
                .thenThrow(new AccessDeniedException("No tiene permisos para acceder o modificar notificaciones de otro usuario."));

        mvc.perform(get("/api/notificaciones")
                        .with(user("ciudadano1").roles("CIUDADANO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].id").value(10));

        mvc.perform(get("/api/notificaciones/no-leidas")
                        .with(user("ciudadano1").roles("CIUDADANO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].leida").value(false));

        mvc.perform(get("/api/notificaciones/contador")
                        .with(user("ciudadano1").roles("CIUDADANO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noLeidas").value(3));

        mvc.perform(patch("/api/notificaciones/10/leer")
                        .with(user("ciudadano1").roles("CIUDADANO"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leida").value(true));

        mvc.perform(patch("/api/notificaciones/leer-todas")
                        .with(user("ciudadano1").roles("CIUDADANO"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actualizadas").value(3));

        // Impedir acceder a notificación de otro usuario -> 403
        mvc.perform(get("/api/notificaciones/99")
                        .with(user("ciudadano1").roles("CIUDADANO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditoriaPermiteConsultaAAdministradorYSupervisorEImpideAFuncionarioYCiudadano() throws Exception {
        AuditoriaResponse a = new AuditoriaResponse(
                77L, 1L, "admin", "Administrador Municipal",
                "VERIFICAR_PAGO", "Pago", 500L,
                "Pago verificado id=500", LocalDateTime.now(), "127.0.0.1");

        when(auditoriaService.listar(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PaginaResponse<>(List.of(a), 0, 20, 1L, 1));
        when(auditoriaService.obtener(77L)).thenReturn(a);

        // ADMINISTRADOR y SUPERVISOR pueden consultar auditoría
        mvc.perform(get("/api/auditorias")
                        .param("accion", "VERIFICAR_PAGO")
                        .param("entidad", "Pago")
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].id").value(77))
                .andExpect(jsonPath("$.contenido[0].accion").value("VERIFICAR_PAGO"));

        mvc.perform(get("/api/auditorias/77")
                        .with(user("supervisor1").roles("SUPERVISOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entidadAfectada").value("Pago"));

        // FUNCIONARIO y CIUDADANO reciben 403 Forbidden
        mvc.perform(get("/api/auditorias")
                        .with(user("funcionario1").roles("FUNCIONARIO")))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/auditorias")
                        .with(user("ciudadano1").roles("CIUDADANO")))
                .andExpect(status().isForbidden());
    }
}
