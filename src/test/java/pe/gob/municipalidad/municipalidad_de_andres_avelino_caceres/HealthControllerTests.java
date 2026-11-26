package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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

/**
 * Pruebas SIN base de datos: JPA queda excluido y los repositorios se simulan.
 * La seguridad real (sesiones, CSRF, 401/403) se prueba en SeguridadTests con PostgreSQL.
 */
@SpringBootTest(properties = "spring.autoconfigure.exclude="
        + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
        + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
        + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,"
        + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration")
@AutoConfigureMockMvc
class HealthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private RolRepository rolRepository;

    @MockitoBean
    private AuditoriaRepository auditoriaRepository;

    @MockitoBean
    private CiudadanoRepository ciudadanoRepository;

    @MockitoBean
    private RepresentanteRepository representanteRepository;

    @MockitoBean
    private EstablecimientoRepository establecimientoRepository;

    @MockitoBean
    private SolicitudRepository solicitudRepository;

    @MockitoBean
    private TipoTramiteRepository tipoTramiteRepository;

    @MockitoBean
    private RequisitoRepository requisitoRepository;

    @MockitoBean
    private TipoTramiteRequisitoRepository tipoTramiteRequisitoRepository;

    @MockitoBean
    private DocumentoRepository documentoRepository;

    @MockitoBean
    private HistorialSolicitudRepository historialSolicitudRepository;

    @MockitoBean
    private NotificacionRepository notificacionRepository;

    @MockitoBean
    private InspeccionRepository inspeccionRepository;

    @MockitoBean
    private LicenciaRepository licenciaRepository;

    @MockitoBean
    private HistorialLicenciaRepository historialLicenciaRepository;

    @MockitoBean
    private pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.PagoRepository pagoRepository;

    @MockitoBean
    private pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.HistorialPagoRepository historialPagoRepository;

    @Test
    void healthEsPublicoYDevuelveUp() throws Exception {
        mockMvc.perform(get("/api/public/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("MUNICIPALIDAD_DE_ANDRES_AVELINO_CACERES"));
    }

    @Test
    void otrosEndpointsRequierenAutenticacion() throws Exception {
        mockMvc.perform(get("/api/privado")).andExpect(status().isUnauthorized());
    }
}
