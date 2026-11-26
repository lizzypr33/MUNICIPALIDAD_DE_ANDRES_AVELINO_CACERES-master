package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ActualizarEstablecimientoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.EstablecimientoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.EstablecimientoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Ciudadano;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Establecimiento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Representante;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ConflictException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ResourceNotFoundException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.EstablecimientoMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.CiudadanoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.EstablecimientoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.RepresentanteRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.SolicitudRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.security.CustomUserDetails;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.AuditoriaService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.EstablecimientoService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.validation.RucValidator;

@Service
public class EstablecimientoServiceImpl implements EstablecimientoService {

    private final EstablecimientoRepository establecimientos;
    private final CiudadanoRepository ciudadanos;
    private final RepresentanteRepository representantes;
    private final SolicitudRepository solicitudes;
    private final EstablecimientoMapper mapper;
    private final RucValidator rucValidator;
    private final AuditoriaService auditoria;

    public EstablecimientoServiceImpl(EstablecimientoRepository establecimientos,
            CiudadanoRepository ciudadanos,
            RepresentanteRepository representantes,
            SolicitudRepository solicitudes,
            EstablecimientoMapper mapper,
            RucValidator rucValidator,
            AuditoriaService auditoria) {
        this.establecimientos = establecimientos;
        this.ciudadanos = ciudadanos;
        this.representantes = representantes;
        this.solicitudes = solicitudes;
        this.mapper = mapper;
        this.rucValidator = rucValidator;
        this.auditoria = auditoria;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<EstablecimientoResponse> listar(String nombreComercial, String ruc, Long ciudadanoId,
            Boolean activo, Pageable pageable) {
        return PaginaResponse.from(
                establecimientos.findAll(filtros(nombreComercial, ruc, ciudadanoId, activo), pageable)
                        .map(mapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public EstablecimientoResponse obtener(Long id) {
        Establecimiento e = buscar(id);
        verificarAccesoEstablecimiento(e);
        return mapper.toResponse(e);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstablecimientoResponse> obtenerPorRuc(String ruc) {
        String rucValido = rucValidator.validarYNormalizar(ruc);
        return establecimientos.findByRuc(rucValido).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstablecimientoResponse> obtenerPorCiudadano(Long ciudadanoId) {
        Ciudadano titular = ciudadanos.findById(ciudadanoId)
                .orElseThrow(() -> new ResourceNotFoundException("Ciudadano no encontrado con id " + ciudadanoId));
        verificarAccesoCiudadano(titular);
        return establecimientos.findByCiudadanoId(ciudadanoId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public EstablecimientoResponse crear(EstablecimientoRequest request) {
        String nombreComercial = normalizarTextoObligatorio(request.nombreComercial(),
                "El nombre comercial es obligatorio.");
        String direccion = normalizarTextoObligatorio(request.direccion(), "La dirección es obligatoria.");
        String giroNegocio = normalizarTextoObligatorio(request.giroNegocio(), "El giro de negocio es obligatorio.");
        validarDimensiones(request.areaMetrosCuadrados(), request.numeroPisos());
        if (request.ciudadanoId() == null) {
            throw new BusinessException("El ciudadano titular es obligatorio.");
        }
        String rucNormalizado = rucValidator.validarYNormalizarOpcional(request.ruc());

        Ciudadano titular = obtenerCiudadanoActivo(request.ciudadanoId());
        Representante representante = obtenerRepresentanteActivoOpcional(request.representanteId());

        Establecimiento e = new Establecimiento();
        e.setNombreComercial(nombreComercial);
        e.setRazonSocial(normalizarOpcional(request.razonSocial()));
        e.setRuc(rucNormalizado);
        e.setDireccion(direccion);
        e.setReferencia(normalizarOpcional(request.referencia()));
        e.setGiroNegocio(giroNegocio);
        e.setAreaMetrosCuadrados(request.areaMetrosCuadrados());
        e.setNumeroPisos(request.numeroPisos());
        e.setTelefono(normalizarOpcional(request.telefono()));
        e.setEmail(normalizarEmail(request.email()));
        e.setCiudadano(titular);
        e.setRepresentante(representante);
        establecimientos.save(e);

        auditoria.registrar("REGISTRAR_ESTABLECIMIENTO", "Establecimiento", e.getId(),
                "Establecimiento registrado: " + e.getNombreComercial()
                        + (e.getRuc() != null ? " (RUC " + e.getRuc() + ")" : ""));
        return mapper.toResponse(e);
    }

    @Override
    @Transactional
    public EstablecimientoResponse actualizar(Long id, ActualizarEstablecimientoRequest request) {
        Establecimiento e = buscar(id);
        verificarAccesoEstablecimiento(e);

        boolean tieneExpedientes = solicitudes.existsByEstablecimientoId(e.getId());
        boolean soloCiudadano = esSoloRolCiudadano();

        // Validar cambio de titularidad
        if (request.ciudadanoId() != null) {
            Long actualId = e.getCiudadano() != null ? e.getCiudadano().getId() : null;
            if (!Objects.equals(actualId, request.ciudadanoId())) {
                if (soloCiudadano) {
                    throw new AccessDeniedException(
                            "No tiene permisos para cambiar la titularidad del establecimiento.");
                }
                if (tieneExpedientes) {
                    throw new ConflictException(
                            "No se puede cambiar la titularidad de un establecimiento con expedientes existentes.");
                }
                e.setCiudadano(obtenerCiudadanoActivo(request.ciudadanoId()));
            }
        }

        // Validar cambio de RUC
        if (request.ruc() != null) {
            String nuevoRuc = rucValidator.validarYNormalizarOpcional(request.ruc());
            if (!Objects.equals(e.getRuc(), nuevoRuc)) {
                if (soloCiudadano) {
                    throw new AccessDeniedException("No tiene permisos para modificar el RUC del establecimiento.");
                }
                if (tieneExpedientes) {
                    throw new ConflictException(
                            "No se puede modificar el RUC de un establecimiento con expedientes existentes.");
                }
                e.setRuc(nuevoRuc);
            }
        }

        // Validar cambio de representante
        if (request.representanteId() != null) {
            Long repActualId = e.getRepresentante() != null ? e.getRepresentante().getId() : null;
            if (!Objects.equals(repActualId, request.representanteId())) {
                if (soloCiudadano) {
                    throw new AccessDeniedException(
                            "No tiene permisos para cambiar el representante del establecimiento.");
                }
                e.setRepresentante(obtenerRepresentanteActivoOpcional(request.representanteId()));
            }
        }

        e.setNombreComercial(
                normalizarTextoObligatorio(request.nombreComercial(), "El nombre comercial es obligatorio."));
        if (request.razonSocial() != null) {
            e.setRazonSocial(normalizarOpcional(request.razonSocial()));
        }
        e.setDireccion(normalizarTextoObligatorio(request.direccion(), "La dirección es obligatoria."));
        e.setReferencia(normalizarOpcional(request.referencia()));
        e.setGiroNegocio(normalizarTextoObligatorio(request.giroNegocio(), "El giro de negocio es obligatorio."));
        validarDimensiones(request.areaMetrosCuadrados(), request.numeroPisos());
        e.setAreaMetrosCuadrados(request.areaMetrosCuadrados());
        e.setNumeroPisos(request.numeroPisos());
        e.setTelefono(normalizarOpcional(request.telefono()));
        e.setEmail(normalizarEmail(request.email()));

        auditoria.registrar("ACTUALIZAR_ESTABLECIMIENTO", "Establecimiento", e.getId(),
                "Establecimiento actualizado: " + e.getNombreComercial());
        return mapper.toResponse(e);
    }

    @Override
    @Transactional
    public EstablecimientoResponse cambiarEstado(Long id, boolean activo) {
        Establecimiento e = buscar(id);
        if (e.isActivo() == activo) {
            return mapper.toResponse(e);
        }
        e.setActivo(activo);

        auditoria.registrar(activo ? "ACTIVAR_ESTABLECIMIENTO" : "DESACTIVAR_ESTABLECIMIENTO", "Establecimiento",
                e.getId(),
                (activo ? "Establecimiento activado: " : "Establecimiento desactivado: ") + e.getNombreComercial());
        return mapper.toResponse(e);
    }

    private Establecimiento buscar(Long id) {
        return establecimientos.findWithRelacionesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Establecimiento no encontrado con id " + id));
    }

    private Ciudadano obtenerCiudadanoActivo(Long ciudadanoId) {
        Ciudadano c = ciudadanos.findById(ciudadanoId)
                .orElseThrow(() -> new ResourceNotFoundException("Ciudadano titular no encontrado con id " + ciudadanoId));
        if (!c.isActivo()) {
            throw new BusinessException("El ciudadano titular se encuentra inactivo.");
        }
        return c;
    }

    private Representante obtenerRepresentanteActivoOpcional(Long representanteId) {
        if (representanteId == null) {
            return null;
        }
        Representante r = representantes.findById(representanteId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Representante no encontrado con id " + representanteId));
        if (!r.isActivo()) {
            throw new BusinessException("El representante indicado se encuentra inactivo.");
        }
        return r;
    }

    private void verificarAccesoEstablecimiento(Establecimiento e) {
        if (!esSoloRolCiudadano()) {
            return;
        }
        Ciudadano titular = e.getCiudadano();
        Long usuarioActualId = obtenerUsuarioActualId();
        if (usuarioActualId == null || titular == null || titular.getUsuario() == null
                || !usuarioActualId.equals(titular.getUsuario().getId())) {
            throw new AccessDeniedException(
                    "No tiene permisos para acceder a establecimientos de otros ciudadanos.");
        }
    }

    private void verificarAccesoCiudadano(Ciudadano titular) {
        if (!esSoloRolCiudadano()) {
            return;
        }
        Long usuarioActualId = obtenerUsuarioActualId();
        if (usuarioActualId == null || titular.getUsuario() == null
                || !usuarioActualId.equals(titular.getUsuario().getId())) {
            throw new AccessDeniedException(
                    "No tiene permisos para consultar establecimientos de otros ciudadanos.");
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

    private Specification<Establecimiento> filtros(String nombreComercial, String ruc, Long ciudadanoId,
            Boolean activo) {
        return (Root<Establecimiento> root, jakarta.persistence.criteria.CriteriaQuery<?> query,
                jakarta.persistence.criteria.CriteriaBuilder cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (StringUtils.hasText(nombreComercial)) {
                predicados.add(cb.like(cb.lower(root.get("nombreComercial")), patron(nombreComercial.trim())));
            }
            if (StringUtils.hasText(ruc)) {
                predicados.add(cb.equal(root.get("ruc"), ruc.trim()));
            }
            if (ciudadanoId != null) {
                predicados.add(cb.equal(root.get("ciudadano").get("id"), ciudadanoId));
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

    private static void validarDimensiones(BigDecimal area, Integer pisos) {
        if (area == null || area.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("El área en metros cuadrados debe ser mayor que cero.");
        }
        if (pisos == null || pisos <= 0) {
            throw new BusinessException("El número de pisos debe ser mayor que cero.");
        }
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
