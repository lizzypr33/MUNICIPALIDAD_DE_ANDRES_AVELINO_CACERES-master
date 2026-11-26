package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.CambiarEstadoLicenciaRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.EmitirLicenciaRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ProgramarInspeccionRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ReprogramarInspeccionRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ResultadoInspeccionRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.InspeccionDetalleResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.LicenciaDetalleResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.VerificacionLicenciaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Ciudadano;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Establecimiento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.HistorialLicencia;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Inspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Licencia;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Notificacion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Rol;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Solicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.TipoTramite;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Usuario;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoInspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoLicencia;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoSolicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.ResultadoInspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoDocumento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ConflictException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ResourceNotFoundException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.InspeccionMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.LicenciaMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.CiudadanoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.DocumentoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.HistorialLicenciaRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.InspeccionRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.LicenciaRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.NotificacionRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.SolicitudRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.TipoTramiteRequisitoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.UsuarioRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.security.CustomUserDetails;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.AuditoriaService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.CertificadoService.CertificadoDescarga;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl.CertificadoServiceImpl;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl.InspeccionServiceImpl;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl.LicenciaServiceImpl;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl.VerificacionLicenciaServiceImpl;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.util.CodigoLicenciaGenerator;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.util.PdfGenerator;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.validation.InspeccionValidator;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.validation.LicenciaValidator;

/**
 * Pruebas unitarias de servicios, validadores, generador de códigos, PDF (OpenPDF) y QR (ZXing) para PROMPT 06.
 */
@ExtendWith(MockitoExtension.class)
class LicenciaInspeccionServiceTests {

    @Mock private InspeccionRepository inspecciones;
    @Mock private LicenciaRepository licencias;
    @Mock private HistorialLicenciaRepository historiales;
    @Mock private SolicitudRepository solicitudes;
    @Mock private DocumentoRepository documentos;
    @Mock private TipoTramiteRequisitoRepository tipoTramiteRequisitos;
    @Mock private CiudadanoRepository ciudadanos;
    @Mock private UsuarioRepository usuarios;
    @Mock private NotificacionRepository notificaciones;
    @Mock private AuditoriaService auditoria;

    private InspeccionValidator inspeccionValidator;
    private LicenciaValidator licenciaValidator;
    private CodigoLicenciaGenerator codigoGenerator;
    private PdfGenerator pdfGenerator;
    private InspeccionMapper inspeccionMapper;
    private LicenciaMapper licenciaMapper;

    private InspeccionServiceImpl inspeccionService;
    private LicenciaServiceImpl licenciaService;
    private CertificadoServiceImpl certificadoService;
    private VerificacionLicenciaServiceImpl verificacionService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        inspeccionValidator = new InspeccionValidator();
        licenciaValidator = new LicenciaValidator();
        codigoGenerator = new CodigoLicenciaGenerator();
        pdfGenerator = new PdfGenerator();
        inspeccionMapper = new InspeccionMapper();
        licenciaMapper = new LicenciaMapper();

        inspeccionService = new InspeccionServiceImpl(
                inspecciones, solicitudes, usuarios, ciudadanos, licencias, notificaciones,
                inspeccionValidator, inspeccionMapper, auditoria);

        licenciaService = new LicenciaServiceImpl(
                licencias, historiales, solicitudes, inspecciones, documentos,
                tipoTramiteRequisitos, ciudadanos, usuarios, notificaciones,
                licenciaValidator, codigoGenerator, licenciaMapper, auditoria);

        certificadoService = new CertificadoServiceImpl(
                licenciaService, pdfGenerator, auditoria, "https://licencias.municaceres.gob.pe");

        verificacionService = new VerificacionLicenciaServiceImpl(
                licencias, codigoGenerator, licenciaMapper);

