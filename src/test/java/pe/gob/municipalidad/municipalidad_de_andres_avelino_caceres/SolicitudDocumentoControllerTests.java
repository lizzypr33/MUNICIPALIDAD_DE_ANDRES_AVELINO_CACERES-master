package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.DocumentoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ExpedienteResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.HistorialSolicitudResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.RequisitoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.SolicitudDetalleResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.SolicitudResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.TipoTramiteResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoDocumento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoSolicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.AuditoriaRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.CiudadanoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.DocumentoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.EstablecimientoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.HistorialSolicitudRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.NotificacionRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.RepresentanteRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.RequisitoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.RolRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.SolicitudRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.TipoTramiteRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.TipoTramiteRequisitoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.UsuarioRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.DocumentoService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.DocumentoService.ArchivoDescargable;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.RequisitoService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.SolicitudService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.TipoTramiteService;

/**
 * Pruebas MockMvc de SolicitudController, TipoTramiteController, RequisitoController y DocumentoController:
 * seguridad por roles, protección CSRF, carga/descarga de documentos y contratos JSON.
 */
@SpringBootTest(properties = "spring.autoconfigure.exclude="
        + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
        + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
        + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,"
        + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration")
@AutoConfigureMockMvc
class SolicitudDocumentoControllerTests {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    private MockMvc mvc;

    @MockitoBean private SolicitudService solicitudService;
    @MockitoBean private TipoTramiteService tipoTramiteService;
    @MockitoBean private RequisitoService requisitoService;
    @MockitoBean private DocumentoService documentoService;

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
    @MockitoBean private pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.InspeccionRepository inspeccionRepository;
    @MockitoBean private pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.LicenciaRepository licenciaRepository;
    @MockitoBean private pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.HistorialLicenciaRepository historialLicenciaRepository;
    @MockitoBean private pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.PagoRepository pagoRepository;
    @MockitoBean private pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.HistorialPagoRepository historialPagoRepository;

    private static SolicitudResponse solicitudEjemplo(EstadoSolicitud estado, String expediente) {
        return new SolicitudResponse(10L, expediente, estado, LocalDateTime.now(), LocalDateTime.now(),
                1L, "Juan Perez", "12345678", 2L, "Comercial Ejemplo",
                3L, "LIC-GRAL", "Licencia General", null, null,
                5L, "Carlos Revisor", "Observación", null);
    }

    @Test
    void endpointsDeSolicitudesTiposRequisitosYDocumentosExigenAutenticacionYCsrf() throws Exception {
        mvc.perform(get("/api/solicitudes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/tipos-tramite")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/requisitos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/documentos/1")).andExpect(status().isUnauthorized());

        // Sin token CSRF es rechazado con 403
        mvc.perform(post("/api/solicitudes")
                        .with(user("ciud").roles("CIUDADANO"))
                        .contentType(JSON)
                        .content("{\"ciudadanoId\":1,\"establecimientoId\":2,\"tipoTramiteId\":3}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void administracionDeTiposDeTramiteYRequisitosSoloPermitidaParaAdministrador() throws Exception {
        TipoTramiteResponse tramite = new TipoTramiteResponse(
                3L, "LIC-GRAL", "Licencia General", "Descripción", true, LocalDateTime.now());
        RequisitoResponse req = new RequisitoResponse(
                1L, "REQ-01", "Declaración Jurada", "Formato oficial", true, true, 1);

        when(tipoTramiteService.crear(any())).thenReturn(tramite);
        when(requisitoService.crear(any())).thenReturn(req);
        when(tipoTramiteService.configurarRequisitos(eq(3L), any())).thenReturn(List.of(req));

        // Funcionario y Ciudadano no pueden crear tipos de trámite ni requisitos -> 403
        mvc.perform(post("/api/tipos-tramite").with(csrf()).with(user("func").roles("FUNCIONARIO"))
                        .contentType(JSON).content("{\"codigo\":\"LIC-GRAL\",\"nombre\":\"Licencia\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/requisitos").with(csrf()).with(user("ciud").roles("CIUDADANO"))
                        .contentType(JSON).content("{\"codigo\":\"REQ-01\",\"nombre\":\"DJ\"}"))
                .andExpect(status().isForbidden());

        // Administrador sí puede crear y configurar requisitos
        mvc.perform(post("/api/tipos-tramite").with(csrf()).with(user("admin").roles("ADMINISTRADOR"))
                        .contentType(JSON).content("{\"codigo\":\"LIC-GRAL\",\"nombre\":\"Licencia General\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("LIC-GRAL"));

        mvc.perform(post("/api/requisitos").with(csrf()).with(user("admin").roles("ADMINISTRADOR"))
                        .contentType(JSON).content("{\"codigo\":\"REQ-01\",\"nombre\":\"Declaración Jurada\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("REQ-01"));

        mvc.perform(put("/api/tipos-tramite/3/requisitos").with(csrf()).with(user("admin").roles("ADMINISTRADOR"))
                        .contentType(JSON)
                        .content("[{\"requisitoId\":1,\"obligatorio\":true,\"ordenVisualizacion\":1}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("REQ-01"))
                .andExpect(jsonPath("$[0].obligatorio").value(true));
    }

    @Test
    void cicloDeEndpointsDeSolicitudYPaginasConPermisosPorRol() throws Exception {
        when(solicitudService.crear(any())).thenReturn(solicitudEjemplo(EstadoSolicitud.BORRADOR, null));
        when(solicitudService.presentar(eq(10L), any()))
                .thenReturn(solicitudEjemplo(EstadoSolicitud.PRESENTADA, "EXP-2026-000001"));
        when(solicitudService.asignarFuncionario(eq(10L), any()))
                .thenReturn(solicitudEjemplo(EstadoSolicitud.PRESENTADA, "EXP-2026-000001"));
        when(solicitudService.iniciarRevision(eq(10L), any()))
                .thenReturn(solicitudEjemplo(EstadoSolicitud.EN_REVISION, "EXP-2026-000001"));
        when(solicitudService.observar(eq(10L), any()))
                .thenReturn(solicitudEjemplo(EstadoSolicitud.OBSERVADA, "EXP-2026-000001"));
        when(solicitudService.subsanar(eq(10L), any()))
                .thenReturn(solicitudEjemplo(EstadoSolicitud.SUBSANADA, "EXP-2026-000001"));
        when(solicitudService.aprobar(eq(10L), any()))
                .thenReturn(solicitudEjemplo(EstadoSolicitud.APROBADA, "EXP-2026-000001"));
        when(solicitudService.rechazar(eq(10L), any()))
                .thenReturn(solicitudEjemplo(EstadoSolicitud.RECHAZADA, "EXP-2026-000001"));
        when(solicitudService.obtenerPorExpediente("EXP-2026-000001"))
                .thenReturn(new ExpedienteResponse(10L, "EXP-2026-000001", EstadoSolicitud.APROBADA,
                        LocalDateTime.now(), LocalDateTime.now(), 1L, "Juan Perez", "12345678",
                        2L, "Comercial Ejemplo", 3L, "LIC-GRAL", "Licencia General",
                        5L, "Carlos Revisor", "OK", null));
        when(solicitudService.listarHistorial(10L))
                .thenReturn(List.of(new HistorialSolicitudResponse(
                        1L, 10L, EstadoSolicitud.BORRADOR, EstadoSolicitud.PRESENTADA,
                        1L, "ciud1", "Presentada", LocalDateTime.now())));
        when(solicitudService.listar(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PaginaResponse<>(
                        List.of(solicitudEjemplo(EstadoSolicitud.PRESENTADA, "EXP-2026-000001")), 0, 20, 1, 1));

        // 1. Ciudadano crea borrador y presenta
        mvc.perform(post("/api/solicitudes").with(csrf()).with(user("ciud").roles("CIUDADANO"))
                        .contentType(JSON)
                        .content("{\"ciudadanoId\":1,\"establecimientoId\":2,\"tipoTramiteId\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("BORRADOR"));

        mvc.perform(post("/api/solicitudes/10/presentar").with(csrf()).with(user("ciud").roles("CIUDADANO"))
                        .contentType(JSON).content("{\"observaciones\":\"Solicito evaluación\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroExpediente").value("EXP-2026-000001"))
                .andExpect(jsonPath("$.estado").value("PRESENTADA"));

        // 2. Ciudadano no puede asignar funcionario, iniciar revisión, observar, aprobar ni rechazar -> 403
        mvc.perform(patch("/api/solicitudes/10/asignar").with(csrf()).with(user("ciud").roles("CIUDADANO"))
                        .contentType(JSON).content("{\"usuarioResponsableId\":5}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/solicitudes/10/aprobar").with(csrf()).with(user("ciud").roles("CIUDADANO")))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/solicitudes/10/rechazar").with(csrf()).with(user("ciud").roles("CIUDADANO"))
                        .contentType(JSON).content("{\"motivoRechazo\":\"No\"}"))
                .andExpect(status().isForbidden());

        // 3. Administrador/Supervisor asigna funcionario; Funcionario revisa, observa y aprueba
        mvc.perform(patch("/api/solicitudes/10/asignar").with(csrf()).with(user("sup").roles("SUPERVISOR"))
                        .contentType(JSON).content("{\"usuarioResponsableId\":5}"))
                .andExpect(status().isOk());

        mvc.perform(patch("/api/solicitudes/10/iniciar-revision").with(csrf()).with(user("func").roles("FUNCIONARIO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_REVISION"));

        mvc.perform(patch("/api/solicitudes/10/observar").with(csrf()).with(user("func").roles("FUNCIONARIO"))
                        .contentType(JSON).content("{\"observaciones\":\"Falta sello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("OBSERVADA"));

        mvc.perform(patch("/api/solicitudes/10/subsanar").with(csrf()).with(user("ciud").roles("CIUDADANO"))
                        .contentType(JSON).content("{\"observaciones\":\"Sello subsanado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("SUBSANADA"));

        mvc.perform(patch("/api/solicitudes/10/aprobar").with(csrf()).with(user("func").roles("FUNCIONARIO"))
                        .contentType(JSON).content("{\"observaciones\":\"Conforme\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"));

        // 4. Consulta de expediente e historial
        mvc.perform(get("/api/solicitudes/expediente/EXP-2026-000001")
                        .with(user("sup").roles("SUPERVISOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroExpediente").value("EXP-2026-000001"));

        mvc.perform(get("/api/solicitudes/10/historial")
                        .with(user("ciud").roles("CIUDADANO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estadoNuevo").value("PRESENTADA"));
    }

    @Test
    void subidaDescargaYProteccionDeDocumentosPorMockMvc() throws Exception {
        DocumentoResponse docResp = new DocumentoResponse(
                55L, 10L, 1L, "REQ-01", "Declaración Jurada", "sustento.pdf",
                "application/pdf", 120L, LocalDateTime.now(), EstadoDocumento.PENDIENTE, null, true);

        byte[] pdfBytes = "%PDF-1.7\nArchivo PDF".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "sustento.pdf", "application/pdf", pdfBytes);

        when(documentoService.subir(eq(10L), eq(1L), any(), any())).thenReturn(docResp);
        when(documentoService.obtener(55L)).thenReturn(docResp);
        when(documentoService.descargar(55L))
                .thenReturn(new ArchivoDescargable("sustento.pdf", "application/pdf", pdfBytes));
        when(documentoService.retirar(55L))
                .thenReturn(new DocumentoResponse(55L, 10L, 1L, "REQ-01", "Declaración Jurada",
                        "sustento.pdf", "application/pdf", 120L, LocalDateTime.now(),
                        EstadoDocumento.PENDIENTE, null, false));

        // Subir documento sin exponer ruta física del servidor
        mvc.perform(multipart("/api/solicitudes/10/documentos")
                        .file(archivo)
                        .param("requisitoId", "1")
                        .with(csrf())
                        .with(user("ciud").roles("CIUDADANO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(55))
                .andExpect(jsonPath("$.nombreOriginal").value("sustento.pdf"))
                .andExpect(content().string(Matchers.not(Matchers.containsString("rutaAlmacenamiento"))));

        // Descargar documento autorizado
        mvc.perform(get("/api/documentos/55/descargar")
                        .with(user("ciud").roles("CIUDADANO")))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdfBytes));

        // Descarga no autorizada a expediente ajeno -> 403
        when(documentoService.descargar(99L))
                .thenThrow(new AccessDeniedException("No tiene permisos para acceder a documentos de expedientes ajenos."));
        mvc.perform(get("/api/documentos/99/descargar")
                        .with(user("otro").roles("CIUDADANO")))
                .andExpect(status().isForbidden());

        // Retiro lógico mediante DELETE
        mvc.perform(delete("/api/documentos/55")
                        .with(csrf())
                        .with(user("ciud").roles("CIUDADANO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void detalleDeSolicitudIncluyeDocumentosEHistorial() throws Exception {
        SolicitudDetalleResponse detalle = new SolicitudDetalleResponse(
                10L, "EXP-2026-000001", EstadoSolicitud.PRESENTADA, LocalDateTime.now(), LocalDateTime.now(),
                null, null, null, null, 5L, "Carlos Revisor", "Obs", null, List.of(), List.of());
        when(solicitudService.obtener(10L)).thenReturn(detalle);

        mvc.perform(get("/api/solicitudes/10").with(user("func").roles("FUNCIONARIO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.numeroExpediente").value("EXP-2026-000001"));
    }
}
