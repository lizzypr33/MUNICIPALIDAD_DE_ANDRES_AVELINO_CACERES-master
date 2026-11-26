package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.AnularPagoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.RechazarPagoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.RegistrarPagoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.VerificarPagoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.AuditoriaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.NotificacionResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PagoDetalleResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PagoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ResumenPagosResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Auditoria;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Ciudadano;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Establecimiento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.HistorialPago;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Notificacion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Pago;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Rol;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Solicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.TipoTramite;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Usuario;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoPago;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoSolicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoDocumento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoNotificacion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ConflictException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ResourceNotFoundException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.AuditoriaMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.NotificacionMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.PagoMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.AuditoriaRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.CiudadanoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.HistorialPagoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.NotificacionRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.PagoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.SolicitudRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.UsuarioRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.security.CustomUserDetails;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.AuditoriaService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.ConstanciaPagoService.ConstanciaPagoDescarga;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl.AuditoriaServiceImpl;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl.ConstanciaPagoServiceImpl;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl.NotificacionServiceImpl;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl.PagoServiceImpl;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.util.PdfGenerator;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.validation.PagoValidator;

@ExtendWith(MockitoExtension.class)
class PagoNotificacionAuditoriaServiceTests {

    @Mock private PagoRepository pagoRepository;
    @Mock private HistorialPagoRepository historialPagoRepository;
    @Mock private SolicitudRepository solicitudRepository;
    @Mock private CiudadanoRepository ciudadanoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private NotificacionRepository notificacionRepository;
    @Mock private AuditoriaRepository auditoriaRepository;
    @Mock private AuditoriaService auditoriaService;

    private PagoServiceImpl pagoService;
    private ConstanciaPagoServiceImpl constanciaPagoService;
    private NotificacionServiceImpl notificacionService;
    private AuditoriaServiceImpl auditoriaServiceImpl;

    private final PagoValidator pagoValidator = new PagoValidator();
    private final PagoMapper pagoMapper = new PagoMapper();
    private final NotificacionMapper notificacionMapper = new NotificacionMapper();
    private final AuditoriaMapper auditoriaMapper = new AuditoriaMapper();
    private final PdfGenerator pdfGenerator = new PdfGenerator();

