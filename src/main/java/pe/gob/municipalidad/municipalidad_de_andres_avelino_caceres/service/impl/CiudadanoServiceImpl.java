package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ActualizarCiudadanoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.CiudadanoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.CiudadanoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Ciudadano;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoDocumento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ConflictException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ResourceNotFoundException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.CiudadanoMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.CiudadanoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.SolicitudRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.security.CustomUserDetails;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.AuditoriaService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.CiudadanoService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.validation.DocumentoIdentidadValidator;

@Service
public class CiudadanoServiceImpl implements CiudadanoService {

    private final CiudadanoRepository ciudadanos;
    private final SolicitudRepository solicitudes;
    private final CiudadanoMapper mapper;
    private final DocumentoIdentidadValidator documentoValidator;
    private final AuditoriaService auditoria;

    public CiudadanoServiceImpl(CiudadanoRepository ciudadanos, SolicitudRepository solicitudes,
            CiudadanoMapper mapper, DocumentoIdentidadValidator documentoValidator,
            AuditoriaService auditoria) {
        this.ciudadanos = ciudadanos;
        this.solicitudes = solicitudes;
        this.mapper = mapper;
        this.documentoValidator = documentoValidator;
        this.auditoria = auditoria;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<CiudadanoResponse> listar(TipoDocumento tipoDocumento, String numeroDocumento,
            String nombres, String apellidos, Boolean activo, Pageable pageable) {
        return PaginaResponse.from(
                ciudadanos.findAll(filtros(tipoDocumento, numeroDocumento, nombres, apellidos, activo), pageable)
                        .map(mapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public CiudadanoResponse obtener(Long id) {
        Ciudadano c = buscar(id);
        verificarAcceso(c);
        return mapper.toResponse(c);
    }

    @Override
    @Transactional(readOnly = true)
    public CiudadanoResponse obtenerMiPerfil() {
        Long usuarioActualId = obtenerUsuarioActualId();
        if (usuarioActualId == null) {
            throw new AccessDeniedException("Sesión no válida.");
        }
        Ciudadano c = ciudadanos.findByUsuarioId(usuarioActualId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Su cuenta no tiene una ficha de ciudadano asociada. Acuda a la municipalidad para registrarla."));
        return mapper.toResponse(c);
    }

    @Override
    @Transactional(readOnly = true)
    public CiudadanoResponse obtenerPorDocumento(TipoDocumento tipoDocumento, String numeroDocumento) {
        String numNormalizado = documentoValidator.validarYNormalizar(tipoDocumento, numeroDocumento);
        Ciudadano c = ciudadanos.findByTipoDocumentoAndNumeroDocumento(tipoDocumento, numNormalizado)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ciudadano no encontrado con documento " + tipoDocumento + " " + numNormalizado));
        verificarAcceso(c);
        return mapper.toResponse(c);
    }

    @Override
    @Transactional
    public CiudadanoResponse crear(CiudadanoRequest request) {
        String numNormalizado = documentoValidator.validarYNormalizar(request.tipoDocumento(),
                request.numeroDocumento());
        String nombres = normalizarTextoObligatorio(request.nombres(), "Los nombres son obligatorios.");
        String apellidos = normalizarTextoObligatorio(request.apellidos(), "Los apellidos son obligatorios.");

        if (ciudadanos.existsByTipoDocumentoAndNumeroDocumento(request.tipoDocumento(), numNormalizado)) {
            throw new ConflictException(
                    "Ya existe un ciudadano registrado con el documento " + request.tipoDocumento() + " "
                            + numNormalizado);
        }

        Ciudadano c = new Ciudadano();
        c.setTipoDocumento(request.tipoDocumento());
        c.setNumeroDocumento(numNormalizado);
        c.setNombres(nombres);
        c.setApellidos(apellidos);
        c.setEmail(normalizarEmail(request.email()));
        c.setTelefono(normalizarOpcional(request.telefono()));
        c.setDireccion(normalizarOpcional(request.direccion()));
        ciudadanos.save(c);

        auditoria.registrar("REGISTRAR_CIUDADANO", "Ciudadano", c.getId(),
                "Ciudadano registrado: " + c.getTipoDocumento() + " " + c.getNumeroDocumento());
        return mapper.toResponse(c);
    }

    @Override
    @Transactional
    public CiudadanoResponse actualizar(Long id, ActualizarCiudadanoRequest request) {
        Ciudadano c = buscar(id);
        verificarAcceso(c);

        if (esSoloRolCiudadano()) {
            // El ciudadano en autoservicio solo puede actualizar datos de contacto autorizados.
            if (request.tipoDocumento() != null && request.tipoDocumento() != c.getTipoDocumento()) {
                throw new AccessDeniedException("No tiene permisos para modificar su tipo de documento.");
            }
            if (StringUtils.hasText(request.numeroDocumento())
                    && !request.numeroDocumento().trim().equalsIgnoreCase(c.getNumeroDocumento())) {
                throw new AccessDeniedException("No tiene permisos para modificar su número de documento.");
            }
        } else if (request.tipoDocumento() != null || StringUtils.hasText(request.numeroDocumento())) {
            TipoDocumento nuevoTipo = request.tipoDocumento() != null ? request.tipoDocumento() : c.getTipoDocumento();
            String nuevoNumRaw = StringUtils.hasText(request.numeroDocumento()) ? request.numeroDocumento()
                    : c.getNumeroDocumento();
            String nuevoNum = documentoValidator.validarYNormalizar(nuevoTipo, nuevoNumRaw);

            boolean cambiaDocumento = nuevoTipo != c.getTipoDocumento() || !nuevoNum.equals(c.getNumeroDocumento());
            if (cambiaDocumento) {
                if (solicitudes.existsByCiudadanoId(c.getId())) {
                    throw new ConflictException(
                            "No se puede modificar el documento de un ciudadano que tiene expedientes vinculados.");
                }
                if (ciudadanos.existsByTipoDocumentoAndNumeroDocumento(nuevoTipo, nuevoNum)) {
                    throw new ConflictException(
                            "Ya existe un ciudadano registrado con el documento " + nuevoTipo + " " + nuevoNum);
                }
                c.setTipoDocumento(nuevoTipo);
                c.setNumeroDocumento(nuevoNum);
            }
        }

        c.setNombres(normalizarTextoObligatorio(request.nombres(), "Los nombres son obligatorios."));
        c.setApellidos(normalizarTextoObligatorio(request.apellidos(), "Los apellidos son obligatorios."));
        c.setEmail(normalizarEmail(request.email()));
        c.setTelefono(normalizarOpcional(request.telefono()));
        c.setDireccion(normalizarOpcional(request.direccion()));

        auditoria.registrar("ACTUALIZAR_CIUDADANO", "Ciudadano", c.getId(),
                "Ciudadano actualizado: " + c.getTipoDocumento() + " " + c.getNumeroDocumento());
        return mapper.toResponse(c);
    }

    @Override
    @Transactional
    public CiudadanoResponse cambiarEstado(Long id, boolean activo) {
        Ciudadano c = buscar(id);
        if (c.isActivo() == activo) {
            return mapper.toResponse(c);
        }
        c.setActivo(activo);

        auditoria.registrar(activo ? "ACTIVAR_CIUDADANO" : "DESACTIVAR_CIUDADANO", "Ciudadano", c.getId(),
                (activo ? "Ciudadano activado: " : "Ciudadano desactivado: ") + c.getTipoDocumento() + " "
                        + c.getNumeroDocumento());
        return mapper.toResponse(c);
    }

    private Ciudadano buscar(Long id) {
        return ciudadanos.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ciudadano no encontrado con id " + id));
    }

    /**
     * Si el usuario autenticado solo tiene el rol CIUDADANO, únicamente puede consultar o modificar
     * su propio registro vinculado mediante {@code usuario_id}.
     */
    private void verificarAcceso(Ciudadano c) {
        if (!esSoloRolCiudadano()) {
            return;
        }
        Long usuarioActualId = obtenerUsuarioActualId();
        if (usuarioActualId == null || c.getUsuario() == null || !usuarioActualId.equals(c.getUsuario().getId())) {
            throw new AccessDeniedException("No tiene permisos para acceder a los datos de otro ciudadano.");
        }
    }

    private boolean esSoloRolCiudadano() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return false;
        }
        List<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        boolean esPersonalMunicipal = authorities.contains("ROLE_ADMINISTRADOR")
                || authorities.contains("ROLE_FUNCIONARIO")
                || authorities.contains("ROLE_SUPERVISOR");
        return !esPersonalMunicipal && authorities.contains("ROLE_CIUDADANO");
    }

    private Long obtenerUsuarioActualId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails details) {
            return details.getId();
        }
        return null;
    }

    private Specification<Ciudadano> filtros(TipoDocumento tipoDocumento, String numeroDocumento,
            String nombres, String apellidos, Boolean activo) {
        return (Root<Ciudadano> root, jakarta.persistence.criteria.CriteriaQuery<?> query,
                jakarta.persistence.criteria.CriteriaBuilder cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (tipoDocumento != null) {
                predicados.add(cb.equal(root.get("tipoDocumento"), tipoDocumento));
            }
            if (StringUtils.hasText(numeroDocumento)) {
                predicados.add(cb.like(cb.lower(root.get("numeroDocumento")), patron(numeroDocumento.trim())));
            }
            if (StringUtils.hasText(nombres)) {
                predicados.add(cb.like(cb.lower(root.get("nombres")), patron(nombres.trim())));
            }
            if (StringUtils.hasText(apellidos)) {
                predicados.add(cb.like(cb.lower(root.get("apellidos")), patron(apellidos.trim())));
            }
            if (activo != null) {
                predicados.add(cb.equal(root.get("activo"), activo));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }

    private static String patron(String texto) {
        return "%" + texto.toLowerCase() + "%";
    }

    private static String normalizarTextoObligatorio(String valor, String mensajeError) {
        if (!StringUtils.hasText(valor)) {
            throw new BusinessException(mensajeError);
        }
        return valor.trim().replaceAll("\\s+", " ");
    }

    private static String normalizarOpcional(String valor) {
        if (!StringUtils.hasText(valor)) {
            return null;
        }
        return valor.trim().replaceAll("\\s+", " ");
    }

    private static String normalizarEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return email.trim().toLowerCase();
    }
}
