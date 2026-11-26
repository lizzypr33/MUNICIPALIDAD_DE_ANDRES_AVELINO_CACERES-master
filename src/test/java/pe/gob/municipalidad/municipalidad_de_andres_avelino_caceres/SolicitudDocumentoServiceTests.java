package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.AsignarFuncionarioRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ObservarSolicitudRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.PresentarSolicitudRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.RequisitoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ResolverSolicitudRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.SolicitudRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.SubsanarSolicitudRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.DocumentoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.RequisitoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.SolicitudResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Ciudadano;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Documento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Establecimiento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.HistorialSolicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Requisito;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Rol;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Solicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.TipoTramite;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.TipoTramiteRequisito;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Usuario;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoDocumento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoSolicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoDocumento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ConflictException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ResourceNotFoundException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.CiudadanoMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.DocumentoMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.EstablecimientoMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.RepresentanteMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.RequisitoMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.SolicitudMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.TipoTramiteMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.CiudadanoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.DocumentoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.EstablecimientoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.HistorialSolicitudRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.NotificacionRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.RepresentanteRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.RequisitoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.SolicitudRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.TipoTramiteRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.TipoTramiteRequisitoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.UsuarioRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.security.CustomUserDetails;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.AuditoriaService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.DocumentoService.ArchivoDescargable;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl.DocumentoServiceImpl;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl.ExpedienteServiceImpl;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl.SolicitudServiceImpl;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl.TipoTramiteServiceImpl;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.storage.FileStorageService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.storage.impl.LocalFileStorageService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.util.CodigoGenerator;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.validation.DocumentoValidator;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.validation.SolicitudValidator;

/**
 * Pruebas unitarias de SolicitudService, ExpedienteService, TipoTramiteService, DocumentoService,
 * validadores (DocumentoValidator, SolicitudValidator) y almacenamiento físico LocalFileStorageService.
 */
class SolicitudDocumentoServiceTests {

    @TempDir
    Path tempStorageDir;

    private SolicitudRepository solicitudes;
    private CiudadanoRepository ciudadanos;
    private EstablecimientoRepository establecimientos;
    private TipoTramiteRepository tiposTramite;
    private TipoTramiteRequisitoRepository tipoTramiteRequisitos;
    private RequisitoRepository requisitos;
    private RepresentanteRepository representantes;
    private DocumentoRepository documentos;
    private HistorialSolicitudRepository historialRepository;
    private UsuarioRepository usuarios;
    private NotificacionRepository notificaciones;
    private AuditoriaService auditoria;

    private CodigoGenerator codigoGenerator;
    private SolicitudValidator solicitudValidator;
    private DocumentoValidator documentoValidator;
    private FileStorageService fileStorageService;

    private ExpedienteServiceImpl expedienteService;
    private TipoTramiteServiceImpl tipoTramiteService;
    private DocumentoServiceImpl documentoService;
    private SolicitudServiceImpl solicitudService;

