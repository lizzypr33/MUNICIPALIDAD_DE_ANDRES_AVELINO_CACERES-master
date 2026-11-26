package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.AuditoriaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Auditoria;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Usuario;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ResourceNotFoundException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.AuditoriaMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.AuditoriaRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.UsuarioRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.AuditoriaService;

@Service
public class AuditoriaServiceImpl implements AuditoriaService {

    private static final Pattern PATRON_CLAVE_O_TOKEN = Pattern.compile(
            "(?i)(password|contrasena|contraseña|token|csrf|cvv)\\s*[:=]\\s*\\S+");

    private final AuditoriaRepository auditorias;
    private final UsuarioRepository usuarios;
    private final AuditoriaMapper mapper;

    @Autowired
    public AuditoriaServiceImpl(AuditoriaRepository auditorias,
            UsuarioRepository usuarios,
            AuditoriaMapper mapper) {
        this.auditorias = auditorias;
        this.usuarios = usuarios;
        this.mapper = mapper;
    }

    /**
     * Constructor de compatibilidad para pruebas unitarias existentes que instancian AuditoriaServiceImpl con 2 argumentos.
     */
    public AuditoriaServiceImpl(AuditoriaRepository auditorias, UsuarioRepository usuarios) {
        this(auditorias, usuarios, new AuditoriaMapper());
    }

    @Override
    @Transactional
    public void registrar(String accion, String entidad, Long entidadId, String descripcion) {
        Auditoria a = new Auditoria();
        a.setUsuario(usuarioActual().orElse(null));
        a.setAccion(accion);
        a.setEntidadAfectada(entidad);
        a.setEntidadId(entidadId);
        a.setDescripcion(sanitizarDescripcion(descripcion));
        a.setDireccionIp(ipActual());
        auditorias.save(a);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<AuditoriaResponse> listar(
            Long usuarioId,
            String accion,
            String entidad,
            Long entidadId,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Pageable pageable) {

        Page<Auditoria> pagina = auditorias.findAll(
                filtros(usuarioId, accion, entidad, entidadId, fechaDesde, fechaHasta),
                pageable);
        return PaginaResponse.from(pagina.map(mapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public AuditoriaResponse obtener(Long id) {
        Auditoria a = auditorias.findWithUsuarioById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de auditoría no encontrado con id " + id));
        return mapper.toResponse(a);
    }

    private Specification<Auditoria> filtros(
            Long usuarioId,
            String accion,
            String entidad,
            Long entidadId,
            LocalDate fechaDesde,
            LocalDate fechaHasta) {

        return (Root<Auditoria> root, jakarta.persistence.criteria.CriteriaQuery<?> query,
                jakarta.persistence.criteria.CriteriaBuilder cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (usuarioId != null) {
                predicados.add(cb.equal(root.get("usuario").get("id"), usuarioId));
            }
            if (StringUtils.hasText(accion)) {
                predicados.add(cb.like(cb.upper(root.get("accion")), "%" + accion.trim().toUpperCase() + "%"));
            }
            if (StringUtils.hasText(entidad)) {
                predicados.add(cb.like(cb.lower(root.get("entidadAfectada")),
                        "%" + entidad.trim().toLowerCase() + "%"));
            }
            if (entidadId != null) {
                predicados.add(cb.equal(root.get("entidadId"), entidadId));
            }
            if (fechaDesde != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("fechaOperacion"), fechaDesde.atStartOfDay()));
            }
            if (fechaHasta != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("fechaOperacion"), fechaHasta.atTime(LocalTime.MAX)));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }

    private static String sanitizarDescripcion(String descripcion) {
        if (!StringUtils.hasText(descripcion)) {
            return null;
        }
        return PATRON_CLAVE_O_TOKEN.matcher(descripcion.trim()).replaceAll("$1=[REDACTADO]");
    }

    private Optional<Usuario> usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return usuarios.findByUsername(auth.getName());
    }

    private String ipActual() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            String ip = servletAttrs.getRequest().getRemoteAddr();
            return ip != null && ip.length() > 45 ? ip.substring(0, 45) : ip;
        }
        return null;
    }
}