        lenient().when(notificaciones.save(any(Notificacion.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Solicitud crearSolicitudBase(EstadoSolicitud estado, boolean requiereInspeccion) {
        Ciudadano c = new Ciudadano();
        c.setId(10L);
        c.setTipoDocumento(TipoDocumento.DNI);
        c.setNumeroDocumento("44556677");
        c.setNombres("Carlos");
        c.setApellidos("Mendoza");
        c.setEmail("carlos.privado@correo.pe");
        c.setTelefono("999111222");
        c.setActivo(true);

        Establecimiento e = new Establecimiento();
        e.setId(20L);
        e.setNombreComercial("Botica San Andrés");
        e.setRazonSocial("Inversiones San Andrés SAC");
        e.setRuc("20601234561");
        e.setDireccion("Av. Mariscal Cáceres 450");
        e.setGiroNegocio("Botica y Farmacia");
        e.setAreaMetrosCuadrados(new BigDecimal("68.50"));
        e.setCiudadano(c);
        e.setActivo(true);

        TipoTramite t = new TipoTramite();
        t.setId(30L);
        t.setCodigo("LIC-FUNC-IND");
        t.setNombre("Licencia de Funcionamiento Indeterminada");
        t.setRequiereInspeccion(requiereInspeccion);
        t.setActivo(true);

        Solicitud s = new Solicitud();
        s.setId(100L);
        s.setNumeroExpediente("EXP-2026-000015");
        s.setCiudadano(c);
        s.setEstablecimiento(e);
        s.setTipoTramite(t);
        s.setEstado(estado);
        return s;
    }

    private Usuario crearUsuarioConRol(Long id, String username, String nombreRol) {
        Rol r = new Rol();
        r.setId(1L);
        r.setNombre(nombreRol);
        Usuario u = new Usuario();
        u.setId(id);
        u.setUsername(username);
        u.setNombres("Inspector");
        u.setApellidos("Municipal");
        u.setActivo(true);
        u.setRoles(Set.of(r));
        return u;
    }

    private void autenticarConRol(Long userId, String username, String authority) {
        String nombreRol = authority.startsWith("ROLE_") ? authority.substring(5) : authority;
        Usuario u = crearUsuarioConRol(userId, username, nombreRol);
        CustomUserDetails principal = new CustomUserDetails(u);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    void cicloCompletoDeInspeccionYProteccionDeResultadosDefinitivos() {
        autenticarConRol(5L, "func.inspector", "ROLE_FUNCIONARIO");
        Solicitud solicitud = crearSolicitudBase(EstadoSolicitud.EN_REVISION, true);
        Usuario inspector = crearUsuarioConRol(5L, "func.inspector", "FUNCIONARIO");
        Usuario ciudadanoSinPermiso = crearUsuarioConRol(9L, "ciudadano.user", "CIUDADANO");

        when(solicitudes.findWithRelacionesById(100L)).thenReturn(Optional.of(solicitud));
        when(solicitudes.findWithRelacionesById(999L)).thenReturn(Optional.empty());
        when(usuarios.findWithRolesById(5L)).thenReturn(Optional.of(inspector));
        when(usuarios.findWithRolesById(9L)).thenReturn(Optional.of(ciudadanoSinPermiso));
        when(inspecciones.save(any(Inspeccion.class))).thenAnswer(inv -> {
            Inspeccion i = inv.getArgument(0);
            i.setId(50L);
            return i;
        });

        LocalDateTime fechaInicial = LocalDateTime.now().plusDays(2);

        // 1. Rechazo de solicitud inexistente
        assertThrows(ResourceNotFoundException.class, () -> inspeccionService.programar(
                new ProgramarInspeccionRequest(999L, 5L, fechaInicial, "Visita inicial")));

        // 2. Rechazo de inspector no autorizado (rol CIUDADANO)
        assertThrows(BusinessException.class, () -> inspeccionService.programar(
                new ProgramarInspeccionRequest(100L, 9L, fechaInicial, "Visita inválida")));

        // 3. Programación correcta
        InspeccionDetalleResponse programada = inspeccionService.programar(
                new ProgramarInspeccionRequest(100L, 5L, fechaInicial, "Verificar extintores y aforo"));
        assertThat(programada.id()).isEqualTo(50L);
        assertThat(programada.estado()).isEqualTo(EstadoInspeccion.PROGRAMADA);

        Inspeccion entidad = new Inspeccion();
        entidad.setId(50L);
        entidad.setSolicitud(solicitud);
        entidad.setInspector(inspector);
        entidad.setFechaProgramada(fechaInicial);
        entidad.setEstado(EstadoInspeccion.PROGRAMADA);
        when(inspecciones.findWithRelacionesById(50L)).thenReturn(Optional.of(entidad));

        // 4. Reprogramación válida
        LocalDateTime nuevaFecha = fechaInicial.plusDays(1);
        InspeccionDetalleResponse reprogramada = inspeccionService.reprogramar(
                50L, new ReprogramarInspeccionRequest(nuevaFecha, 5L, "Reprogramación por lluvias intensas"));
        assertThat(reprogramada.fechaProgramada()).isEqualTo(nuevaFecha);
        assertThat(reprogramada.motivoReprogramacion()).contains("lluvias intensas");

        // 5. Inicio de inspección
        InspeccionDetalleResponse enProceso = inspeccionService.iniciar(50L);
        assertThat(enProceso.estado()).isEqualTo(EstadoInspeccion.EN_PROCESO);

        // 6. Rechazo de reprogramación cuando ya está EN_PROCESO
        assertThrows(ConflictException.class, () -> inspeccionService.reprogramar(
                50L, new ReprogramarInspeccionRequest(nuevaFecha.plusDays(2), 5L, "Intento inválido")));

        // 7. Registro de resultado desfavorable y luego intento de sobrescritura silenciosa
        when(licencias.existsBySolicitudId(100L)).thenReturn(false);
        InspeccionDetalleResponse finalizada = inspeccionService.registrarResultado(
                50L, new ResultadoInspeccionRequest(
                        ResultadoInspeccion.DESFAVORABLE, LocalDateTime.now(),
                        "Extintor vencido", "Recargar extintor antes de nueva visita"));
        assertThat(finalizada.estado()).isEqualTo(EstadoInspeccion.FINALIZADA);
        assertThat(finalizada.resultado()).isEqualTo(ResultadoInspeccion.DESFAVORABLE);

        // Protección de resultados definitivos: rechaza modificación silenciosa sin justificación ni rol supervisor
        assertThrows(ConflictException.class, () -> inspeccionService.registrarResultado(
                50L, new ResultadoInspeccionRequest(
                        ResultadoInspeccion.FAVORABLE, LocalDateTime.now(),
                        "Intento de cambio silencioso", null)));

        // Con rol SUPERVISOR y justificación administrativa expresa, conserva el historial y rectifica
        autenticarConRol(2L, "sup.municipal", "ROLE_SUPERVISOR");
        InspeccionDetalleResponse rectificada = inspeccionService.registrarResultado(
                50L, new ResultadoInspeccionRequest(
                        ResultadoInspeccion.FAVORABLE, LocalDateTime.now(),
                        "Cumple condiciones tras verificación física de acta",
                        "Mantener señalización visible",
                        "Error material de transcripción de acta N° 045"));
        assertThat(rectificada.resultado()).isEqualTo(ResultadoInspeccion.FAVORABLE);
        assertThat(rectificada.observaciones()).contains("Rectificación autorizada de resultado previo (DESFAVORABLE)");
    }

    @Test
    void emisionSuspensionReactivacionRevocacionEHistorialDeLicencia() {
        autenticarConRol(1L, "admin.municipal", "ROLE_ADMINISTRADOR");
        Solicitud solicitud = crearSolicitudBase(EstadoSolicitud.EN_REVISION, true);

        Inspeccion inspFavorable = new Inspeccion();
        inspFavorable.setId(50L);
        inspFavorable.setSolicitud(solicitud);
        inspFavorable.setEstado(EstadoInspeccion.FINALIZADA);
        inspFavorable.setResultado(ResultadoInspeccion.FAVORABLE);

        List<HistorialLicencia> historialGuardado = new ArrayList<>();
        when(solicitudes.findWithRelacionesById(100L)).thenReturn(Optional.of(solicitud));
        when(inspecciones.findBySolicitudId(100L)).thenReturn(List.of(inspFavorable));
        when(documentos.findBySolicitudIdAndActivoTrueOrderByFechaSubidaAscIdAsc(100L)).thenReturn(List.of());
        when(tipoTramiteRequisitos.findByTipoTramiteIdOrderByOrdenVisualizacionAsc(30L)).thenReturn(List.of());
        when(historiales.save(any(HistorialLicencia.class))).thenAnswer(inv -> {
            HistorialLicencia h = inv.getArgument(0);
            h.setId((long) (historialGuardado.size() + 1));
            h.setFechaCambio(LocalDateTime.now());
            historialGuardado.add(h);
            return h;
        });
        when(historiales.findByLicenciaIdOrderByFechaCambioAscIdAsc(70L)).thenAnswer(inv -> historialGuardado);

        // 1. Rechazo de solicitud NO aprobada
        when(licencias.existsBySolicitudId(100L)).thenReturn(false);
        assertThrows(ConflictException.class,
                () -> licenciaService.emitir(new EmitirLicenciaRequest(100L, "Intento antes de aprobar")));

        // 2. Aprobar solicitud y emitir licencia
        solicitud.setEstado(EstadoSolicitud.APROBADA);
        when(licencias.obtenerSiguienteCorrelativoLicencia()).thenReturn(1L);
        when(licencias.saveAndFlush(any(Licencia.class))).thenAnswer(inv -> {
            Licencia l = inv.getArgument(0);
            l.setId(70L);
            return l;
        });

        LicenciaDetalleResponse emitida = licenciaService.emitir(
                new EmitirLicenciaRequest(100L, null, "Emisión conforme a inspección técnica favorable"));
        String codigoEsperado = "LIC-" + Year.now().getValue() + "-000001";
        assertThat(emitida.codigoLicencia()).isEqualTo(codigoEsperado);
        assertThat(emitida.estado()).isEqualTo(EstadoLicencia.VIGENTE);
        assertThat(emitida.fechaVencimiento()).isNull();
        assertThat(emitida.tokenVerificacion()).hasSizeGreaterThanOrEqualTo(32);

        // 3. Rechazo de emisión duplicada
        when(licencias.existsBySolicitudId(100L)).thenReturn(true);
        assertThrows(ConflictException.class,
                () -> licenciaService.emitir(new EmitirLicenciaRequest(100L, "Emisión duplicada")));

        // Preparar entidad para suspensión, reactivación y revocación
        Licencia entidadLicencia = new Licencia();
        entidadLicencia.setId(70L);
        entidadLicencia.setCodigoLicencia(codigoEsperado);
        entidadLicencia.setTokenVerificacion(emitida.tokenVerificacion());
        entidadLicencia.setSolicitud(solicitud);
        entidadLicencia.setFechaEmision(LocalDate.now());
        entidadLicencia.setEstado(EstadoLicencia.VIGENTE);
        when(licencias.findWithRelacionesById(70L)).thenReturn(Optional.of(entidadLicencia));

        // 4. Suspensión
        LicenciaDetalleResponse suspendida = licenciaService.suspender(
                70L, new CambiarEstadoLicenciaRequest("Incumplimiento temporal de medidas de seguridad"));
        assertThat(suspendida.estado()).isEqualTo(EstadoLicencia.SUSPENDIDA);

        // 5. Reactivación
        LicenciaDetalleResponse reactivada = licenciaService.reactivar(
                70L, new CambiarEstadoLicenciaRequest("Subsanación verificada mediante acta municipal"));
        assertThat(reactivada.estado()).isEqualTo(EstadoLicencia.VIGENTE);

        // 6. Revocación
        LicenciaDetalleResponse revocada = licenciaService.revocar(
                70L, new CambiarEstadoLicenciaRequest("Resolución Gerencial N° 120-2026-MDAACD"));
        assertThat(revocada.estado()).isEqualTo(EstadoLicencia.REVOCADA);
        assertThat(revocada.historial()).hasSize(4);

        // No se puede reactivar una licencia REVOCADA
        assertThrows(ConflictException.class, () -> licenciaService.reactivar(
                70L, new CambiarEstadoLicenciaRequest("Intento inválido desde REVOCADA")));
    }

    @Test
    void generacionConcurrenteDeCodigosDeLicenciaSinColisiones() throws Exception {
        AtomicLong secuencia = new AtomicLong(0L);
        int hilos = 16;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch listo = new CountDownLatch(hilos);
        CountDownLatch inicio = new CountDownLatch(1);
        Set<String> codigos = Collections.synchronizedSet(new java.util.HashSet<>());
        Set<String> tokens = Collections.synchronizedSet(new java.util.HashSet<>());

        for (int i = 0; i < hilos; i++) {
            pool.submit(() -> {
                listo.countDown();
                try {
                    inicio.await();
                    long corr = secuencia.incrementAndGet();
                    codigos.add(codigoGenerator.formatearCodigoLicencia(corr));
                    tokens.add(codigoGenerator.generarTokenVerificacion());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        listo.await(5, TimeUnit.SECONDS);
        inicio.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        assertThat(codigos).hasSize(hilos);
        assertThat(tokens).hasSize(hilos);
    }

    @Test
    void generacionDeCertificadoPdfValidoYDecodificacionDeCodigoQr() throws Exception {
        autenticarConRol(1L, "admin.municipal", "ROLE_ADMINISTRADOR");
        Solicitud solicitud = crearSolicitudBase(EstadoSolicitud.APROBADA, false);

        Licencia l = new Licencia();
        l.setId(85L);
        l.setCodigoLicencia("LIC-2026-000085");
        l.setTokenVerificacion("a1b2c3d4e5f60718293a4b5c6d7e8f9012345678");
        l.setSolicitud(solicitud);
        l.setFechaEmision(LocalDate.of(2026, 11, 12));
        l.setEstado(EstadoLicencia.VIGENTE);

        when(licencias.findWithRelacionesById(85L)).thenReturn(Optional.of(l));
        when(historiales.findByLicenciaIdOrderByFechaCambioAscIdAsc(85L)).thenReturn(List.of());

        // 1. Verificar generación y decodificación de QR con ZXing
        String urlEsperada = "https://licencias.municaceres.gob.pe/api/public/licencias/verificar/"
                + l.getTokenVerificacion();
        byte[] qrPng = pdfGenerator.generarQrPng(urlEsperada, 180, 180);
        assertThat(qrPng).isNotEmpty();

        BufferedImage qrImage = ImageIO.read(new ByteArrayInputStream(qrPng));
        int w = qrImage.getWidth();
        int h = qrImage.getHeight();
        int[] pixels = qrImage.getRGB(0, 0, w, h, null, 0, w);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(w, h, pixels)));
        Result qrResult = new QRCodeReader().decode(bitmap);
        assertThat(qrResult.getText()).isEqualTo(urlEsperada);

        // 2. Generar certificado PDF completo y comprobar su contenido textual con PdfReader
        CertificadoDescarga certificado = certificadoService.generarCertificadoLicencia(85L);
        assertThat(certificado.nombreArchivo()).isEqualTo("certificado-LIC-2026-000085.pdf");
        assertThat(certificado.tipoMime()).isEqualTo("application/pdf");
        assertThat(new String(certificado.contenido(), 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");

        try (PdfReader reader = new PdfReader(certificado.contenido())) {
            assertThat(reader.getNumberOfPages()).isEqualTo(1);
            String textoExtraido = new PdfTextExtractor(reader).getTextFromPage(1);
            assertThat(textoExtraido)
                    .contains("MUNICIPALIDAD DISTRITAL DE ANDRÉS AVELINO CÁCERES DORREGARAY")
                    .contains("LICENCIA DE FUNCIONAMIENTO")
                    .contains("LIC-2026-000085")
                    .contains("EXP-2026-000015")
                    .contains("Botica San Andrés")
                    .contains("20601234561")
                    .contains("Av. Mariscal Cáceres 450")
                    .contains("Botica y Farmacia")
                    .contains("68.50")
                    .contains("Indeterminada");
        }
        verify(auditoria).registrar(any(), any(), any(), any());
    }

    @Test
    void verificacionPublicaPorTokenProtegeDatosPersonalesYRechazaTokenInexistente() {
        Solicitud solicitud = crearSolicitudBase(EstadoSolicitud.APROBADA, false);
        Licencia l = new Licencia();
        l.setId(90L);
        l.setCodigoLicencia("LIC-2026-000090");
        l.setTokenVerificacion("f0e1d2c3b4a5968778695a4b3c2d1e0f11223344");
        l.setSolicitud(solicitud);
        l.setFechaEmision(LocalDate.now());
        l.setEstado(EstadoLicencia.VIGENTE);

        when(licencias.findByTokenVerificacion(l.getTokenVerificacion())).thenReturn(Optional.of(l));
        when(licencias.findByTokenVerificacion("00000000000000000000000000000000")).thenReturn(Optional.empty());

        VerificacionLicenciaResponse publica = verificacionService.verificarPorToken(l.getTokenVerificacion());
        assertThat(publica.autentica()).isTrue();
        assertThat(publica.vigente()).isTrue();
        assertThat(publica.codigoLicencia()).isEqualTo("LIC-2026-000090");
        assertThat(publica.nombreComercial()).isEqualTo("Botica San Andrés");
        assertThat(publica.vigencia()).isEqualTo("INDETERMINADA");
        // La representación textual de la respuesta pública jamás contiene el DNI, correo ni teléfono privado del ciudadano
        assertThat(publica.toString())
                .doesNotContain("44556677")
                .doesNotContain("carlos.privado@correo.pe")
                .doesNotContain("999111222");

        // Token inexistente o inválido lanza ResourceNotFoundException (404)
        assertThrows(ResourceNotFoundException.class,
                () -> verificacionService.verificarPorToken("00000000000000000000000000000000"));
        assertThrows(ResourceNotFoundException.class,
                () -> verificacionService.verificarPorToken("token-invalido-con-simbolos-$$$"));
    }

    @Test
    void ciudadanoNoPuedeEmitirLicenciasNiAccederALicenciasAjenas() {
        autenticarConRol(33L, "ciudadano.propio", "ROLE_CIUDADANO");
        Ciudadano propio = new Ciudadano();
        propio.setId(999L);
        lenient().when(ciudadanos.findByUsuarioId(33L)).thenReturn(Optional.of(propio));

        Solicitud solicitudAjena = crearSolicitudBase(EstadoSolicitud.APROBADA, false); // ciudadanoId = 10L
        Licencia licenciaAjena = new Licencia();
        licenciaAjena.setId(77L);
        licenciaAjena.setCodigoLicencia("LIC-2026-000077");
        licenciaAjena.setSolicitud(solicitudAjena);
        lenient().when(licencias.findWithRelacionesById(77L)).thenReturn(Optional.of(licenciaAjena));

        assertThrows(AccessDeniedException.class,
                () -> licenciaService.emitir(new EmitirLicenciaRequest(100L, "Intento ciudadano")));
        assertThrows(AccessDeniedException.class,
                () -> licenciaService.obtener(77L));
        assertThrows(AccessDeniedException.class,
                () -> licenciaService.suspender(77L, new CambiarEstadoLicenciaRequest("Intento ciudadano")));
    }
}