    @BeforeEach
    void setUp() {
        pagoService = new PagoServiceImpl(
                pagoRepository,
                historialPagoRepository,
                solicitudRepository,
                ciudadanoRepository,
                usuarioRepository,
                notificacionRepository,
                pagoValidator,
                pagoMapper,
                auditoriaService);
        constanciaPagoService = new ConstanciaPagoServiceImpl(
                pagoService,
                pdfGenerator,
                auditoriaService);
        notificacionService = new NotificacionServiceImpl(
                notificacionRepository,
                usuarioRepository,
                notificacionMapper);
        auditoriaServiceImpl = new AuditoriaServiceImpl(
                auditoriaRepository,
                usuarioRepository,
                auditoriaMapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(Long id, String username, String nombreRol) {
        Rol r = new Rol();
        r.setId(1L);
        r.setNombre(nombreRol);
        Usuario u = new Usuario();
        u.setId(id);
        u.setUsername(username);
        u.setPassword("hash");
        u.setNombres("Usuario");
        u.setApellidos(nombreRol);
        u.setEmail(username + "@muni.gob.pe");
        u.setActivo(true);
        u.setRoles(Set.of(r));

        CustomUserDetails principal = new CustomUserDetails(u);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + nombreRol)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Solicitud crearSolicitudBase(Long id, EstadoSolicitud estado, Long ciudadanoUsuarioId) {
        Usuario usuarioCiudadano = new Usuario();
        usuarioCiudadano.setId(ciudadanoUsuarioId);
        usuarioCiudadano.setUsername("ciudadano1");
        usuarioCiudadano.setNombres("Carlos");
        usuarioCiudadano.setApellidos("Mendoza");

        Ciudadano c = new Ciudadano();
        c.setId(10L);
        c.setTipoDocumento(TipoDocumento.DNI);
        c.setNumeroDocumento("44556677");
        c.setNombres("Carlos");
        c.setApellidos("Mendoza");
        c.setUsuario(usuarioCiudadano);

        Establecimiento e = new Establecimiento();
        e.setId(20L);
        e.setNombreComercial("Botica San Andrés");
        e.setDireccion("Av. Mariscal Cáceres 450");
        e.setAreaMetrosCuadrados(new BigDecimal("68.50"));

        TipoTramite tt = new TipoTramite();
        tt.setId(30L);
        tt.setCodigo("LIC-FUNC-IND");
        tt.setNombre("Licencia de Funcionamiento Indeterminada");

        Usuario revisor = new Usuario();
        revisor.setId(5L);
        revisor.setUsername("funcionario1");
        revisor.setNombres("Pedro");
        revisor.setApellidos("Revisor");

        Solicitud s = new Solicitud();
        s.setId(id);
        s.setNumeroExpediente("EXP-2026-000015");
        s.setCiudadano(c);
        s.setEstablecimiento(e);
        s.setTipoTramite(tt);
        s.setEstado(estado);
        s.setUsuarioResponsable(revisor);
        return s;
    }

    @Test
    void registrarPagoValidoQuedaPendienteVerificacionGeneraHistorialNotificacionYAuditoria() {
        autenticarComo(5L, "funcionario1", "FUNCIONARIO");
        Solicitud sol = crearSolicitudBase(100L, EstadoSolicitud.PRESENTADA, 50L);
        when(solicitudRepository.findWithRelacionesById(100L)).thenReturn(Optional.of(sol));
        when(pagoRepository.existsByCodigoOperacionIgnoreCaseAndMedioPagoIgnoreCaseAndEstadoNotIn(
                eq("OP-2026-00991"), eq("CAJA_MUNICIPAL"), any())).thenReturn(false);

        Usuario actor = new Usuario();
        actor.setId(5L);
        actor.setNombres("Pedro");
        actor.setApellidos("Revisor");
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(actor));

        when(pagoRepository.saveAndFlush(any(Pago.class))).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            p.setId(500L);
            return p;
        });
        when(historialPagoRepository.findByPagoIdOrderByFechaCambioAscIdAsc(500L)).thenReturn(List.of());

        RegistrarPagoRequest req = new RegistrarPagoRequest(
                100L,
                "OP-2026-00991",
                "Derecho de trámite de Licencia de Funcionamiento",
                new BigDecimal("125.50"),
                "PEN",
                LocalDateTime.now().minusMinutes(5),
                "CAJA_MUNICIPAL",
                "Recibo de caja N° 001-04521");

        PagoDetalleResponse resp = pagoService.registrar(req);

        assertThat(resp.id()).isEqualTo(500L);
        assertThat(resp.estado()).isEqualTo(EstadoPago.PENDIENTE_VERIFICACION);
        assertThat(resp.monto()).isEqualByComparingTo(new BigDecimal("125.50"));
        assertThat(resp.moneda()).isEqualTo("PEN");
        assertThat(resp.codigoOperacion()).isEqualTo("OP-2026-00991");

