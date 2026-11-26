package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ActualizarCiudadanoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ActualizarEstablecimientoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.CiudadanoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.EstablecimientoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.RepresentanteRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.CiudadanoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.EstablecimientoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.RepresentanteResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Ciudadano;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Establecimiento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Representante;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Rol;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Usuario;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoDocumento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ConflictException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ResourceNotFoundException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.CiudadanoMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.EstablecimientoMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.RepresentanteMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.CiudadanoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.EstablecimientoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.RepresentanteRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.SolicitudRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.security.CustomUserDetails;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.AuditoriaService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl.CiudadanoServiceImpl;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl.EstablecimientoServiceImpl;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl.RepresentanteServiceImpl;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.validation.DocumentoIdentidadValidator;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.validation.RucValidator;

/**
 * Pruebas unitarias de la lógica de negocio de CiudadanoService, RepresentanteService y EstablecimientoService.
 */
class CiudadanoEstablecimientoServiceTests {

    private CiudadanoRepository ciudadanos;
    private RepresentanteRepository representantes;
    private EstablecimientoRepository establecimientos;
    private SolicitudRepository solicitudes;
    private AuditoriaService auditoria;

    private CiudadanoServiceImpl ciudadanoService;
    private RepresentanteServiceImpl representanteService;
    private EstablecimientoServiceImpl establecimientoService;

