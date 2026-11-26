package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.CambiarEstadoLicenciaRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.EmitirLicenciaRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ObservarSolicitudRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.PresentarSolicitudRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ProgramarInspeccionRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ResolverSolicitudRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ResultadoInspeccionRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.SolicitudRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.TipoTramiteRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.InspeccionDetalleResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.LicenciaDetalleResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.SolicitudResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.TipoTramiteResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.VerificacionLicenciaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Ciudadano;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Establecimiento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Usuario;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoInspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoLicencia;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.ResultadoInspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoDocumento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ConflictException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.CiudadanoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.EstablecimientoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.RolRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.UsuarioRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.security.CustomUserDetails;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.CertificadoService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.CertificadoService.CertificadoDescarga;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.InspeccionService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.LicenciaService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.SolicitudService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.TipoTramiteService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.VerificacionLicenciaService;

/**
 * Pruebas de integración contra PostgreSQL real (base andres_test) para el flujo completo de PROMPT 06:
 * migración V4, inspecciones técnicas, emisión de licencias, certificado PDF, QR y verificación pública.
 */
@SpringBootTest
@Transactional
class LicenciaInspeccionIntegrationTests {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private TipoTramiteService tipoTramiteService;
    @Autowired private SolicitudService solicitudService;
    @Autowired private InspeccionService inspeccionService;
    @Autowired private LicenciaService licenciaService;
    @Autowired private CertificadoService certificadoService;
    @Autowired private VerificacionLicenciaService verificacionLicenciaService;

    @Autowired private CiudadanoRepository ciudadanos;
    @Autowired private EstablecimientoRepository establecimientos;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private RolRepository roles;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(Usuario u, String rol) {
        CustomUserDetails cud = new CustomUserDetails(u);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(cud, null, cud.getAuthorities()));
    }

    @Test
    void migracionV4YFlujoCompletoDeInspeccionEmisionCertificadoYVerificacionPublica() {
        Integer v4Aplicada = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success and version = '4'", Integer.class);
        assertThat(v4Aplicada).isEqualTo(1);

        String sufijo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Usuario admin = new Usuario();
        admin.setUsername("admin_" + sufijo);
        admin.setPassword("$2a$10$hashDePruebaNoReal");
        admin.setNombres("Admin");
        admin.setApellidos("Municipal");
        admin.setEmail("admin_" + sufijo + "@test.pe");
        admin.setRoles(Set.of(roles.findByNombre("ADMINISTRADOR").orElseThrow()));
        usuarios.saveAndFlush(admin);
        autenticarComo(admin, "ADMINISTRADOR");

        Ciudadano ciudadano = new Ciudadano();
        ciudadano.setTipoDocumento(TipoDocumento.DNI);
        ciudadano.setNumeroDocumento(String.valueOf(10000000 + Math.abs(sufijo.hashCode() % 89999999)));
        ciudadano.setNombres("Elena");
        ciudadano.setApellidos("Quispe");
        ciudadano.setActivo(true);
        ciudadanos.saveAndFlush(ciudadano);

        Establecimiento est = new Establecimiento();
        est.setNombreComercial("Restaurante Ayacuchano " + sufijo);
        est.setDireccion("Jr. Asamblea 230");
        est.setGiroNegocio("Restaurante");
        est.setAreaMetrosCuadrados(new BigDecimal("80.00"));
        est.setCiudadano(ciudadano);
        est.setActivo(true);
        establecimientos.saveAndFlush(est);

        TipoTramiteResponse tramite = tipoTramiteService.crear(
                new TipoTramiteRequest("TRAM-INSP-" + sufijo, "Licencia con Inspección " + sufijo, "Trámite", true));

        SolicitudResponse borrador = solicitudService.crear(
                new SolicitudRequest(ciudadano.getId(), est.getId(), tramite.id(), null, "Solicitud inicial"));
        solicitudService.presentar(borrador.id(), new PresentarSolicitudRequest("Presentación"));
        solicitudService.iniciarRevision(borrador.id(), "Revisión iniciada");

        // Programar, iniciar y finalizar inspección con resultado FAVORABLE
        InspeccionDetalleResponse prog = inspeccionService.programar(
                new ProgramarInspeccionRequest(borrador.id(), admin.getId(), LocalDateTime.now().plusDays(1), "Visita"));
        inspeccionService.iniciar(prog.id());
        InspeccionDetalleResponse fin = inspeccionService.registrarResultado(
                prog.id(), new ResultadoInspeccionRequest(
                        ResultadoInspeccion.FAVORABLE, LocalDateTime.now(),
                        "Establecimiento cumple condiciones técnicas", "Mantener extintores vigentes"));
        assertThat(fin.estado()).isEqualTo(EstadoInspeccion.FINALIZADA);

        // Aprobar solicitud y emitir licencia
        solicitudService.aprobar(borrador.id(),
                new ResolverSolicitudRequest("Aprobada tras inspección favorable", null));
        LicenciaDetalleResponse licencia = licenciaService.emitir(
                new EmitirLicenciaRequest(borrador.id(), null, "Emisión oficial"));
        assertThat(licencia.codigoLicencia()).startsWith("LIC-");
        assertThat(licencia.estado()).isEqualTo(EstadoLicencia.VIGENTE);

        // Rechazar segunda emisión para la misma solicitud
        assertThrows(ConflictException.class,
                () -> licenciaService.emitir(new EmitirLicenciaRequest(borrador.id(), "Duplicada")));

        // Generar certificado PDF
        CertificadoDescarga pdf = certificadoService.generarCertificadoLicencia(licencia.id());
        assertThat(pdf.contenido()).isNotEmpty();

        // Verificar autenticidad mediante token público
        VerificacionLicenciaResponse verif = verificacionLicenciaService.verificarPorToken(licencia.tokenVerificacion());
        assertThat(verif.autentica()).isTrue();
        assertThat(verif.vigente()).isTrue();
        assertThat(verif.codigoLicencia()).isEqualTo(licencia.codigoLicencia());
    }
}
