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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.CiudadanoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.EstablecimientoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.RepresentanteResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoDocumento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ConflictException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ResourceNotFoundException;
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
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.CiudadanoService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.EstablecimientoService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.RepresentanteService;

/**
 * Pruebas MockMvc de los controladores, validaciones Bean Validation, CSRF, permisos por rol
 * (ADMINISTRADOR, FUNCIONARIO, SUPERVISOR, CIUDADANO) y manejo de errores HTTP.
 */
@SpringBootTest(properties = "spring.autoconfigure.exclude="
        + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
        + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
        + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,"
        + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration")
@AutoConfigureMockMvc
class CiudadanoEstablecimientoControllerTests {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    private MockMvc mvc;

    @MockitoBean private CiudadanoService ciudadanoService;
    @MockitoBean private RepresentanteService representanteService;
    @MockitoBean private EstablecimientoService establecimientoService;

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

    private static CiudadanoResponse ciudadanoEjemplo() {
        return new CiudadanoResponse(1L, TipoDocumento.DNI, "12345678", "Juan", "Perez",
                "juan@example.com", "999999999", "Ayacucho, Peru", true, LocalDateTime.now(), null);
    }

    private static RepresentanteResponse representanteEjemplo() {
        return new RepresentanteResponse(1L, TipoDocumento.DNI, "87654321", "Maria", "Lopez",
                "Apoderada", "988888888", "maria@example.com", true);
    }

    private static EstablecimientoResponse establecimientoEjemplo() {
        return new EstablecimientoResponse(1L, "Comercial Ejemplo", "Comercial Ejemplo SAC", "20100070970",
                "Distrito Andres Avelino Caceres Dorregaray", "Cerca del parque", "Venta de abarrotes",
                new BigDecimal("80.50"), 1, "999999999", "contacto@example.com", true,
                LocalDateTime.now(), 1L, "Juan Perez", "12345678", null, null);
    }

    @Test
    void accesoNoAutenticadoEsRechazadoCon401() throws Exception {
        mvc.perform(get("/api/ciudadanos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/representantes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/establecimientos")).andExpect(status().isUnauthorized());
    }

    @Test
    void administradorYFuncionarioPuedenRegistrarCiudadanoRepresentanteYEstablecimiento() throws Exception {
        when(ciudadanoService.crear(any())).thenReturn(ciudadanoEjemplo());
        when(representanteService.crear(any())).thenReturn(representanteEjemplo());
        when(establecimientoService.crear(any())).thenReturn(establecimientoEjemplo());

        String bodyCiudadano = """
                {
                  "tipoDocumento": "DNI",
                  "numeroDocumento": "12345678",
                  "nombres": "Juan",
                  "apellidos": "Perez",
                  "email": "juan@example.com",
                  "telefono": "999999999",
                  "direccion": "Ayacucho, Peru"
                }
                """;

        mvc.perform(post("/api/ciudadanos").with(csrf())
                        .with(user("admin").roles("ADMINISTRADOR"))
                        .contentType(JSON).content(bodyCiudadano))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.numeroDocumento").value("12345678"));

        mvc.perform(post("/api/ciudadanos").with(csrf())
                        .with(user("func").roles("FUNCIONARIO"))
                        .contentType(JSON).content(bodyCiudadano))
                .andExpect(status().isCreated());

        String bodyRep = """
                {
                  "tipoDocumento": "DNI",
                  "numeroDocumento": "87654321",
                  "nombres": "Maria",
                  "apellidos": "Lopez",
                  "cargo": "Apoderada",
                  "telefono": "988888888",
                  "email": "maria@example.com"
                }
                """;

        mvc.perform(post("/api/representantes").with(csrf())
                        .with(user("func").roles("FUNCIONARIO"))
                        .contentType(JSON).content(bodyRep))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroDocumento").value("87654321"));

        String bodyEst = """
                {
                  "nombreComercial": "Comercial Ejemplo",
                  "razonSocial": "Comercial Ejemplo SAC",
                  "ruc": "20100070970",
                  "direccion": "Distrito Andres Avelino Caceres Dorregaray",
                  "referencia": "Cerca del parque",
                  "giroNegocio": "Venta de abarrotes",
                  "areaMetrosCuadrados": 80.50,
                  "numeroPisos": 1,
                  "telefono": "999999999",
                  "email": "contacto@example.com",
                  "ciudadanoId": 1,
                  "representanteId": null
                }
                """;

        mvc.perform(post("/api/establecimientos").with(csrf())
                        .with(user("func").roles("FUNCIONARIO"))
                        .contentType(JSON).content(bodyEst))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreComercial").value("Comercial Ejemplo"));
    }

    @Test
    void supervisorPuedeConsultarPeroNoCrearNiModificar() throws Exception {
        when(ciudadanoService.listar(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PaginaResponse<>(List.of(ciudadanoEjemplo()), 0, 20, 1, 1));
        when(representanteService.listar(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PaginaResponse<>(List.of(representanteEjemplo()), 0, 20, 1, 1));
        when(establecimientoService.listar(any(), any(), any(), any(), any()))
                .thenReturn(new PaginaResponse<>(List.of(establecimientoEjemplo()), 0, 20, 1, 1));

        mvc.perform(get("/api/ciudadanos").with(user("sup").roles("SUPERVISOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].numeroDocumento").value("12345678"));

        mvc.perform(get("/api/representantes").with(user("sup").roles("SUPERVISOR")))
                .andExpect(status().isOk());

        mvc.perform(get("/api/establecimientos").with(user("sup").roles("SUPERVISOR")))
                .andExpect(status().isOk());

        // Supervisor no puede registrar ni cambiar estado
        mvc.perform(post("/api/ciudadanos").with(csrf()).with(user("sup").roles("SUPERVISOR"))
                        .contentType(JSON).content("{}"))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/establecimientos/1/estado").with(csrf()).with(user("sup").roles("SUPERVISOR"))
                        .contentType(JSON).content("{\"activo\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void desactivacionLogicaSoloPermitidaParaAdministrador() throws Exception {
        CiudadanoResponse inactivo = new CiudadanoResponse(1L, TipoDocumento.DNI, "12345678", "Juan", "Perez",
                null, null, null, false, LocalDateTime.now(), null);
        when(ciudadanoService.cambiarEstado(1L, false)).thenReturn(inactivo);

        // Funcionario no puede desactivar ciudadano
        mvc.perform(patch("/api/ciudadanos/1/estado").with(csrf()).with(user("func").roles("FUNCIONARIO"))
                        .contentType(JSON).content("{\"activo\":false}"))
                .andExpect(status().isForbidden());

        // Administrador sí puede
        mvc.perform(patch("/api/ciudadanos/1/estado").with(csrf()).with(user("admin").roles("ADMINISTRADOR"))
                        .contentType(JSON).content("{\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void ciudadanoNoPuedeListarTodosNiConsultarOtrosCiudadanos() throws Exception {
        mvc.perform(get("/api/ciudadanos").with(user("ciud").roles("CIUDADANO")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/ciudadanos/documento")
                        .param("tipoDocumento", "DNI").param("numeroDocumento", "12345678")
                        .with(user("ciud").roles("CIUDADANO")))
                .andExpect(status().isForbidden());

        when(ciudadanoService.obtener(99L))
                .thenThrow(new AccessDeniedException("No tiene permisos para acceder a los datos de otro ciudadano."));
        mvc.perform(get("/api/ciudadanos/99").with(user("ciud").roles("CIUDADANO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void manejoCorrectoDeErrores400_404_409() throws Exception {
        // 400 Bean Validation (campos vacíos o área negativa)
        mvc.perform(post("/api/establecimientos").with(csrf()).with(user("admin").roles("ADMINISTRADOR"))
                        .contentType(JSON)
                        .content("{\"nombreComercial\":\"\",\"direccion\":\"\",\"giroNegocio\":\"\","
                                + "\"areaMetrosCuadrados\":-5,\"numeroPisos\":0}"))
                .andExpect(status().isBadRequest());

        // 400 Regla de negocio (DNI inválido o RUC inválido)
        when(ciudadanoService.crear(any()))
                .thenThrow(new BusinessException("El DNI debe contener exactamente 8 dígitos numéricos."));
        mvc.perform(post("/api/ciudadanos").with(csrf()).with(user("admin").roles("ADMINISTRADOR"))
                        .contentType(JSON)
                        .content("{\"tipoDocumento\":\"DNI\",\"numeroDocumento\":\"123\","
                                + "\"nombres\":\"Juan\",\"apellidos\":\"Perez\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // 404 No encontrado
        when(ciudadanoService.obtener(404L))
                .thenThrow(new ResourceNotFoundException("Ciudadano no encontrado con id 404"));
        mvc.perform(get("/api/ciudadanos/404").with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        // 409 Conflicto (documento duplicado)
        org.mockito.Mockito.doReturn(null).when(ciudadanoService).crear(any());
        org.mockito.Mockito.doThrow(new ConflictException("Ya existe un ciudadano registrado con el documento DNI 12345678"))
                .when(ciudadanoService).crear(any());
        mvc.perform(post("/api/ciudadanos").with(csrf()).with(user("admin").roles("ADMINISTRADOR"))
                        .contentType(JSON)
                        .content("{\"tipoDocumento\":\"DNI\",\"numeroDocumento\":\"12345678\","
                                + "\"nombres\":\"Juan\",\"apellidos\":\"Perez\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void consultasPorDocumentoRucYCiudadanoTitularFuncionan() throws Exception {
        when(ciudadanoService.obtenerPorDocumento(eq(TipoDocumento.DNI), eq("12345678")))
                .thenReturn(ciudadanoEjemplo());
        when(establecimientoService.obtenerPorRuc("20100070970"))
                .thenReturn(List.of(establecimientoEjemplo()));
        when(establecimientoService.obtenerPorCiudadano(1L))
                .thenReturn(List.of(establecimientoEjemplo()));

        mvc.perform(get("/api/ciudadanos/documento")
                        .param("tipoDocumento", "DNI").param("numeroDocumento", "12345678")
                        .with(user("func").roles("FUNCIONARIO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroDocumento").value("12345678"));

        mvc.perform(get("/api/establecimientos/ruc/20100070970")
                        .with(user("func").roles("FUNCIONARIO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ruc").value("20100070970"));

        mvc.perform(get("/api/establecimientos/ciudadano/1")
                        .with(user("func").roles("FUNCIONARIO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ciudadanoId").value(1));
    }
}
