package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.PresentarSolicitudRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.RegistrarPagoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.SolicitudRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.TipoTramiteRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.VerificarPagoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.AuditoriaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.NotificacionResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PagoDetalleResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.SolicitudResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.TipoTramiteResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Ciudadano;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Establecimiento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Usuario;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoPago;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoDocumento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ConflictException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.CiudadanoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.EstablecimientoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.RolRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.UsuarioRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.security.CustomUserDetails;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.AuditoriaService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.ConstanciaPagoService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.ConstanciaPagoService.ConstanciaPagoDescarga;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.NotificacionService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.PagoService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.SolicitudService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.TipoTramiteService;

/**
 * Pruebas de integración contra PostgreSQL real para PROMPT 07:
 * migración V5, registro/verificación de pagos, constancia PDF, notificaciones internas y auditoría.
 */
@SpringBootTest
@Transactional
class PagoNotificacionAuditoriaIntegrationTests {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private TipoTramiteService tipoTramiteService;
    @Autowired private SolicitudService solicitudService;
    @Autowired private PagoService pagoService;
    @Autowired private ConstanciaPagoService constanciaPagoService;
    @Autowired private NotificacionService notificacionService;
    @Autowired private AuditoriaService auditoriaService;

    @Autowired private CiudadanoRepository ciudadanos;
    @Autowired private EstablecimientoRepository establecimientos;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private RolRepository roles;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(Usuario u) {
        CustomUserDetails cud = new CustomUserDetails(u);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(cud, null, cud.getAuthorities()));
    }

    @Test
    void migracionV5YFlujoCompletoDePagoVerificacionConstanciaNotificacionYAuditoria() {
        Integer v5Aplicada = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success and version = '5'", Integer.class);
        assertThat(v5Aplicada).isEqualTo(1);

        String sufijo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Usuario admin = new Usuario();
        admin.setUsername("admin_pago_" + sufijo);
        admin.setPassword("$2a$10$hashDePruebaNoReal");
        admin.setNombres("Admin");
        admin.setApellidos("Pagos");
        admin.setEmail("admin_pago_" + sufijo + "@test.pe");
        admin.setRoles(Set.of(roles.findByNombre("ADMINISTRADOR").orElseThrow()));
        usuarios.saveAndFlush(admin);
        autenticarComo(admin);

        Ciudadano ciudadano = new Ciudadano();
        ciudadano.setTipoDocumento(TipoDocumento.DNI);
        ciudadano.setNumeroDocumento(String.valueOf(10000000 + Math.abs(sufijo.hashCode() % 89999999)));
        ciudadano.setNombres("Lucia");
        ciudadano.setApellidos("Huaman");
        ciudadano.setUsuario(admin);
        ciudadano.setActivo(true);
        ciudadanos.saveAndFlush(ciudadano);

        Establecimiento est = new Establecimiento();
        est.setNombreComercial("Comercial Ayacucho " + sufijo);
        est.setDireccion("Av. Ramón Castilla 120");
        est.setGiroNegocio("Bodega");
        est.setAreaMetrosCuadrados(new BigDecimal("45.00"));
        est.setCiudadano(ciudadano);
        est.setActivo(true);
        establecimientos.saveAndFlush(est);

        TipoTramiteResponse tramite = tipoTramiteService.crear(
                new TipoTramiteRequest("TRAM-PAGO-" + sufijo, "Licencia General " + sufijo, "Trámite", true));

        SolicitudResponse borrador = solicitudService.crear(
                new SolicitudRequest(ciudadano.getId(), est.getId(), tramite.id(), null, "Solicitud con pago"));
        solicitudService.presentar(borrador.id(), new PresentarSolicitudRequest("Presentación"));

        // Registrar pago válido
        RegistrarPagoRequest reqPago = new RegistrarPagoRequest(
                borrador.id(),
                "OP-INT-" + sufijo,
                "Derecho de trámite de Licencia de Funcionamiento",
                new BigDecimal("118.50"),
                "PEN",
                LocalDateTime.now().minusMinutes(5),
                "CAJA_MUNICIPAL",
                "Recibo municipal");
        PagoDetalleResponse registrado = pagoService.registrar(reqPago);
        assertThat(registrado.estado()).isEqualTo(EstadoPago.PENDIENTE_VERIFICACION);
        assertThat(registrado.historial()).hasSize(1);

        // Rechazar duplicado por código de operación y medio de pago
        assertThrows(ConflictException.class, () -> pagoService.registrar(reqPago));

        // Verificar pago
        PagoDetalleResponse verificado = pagoService.verificar(
                registrado.id(), new VerificarPagoRequest("Verificado conforme en tesorería"));
        assertThat(verificado.estado()).isEqualTo(EstadoPago.VERIFICADO);
        assertThat(verificado.historial()).hasSize(2);

        // Generar constancia PDF
        ConstanciaPagoDescarga constancia = constanciaPagoService.generarConstanciaPago(registrado.id());
        assertThat(constancia.contenido()).isNotEmpty();

        // Verificar notificaciones internas generadas
        PaginaResponse<NotificacionResponse> misNotificaciones = notificacionService
                .listarMisNotificaciones(PageRequest.of(0, 10));
        assertThat(misNotificaciones.contenido()).isNotEmpty();

        // Consultar auditoría registrada
        PaginaResponse<AuditoriaResponse> auditoriasPago = auditoriaService.listar(
                admin.getId(), "VERIFICAR_PAGO", "Pago", registrado.id(),
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), PageRequest.of(0, 10));
        assertThat(auditoriasPago.contenido()).isNotEmpty();
    }
}
