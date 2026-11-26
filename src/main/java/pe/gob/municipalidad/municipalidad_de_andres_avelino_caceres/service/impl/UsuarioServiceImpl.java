package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ActualizarUsuarioRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.CrearUsuarioRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.UsuarioResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Rol;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Usuario;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ConflictException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ResourceNotFoundException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.UsuarioMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.RolRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.UsuarioRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.security.CustomUserDetails;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.security.SesionesUsuarioManager;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.AuditoriaService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.UsuarioService;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private static final String ROL_ADMIN = "ADMINISTRADOR";

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final UsuarioMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final SesionesUsuarioManager sesiones;
    private final AuditoriaService auditoria;

    public UsuarioServiceImpl(UsuarioRepository usuarios, RolRepository roles, UsuarioMapper mapper,
            PasswordEncoder passwordEncoder, SesionesUsuarioManager sesiones, AuditoriaService auditoria) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
        this.sesiones = sesiones;
        this.auditoria = auditoria;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<UsuarioResponse> listar(String username, String nombre, Boolean activo, String rol,
            Pageable pageable) {
        return PaginaResponse.from(usuarios.findAll(filtros(username, nombre, activo, rol), pageable)
                .map(mapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return mapper.toResponse(buscar(id));
    }

    @Override
    @Transactional
    public UsuarioResponse crear(CrearUsuarioRequest request) {
        String email = request.email().toLowerCase();
        if (usuarios.existsByUsername(request.username())) {
            throw new ConflictException("El nombre de usuario ya está registrado.");
        }
        if (usuarios.existsByEmail(email)) {
            throw new ConflictException("El correo electrónico ya está registrado.");
        }

        Usuario u = new Usuario();
        u.setUsername(request.username());
        u.setPassword(passwordEncoder.encode(request.password()));
        u.setNombres(request.nombres());
        u.setApellidos(request.apellidos());
        u.setEmail(email);
        u.setTelefono(request.telefono());
        u.setRoles(resolverRoles(request.rolIds()));
        usuarios.save(u);

        auditoria.registrar("CREAR_USUARIO", "Usuario", u.getId(), "Usuario creado: " + u.getUsername());
        return mapper.toResponse(u);
    }

    @Override
    @Transactional
    public UsuarioResponse actualizar(Long id, ActualizarUsuarioRequest request) {
        Usuario u = buscar(id);
        String email = request.email().toLowerCase();
        if (!email.equals(u.getEmail()) && usuarios.existsByEmail(email)) {
            throw new ConflictException("El correo electrónico ya está registrado.");
        }
        u.setNombres(request.nombres());
        u.setApellidos(request.apellidos());
        u.setEmail(email);
        u.setTelefono(request.telefono());

        auditoria.registrar("ACTUALIZAR_USUARIO", "Usuario", u.getId(), "Datos actualizados: " + u.getUsername());
        return mapper.toResponse(u);
    }

    @Override
    @Transactional
    public UsuarioResponse cambiarEstado(Long id, boolean activo) {
        Usuario u = buscar(id);
        if (u.isActivo() == activo) {
            return mapper.toResponse(u);
        }
        if (!activo) {
            validarNoEsUltimoAdminActivo(u);
        }
        u.setActivo(activo);
        if (!activo) {
            // Desactivar expulsa las sesiones abiertas; el login ya queda bloqueado por isEnabled().
            sesiones.cerrarSesiones(u.getUsername(), false);
        }

        auditoria.registrar(activo ? "ACTIVAR_USUARIO" : "DESACTIVAR_USUARIO", "Usuario", u.getId(),
                (activo ? "Usuario activado: " : "Usuario desactivado: ") + u.getUsername());
        return mapper.toResponse(u);
    }

    @Override
    @Transactional
    public UsuarioResponse asignarRoles(Long id, Set<Long> rolIds) {
        Usuario u = buscar(id);
        Set<Rol> nuevos = resolverRoles(rolIds);
        boolean pierdeAdmin = esAdminActivo(u) && nuevos.stream().noneMatch(r -> ROL_ADMIN.equals(r.getNombre()));
        if (pierdeAdmin) {
            validarNoEsUltimoAdminActivo(u);
        }
        u.setRoles(nuevos);
        // Las authorities de una sesión existente no se recargan: se fuerza nuevo inicio de sesión.
        sesiones.cerrarSesiones(u.getUsername(), false);

        String nombresRoles = nuevos.stream().map(Rol::getNombre).sorted().toList().toString();
        auditoria.registrar("ASIGNAR_ROLES", "Usuario", u.getId(),
                "Roles de " + u.getUsername() + " actualizados a " + nombresRoles);
        return mapper.toResponse(u);
    }

    private Usuario buscar(Long id) {
        return usuarios.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id " + id));
    }

    private Set<Rol> resolverRoles(Set<Long> rolIds) {
        List<Rol> encontrados = roles.findAllById(rolIds);
        if (encontrados.size() != new HashSet<>(rolIds).size()) {
            throw new BusinessException("Uno o más roles indicados no existen.");
        }
        if (encontrados.stream().anyMatch(r -> !r.isActivo())) {
            throw new BusinessException("No se puede asignar un rol inactivo.");
        }
        return new HashSet<>(encontrados);
    }

    /** Impide dejar el sistema sin administrador activo (incluido el caso de autodesactivación). */
    private void validarNoEsUltimoAdminActivo(Usuario u) {
        if (esAdminActivo(u) && usuarios.countActivosConRol(ROL_ADMIN) <= 1) {
            throw new ConflictException("No se puede dejar el sistema sin un administrador activo.");
        }
    }

    private boolean esAdminActivo(Usuario u) {
        return u.isActivo() && u.getRoles().stream().anyMatch(r -> ROL_ADMIN.equals(r.getNombre()));
    }

    private Specification<Usuario> filtros(String username, String nombre, Boolean activo, String rol) {
        return (Root<Usuario> root, jakarta.persistence.criteria.CriteriaQuery<?> query,
                jakarta.persistence.criteria.CriteriaBuilder cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (StringUtils.hasText(username)) {
                predicados.add(cb.like(cb.lower(root.get("username")), patron(username)));
            }
            if (StringUtils.hasText(nombre)) {
                predicados.add(cb.or(
                        cb.like(cb.lower(root.get("nombres")), patron(nombre)),
                        cb.like(cb.lower(root.get("apellidos")), patron(nombre))));
            }
            if (activo != null) {
                predicados.add(cb.equal(root.get("activo"), activo));
            }
            if (StringUtils.hasText(rol)) {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<Usuario> usuarioSub = sub.correlate(root);
                sub.select(cb.literal(1L))
                        .where(cb.equal(usuarioSub.join("roles").get("nombre"), rol));
                predicados.add(cb.exists(sub));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }

    private static String patron(String texto) {
        return "%" + texto.toLowerCase() + "%";
    }
}