    @BeforeEach
    void setUp() {
        ciudadanos = mock(CiudadanoRepository.class);
        representantes = mock(RepresentanteRepository.class);
        establecimientos = mock(EstablecimientoRepository.class);
        solicitudes = mock(SolicitudRepository.class);
        auditoria = mock(AuditoriaService.class);

        RucValidator rucValidator = new RucValidator();
        DocumentoIdentidadValidator docValidator = new DocumentoIdentidadValidator(rucValidator);

        ciudadanoService = new CiudadanoServiceImpl(ciudadanos, solicitudes, new CiudadanoMapper(),
                docValidator, auditoria);
        representanteService = new RepresentanteServiceImpl(representantes, solicitudes,
                new RepresentanteMapper(), docValidator, auditoria);
        establecimientoService = new EstablecimientoServiceImpl(establecimientos, ciudadanos, representantes,
                solicitudes, new EstablecimientoMapper(), rucValidator, auditoria);
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
        u.setNombres("Test");
        u.setApellidos("User");
        u.setEmail("u@test.pe");
        u.setRoles(Set.of(rol));
        CustomUserDetails details = new CustomUserDetails(u);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    @Test
    void registroCorrectoDeCiudadanoNormalizaYAudita() {
        when(ciudadanos.existsByTipoDocumentoAndNumeroDocumento(TipoDocumento.DNI, "12345678")).thenReturn(false);
        when(ciudadanos.save(any(Ciudadano.class))).thenAnswer(inv -> {
            Ciudadano c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        CiudadanoResponse res = ciudadanoService.crear(new CiudadanoRequest(
                TipoDocumento.DNI, " 12345678 ", "  Juan  Carlos ", " Perez ", "JUAN@EXAMPLE.COM", "999999999",
                "Ayacucho"));

        assertThat(res.id()).isEqualTo(10L);
        assertThat(res.numeroDocumento()).isEqualTo("12345678");
        assertThat(res.nombres()).isEqualTo("Juan Carlos");
        assertThat(res.apellidos()).isEqualTo("Perez");
        assertThat(res.email()).isEqualTo("juan@example.com");
        assertThat(res.activo()).isTrue();
        verify(auditoria).registrar(eq("REGISTRAR_CIUDADANO"), eq("Ciudadano"), eq(10L), any());
    }

    @Test
    void rechazoDeDniInvalidoYDocumentoDuplicado() {
        assertThatThrownBy(() -> ciudadanoService.crear(new CiudadanoRequest(
                TipoDocumento.DNI, "1234", "Juan", "Perez", null, null, null)))
                .isInstanceOf(BusinessException.class);

        when(ciudadanos.existsByTipoDocumentoAndNumeroDocumento(TipoDocumento.DNI, "12345678")).thenReturn(true);
        assertThatThrownBy(() -> ciudadanoService.crear(new CiudadanoRequest(
                TipoDocumento.DNI, "12345678", "Juan", "Perez", null, null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void consultaPorDocumentoYActualizacionConRestriccionDeExpedientes() {
        Ciudadano existente = new Ciudadano();
        existente.setId(5L);
        existente.setTipoDocumento(TipoDocumento.DNI);
        existente.setNumeroDocumento("87654321");
        existente.setNombres("Maria");
        existente.setApellidos("Gomez");
        when(ciudadanos.findByTipoDocumentoAndNumeroDocumento(TipoDocumento.DNI, "87654321"))
                .thenReturn(Optional.of(existente));
        when(ciudadanos.findById(5L)).thenReturn(Optional.of(existente));

        CiudadanoResponse consultado = ciudadanoService.obtenerPorDocumento(TipoDocumento.DNI, "87654321");
        assertThat(consultado.id()).isEqualTo(5L);

        // Si tiene expedientes vinculados, no puede cambiar número de documento
        when(solicitudes.existsByCiudadanoId(5L)).thenReturn(true);
        assertThatThrownBy(() -> ciudadanoService.actualizar(5L, new ActualizarCiudadanoRequest(
                TipoDocumento.DNI, "11223344", "Maria", "Gomez", "m@test.pe", "999", "Dir")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("expedientes vinculados");

        // Actualizar datos de contacto sin cambiar documento funciona
        CiudadanoResponse actualizado = ciudadanoService.actualizar(5L, new ActualizarCiudadanoRequest(
                null, null, "Maria Elena", "Gomez", "maria@test.pe", "988888888", "Av. Nueva 123"));
        assertThat(actualizado.nombres()).isEqualTo("Maria Elena");
        assertThat(actualizado.email()).isEqualTo("maria@test.pe");
    }

    @Test
    void desactivacionLogicaDeCiudadano() {
        Ciudadano c = new Ciudadano();
        c.setId(7L);
        c.setTipoDocumento(TipoDocumento.DNI);
        c.setNumeroDocumento("44556677");
        c.setNombres("Luis");
        c.setApellidos("Rojas");
        c.setActivo(true);
        when(ciudadanos.findById(7L)).thenReturn(Optional.of(c));

        CiudadanoResponse res = ciudadanoService.cambiarEstado(7L, false);
        assertThat(res.activo()).isFalse();
        verify(auditoria).registrar(eq("DESACTIVAR_CIUDADANO"), eq("Ciudadano"), eq(7L), any());
    }

    @Test
    void proteccionDeDatosDeOtrosCiudadanosParaRolCiudadano() {
        Usuario uPropio = new Usuario();
        uPropio.setId(100L);
        Ciudadano cPropio = new Ciudadano();
        cPropio.setId(1L);
        cPropio.setTipoDocumento(TipoDocumento.DNI);
        cPropio.setNumeroDocumento("11111111");
        cPropio.setNombres("Propio");
        cPropio.setApellidos("Test");
        cPropio.setUsuario(uPropio);

        Usuario uOtro = new Usuario();
        uOtro.setId(200L);
        Ciudadano cOtro = new Ciudadano();
        cOtro.setId(2L);
        cOtro.setTipoDocumento(TipoDocumento.DNI);
        cOtro.setNumeroDocumento("22222222");
        cOtro.setNombres("Otro");
        cOtro.setApellidos("Test");
        cOtro.setUsuario(uOtro);

        when(ciudadanos.findById(1L)).thenReturn(Optional.of(cPropio));
        when(ciudadanos.findById(2L)).thenReturn(Optional.of(cOtro));

        autenticarComo(100L, "CIUDADANO");

        // Puede consultar el suyo
        assertThat(ciudadanoService.obtener(1L).id()).isEqualTo(1L);
        // No puede consultar el de otro ciudadano (403 AccessDeniedException)
        assertThatThrownBy(() -> ciudadanoService.obtener(2L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void registroYValidacionDeRepresentante() {
        when(representantes.existsByTipoDocumentoAndNumeroDocumento(TipoDocumento.DNI, "33445566"))
                .thenReturn(false);
        when(representantes.save(any(Representante.class))).thenAnswer(inv -> {
            Representante r = inv.getArgument(0);
            r.setId(15L);
            return r;
        });

        RepresentanteResponse res = representanteService.crear(new RepresentanteRequest(
                TipoDocumento.DNI, "33445566", "Pedro", "Castillo", "Gerente", "911222333", "pedro@example.com"));
        assertThat(res.id()).isEqualTo(15L);
        assertThat(res.cargo()).isEqualTo("Gerente");

        when(representantes.existsByTipoDocumentoAndNumeroDocumento(TipoDocumento.DNI, "33445566"))
                .thenReturn(true);
        assertThatThrownBy(() -> representanteService.crear(new RepresentanteRequest(
                TipoDocumento.DNI, "33445566", "Pedro", "Castillo", "Gerente", null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void registroDeEstablecimientoValidandoTitularRucYRepresentante() {
        Ciudadano titular = new Ciudadano();
        titular.setId(1L);
        titular.setTipoDocumento(TipoDocumento.DNI);
        titular.setNumeroDocumento("12345678");
        titular.setNombres("Juan");
        titular.setApellidos("Perez");
        titular.setActivo(true);

        when(ciudadanos.findById(1L)).thenReturn(Optional.of(titular));
        when(establecimientos.save(any(Establecimiento.class))).thenAnswer(inv -> {
            Establecimiento e = inv.getArgument(0);
            e.setId(25L);
            return e;
        });

        EstablecimientoResponse creado = establecimientoService.crear(new EstablecimientoRequest(
                "Bodega Central", "Bodega Central SAC", "20100070970", "Jr. Comercio 100",
                "Frente a plaza", "Abarrotes", new BigDecimal("60.00"), 1, "999888777",
                "bodega@example.com", 1L, null));

        assertThat(creado.id()).isEqualTo(25L);
        assertThat(creado.ruc()).isEqualTo("20100070970");
        assertThat(creado.ciudadanoId()).isEqualTo(1L);

        // Rechazo de RUC inválido
        assertThatThrownBy(() -> establecimientoService.crear(new EstablecimientoRequest(
                "Bodega", null, "20100070971", "Jr. Comercio 100", null, "Abarrotes",
                new BigDecimal("60.00"), 1, null, null, 1L, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("dígito verificador");

        // Rechazo de establecimiento con titular inexistente
        when(ciudadanos.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> establecimientoService.crear(new EstablecimientoRequest(
                "Bodega", null, null, "Jr. Comercio 100", null, "Abarrotes",
                new BigDecimal("60.00"), 1, null, null, 99L, null)))
                .isInstanceOf(ResourceNotFoundException.class);

        // Rechazo de establecimiento con titular inactivo
        Ciudadano inactivo = new Ciudadano();
        inactivo.setId(2L);
        inactivo.setActivo(false);
        when(ciudadanos.findById(2L)).thenReturn(Optional.of(inactivo));
        assertThatThrownBy(() -> establecimientoService.crear(new EstablecimientoRequest(
                "Bodega", null, null, "Jr. Comercio 100", null, "Abarrotes",
                new BigDecimal("60.00"), 1, null, null, 2L, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inactivo");
    }

    @Test
    void noPermiteCambiarTitularDeEstablecimientoConExpedientes() {
        Ciudadano titular1 = new Ciudadano();
        titular1.setId(1L);
        titular1.setNombres("Juan");
        titular1.setApellidos("Perez");
        titular1.setActivo(true);

        Establecimiento est = new Establecimiento();
        est.setId(10L);
        est.setNombreComercial("Local 1");
        est.setDireccion("Dir 1");
        est.setGiroNegocio("Giro 1");
        est.setAreaMetrosCuadrados(new BigDecimal("40.00"));
        est.setNumeroPisos(1);
        est.setCiudadano(titular1);

        when(establecimientos.findWithRelacionesById(10L)).thenReturn(Optional.of(est));
        when(solicitudes.existsByEstablecimientoId(10L)).thenReturn(true);

        assertThatThrownBy(() -> establecimientoService.actualizar(10L, new ActualizarEstablecimientoRequest(
                "Local 1", null, null, "Dir 1", null, "Giro 1", new BigDecimal("40.00"), 1, null, null, 2L, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("titularidad");
    }

    @Test
    @SuppressWarnings("unchecked")
    void paginacionYBusquedaPorCiudadanoTitular() {
        Ciudadano titular = new Ciudadano();
        titular.setId(1L);
        titular.setNombres("Juan");
        titular.setApellidos("Perez");
        titular.setNumeroDocumento("12345678");

        Establecimiento e = new Establecimiento();
        e.setId(50L);
        e.setNombreComercial("Farmacia Salud");
        e.setDireccion("Av. Principal 123");
        e.setGiroNegocio("Botica");
        e.setAreaMetrosCuadrados(new BigDecimal("50.00"));
        e.setNumeroPisos(1);
        e.setCiudadano(titular);

        when(ciudadanos.findById(1L)).thenReturn(Optional.of(titular));
        when(establecimientos.findByCiudadanoId(1L)).thenReturn(List.of(e));

        List<EstablecimientoResponse> porTitular = establecimientoService.obtenerPorCiudadano(1L);
        assertThat(porTitular).hasSize(1);
        assertThat(porTitular.get(0).nombreComercial()).isEqualTo("Farmacia Salud");

        Pageable pageable = PageRequest.of(0, 10);
        when(establecimientos.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(e), pageable, 1));

        PaginaResponse<EstablecimientoResponse> pagina = establecimientoService.listar("Farmacia", null, 1L, true,
                pageable);
        assertThat(pagina.totalElementos()).isEqualTo(1);
        assertThat(pagina.contenido()).hasSize(1);
    }
}
