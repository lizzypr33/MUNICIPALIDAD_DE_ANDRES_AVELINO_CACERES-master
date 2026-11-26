package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

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

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Usuario;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.AuditoriaRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.RolRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.UsuarioRepository;

/**
 * Pruebas de seguridad contra PostgreSQL real (base andres_test; requiere DB_PASSWORD).
 * Cada prueba se revierte al terminar. Las contraseñas son ficticias y solo existen en memoria.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SeguridadTests {

    private static final String PASS = "Clave-de-prueba-1";
    private static final String PASS_NUEVA = "Clave-de-prueba-2";
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired RolRepository roles;
    @Autowired AuditoriaRepository auditorias;
    @Autowired PasswordEncoder encoder;

    private static String uid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private Usuario crearUsuario(String rol, boolean activo) {
        Usuario u = new Usuario();
        u.setUsername("u" + uid());
        u.setPassword(encoder.encode(PASS));
        u.setNombres("Prueba");
        u.setApellidos("Seguridad");
        u.setEmail(uid() + "@test.pe");
        u.setActivo(activo);
        u.setRoles(new HashSet<>(Set.of(roles.findByNombre(rol).orElseThrow())));
        return usuarios.saveAndFlush(u);
    }

    private static String credenciales(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    /** Inicia sesión y devuelve la sesión HTTP resultante. */
    private MockHttpSession login(String username, String password) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(JSON).content(credenciales(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) r.getRequest().getSession(false);
    }

    private MockHttpSession loginAdmin() throws Exception {
        Usuario admin = crearUsuario("ADMINISTRADOR", true);
        return login(admin.getUsername(), PASS);
    }

    @Test
    void loginCorrectoDevuelveUsuarioYRolesSinPassword() throws Exception {
        Usuario admin = crearUsuario("ADMINISTRADOR", true);
        mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(JSON).content(credenciales(admin.getUsername(), PASS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(admin.getUsername()))
                .andExpect(jsonPath("$.roles[0]").value("ADMINISTRADOR"))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    @Test
    void loginRegistraAuditoria() throws Exception {
        Usuario u = crearUsuario("FUNCIONARIO", true);
        login(u.getUsername(), PASS);
        assertThat(auditorias.findAll())
                .anyMatch(a -> "LOGIN_EXITOSO".equals(a.getAccion()) && u.getId().equals(a.getEntidadId()));
    }

    @Test
    void loginIncorrectoDevuelve401Generico() throws Exception {
        Usuario u = crearUsuario("FUNCIONARIO", true);
        mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(JSON).content(credenciales(u.getUsername(), "otra-clave-incorrecta")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas o sesión no iniciada."));
        // Usuario inexistente: misma respuesta, sin revelar que no existe
        mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(JSON).content(credenciales("noexiste" + uid(), PASS)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas o sesión no iniciada."));
    }

    @Test
    void usuarioDesactivadoNoPuedeIniciarSesion() throws Exception {
        Usuario u = crearUsuario("FUNCIONARIO", false);
        mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(JSON).content(credenciales(u.getUsername(), PASS)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accesoSinAutenticacionDevuelve401Json() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void rolInsuficienteDevuelve403() throws Exception {
        Usuario ciudadano = crearUsuario("CIUDADANO", true);
        MockHttpSession sesion = login(ciudadano.getUsername(), PASS);
        mvc.perform(get("/api/usuarios").session(sesion))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void administradorAccedeAlListadoPaginado() throws Exception {
        MockHttpSession sesion = loginAdmin();
        mvc.perform(get("/api/usuarios").param("size", "5").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").isArray())
                .andExpect(jsonPath("$.tamanio").value(5));
    }

    @Test
    void loginYLogoutSinCsrfSonRechazados() throws Exception {
        Usuario u = crearUsuario("FUNCIONARIO", true);
        mvc.perform(post("/api/auth/login").contentType(JSON).content(credenciales(u.getUsername(), PASS)))
                .andExpect(status().isForbidden());

        MockHttpSession sesion = login(u.getUsername(), PASS);
        mvc.perform(post("/api/auth/logout").session(sesion))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutCierraLaSesion() throws Exception {
        Usuario u = crearUsuario("FUNCIONARIO", true);
        MockHttpSession sesion = login(u.getUsername(), PASS);
        mvc.perform(post("/api/auth/logout").with(csrf()).session(sesion))
                .andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").session(sesion))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cambioDePasswordCorrectoPermiteEntrarConLaNueva() throws Exception {
        Usuario u = crearUsuario("FUNCIONARIO", true);
        MockHttpSession sesion = login(u.getUsername(), PASS);
        mvc.perform(post("/api/auth/change-password").with(csrf()).session(sesion)
                        .contentType(JSON)
                        .content("{\"currentPassword\":\"" + PASS + "\",\"newPassword\":\"" + PASS_NUEVA + "\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(JSON).content(credenciales(u.getUsername(), PASS)))
                .andExpect(status().isUnauthorized());
        login(u.getUsername(), PASS_NUEVA);
    }

    @Test
    void cambioDePasswordValidaContrasenaActualYLongitud() throws Exception {
        Usuario u = crearUsuario("FUNCIONARIO", true);
        MockHttpSession sesion = login(u.getUsername(), PASS);
        // contraseña actual incorrecta
        mvc.perform(post("/api/auth/change-password").with(csrf()).session(sesion)
                        .contentType(JSON)
                        .content("{\"currentPassword\":\"equivocada-123\",\"newPassword\":\"" + PASS_NUEVA + "\"}"))
                .andExpect(status().isBadRequest());
        // nueva igual a la actual
        mvc.perform(post("/api/auth/change-password").with(csrf()).session(sesion)
                        .contentType(JSON)
                        .content("{\"currentPassword\":\"" + PASS + "\",\"newPassword\":\"" + PASS + "\"}"))
                .andExpect(status().isBadRequest());
        // nueva menor de 12 caracteres
        mvc.perform(post("/api/auth/change-password").with(csrf()).session(sesion)
                        .contentType(JSON)
                        .content("{\"currentPassword\":\"" + PASS + "\",\"newPassword\":\"corta\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void administradorCreaUsuarioSinExponerPassword() throws Exception {
        MockHttpSession admin = loginAdmin();
        Long funcionarioId = roles.findByNombre("FUNCIONARIO").orElseThrow().getId();
        String username = "nuevo" + uid();
        mvc.perform(post("/api/usuarios").with(csrf()).session(admin)
                        .contentType(JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + PASS
                                + "\",\"nombres\":\"Ana\",\"apellidos\":\"Lopez\",\"email\":\"ana" + uid()
                                + "@test.pe\",\"rolIds\":[" + funcionarioId + "]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.roles[0]").value("FUNCIONARIO"))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    /** Campos como "activo" o "id" enviados en el JSON no deben tener efecto (sin escalamiento). */
    @Test
    void camposExtraEnJsonNoSeMapean() throws Exception {
        MockHttpSession admin = loginAdmin();
        Long ciudadanoId = roles.findByNombre("CIUDADANO").orElseThrow().getId();
        String username = "extra" + uid();
        mvc.perform(post("/api/usuarios").with(csrf()).session(admin)
                        .contentType(JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + PASS
                                + "\",\"nombres\":\"A\",\"apellidos\":\"B\",\"email\":\"e" + uid()
                                + "@test.pe\",\"rolIds\":[" + ciudadanoId + "],"
                                + "\"id\":999999,\"activo\":false,\"roles\":[\"ADMINISTRADOR\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not(999999)))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.roles[0]").value("CIUDADANO"))
                .andExpect(jsonPath("$.roles.length()").value(1));
    }

    @Test
    void usernameDuplicadoDevuelve409() throws Exception {
        MockHttpSession admin = loginAdmin();
        Usuario existente = crearUsuario("CIUDADANO", true);
        Long rolId = roles.findByNombre("CIUDADANO").orElseThrow().getId();
        mvc.perform(post("/api/usuarios").with(csrf()).session(admin)
                        .contentType(JSON)
                        .content("{\"username\":\"" + existente.getUsername() + "\",\"password\":\"" + PASS
                                + "\",\"nombres\":\"A\",\"apellidos\":\"B\",\"email\":\"x" + uid()
                                + "@test.pe\",\"rolIds\":[" + rolId + "]}"))
                .andExpect(status().isConflict());
    }

    @Test
    void emailDuplicadoDevuelve409() throws Exception {
        MockHttpSession admin = loginAdmin();
        Usuario existente = crearUsuario("CIUDADANO", true);
        Long rolId = roles.findByNombre("CIUDADANO").orElseThrow().getId();
        mvc.perform(post("/api/usuarios").with(csrf()).session(admin)
                        .contentType(JSON)
                        .content("{\"username\":\"nuevo" + uid() + "\",\"password\":\"" + PASS
                                + "\",\"nombres\":\"A\",\"apellidos\":\"B\",\"email\":\"" + existente.getEmail()
                                + "\",\"rolIds\":[" + rolId + "]}"))
                .andExpect(status().isConflict());
    }

    @Test
    void asignacionDeRolesActualizaElUsuario() throws Exception {
        MockHttpSession admin = loginAdmin();
        Usuario u = crearUsuario("CIUDADANO", true);
        Long funcionarioId = roles.findByNombre("FUNCIONARIO").orElseThrow().getId();
        mvc.perform(put("/api/usuarios/{id}/roles", u.getId()).with(csrf()).session(admin)
                        .contentType(JSON).content("{\"rolIds\":[" + funcionarioId + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("FUNCIONARIO"))
                .andExpect(jsonPath("$.roles.length()").value(1));
    }

    @Test
    void noSePuedeDesactivarAlUltimoAdministradorActivo() throws Exception {
        Usuario admin = crearUsuario("ADMINISTRADOR", true);
        // Dejar al administrador de la prueba como único activo de la base de pruebas
        for (Usuario otro : usuarios.findAll()) {
            boolean esAdmin = otro.getRoles().stream().anyMatch(r -> "ADMINISTRADOR".equals(r.getNombre()));
            if (esAdmin && otro.isActivo() && !otro.getId().equals(admin.getId())) {
                otro.setActivo(false);
            }
        }
        usuarios.flush();

        MockHttpSession sesion = login(admin.getUsername(), PASS);
        mvc.perform(patch("/api/usuarios/{id}/estado", admin.getId()).with(csrf()).session(sesion)
                        .contentType(JSON).content("{\"activo\":false}"))
                .andExpect(status().isConflict());
    }

    @Test
    void desactivarUsuarioExpulsaSuSesion() throws Exception {
        MockHttpSession admin = loginAdmin();
        Usuario u = crearUsuario("FUNCIONARIO", true);
        MockHttpSession sesionUsuario = login(u.getUsername(), PASS);
        mvc.perform(get("/api/auth/me").session(sesionUsuario)).andExpect(status().isOk());

        mvc.perform(patch("/api/usuarios/{id}/estado", u.getId()).with(csrf()).session(admin)
                        .contentType(JSON).content("{\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        mvc.perform(get("/api/auth/me").session(sesionUsuario))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void detalleDeUsuarioNoExponePassword() throws Exception {
        MockHttpSession admin = loginAdmin();
        Usuario u = crearUsuario("FUNCIONARIO", true);
        mvc.perform(get("/api/usuarios/{id}", u.getId()).session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(u.getUsername()))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    @Test
    void listaDeRolesSoloParaAdministrador() throws Exception {
        Usuario ciudadano = crearUsuario("CIUDADANO", true);
        MockHttpSession sesion = login(ciudadano.getUsername(), PASS);
        mvc.perform(get("/api/roles").session(sesion)).andExpect(status().isForbidden());

        MockHttpSession admin = loginAdmin();
        mvc.perform(get("/api/roles").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").exists());
    }

    @Test
    void validacionRechazaCamposInvalidos() throws Exception {
        MockHttpSession admin = loginAdmin();
        mvc.perform(post("/api/usuarios").with(csrf()).session(admin)
                        .contentType(JSON)
                        .content("{\"username\":\"\",\"password\":\"corta\",\"email\":\"no-es-email\",\"rolIds\":[]}"))
                .andExpect(status().isBadRequest());
    }
}