        verify(historialPagoRepository).save(any(HistorialPago.class));
        verify(notificacionRepository, atLeastOnce()).save(any(Notificacion.class));
        verify(auditoriaService).registrar(eq("REGISTRAR_PAGO"), eq("Pago"), eq(500L), anyString());
    }

    @Test
    void rechazarPagoConMontoCeroNegativoMasDeDosDecimalesOMonedaInvalida() {
        autenticarComo(5L, "funcionario1", "FUNCIONARIO");
        Solicitud sol = crearSolicitudBase(100L, EstadoSolicitud.PRESENTADA, 50L);
        when(solicitudRepository.findWithRelacionesById(100L)).thenReturn(Optional.of(sol));

        RegistrarPagoRequest reqCero = new RegistrarPagoRequest(
                100L, "OP-001", "Concepto municipal", BigDecimal.ZERO, "PEN",
                LocalDateTime.now().minusMinutes(5), "CAJA_MUNICIPAL", null);
        assertThatThrownBy(() -> pagoService.registrar(reqCero))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("mayor que cero");

        RegistrarPagoRequest reqNegativo = new RegistrarPagoRequest(
                100L, "OP-002", "Concepto municipal", new BigDecimal("-15.00"), "PEN",
                LocalDateTime.now().minusMinutes(5), "CAJA_MUNICIPAL", null);
        assertThatThrownBy(() -> pagoService.registrar(reqNegativo))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("mayor que cero");

        RegistrarPagoRequest reqTresDecimales = new RegistrarPagoRequest(
                100L, "OP-003", "Concepto municipal", new BigDecimal("50.125"), "PEN",
                LocalDateTime.now().minusMinutes(5), "CAJA_MUNICIPAL", null);
        assertThatThrownBy(() -> pagoService.registrar(reqTresDecimales))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("dos decimales");

        RegistrarPagoRequest reqMonedaInvalida = new RegistrarPagoRequest(
                100L, "OP-004", "Concepto municipal", new BigDecimal("50.00"), "USD",
                LocalDateTime.now().minusMinutes(5), "CAJA_MUNICIPAL", null);
        assertThatThrownBy(() -> pagoService.registrar(reqMonedaInvalida))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PEN");
    }

    @Test
    void rechazarPagoConDatosSensiblesDeTarjetaOCvv() {
        autenticarComo(5L, "funcionario1", "FUNCIONARIO");
        Solicitud sol = crearSolicitudBase(100L, EstadoSolicitud.PRESENTADA, 50L);
        when(solicitudRepository.findWithRelacionesById(100L)).thenReturn(Optional.of(sol));

        RegistrarPagoRequest reqConPan = new RegistrarPagoRequest(
                100L, "4557880011223344", "Concepto municipal", new BigDecimal("80.00"), "PEN",
                LocalDateTime.now().minusMinutes(5), "TRANSFERENCIA", "Pago con tarjeta");
        assertThatThrownBy(() -> pagoService.registrar(reqConPan))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("números completos de tarjeta");

        RegistrarPagoRequest reqConCvv = new RegistrarPagoRequest(
                100L, "OP-2026-100", "Concepto municipal", new BigDecimal("80.00"), "PEN",
                LocalDateTime.now().minusMinutes(5), "TRANSFERENCIA", "Operación aprobada cvv: 123");
        assertThatThrownBy(() -> pagoService.registrar(reqConCvv))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CVV");
    }

    @Test
    void rechazarPagoAsociadoASolicitudInexistenteORechazada() {
        autenticarComo(5L, "funcionario1", "FUNCIONARIO");
        when(solicitudRepository.findWithRelacionesById(999L)).thenReturn(Optional.empty());

        RegistrarPagoRequest reqInexistente = new RegistrarPagoRequest(
                999L, "OP-999", "Concepto municipal", new BigDecimal("90.00"), "PEN",
                LocalDateTime.now().minusMinutes(5), "CAJA_MUNICIPAL", null);
        assertThatThrownBy(() -> pagoService.registrar(reqInexistente))
                .isInstanceOf(ResourceNotFoundException.class);

        Solicitud solRechazada = crearSolicitudBase(101L, EstadoSolicitud.RECHAZADA, 50L);
        when(solicitudRepository.findWithRelacionesById(101L)).thenReturn(Optional.of(solRechazada));

        RegistrarPagoRequest reqRechazada = new RegistrarPagoRequest(
                101L, "OP-101", "Concepto municipal", new BigDecimal("90.00"), "PEN",
                LocalDateTime.now().minusMinutes(5), "CAJA_MUNICIPAL", null);
        assertThatThrownBy(() -> pagoService.registrar(reqRechazada))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("RECHAZADA");
    }

    @Test
    void rechazarPagoDuplicadoPorCodigoOperacionYMedioPago() {
        autenticarComo(5L, "funcionario1", "FUNCIONARIO");
        Solicitud sol = crearSolicitudBase(100L, EstadoSolicitud.PRESENTADA, 50L);
        when(solicitudRepository.findWithRelacionesById(100L)).thenReturn(Optional.of(sol));
        when(pagoRepository.existsByCodigoOperacionIgnoreCaseAndMedioPagoIgnoreCaseAndEstadoNotIn(
                eq("OP-DUP-001"), eq("BANCO_NACION"), any())).thenReturn(true);

        RegistrarPagoRequest req = new RegistrarPagoRequest(
                100L, "OP-DUP-001", "Derecho de trámite", new BigDecimal("100.00"), "PEN",
                LocalDateTime.now().minusMinutes(5), "BANCO_NACION", null);

        assertThatThrownBy(() -> pagoService.registrar(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("OP-DUP-001");
    }

    @Test
    void verificarPagoPendienteActualizaEstadoRegistraVerificadorNotificaYAudita() {
        autenticarComo(5L, "funcionario1", "FUNCIONARIO");
        Solicitud sol = crearSolicitudBase(100L, EstadoSolicitud.PRESENTADA, 50L);

        Pago pago = new Pago();
        pago.setId(500L);
        pago.setSolicitud(sol);
        pago.setConcepto("Derecho de trámite");
        pago.setMonto(new BigDecimal("120.00"));
        pago.setMoneda("PEN");
        pago.setMedioPago("CAJA_MUNICIPAL");
        pago.setCodigoOperacion("OP-500");
        pago.setEstado(EstadoPago.PENDIENTE_VERIFICACION);
        pago.setFechaPago(LocalDateTime.now().minusHours(1));

        Usuario verificador = new Usuario();
        verificador.setId(5L);
        verificador.setNombres("Pedro");
        verificador.setApellidos("Revisor");

        when(pagoRepository.findWithRelacionesById(500L)).thenReturn(Optional.of(pago));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(verificador));
        when(pagoRepository.saveAndFlush(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
        when(historialPagoRepository.findByPagoIdOrderByFechaCambioAscIdAsc(500L)).thenReturn(List.of());

        PagoDetalleResponse resp = pagoService.verificar(500L, new VerificarPagoRequest("Verificado conforme con voucher"));

        assertThat(resp.estado()).isEqualTo(EstadoPago.VERIFICADO);
        assertThat(resp.usuarioVerificadorId()).isEqualTo(5L);
        assertThat(resp.fechaVerificacion()).isNotNull();

        verify(historialPagoRepository).save(any(HistorialPago.class));
        verify(notificacionRepository).save(any(Notificacion.class));
        verify(auditoriaService).registrar(eq("VERIFICAR_PAGO"), eq("Pago"), eq(500L), anyString());
    }

    @Test
    void impedirVerificarPagoYaVerificado() {
        autenticarComo(5L, "funcionario1", "FUNCIONARIO");
        Solicitud sol = crearSolicitudBase(100L, EstadoSolicitud.PRESENTADA, 50L);

        Pago pago = new Pago();
        pago.setId(500L);
        pago.setSolicitud(sol);
        pago.setEstado(EstadoPago.VERIFICADO);

        when(pagoRepository.findWithRelacionesById(500L)).thenReturn(Optional.of(pago));

        assertThatThrownBy(() -> pagoService.verificar(500L, new VerificarPagoRequest("Intento doble")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("VERIFICADO");
    }

    @Test
    void rechazarPagoPendienteExigeMotivoYTransicionaARechazado() {
        autenticarComo(5L, "funcionario1", "FUNCIONARIO");
        Solicitud sol = crearSolicitudBase(100L, EstadoSolicitud.PRESENTADA, 50L);

        Pago pago = new Pago();
        pago.setId(501L);
        pago.setSolicitud(sol);
        pago.setConcepto("Derecho de trámite");
        pago.setMonto(new BigDecimal("120.00"));
        pago.setMoneda("PEN");
        pago.setEstado(EstadoPago.PENDIENTE_VERIFICACION);

        when(pagoRepository.findWithRelacionesById(501L)).thenReturn(Optional.of(pago));

        assertThatThrownBy(() -> pagoService.rechazar(501L, new RechazarPagoRequest("   ", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("motivo");

        Usuario verificador = new Usuario();
        verificador.setId(5L);
        verificador.setNombres("Pedro");
        verificador.setApellidos("Revisor");
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(verificador));
        when(pagoRepository.saveAndFlush(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
        when(historialPagoRepository.findByPagoIdOrderByFechaCambioAscIdAsc(501L)).thenReturn(List.of());

        PagoDetalleResponse resp = pagoService.rechazar(501L,
                new RechazarPagoRequest("Código de operación no figura en extracto bancario", "Revisado en caja"));

        assertThat(resp.estado()).isEqualTo(EstadoPago.RECHAZADO);
        assertThat(resp.motivoRechazoAnulacion()).isEqualTo("Código de operación no figura en extracto bancario");
        verify(auditoriaService).registrar(eq("RECHAZAR_PAGO"), eq("Pago"), eq(501L), anyString());
    }

    @Test
    void anularPagoPendienteYControlarAnulacionExcepcionalDePagoVerificadoPorRol() {
        Solicitud sol = crearSolicitudBase(100L, EstadoSolicitud.PRESENTADA, 50L);

        Pago pagoVerificado = new Pago();
        pagoVerificado.setId(502L);
        pagoVerificado.setSolicitud(sol);
        pagoVerificado.setConcepto("Derecho de trámite");
        pagoVerificado.setMonto(new BigDecimal("120.00"));
        pagoVerificado.setMoneda("PEN");
        pagoVerificado.setEstado(EstadoPago.VERIFICADO);

        when(pagoRepository.findWithRelacionesById(502L)).thenReturn(Optional.of(pagoVerificado));

        // FUNCIONARIO no puede anular un pago ya VERIFICADO
        autenticarComo(5L, "funcionario1", "FUNCIONARIO");
        assertThatThrownBy(() -> pagoService.anular(502L, new AnularPagoRequest("Error de digitación", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ADMINISTRADOR o SUPERVISOR");

        // SUPERVISOR sí puede anular excepcionalmente un pago VERIFICADO con motivo obligatorio
        autenticarComo(2L, "supervisor1", "SUPERVISOR");
        Usuario sup = new Usuario();
        sup.setId(2L);
        sup.setNombres("Ana");
        sup.setApellidos("Supervisora");
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(sup));
        when(pagoRepository.saveAndFlush(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
        when(historialPagoRepository.findByPagoIdOrderByFechaCambioAscIdAsc(502L)).thenReturn(List.of());

        PagoDetalleResponse resp = pagoService.anular(502L,
                new AnularPagoRequest("Anulación autorizada por Resolución Gerencial N° 012-2026", null));

        assertThat(resp.estado()).isEqualTo(EstadoPago.ANULADO);
        assertThat(resp.motivoRechazoAnulacion()).contains("Resolución Gerencial");
        verify(auditoriaService).registrar(eq("ANULAR_PAGO"), eq("Pago"), eq(502L), anyString());
    }

    @Test
    void ciudadanoNoPuedeVerificarRechazarAnularNiConsultarPagosDeOtroCiudadano() {
        autenticarComo(99L, "ciudadanoAjeno", "CIUDADANO");
        Solicitud solDeOtro = crearSolicitudBase(100L, EstadoSolicitud.PRESENTADA, 50L);

        Pago pago = new Pago();
        pago.setId(500L);
        pago.setSolicitud(solDeOtro);
        pago.setEstado(EstadoPago.PENDIENTE_VERIFICACION);

        when(pagoRepository.findWithRelacionesById(500L)).thenReturn(Optional.of(pago));
        when(ciudadanoRepository.findByUsuarioId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pagoService.obtener(500L))
                .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> pagoService.verificar(500L, new VerificarPagoRequest("Intento ciudadano")))
                .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> pagoService.rechazar(500L, new RechazarPagoRequest("Motivo", null)))
                .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> pagoService.anular(500L, new AnularPagoRequest("Motivo", null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void generarConstanciaPdfDePagoProduceDocumentoPdfValidoYRegistraAuditoria() {
        autenticarComo(50L, "ciudadano1", "CIUDADANO");
        Solicitud sol = crearSolicitudBase(100L, EstadoSolicitud.PRESENTADA, 50L);

        Pago pago = new Pago();
        pago.setId(500L);
        pago.setSolicitud(sol);
        pago.setConcepto("Derecho de trámite de Licencia de Funcionamiento");
        pago.setMonto(new BigDecimal("125.50"));
        pago.setMoneda("PEN");
        pago.setMedioPago("CAJA_MUNICIPAL");
        pago.setCodigoOperacion("OP-2026-00991");
        pago.setEstado(EstadoPago.VERIFICADO);
        pago.setFechaPago(LocalDateTime.now().minusDays(1));
        pago.setFechaVerificacion(LocalDateTime.now());

        when(pagoRepository.findWithRelacionesById(500L)).thenReturn(Optional.of(pago));
        when(ciudadanoRepository.findByUsuarioId(50L)).thenReturn(Optional.of(sol.getCiudadano()));
        when(historialPagoRepository.findByPagoIdOrderByFechaCambioAscIdAsc(500L)).thenReturn(List.of());

        ConstanciaPagoDescarga descarga = constanciaPagoService.generarConstanciaPago(500L);

        assertThat(descarga.nombreArchivo()).isEqualTo("constancia-pago-500.pdf");
        assertThat(descarga.tipoMime()).isEqualTo("application/pdf");
        assertThat(descarga.contenido()).isNotEmpty();
        String cabecera = new String(descarga.contenido(), 0, 4, StandardCharsets.US_ASCII);
        assertThat(cabecera).isEqualTo("%PDF");

        verify(auditoriaService).registrar(eq("GENERAR_CONSTANCIA_PAGO"), eq("Pago"), eq(500L), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void listarPagosYResumenConsolidadoCalculanTotalesPorEstado() {
        autenticarComo(1L, "admin", "ADMINISTRADOR");
        Solicitud sol = crearSolicitudBase(100L, EstadoSolicitud.PRESENTADA, 50L);

        Pago p1 = new Pago();
        p1.setId(1L);
        p1.setSolicitud(sol);
        p1.setConcepto("Concepto 1");
        p1.setMonto(new BigDecimal("100.00"));
        p1.setMoneda("PEN");
        p1.setEstado(EstadoPago.VERIFICADO);

        Pago p2 = new Pago();
        p2.setId(2L);
        p2.setSolicitud(sol);
        p2.setConcepto("Concepto 2");
        p2.setMonto(new BigDecimal("50.50"));
        p2.setMoneda("PEN");
        p2.setEstado(EstadoPago.PENDIENTE_VERIFICACION);

        when(pagoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p1, p2)));
        when(pagoRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(p1, p2));

        PaginaResponse<PagoResponse> pagina = pagoService.listar(
                100L, null, null, null, null, LocalDate.now().minusDays(7), LocalDate.now(), PageRequest.of(0, 10));
        assertThat(pagina.contenido()).hasSize(2);

        ResumenPagosResponse resumen = pagoService.obtenerResumen(
                LocalDate.now().minusDays(7), LocalDate.now(), null, null, null);
        assertThat(resumen.cantidadTotalPagos()).isEqualTo(2);
        assertThat(resumen.totalPagosVerificados()).isEqualTo(1);
        assertThat(resumen.cantidadPagosPendientes()).isEqualTo(1);
        assertThat(resumen.montoTotalVerificado()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void notificacionesPropiasLecturaContadorYBloqueoDeNotificacionesAjenas() {
        autenticarComo(50L, "ciudadano1", "CIUDADANO");

        Usuario uPropio = new Usuario();
        uPropio.setId(50L);
        uPropio.setUsername("ciudadano1");

        Notificacion nPropia = new Notificacion();
        nPropia.setId(10L);
        nPropia.setUsuario(uPropio);
        nPropia.setTitulo("Pago verificado");
        nPropia.setMensaje("Su pago ha sido verificado.");
        nPropia.setTipo(TipoNotificacion.PAGO);
        nPropia.setLeida(false);
        nPropia.setFechaCreacion(LocalDateTime.now());

        when(notificacionRepository.findByUsuarioIdOrderByFechaCreacionDesc(eq(50L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(nPropia)));
        when(notificacionRepository.findByUsuarioIdAndLeidaFalseOrderByFechaCreacionDesc(eq(50L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(nPropia)));
        when(notificacionRepository.countByUsuarioIdAndLeidaFalse(50L)).thenReturn(1L);
        when(notificacionRepository.findWithUsuarioById(10L)).thenReturn(Optional.of(nPropia));
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(notificacionService.listarMisNotificaciones(PageRequest.of(0, 10)).contenido()).hasSize(1);
        assertThat(notificacionService.listarMisNoLeidas(PageRequest.of(0, 10)).contenido()).hasSize(1);
        assertThat(notificacionService.contarMisNoLeidas()).isEqualTo(1L);

        NotificacionResponse marcada = notificacionService.marcarComoLeida(10L);
        assertThat(marcada.leida()).isTrue();
        assertThat(marcada.fechaLectura()).isNotNull();

        // Marcar todas como leídas
        nPropia.setLeida(false);
        when(notificacionRepository.findByUsuarioIdAndLeidaFalse(50L)).thenReturn(List.of(nPropia));
        int totalActualizadas = notificacionService.marcarTodasComoLeidas();
        assertThat(totalActualizadas).isEqualTo(1);

        // Impedir leer o modificar notificación de otro usuario
        Usuario uAjeno = new Usuario();
        uAjeno.setId(99L);
        uAjeno.setUsername("otro");

        Notificacion nAjena = new Notificacion();
        nAjena.setId(20L);
        nAjena.setUsuario(uAjeno);
        nAjena.setTitulo("Privada");
        nAjena.setMensaje("De otro usuario");
        nAjena.setTipo(TipoNotificacion.INFORMATIVA);

        when(notificacionRepository.findWithUsuarioById(20L)).thenReturn(Optional.of(nAjena));

        assertThatThrownBy(() -> notificacionService.obtenerMiNotificacion(20L))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> notificacionService.marcarComoLeida(20L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void auditoriaSanitizaDatosSensiblesYPermiteConsultaConFiltros() {
        autenticarComo(1L, "admin", "ADMINISTRADOR");
        Usuario admin = new Usuario();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setNombres("Admin");
        admin.setApellidos("Sistema");
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        auditoriaServiceImpl.registrar("LOGIN", "Usuario", 1L,
                "Intento con password=secreto123 y token: abcdef");

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(captor.capture());
        Auditoria guardada = captor.getValue();
        assertThat(guardada.getDescripcion()).doesNotContain("secreto123");
        assertThat(guardada.getDescripcion()).doesNotContain("abcdef");
        assertThat(guardada.getDescripcion()).contains("[REDACTADO]");

        guardada.setId(77L);
        guardada.setFechaOperacion(LocalDateTime.now());
        when(auditoriaRepository.findWithUsuarioById(77L)).thenReturn(Optional.of(guardada));
        when(auditoriaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(guardada)));

        AuditoriaResponse detalle = auditoriaServiceImpl.obtener(77L);
        assertThat(detalle.id()).isEqualTo(77L);
        assertThat(detalle.accion()).isEqualTo("LOGIN");

        PaginaResponse<AuditoriaResponse> lista = auditoriaServiceImpl.listar(
                1L, "LOGIN", "Usuario", 1L, LocalDate.now().minusDays(1), LocalDate.now(), PageRequest.of(0, 10));
        assertThat(lista.contenido()).hasSize(1);
    }
}
