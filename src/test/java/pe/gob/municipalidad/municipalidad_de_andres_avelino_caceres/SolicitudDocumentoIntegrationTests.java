package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Ciudadano;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Documento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Establecimiento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Requisito;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Solicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.TipoTramite;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Usuario;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoSolicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoDocumento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.AuditoriaRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.CiudadanoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.DocumentoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.EstablecimientoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.HistorialSolicitudRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.NotificacionRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.RequisitoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.RolRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.SolicitudRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.TipoTramiteRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.UsuarioRepository;

/**
 * Pruebas de integración contra PostgreSQL real (base andres_test; requiere DB_PASSWORD)
 * para Solicitudes, Tipos de Trámite, Requisitos, Documentos e Historial de Estados.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SolicitudDocumentoIntegrationTests {

    private static final String PASS = "Clave-de-prueba-1";
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired RolRepository roles;
    @Autowired CiudadanoRepository ciudadanos;
    @Autowired EstablecimientoRepository establecimientos;
    @Autowired TipoTramiteRepository tiposTramite;
    @Autowired RequisitoRepository requisitos;
    @Autowired SolicitudRepository solicitudes;
    @Autowired DocumentoRepository documentos;
    @Autowired HistorialSolicitudRepository historialRepository;
    @Autowired NotificacionRepository notificaciones;
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
        u.setNombres("Usuario");
        u.setApellidos(rol);
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
    void cicloCompletoSolicitudRequisitosDocumentosHistorialYAuditoriaEnPostgreSQL() throws Exception {
        Usuario adminUser = crearUsuario("ADMINISTRADOR");
        Usuario funcUser = crearUsuario("FUNCIONARIO");
        Usuario ciudUser = crearUsuario("CIUDADANO");
        Usuario otroCiudUser = crearUsuario("CIUDADANO");

        MockHttpSession sesionAdmin = login(adminUser);
        MockHttpSession sesionFunc = login(funcUser);
        MockHttpSession sesionCiud = login(ciudUser);
        MockHttpSession sesionOtro = login(otroCiudUser);

        Ciudadano titular = new Ciudadano();
        titular.setTipoDocumento(TipoDocumento.DNI);
        titular.setNumeroDocumento(dniUnico());
        titular.setNombres("Juan");
        titular.setApellidos("Perez");
        titular.setUsuario(ciudUser);
        ciudadanos.saveAndFlush(titular);

        Establecimiento est = new Establecimiento();
        est.setNombreComercial("Bodega Ayacucho");
        est.setDireccion("Jr. Asamblea 123");
        est.setGiroNegocio("Abarrotes");
        est.setAreaMetrosCuadrados(new BigDecimal("45.00"));
        est.setNumeroPisos(1);
        est.setCiudadano(titular);
        establecimientos.saveAndFlush(est);

        // 1. Administrador crea TipoTramite y Requisito, y los vincula
        String codTramite = "TRAM-" + uid().toUpperCase();
        String codReq = "REQ-" + uid().toUpperCase();

        mvc.perform(post("/api/tipos-tramite").with(csrf()).session(sesionAdmin)
                        .contentType(JSON)
                        .content("{\"codigo\":\"" + codTramite + "\",\"nombre\":\"Licencia Indeterminada\"}"))
                .andExpect(status().isCreated());
        TipoTramite tramite = tiposTramite.findByCodigo(codTramite).orElseThrow();

        mvc.perform(post("/api/requisitos").with(csrf()).session(sesionAdmin)
                        .contentType(JSON)
                        .content("{\"codigo\":\"" + codReq + "\",\"nombre\":\"Declaración Jurada\"}"))
                .andExpect(status().isCreated());
        Requisito requisito = requisitos.findByCodigo(codReq).orElseThrow();

        mvc.perform(put("/api/tipos-tramite/{id}/requisitos", tramite.getId())
                        .with(csrf()).session(sesionAdmin)
                        .contentType(JSON)
                        .content("[{\"requisitoId\":" + requisito.getId()
                                + ",\"obligatorio\":true,\"ordenVisualizacion\":1}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(requisito.getId()));

        // 2. Ciudadano registra solicitud en BORRADOR
        mvc.perform(post("/api/solicitudes").with(csrf()).session(sesionCiud)
                        .contentType(JSON)
                        .content("{\"ciudadanoId\":" + titular.getId()
                                + ",\"establecimientoId\":" + est.getId()
                                + ",\"tipoTramiteId\":" + tramite.getId()
                                + ",\"observaciones\":\"Borrador inicial\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("BORRADOR"));

        Solicitud sol = solicitudes.findByCiudadanoId(titular.getId()).get(0);

        // 3. Intentar presentar sin el documento obligatorio -> 400
        mvc.perform(post("/api/solicitudes/{id}/presentar", sol.getId())
                        .with(csrf()).session(sesionCiud))
                .andExpect(status().isBadRequest());

        // 4. Subir PDF válido para el requisito obligatorio
        byte[] pdfBytes = "%PDF-1.7\nDocumento PDF real de prueba".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile archivoPdf = new MockMultipartFile(
                "archivo", "declaracion.pdf", "application/pdf", pdfBytes);

        mvc.perform(multipart("/api/solicitudes/{id}/documentos", sol.getId())
                        .file(archivoPdf)
                        .param("requisitoId", String.valueOf(requisito.getId()))
                        .with(csrf()).session(sesionCiud))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreOriginal").value("declaracion.pdf"));

        Documento doc = documentos.findBySolicitudId(sol.getId()).get(0);

        // 5. Otro ciudadano no puede consultar el expediente ni descargar el documento -> 403
        mvc.perform(get("/api/solicitudes/{id}", sol.getId()).session(sesionOtro))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/documentos/{id}/descargar", doc.getId()).session(sesionOtro))
                .andExpect(status().isForbidden());

        // 6. Ciudadano titular descarga su documento y presenta la solicitud
        mvc.perform(get("/api/documentos/{id}/descargar", doc.getId()).session(sesionCiud))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pdfBytes));

        mvc.perform(post("/api/solicitudes/{id}/presentar", sol.getId())
                        .with(csrf()).session(sesionCiud)
                        .contentType(JSON)
                        .content("{\"observaciones\":\"Presento expediente completo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PRESENTADA"))
                .andExpect(jsonPath("$.numeroExpediente").exists());

        Solicitud actualizada = solicitudes.findById(sol.getId()).orElseThrow();
        String expediente = actualizada.getNumeroExpediente();
        assertThat(expediente).startsWith("EXP-");

        // 7. Asignar funcionario, iniciar revisión, observar, subsanar, volver a revisión y aprobar
        mvc.perform(patch("/api/solicitudes/{id}/asignar", sol.getId())
                        .with(csrf()).session(sesionAdmin)
                        .contentType(JSON)
                        .content("{\"usuarioResponsableId\":" + funcUser.getId() + "}"))
                .andExpect(status().isOk());

        mvc.perform(patch("/api/solicitudes/{id}/iniciar-revision", sol.getId())
                        .with(csrf()).session(sesionFunc))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_REVISION"));

        mvc.perform(patch("/api/solicitudes/{id}/observar", sol.getId())
                        .with(csrf()).session(sesionFunc)
                        .contentType(JSON)
                        .content("{\"observaciones\":\"Aclarar croquis\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("OBSERVADA"));

        mvc.perform(patch("/api/solicitudes/{id}/subsanar", sol.getId())
                        .with(csrf()).session(sesionCiud)
                        .contentType(JSON)
                        .content("{\"observaciones\":\"Croquis aclarado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("SUBSANADA"));

        mvc.perform(patch("/api/solicitudes/{id}/iniciar-revision", sol.getId())
                        .with(csrf()).session(sesionFunc))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_REVISION"));

        mvc.perform(patch("/api/solicitudes/{id}/aprobar", sol.getId())
                        .with(csrf()).session(sesionFunc)
                        .contentType(JSON)
                        .content("{\"observaciones\":\"Aprobación técnica conforme\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"));

        // 8. Verificar historial, notificaciones y auditoría en base de datos
        assertThat(historialRepository.findBySolicitudIdOrderByFechaCambioAscIdAsc(sol.getId()))
                .extracting(h -> h.getEstadoNuevo())
                .contains(EstadoSolicitud.BORRADOR, EstadoSolicitud.PRESENTADA,
                        EstadoSolicitud.EN_REVISION, EstadoSolicitud.OBSERVADA,
                        EstadoSolicitud.SUBSANADA, EstadoSolicitud.APROBADA);

        assertThat(notificaciones.findByUsuarioIdOrderByFechaCreacionDesc(ciudUser.getId())).isNotEmpty();
        assertThat(auditorias.findAll())
                .anyMatch(a -> "PRESENTAR_SOLICITUD".equals(a.getAccion()) && sol.getId().equals(a.getEntidadId()))
                .anyMatch(a -> "APROBAR_SOLICITUD".equals(a.getAccion()) && sol.getId().equals(a.getEntidadId()));
    }
}