    @BeforeEach
    void setUp() {
        solicitudes = mock(SolicitudRepository.class);
        ciudadanos = mock(CiudadanoRepository.class);
        establecimientos = mock(EstablecimientoRepository.class);
        tiposTramite = mock(TipoTramiteRepository.class);
        tipoTramiteRequisitos = mock(TipoTramiteRequisitoRepository.class);
        requisitos = mock(RequisitoRepository.class);
        representantes = mock(RepresentanteRepository.class);
        documentos = mock(DocumentoRepository.class);
        historialRepository = mock(HistorialSolicitudRepository.class);
        usuarios = mock(UsuarioRepository.class);
        notificaciones = mock(NotificacionRepository.class);
        auditoria = mock(AuditoriaService.class);

        codigoGenerator = new CodigoGenerator();
        solicitudValidator = new SolicitudValidator();
        documentoValidator = new DocumentoValidator(1024 * 1024); // 1 MB para pruebas de límite
        fileStorageService = new LocalFileStorageService(tempStorageDir.toString());

        SolicitudMapper solicitudMapper = new SolicitudMapper(
                new CiudadanoMapper(), new EstablecimientoMapper(), new TipoTramiteMapper(), new RepresentanteMapper());
        DocumentoMapper documentoMapper = new DocumentoMapper();

        expedienteService = new ExpedienteServiceImpl(solicitudes, codigoGenerator, solicitudMapper);
        tipoTramiteService = new TipoTramiteServiceImpl(tiposTramite, requisitos, tipoTramiteRequisitos,
                solicitudes, new TipoTramiteMapper(), new RequisitoMapper(), codigoGenerator, auditoria);
        documentoService = new DocumentoServiceImpl(documentos, solicitudes, requisitos, documentoValidator,
                solicitudValidator, fileStorageService, codigoGenerator, documentoMapper, auditoria);
        solicitudService = new SolicitudServiceImpl(solicitudes, ciudadanos, establecimientos, tiposTramite,
                tipoTramiteRequisitos, requisitos, representantes, documentos, historialRepository, usuarios,
                notificaciones, expedienteService, solicitudValidator, solicitudMapper, documentoMapper, auditoria);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(Long usuarioId, String rolNombre) {
        Rol rol = new Rol();
        rol.setNombre(rolNombre);
        Usuario u = new Usuario();
        u.setId(usuarioId);
        u.setUsername("user" + usuarioId);
        u.setPassword("hash");
        u.setNombres("Usuario");
        u.setApellidos("Prueba");
        u.setEmail("u@test.pe");
        u.setRoles(Set.of(rol));
        CustomUserDetails details = new CustomUserDetails(u);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    private Ciudadano crearCiudadano(Long id, Long usuarioId) {
        Ciudadano c = new Ciudadano();
        c.setId(id);
        c.setTipoDocumento(TipoDocumento.DNI);
        c.setNumeroDocumento("12345678");
        c.setNombres("Juan");
        c.setApellidos("Perez");
        c.setActivo(true);
        if (usuarioId != null) {
            Usuario u = new Usuario();
            u.setId(usuarioId);
            u.setUsername("ciud" + usuarioId);
            c.setUsuario(u);
        }
        return c;
    }

    private Establecimiento crearEstablecimiento(Long id, Ciudadano titular) {
        Establecimiento e = new Establecimiento();
        e.setId(id);
        e.setNombreComercial("Comercial Andino");
        e.setDireccion("Av. Principal 100");
        e.setGiroNegocio("Bodega");
        e.setAreaMetrosCuadrados(new BigDecimal("50.00"));
        e.setNumeroPisos(1);
        e.setActivo(true);
        e.setCiudadano(titular);
        return e;
    }

    private TipoTramite crearTipoTramite(Long id) {
        TipoTramite t = new TipoTramite();
        t.setId(id);
        t.setCodigo("LIC-INDETERMINADA");
        t.setNombre("Licencia de Funcionamiento Indeterminada");
        t.setActivo(true);
        return t;
    }

    @Test
    void crearBorradorValidaCiudadanoEstablecimientoTitularidadYRegistraHistorial() {
        Ciudadano c1 = crearCiudadano(1L, 100L);
        Ciudadano c2 = crearCiudadano(2L, 200L);
        Establecimiento estDeC1 = crearEstablecimiento(10L, c1);
        TipoTramite tt = crearTipoTramite(5L);

        when(ciudadanos.findById(1L)).thenReturn(Optional.of(c1));
        when(ciudadanos.findById(2L)).thenReturn(Optional.of(c2));
        when(ciudadanos.findById(99L)).thenReturn(Optional.empty());
        when(establecimientos.findWithRelacionesById(10L)).thenReturn(Optional.of(estDeC1));
        when(establecimientos.findWithRelacionesById(99L)).thenReturn(Optional.empty());
        when(tiposTramite.findById(5L)).thenReturn(Optional.of(tt));
        when(solicitudes.save(any(Solicitud.class))).thenAnswer(inv -> {
            Solicitud s = inv.getArgument(0);
            s.setId(50L);
            return s;
        });

        // 1. Validar ciudadano inexistente -> 404
        assertThatThrownBy(() -> solicitudService.crear(new SolicitudRequest(99L, 10L, 5L, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);

        // 2. Validar establecimiento inexistente -> 404
        assertThatThrownBy(() -> solicitudService.crear(new SolicitudRequest(1L, 99L, 5L, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);

        // 3. Validar titularidad (establecimiento de c1 usado por c2) -> 409 Conflict
        assertThatThrownBy(() -> solicitudService.crear(new SolicitudRequest(2L, 10L, 5L, null, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("no pertenece al ciudadano titular");

        // 4. Ciudadano autenticado no puede crear solicitud a nombre de otro ciudadano -> 403
        autenticarComo(100L, "CIUDADANO");
        assertThatThrownBy(() -> solicitudService.crear(new SolicitudRequest(2L, 10L, 5L, null, null)))
                .isInstanceOf(AccessDeniedException.class);

        // 5. Crear borrador propio exitosamente
        SolicitudResponse borrador = solicitudService.crear(
                new SolicitudRequest(1L, 10L, 5L, null, "Solicitud inicial"));
        assertThat(borrador.id()).isEqualTo(50L);
        assertThat(borrador.estado()).isEqualTo(EstadoSolicitud.BORRADOR);
        assertThat(borrador.numeroExpediente()).isNull();
        verify(historialRepository).save(any(HistorialSolicitud.class));
        verify(auditoria).registrar(eq("CREAR_BORRADOR_SOLICITUD"), eq("Solicitud"), eq(50L), any());
    }

    @Test
    void generacionDeExpedienteUnicoBajoConcurrencia() throws Exception {
        AtomicLong secuencia = new AtomicLong(0);
        when(solicitudes.obtenerSiguienteCorrelativoExpediente()).thenAnswer(inv -> secuencia.incrementAndGet());
        when(solicitudes.existsByNumeroExpediente(any())).thenReturn(false);

        int hilos = 16;
        int totalSolicitudes = 64;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        Set<String> generados = ConcurrentHashMap.newKeySet();
        List<Callable<String>> tareas = new ArrayList<>();

        for (int i = 0; i < totalSolicitudes; i++) {
            tareas.add(() -> {
                String exp = expedienteService.generarNumeroExpediente();
                generados.add(exp);
                return exp;
            });
        }

        List<Future<String>> resultados = pool.invokeAll(tareas);
        for (Future<String> f : resultados) {
            assertThat(f.get()).matches("^EXP-\\d{4}-\\d{6}$");
        }
        pool.shutdown();

        assertThat(generados).hasSize(totalSolicitudes);
    }

    @Test
    void flujoCompletoDeTransicionesRevisionObservacionSubsanacionAprobacionYRechazo() {
        Ciudadano c = crearCiudadano(1L, 100L);
        Establecimiento est = crearEstablecimiento(10L, c);
        TipoTramite tt = crearTipoTramite(5L);

        Requisito reqOblig = new Requisito();
        reqOblig.setId(7L);
        reqOblig.setCodigo("REQ-DJ");
        reqOblig.setNombre("Declaración Jurada");
        reqOblig.setActivo(true);

        TipoTramiteRequisito ttr = new TipoTramiteRequisito();
        ttr.setTipoTramite(tt);
        ttr.setRequisito(reqOblig);
        ttr.setObligatorio(true);
        ttr.setOrdenVisualizacion(1);

        Solicitud s = new Solicitud();
        s.setId(30L);
        s.setCiudadano(c);
        s.setEstablecimiento(est);
        s.setTipoTramite(tt);
        s.setEstado(EstadoSolicitud.BORRADOR);

        when(solicitudes.findWithRelacionesById(30L)).thenReturn(Optional.of(s));
        when(tipoTramiteRequisitos.findByTipoTramiteIdOrderByOrdenVisualizacionAsc(5L)).thenReturn(List.of(ttr));
        when(requisitos.findAllById(any())).thenReturn(List.of(reqOblig));
        when(solicitudes.obtenerSiguienteCorrelativoExpediente()).thenReturn(1L);
        when(solicitudes.existsByNumeroExpediente(any())).thenReturn(false);

        // Si intenta presentar sin adjuntar documento obligatorio -> 400 BusinessException y sigue en BORRADOR
        when(documentos.findBySolicitudIdAndActivoTrueOrderByFechaSubidaAscIdAsc(30L)).thenReturn(List.of());
        assertThatThrownBy(() -> solicitudService.presentar(30L, new PresentarSolicitudRequest("Presento")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Faltan documentos obligatorios");
        assertThat(s.getEstado()).isEqualTo(EstadoSolicitud.BORRADOR);
        assertThat(s.getNumeroExpediente()).isNull();

        // No se puede aprobar directamente un BORRADOR -> 409 ConflictException
        autenticarComo(1L, "FUNCIONARIO");
        assertThatThrownBy(() -> solicitudService.aprobar(30L, null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("no se puede pasar de BORRADOR a APROBADA");

        // Adjunta documento obligatorio -> presenta solicitud exitosamente
        Documento doc = new Documento();
        doc.setId(80L);
        doc.setSolicitud(s);
        doc.setRequisito(reqOblig);
        doc.setEstado(EstadoDocumento.PENDIENTE);
        doc.setActivo(true);
        when(documentos.findBySolicitudIdAndActivoTrueOrderByFechaSubidaAscIdAsc(30L)).thenReturn(List.of(doc));

        SolicitudResponse presentada = solicitudService.presentar(30L, new PresentarSolicitudRequest("Presento"));
        assertThat(presentada.estado()).isEqualTo(EstadoSolicitud.PRESENTADA);
        assertThat(presentada.numeroExpediente()).matches("^EXP-\\d{4}-000001$");
        assertThat(s.getFechaPresentacion()).isNotNull();

        // No se puede presentar dos veces -> 409 ConflictException
        assertThatThrownBy(() -> solicitudService.presentar(30L, null))
                .isInstanceOf(ConflictException.class);

        // Asignar funcionario responsable
        Rol rolFunc = new Rol();
        rolFunc.setNombre("FUNCIONARIO");
        Usuario funcionario = new Usuario();
        funcionario.setId(20L);
        funcionario.setUsername("func.revisor");
        funcionario.setNombres("Carlos");
        funcionario.setApellidos("Vargas");
        funcionario.setActivo(true);
        funcionario.setRoles(Set.of(rolFunc));
        when(usuarios.findWithRolesById(20L)).thenReturn(Optional.of(funcionario));

        SolicitudResponse asignada = solicitudService.asignarFuncionario(
                30L, new AsignarFuncionarioRequest(20L, "Asignado para evaluación"));
        assertThat(asignada.usuarioResponsableId()).isEqualTo(20L);

        // Iniciar revisión (PRESENTADA -> EN_REVISION)
        SolicitudResponse enRevision = solicitudService.iniciarRevision(30L, "Revisión iniciada");
        assertThat(enRevision.estado()).isEqualTo(EstadoSolicitud.EN_REVISION);

        // Observar solicitud (EN_REVISION -> OBSERVADA)
        SolicitudResponse observada = solicitudService.observar(
                30L, new ObservarSolicitudRequest("Subsanar firma en declaración jurada"));
        assertThat(observada.estado()).isEqualTo(EstadoSolicitud.OBSERVADA);

        // Subsanar solicitud como ciudadano titular (OBSERVADA -> SUBSANADA)
        autenticarComo(100L, "CIUDADANO");
        SolicitudResponse subsanada = solicitudService.subsanar(
                30L, new SubsanarSolicitudRequest("Se adjuntó declaración jurada firmada"));
        assertThat(subsanada.estado()).isEqualTo(EstadoSolicitud.SUBSANADA);

        // Ciudadano nunca puede aprobar su propia solicitud -> 403 AccessDeniedException
        assertThatThrownBy(() -> solicitudService.aprobar(30L, null))
                .isInstanceOf(AccessDeniedException.class);

        // Volver a EN_REVISION y aprobar como funcionario (SUBSANADA -> EN_REVISION -> APROBADA)
        autenticarComo(20L, "FUNCIONARIO");
        solicitudService.iniciarRevision(30L, "Revisión de subsanación");
        SolicitudResponse aprobada = solicitudService.aprobar(
                30L, new ResolverSolicitudRequest("Cumple todos los requisitos", null));
        assertThat(aprobada.estado()).isEqualTo(EstadoSolicitud.APROBADA);
        assertThat(doc.getEstado()).isEqualTo(EstadoDocumento.VALIDADO);
    }

    @Test
    void rechazoExigeMotivoYBloqueaAprobacionPosterior() {
        Ciudadano c = crearCiudadano(1L, 100L);
        Establecimiento est = crearEstablecimiento(10L, c);
        TipoTramite tt = crearTipoTramite(5L);

        Solicitud s = new Solicitud();
        s.setId(40L);
        s.setNumeroExpediente("EXP-2026-000099");
        s.setCiudadano(c);
        s.setEstablecimiento(est);
        s.setTipoTramite(tt);
        s.setEstado(EstadoSolicitud.EN_REVISION);

        when(solicitudes.findWithRelacionesById(40L)).thenReturn(Optional.of(s));
        autenticarComo(20L, "FUNCIONARIO");

        // Rechazar sin motivo -> 400 BusinessException
        assertThatThrownBy(() -> solicitudService.rechazar(40L, new ResolverSolicitudRequest(null, "   ")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("motivo de rechazo es obligatorio");

        SolicitudResponse rechazada = solicitudService.rechazar(
                40L, new ResolverSolicitudRequest(null, "Zonificación incompatible"));
        assertThat(rechazada.estado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(rechazada.motivoRechazo()).isEqualTo("Zonificación incompatible");

        // No se puede aprobar una solicitud rechazada -> 409 ConflictException
        assertThatThrownBy(() -> solicitudService.aprobar(40L, null))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void validacionSubidaDescargaYRetiroControladoDeDocumentosEnDisco() {
        Ciudadano c = crearCiudadano(1L, 100L);
        Establecimiento est = crearEstablecimiento(10L, c);
        TipoTramite tt = crearTipoTramite(5L);

        Solicitud s = new Solicitud();
        s.setId(60L);
        s.setCiudadano(c);
        s.setEstablecimiento(est);
        s.setTipoTramite(tt);
        s.setEstado(EstadoSolicitud.BORRADOR);

        when(solicitudes.findWithRelacionesById(60L)).thenReturn(Optional.of(s));
        when(documentos.save(any(Documento.class))).thenAnswer(inv -> {
            Documento d = inv.getArgument(0);
            d.setId(90L);
            return d;
        });

        // 1. Subir PDF válido con firma real %PDF-1.7
        byte[] pdfBytes = "%PDF-1.7\nContenido de prueba PDF municipal".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile pdfValido = new MockMultipartFile(
                "archivo", "declaracion_jurada.pdf", "application/pdf", pdfBytes);

        autenticarComo(100L, "CIUDADANO");
        DocumentoResponse subido = documentoService.subir(60L, null, "Adjunto inicial", pdfValido);
        assertThat(subido.id()).isEqualTo(90L);
        assertThat(subido.nombreOriginal()).isEqualTo("declaracion_jurada.pdf");
        assertThat(subido.tipoMime()).isEqualTo("application/pdf");
        assertThat(subido.activo()).isTrue();

        // 2. Rechazar archivo demasiado grande (> 1 MB configurado en el test)
        byte[] grande = new byte[1024 * 1024 + 100];
        grande[0] = '%';
        grande[1] = 'P';
        grande[2] = 'D';
        grande[3] = 'F';
        grande[4] = '-';
        MockMultipartFile archivoGrande = new MockMultipartFile(
                "archivo", "pesado.pdf", "application/pdf", grande);
        assertThatThrownBy(() -> documentoService.subir(60L, null, null, archivoGrande))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tamaño máximo");

        // 3. Rechazar archivo con extensión no permitida (.exe) o contenido falso
        MockMultipartFile exeFalso = new MockMultipartFile(
                "archivo", "malware.pdf", "application/pdf", new byte[] { 0x4D, 0x5A, 0x00, 0x01, 0x02 });
        assertThatThrownBy(() -> documentoService.subir(60L, null, null, exeFalso))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ejecutable");

        MockMultipartFile textoDisfrazado = new MockMultipartFile(
                "archivo", "falso.pdf", "application/pdf", "Hola mundo no soy PDF".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> documentoService.subir(60L, null, null, textoDisfrazado))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PDF válido");

        // 4. Descargar documento autorizado y rechazar descarga de otro ciudadano
        Documento docGuardado = new Documento();
        docGuardado.setId(90L);
        docGuardado.setSolicitud(s);
        docGuardado.setNombreOriginal("declaracion_jurada.pdf");
        docGuardado.setTipoMime("application/pdf");
        docGuardado.setActivo(true);
        // Capturar la ruta relativa real creada en tempStorageDir
        String nombreEnDisco = tempStorageDir.resolve("60").toFile().list()[0];
        docGuardado.setRutaAlmacenamiento("60/" + nombreEnDisco);
        when(documentos.findWithRelacionesById(90L)).thenReturn(Optional.of(docGuardado));

        ArchivoDescargable descargado = documentoService.descargar(90L);
        assertThat(descargado.nombreOriginal()).isEqualTo("declaracion_jurada.pdf");
        assertThat(descargado.contenido()).isEqualTo(pdfBytes);

        // Otro ciudadano (usuario 999) no puede descargar el documento -> 403 AccessDeniedException
        autenticarComo(999L, "CIUDADANO");
        assertThatThrownBy(() -> documentoService.descargar(90L))
                .isInstanceOf(AccessDeniedException.class);

        // 5. No permitir retirar evidencia si la solicitud ya fue PRESENTADA -> 409 ConflictException
        autenticarComo(100L, "CIUDADANO");
        s.setEstado(EstadoSolicitud.PRESENTADA);
        assertThatThrownBy(() -> documentoService.retirar(90L))
                .isInstanceOf(ConflictException.class);

        // En estado BORRADOR sí permite retiro lógico
        s.setEstado(EstadoSolicitud.BORRADOR);
        DocumentoResponse retirado = documentoService.retirar(90L);
        assertThat(retirado.activo()).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void configuracionDeRequisitosYListadoConFiltros() {
        TipoTramite tt = crearTipoTramite(5L);
        Requisito r1 = new Requisito();
        r1.setId(1L);
        r1.setCodigo("REQ-01");
        r1.setNombre("Solicitud con carácter de DJ");
        r1.setActivo(true);

        when(tiposTramite.findById(5L)).thenReturn(Optional.of(tt));
        when(requisitos.findById(1L)).thenReturn(Optional.of(r1));

        TipoTramiteRequisito ttrGuardado = new TipoTramiteRequisito();
        ttrGuardado.setTipoTramite(tt);
        ttrGuardado.setRequisito(r1);
        ttrGuardado.setObligatorio(true);
        ttrGuardado.setOrdenVisualizacion(1);
        when(tipoTramiteRequisitos.findByTipoTramiteIdOrderByOrdenVisualizacionAsc(5L))
                .thenReturn(List.of(ttrGuardado));

        List<RequisitoResponse> configurados = tipoTramiteService.configurarRequisitos(
                5L, List.of(new RequisitoRequest.AsignacionTramiteItem(1L, true, 1)));
        assertThat(configurados).hasSize(1);
        assertThat(configurados.get(0).codigo()).isEqualTo("REQ-01");
        assertThat(configurados.get(0).obligatorio()).isTrue();

        // Rechazar duplicados en la misma configuración -> 409 ConflictException
        assertThatThrownBy(() -> tipoTramiteService.configurarRequisitos(
                5L, List.of(
                        new RequisitoRequest.AsignacionTramiteItem(1L, true, 1),
                        new RequisitoRequest.AsignacionTramiteItem(1L, false, 2))))
                .isInstanceOf(ConflictException.class);

        // Listado paginado de solicitudes con filtros
        Ciudadano c = crearCiudadano(1L, 100L);
        Solicitud sol = new Solicitud();
        sol.setId(77L);
        sol.setNumeroExpediente("EXP-2026-000077");
        sol.setCiudadano(c);
        sol.setEstablecimiento(crearEstablecimiento(10L, c));
        sol.setTipoTramite(tt);
        sol.setEstado(EstadoSolicitud.PRESENTADA);

        Pageable pageable = PageRequest.of(0, 10);
        when(solicitudes.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(sol), pageable, 1));

        autenticarComo(1L, "SUPERVISOR");
        PaginaResponse<SolicitudResponse> pagina = solicitudService.listar(
                "EXP-2026", 1L, 10L, 5L, EstadoSolicitud.PRESENTADA, null, null, null, pageable);
        assertThat(pagina.totalElementos()).isEqualTo(1);
        assertThat(pagina.contenido().get(0).numeroExpediente()).isEqualTo("EXP-2026-000077");
    }
}
