package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Ciudadano;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Establecimiento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Representante;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Solicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.TipoTramite;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Usuario;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoDocumento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.AuditoriaRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.CiudadanoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.EstablecimientoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.RepresentanteRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.RolRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.SolicitudRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.TipoTramiteRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.UsuarioRepository;

/**
 * Pruebas de integración contra PostgreSQL real (base andres_test; requiere DB_PASSWORD)
 * para los módulos de Ciudadanos, Representantes y Establecimientos Comerciales.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CiudadanoEstablecimientoIntegrationTests {

    private static final String PASS = "Clave-de-prueba-1";
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired RolRepository roles;
    @Autowired CiudadanoRepository ciudadanos;
    @Autowired RepresentanteRepository representantes;
    @Autowired EstablecimientoRepository establecimientos;
    @Autowired SolicitudRepository solicitudes;
    @Autowired TipoTramiteRepository tiposTramite;
    @Autowired AuditoriaRepository auditorias;
    @Autowired PasswordEncoder encoder;

    private static String uid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static String dniUnico() {
        int num = Math.abs(UUID.randomUUID().hashCode()) % 90_000_000 + 10_000_000;
        return String.valueOf(num);
    }

    private Usuario crearUsuario(String rol) {
        Usuario u = new Usuario();
        u.setUsername("u" + uid());
        u.setPassword(encoder.encode(PASS));
        u.setNombres("Prueba");
        u.setApellidos("Integracion");
        u.setEmail(uid() + "@test.pe");
        u.setActivo(true);
        u.setRoles(new HashSet<>(Set.of(roles.findByNombre(rol).orElseThrow())));
        return usuarios.saveAndFlush(u);
    }

    private MockHttpSession login(Usuario u) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(JSON)
                        .content("{\"username\":\"" + u.getUsername() + "\",\"password\":\"" + PASS + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) r.getRequest().getSession(false);
    }

    @Test
    void cicloCompletoCiudadanoRepresentanteYEstablecimientoEnPostgreSQL() throws Exception {
        MockHttpSession admin = login(crearUsuario("ADMINISTRADOR"));
        String dni = dniUnico();

        // 1. Registrar ciudadano
        mvc.perform(post("/api/ciudadanos").with(csrf()).session(admin)
                        .contentType(JSON)
                        .content("{\"tipoDocumento\":\"DNI\",\"numeroDocumento\":\"" + dni
                                + "\",\"nombres\":\"Juan\",\"apellidos\":\"Perez\",\"email\":\"juan@example.com\","
                                + "\"telefono\":\"999999999\",\"direccion\":\"Ayacucho\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroDocumento").value(dni))
                .andExpect(jsonPath("$.activo").value(true));

        Ciudadano c = ciudadanos.findByTipoDocumentoAndNumeroDocumento(TipoDocumento.DNI, dni).orElseThrow();

        // 2. Documento duplicado -> 409
        mvc.perform(post("/api/ciudadanos").with(csrf()).session(admin)
                        .contentType(JSON)
                        .content("{\"tipoDocumento\":\"DNI\",\"numeroDocumento\":\"" + dni
                                + "\",\"nombres\":\"Otro\",\"apellidos\":\"Nombre\"}"))
                .andExpect(status().isConflict());

        // 3. Consulta por documento y filtros paginados
        mvc.perform(get("/api/ciudadanos/documento")
                        .param("tipoDocumento", "DNI").param("numeroDocumento", dni)
                        .session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(c.getId()));

        mvc.perform(get("/api/ciudadanos")
                        .param("numeroDocumento", dni).param("activo", "true")
                        .session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1));

        // 4. Registrar representante
        String dniRep = dniUnico();
        mvc.perform(post("/api/representantes").with(csrf()).session(admin)
                        .contentType(JSON)
                        .content("{\"tipoDocumento\":\"DNI\",\"numeroDocumento\":\"" + dniRep
                                + "\",\"nombres\":\"Carlos\",\"apellidos\":\"Soto\",\"cargo\":\"Apoderado\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroDocumento").value(dniRep));

        Representante rep = representantes.findByTipoDocumentoAndNumeroDocumento(TipoDocumento.DNI, dniRep)
                .orElseThrow();

        // 5. Registrar establecimiento con RUC válido (20100070970)
        mvc.perform(post("/api/establecimientos").with(csrf()).session(admin)
                        .contentType(JSON)
                        .content("{\"nombreComercial\":\"Comercial Ejemplo\",\"razonSocial\":\"Comercial SAC\","
                                + "\"ruc\":\"20100070970\",\"direccion\":\"Av. Principal 100\","
                                + "\"giroNegocio\":\"Venta de abarrotes\",\"areaMetrosCuadrados\":80.50,"
                                + "\"numeroPisos\":1,\"ciudadanoId\":" + c.getId()
                                + ",\"representanteId\":" + rep.getId() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ciudadanoId").value(c.getId()))
                .andExpect(jsonPath("$.representanteId").value(rep.getId()));

        // 6. Búsqueda por ciudadano titular
        mvc.perform(get("/api/establecimientos/ciudadano/{ciudadanoId}", c.getId()).session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombreComercial").value("Comercial Ejemplo"));

        // 7. Auditoría registrada
        assertThat(auditorias.findAll())
                .anyMatch(a -> "REGISTRAR_CIUDADANO".equals(a.getAccion()) && c.getId().equals(a.getEntidadId()))
                .anyMatch(a -> "REGISTRAR_REPRESENTANTE".equals(a.getAccion()) && rep.getId().equals(a.getEntidadId()))
                .anyMatch(a -> "REGISTRAR_ESTABLECIMIENTO".equals(a.getAccion()));
    }

    @Test
    void integridadDeRelacionesImpideCambiarTitularODocumentoConExpedienteVinculado() throws Exception {
        MockHttpSession admin = login(crearUsuario("ADMINISTRADOR"));

        Ciudadano c1 = new Ciudadano();
        c1.setTipoDocumento(TipoDocumento.DNI);
        c1.setNumeroDocumento(dniUnico());
        c1.setNombres("Titular");
        c1.setApellidos("Uno");
        ciudadanos.saveAndFlush(c1);

        Ciudadano c2 = new Ciudadano();
        c2.setTipoDocumento(TipoDocumento.DNI);
        c2.setNumeroDocumento(dniUnico());
        c2.setNombres("Titular");
        c2.setApellidos("Dos");
        ciudadanos.saveAndFlush(c2);

        Establecimiento est = new Establecimiento();
        est.setNombreComercial("Local Con Expediente");
        est.setDireccion("Jr. Ayacucho 200");
        est.setGiroNegocio("Bodega");
        est.setAreaMetrosCuadrados(new BigDecimal("40.00"));
        est.setNumeroPisos(1);
        est.setCiudadano(c1);
        establecimientos.saveAndFlush(est);

        TipoTramite tt = new TipoTramite();
        tt.setCodigo("TT-" + uid());
        tt.setNombre("Licencia General");
        tiposTramite.saveAndFlush(tt);

        Solicitud sol = new Solicitud();
        sol.setNumeroExpediente("EXP-" + uid());
        sol.setCiudadano(c1);
        sol.setEstablecimiento(est);
        sol.setTipoTramite(tt);
        solicitudes.saveAndFlush(sol);

        // No puede cambiar el documento del ciudadano con expediente
        mvc.perform(put("/api/ciudadanos/{id}", c1.getId()).with(csrf()).session(admin)
                        .contentType(JSON)
                        .content("{\"tipoDocumento\":\"DNI\",\"numeroDocumento\":\"" + dniUnico()
                                + "\",\"nombres\":\"Titular\",\"apellidos\":\"Uno\"}"))
                .andExpect(status().isConflict());

        // No puede cambiar el titular del establecimiento con expediente
        mvc.perform(put("/api/establecimientos/{id}", est.getId()).with(csrf()).session(admin)
                        .contentType(JSON)
                        .content("{\"nombreComercial\":\"Local Con Expediente\",\"direccion\":\"Jr. Ayacucho 200\","
                                + "\"giroNegocio\":\"Bodega\",\"areaMetrosCuadrados\":40.00,\"numeroPisos\":1,"
                                + "\"ciudadanoId\":" + c2.getId() + "}"))
                .andExpect(status().isConflict());

        // Desactivación lógica conserva el registro físicamente
        mvc.perform(patch("/api/establecimientos/{id}/estado", est.getId()).with(csrf()).session(admin)
                        .contentType(JSON).content("{\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
        assertThat(establecimientos.findById(est.getId())).isPresent();
    }

    @Test
    void ciudadanoAutenticadoSoloAccedeASusPropiosRegistros() throws Exception {
        Usuario u1 = crearUsuario("CIUDADANO");
        Usuario u2 = crearUsuario("CIUDADANO");

        Ciudadano c1 = new Ciudadano();
        c1.setTipoDocumento(TipoDocumento.DNI);
        c1.setNumeroDocumento(dniUnico());
        c1.setNombres("Ciudadano");
        c1.setApellidos("Uno");
        c1.setUsuario(u1);
        ciudadanos.saveAndFlush(c1);

        Ciudadano c2 = new Ciudadano();
        c2.setTipoDocumento(TipoDocumento.DNI);
        c2.setNumeroDocumento(dniUnico());
        c2.setNombres("Ciudadano");
        c2.setApellidos("Dos");
        c2.setUsuario(u2);
        ciudadanos.saveAndFlush(c2);

        MockHttpSession sesionC1 = login(u1);

        // Accede a su propio registro
        mvc.perform(get("/api/ciudadanos/{id}", c1.getId()).session(sesionC1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(c1.getId()));

        // Rechaza acceso al registro de c2 con 403
        mvc.perform(get("/api/ciudadanos/{id}", c2.getId()).session(sesionC1))
                .andExpect(status().isForbidden());
    }
}
