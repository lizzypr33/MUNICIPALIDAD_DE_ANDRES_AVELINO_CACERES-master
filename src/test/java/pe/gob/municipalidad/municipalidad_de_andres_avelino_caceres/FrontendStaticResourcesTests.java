package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.AuthenticatedUserResponse;
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
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.AuthService;

/**
 * Pruebas MockMvc de integración del Frontend Base (PROMPT 09):
 * disponibilidad de páginas HTML5, hojas de estilo CSS3, módulos JavaScript ES6,
 * emisión de token CSRF, consulta de perfil autenticado y protección de endpoints privados.
 */
@SpringBootTest(properties = "spring.autoconfigure.exclude="
        + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
        + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
        + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,"
        + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration")
@AutoConfigureMockMvc
class FrontendStaticResourcesTests {

    @Autowired
    private MockMvc mvc;

    @MockitoBean private AuthService authService;

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

    @Test
    void paginasHtmlPrincipalesSeSirvenCorrectamenteConIdentidadInstitucional() throws Exception {
        String indexHtml = new String(
                mvc.perform(get("/index.html")).andExpect(status().isOk()).andReturn()
                        .getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8);
        assertThat(indexHtml).contains("MUNICIPALIDAD DISTRITAL DE ANDRÉS AVELINO CÁCERES DORREGARAY");

        String loginHtml = new String(
                mvc.perform(get("/login.html")).andExpect(status().isOk()).andReturn()
                        .getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8);
        assertThat(loginHtml)
                .contains("MUNICIPALIDAD DISTRITAL DE ANDRÉS AVELINO CÁCERES DORREGARAY")
                .contains("Sistema de Gestión de Licencias Municipales")
                .contains("Ayacucho - Perú")
                .contains("Iniciar sesión");

        String dashboardHtml = new String(
                mvc.perform(get("/dashboard.html")).andExpect(status().isOk()).andReturn()
                        .getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8);
        assertThat(dashboardHtml)
                .contains("Sistema de Gestión de Licencias Municipales")
                .contains("/assets/css/dashboard.css")
                .contains("header-notif-panel")
                .contains("Cerrar sesión");

        String verificarLicenciaHtml = new String(
                mvc.perform(get("/pages/verificar-licencia.html")).andExpect(status().isOk()).andReturn()
                        .getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8);
        assertThat(verificarLicenciaHtml)
                .contains("MUNICIPALIDAD DISTRITAL DE ANDRÉS AVELINO CÁCERES DORREGARAY")
                .contains("VERIFICACIÓN DE LICENCIA DE FUNCIONAMIENTO")
                .contains("Identificador o Token de Verificación (QR)");
    }

    @Test
    void archivosCssInstitucionalesCarganConPaletaDeColoresYEstilos() throws Exception {
        mvc.perform(get("/assets/css/variables.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("--color-primary: #0B4A87")))
                .andExpect(content().string(containsString("--color-primary-dark: #082F57")));

        for (String css : List.of(
                "/assets/css/reset.css",
                "/assets/css/global.css",
                "/assets/css/layout.css",
                "/assets/css/components.css",
                "/assets/css/login.css",
                "/assets/css/dashboard.css",
                "/assets/css/forms.css",
                "/assets/css/tables.css",
                "/assets/css/management.css",
                "/assets/css/solicitudes.css",
                "/assets/css/expediente.css",
                "/assets/css/documentos.css",
                "/assets/css/timeline.css",
                "/assets/css/licencias.css",
                "/assets/css/inspecciones.css",
                "/assets/css/pagos.css",
                "/assets/css/reportes.css",
                "/assets/css/notificaciones.css",
                "/assets/css/auditoria.css",
                "/assets/css/responsive.css")) {
            mvc.perform(get(css)).andExpect(status().isOk());
        }

        mvc.perform(get("/assets/images/escudo-mdaacd.png"))
                .andExpect(status().isOk());
    }

    @Test
    void modulosJavaScriptEs6CarganCorrectamente() throws Exception {
        for (String js : List.of(
                "/assets/js/config.js",
                "/assets/js/api.js",
                "/assets/js/auth.js",
                "/assets/js/router.js",
                "/assets/js/layout.js",
                "/assets/js/dom.js",
                "/assets/js/alerts.js",
                "/assets/js/ui.js",
                "/assets/js/main.js",
                "/assets/js/dashboard/dashboard-api.js",
                "/assets/js/dashboard/dashboard-cards.js",
                "/assets/js/dashboard/dashboard-charts.js",
                "/assets/js/dashboard/dashboard-activity.js",
                "/assets/js/dashboard/dashboard.js",
                "/assets/js/components/data-table.js",
                "/assets/js/components/modal.js",
                "/assets/js/components/pagination.js",
                "/assets/js/components/form-validation.js",
                "/assets/js/components/confirm-dialog.js",
                "/assets/js/usuarios/usuarios-api.js",
                "/assets/js/usuarios/usuarios.js",
                "/assets/js/ciudadanos/ciudadanos-api.js",
                "/assets/js/ciudadanos/ciudadanos.js",
                "/assets/js/representantes/representantes-api.js",
                "/assets/js/representantes/representantes.js",
                "/assets/js/establecimientos/establecimientos-api.js",
                "/assets/js/establecimientos/establecimientos.js",
                "/assets/js/tramites/tramites-api.js",
                "/assets/js/tramites/tramites.js",
                "/assets/js/requisitos/requisitos-api.js",
                "/assets/js/requisitos/requisitos.js",
                "/assets/js/documentos/documentos-api.js",
                "/assets/js/documentos/upload.js",
                "/assets/js/documentos/documentos.js",
                "/assets/js/solicitudes/solicitudes-api.js",
                "/assets/js/solicitudes/solicitud-estados.js",
                "/assets/js/solicitudes/solicitud-historial.js",
                "/assets/js/solicitudes/solicitud-form.js",
                "/assets/js/solicitudes/solicitud-detalle.js",
                "/assets/js/solicitudes/solicitudes.js",
                "/assets/js/licencias/licencias-api.js",
                "/assets/js/licencias/licencias.js",
                "/assets/js/licencias/licencia-detalle.js",
                "/assets/js/licencias/licencia-emision.js",
                "/assets/js/licencias/licencia-verificacion.js",
                "/assets/js/inspecciones/inspecciones-api.js",
                "/assets/js/inspecciones/inspecciones.js",
                "/assets/js/inspecciones/inspeccion-form.js",
                "/assets/js/inspecciones/inspeccion-detalle.js",
                "/assets/js/pagos/pagos-api.js",
                "/assets/js/pagos/pagos.js",
                "/assets/js/pagos/pago-form.js",
                "/assets/js/pagos/pago-detalle.js",
                "/assets/js/reportes/reportes-api.js",
                "/assets/js/reportes/reportes.js",
                "/assets/js/reportes/reportes-filtros.js",
                "/assets/js/reportes/reportes-graficos.js",
                "/assets/js/reportes/reportes-exportacion.js",
                "/assets/js/notificaciones/notificaciones-api.js",
                "/assets/js/notificaciones/notificaciones.js",
                "/assets/js/notificaciones/notificaciones-detalle.js",
                "/assets/js/auditoria/auditoria-api.js",
                "/assets/js/auditoria/auditoria.js",
                "/assets/js/auditoria/auditoria-detalle.js")) {
            mvc.perform(get(js)).andExpect(status().isOk());
        }
    }

    @Test
    void paginasHtmlPrivadasEstanProtegidasPorSeguridadYRolesEnBackend() throws Exception {
        // Sin sesión autenticada, el backend debe rechazar con 401
        for (String privatePage : List.of(
                "/pages/usuarios.html",
                "/pages/ciudadanos.html",
                "/pages/representantes.html",
                "/pages/establecimientos.html",
                "/pages/solicitudes.html",
                "/pages/nueva-solicitud.html",
                "/pages/detalle-solicitud.html",
                "/pages/tipos-tramite.html",
                "/pages/requisitos.html",
                "/pages/licencias.html",
                "/pages/detalle-licencia.html",
                "/pages/emitir-licencia.html",
                "/pages/inspecciones.html",
                "/pages/detalle-inspeccion.html",
                "/pages/pagos.html",
                "/pages/detalle-pago.html",
                "/pages/reportes.html",
                "/pages/reporte-detalle.html",
                "/pages/notificaciones.html",
                "/pages/notificacion-detalle.html",
                "/pages/auditoria.html",
                "/pages/auditoria-detalle.html")) {
            mvc.perform(get(privatePage)).andExpect(status().isUnauthorized());
        }

        var adminAuth = user("admin.municipal").roles("ADMINISTRADOR");
        var funcAuth = user("func.municipal").roles("FUNCIONARIO");
        var ciudAuth = user("ciud.municipal").roles("CIUDADANO");

        // /pages/usuarios.html, /pages/tipos-tramite.html, /pages/requisitos.html: solo ADMINISTRADOR
        mvc.perform(get("/pages/usuarios.html").with(funcAuth))
                .andExpect(status().isForbidden());
        mvc.perform(get("/pages/tipos-tramite.html").with(funcAuth))
                .andExpect(status().isForbidden());
        mvc.perform(get("/pages/requisitos.html").with(ciudAuth))
                .andExpect(status().isForbidden());

        String usuariosHtml = new String(
                mvc.perform(get("/pages/usuarios.html").with(adminAuth))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8);
        assertThat(usuariosHtml).contains("GESTIÓN DE USUARIOS");

        String tramitesHtml = new String(
                mvc.perform(get("/pages/tipos-tramite.html").with(adminAuth))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8);
        assertThat(tramitesHtml).contains("TIPOS DE TRÁMITE MUNICIPAL");

        String requisitosHtml = new String(
                mvc.perform(get("/pages/requisitos.html").with(adminAuth))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8);
        assertThat(requisitosHtml).contains("GESTIÓN DE REQUISITOS");

        // /pages/representantes.html y /pages/emitir-licencia.html: prohibido para CIUDADANO, permitido para FUNCIONARIO/ADMIN
        mvc.perform(get("/pages/representantes.html").with(ciudAuth))
                .andExpect(status().isForbidden());
        mvc.perform(get("/pages/emitir-licencia.html").with(ciudAuth))
                .andExpect(status().isForbidden());

        String emitirLicenciaHtml = new String(
                mvc.perform(get("/pages/emitir-licencia.html").with(funcAuth))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8);
        assertThat(emitirLicenciaHtml).contains("EMISIÓN DE LICENCIA DE FUNCIONAMIENTO");

        String representantesHtml = new String(
                mvc.perform(get("/pages/representantes.html").with(funcAuth))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8);
        assertThat(representantesHtml).contains("GESTIÓN DE REPRESENTANTES");

        // /pages/ciudadanos.html y /pages/establecimientos.html
        String ciudadanosHtml = new String(
                mvc.perform(get("/pages/ciudadanos.html").with(funcAuth))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8);
        assertThat(ciudadanosHtml).contains("GESTIÓN DE CIUDADANOS");

        String establecimientosHtml = new String(
                mvc.perform(get("/pages/establecimientos.html").with(funcAuth))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8);
        assertThat(establecimientosHtml).contains("GESTIÓN DE ESTABLECIMIENTOS");

        // /pages/solicitudes.html, /pages/nueva-solicitud.html, /pages/detalle-solicitud.html
        String solicitudesHtml = new String(
                mvc.perform(get("/pages/solicitudes.html").with(ciudAuth))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8);
        assertThat(solicitudesHtml).contains("GESTIÓN DE SOLICITUDES MUNICIPALES");

        String nuevaSolicitudHtml = new String(
                mvc.perform(get("/pages/nueva-solicitud.html").with(ciudAuth))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8);
        assertThat(nuevaSolicitudHtml).contains("NUEVA SOLICITUD MUNICIPAL");

        String detalleSolicitudHtml = new String(
                mvc.perform(get("/pages/detalle-solicitud.html").with(funcAuth))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8);
        assertThat(detalleSolicitudHtml).contains("DETALLE DEL EXPEDIENTE");

        // /pages/licencias.html, /pages/detalle-licencia.html
        String licenciasHtml = new String(
                mvc.perform(get("/pages/licencias.html").with(ciudAuth))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8);
        assertThat(licenciasHtml).contains("GESTIÓN DE LICENCIAS DE FUNCIONAMIENTO");

        String detalleLicenciaHtml = new String(
                mvc.perform(get("/pages/detalle-licencia.html").with(ciudAuth))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8);
        assertThat(detalleLicenciaHtml).contains("DETALLE DE LICENCIA DE FUNCIONAMIENTO");

        // /pages/inspecciones.html, /pages/detalle-inspeccion.html
        String inspeccionesHtml = new String(
                mvc.perform(get("/pages/inspecciones.html").with(ciudAuth))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8);
        assertThat(inspeccionesHtml).contains("GESTIÓN DE INSPECCIONES MUNICIPALES");

        String detalleInspeccionHtml = new String(
                mvc.perform(get("/pages/detalle-inspeccion.html").with(ciudAuth))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8);
        assertThat(detalleInspeccionHtml).contains("DETALLE DE INSPECCIÓN TÉCNICA");

        // /pages/pagos.html, /pages/detalle-pago.html
        String pagosHtml = new String(
                mvc.perform(get("/pages/pagos.html").with(ciudAuth))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8);
        assertThat(pagosHtml).contains("GESTIÓN DE PAGOS MUNICIPALES");

        String detallePagoHtml = new String(
                mvc.perform(get("/pages/detalle-pago.html").with(ciudAuth))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8);
        assertThat(detallePagoHtml).contains("DETALLE DE PAGO MUNICIPAL");

        // /pages/auditoria.html, /pages/auditoria-detalle.html: solo ADMINISTRADOR y SUPERVISOR
        mvc.perform(get("/pages/auditoria.html").with(ciudAuth))
                .andExpect(status().isForbidden());
        mvc.perform(get("/pages/auditoria.html").with(funcAuth))
                .andExpect(status().isForbidden());
        mvc.perform(get("/pages/auditoria-detalle.html").with(ciudAuth))
                .andExpect(status().isForbidden());

        String auditoriaHtml = new String(
                mvc.perform(get("/pages/auditoria.html").with(adminAuth))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8);
        assertThat(auditoriaHtml).contains("AUDITORÍA DEL SISTEMA");

        String auditoriaDetalleHtml = new String(
                mvc.perform(get("/pages/auditoria-detalle.html").with(adminAuth))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8);
        assertThat(auditoriaDetalleHtml).contains("DETALLE DE AUDITORÍA");

        // /pages/reportes.html, /pages/reporte-detalle.html: prohibido para CIUDADANO, permitido para FUNCIONARIO/ADMIN
        mvc.perform(get("/pages/reportes.html").with(ciudAuth))
                .andExpect(status().isForbidden());
        mvc.perform(get("/pages/reporte-detalle.html").with(ciudAuth))
                .andExpect(status().isForbidden());

        String reportesHtml = new String(
                mvc.perform(get("/pages/reportes.html").with(funcAuth))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8);
        assertThat(reportesHtml).contains("REPORTES MUNICIPALES");

        String reporteDetalleHtml = new String(
                mvc.perform(get("/pages/reporte-detalle.html").with(funcAuth))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8);
        assertThat(reporteDetalleHtml).contains("DETALLE DE REPORTE MUNICIPAL");

        // /pages/notificaciones.html, /pages/notificacion-detalle.html: permitido para todos autenticados (incluso CIUDADANO)
        String notificacionesHtml = new String(
                mvc.perform(get("/pages/notificaciones.html").with(ciudAuth))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8);
        assertThat(notificacionesHtml).contains("CENTRO DE NOTIFICACIONES");

        String notificacionDetalleHtml = new String(
                mvc.perform(get("/pages/notificacion-detalle.html").with(ciudAuth))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8);
        assertThat(notificacionDetalleHtml).contains("DETALLE DE NOTIFICACIÓN");
    }

    @Test
    void csrfEmiteCookieYEndpointsPrivadosSiguenProtegidosCon401() throws Exception {
        mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"));

        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/dashboard/resumen"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meYLogoutFuncionanConSesionAutenticadaYProteccionCsrf() throws Exception {
        when(authService.me()).thenReturn(new AuthenticatedUserResponse(
                1L, "admin.municipal", "Carlos", "Ayala",
                "carlos.ayala@municaceres.gob.pe", List.of("ADMINISTRADOR"), true));

        var auth = user("admin.municipal").roles("ADMINISTRADOR");

        mvc.perform(get("/api/auth/me").with(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin.municipal"))
                .andExpect(jsonPath("$.nombres").value("Carlos"))
                .andExpect(jsonPath("$.roles[0]").value("ADMINISTRADOR"));

        // Logout sin CSRF debe ser rechazado con 403
        mvc.perform(post("/api/auth/logout").with(auth))
                .andExpect(status().isForbidden());

        // Logout con CSRF responde 200 OK
        mvc.perform(post("/api/auth/logout").with(auth).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Sesión cerrada correctamente"));
    }
}
