package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.security;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Rol;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Usuario;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.RolRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.UsuarioRepository;

/**
 * Crea un usuario por defecto para cada rol municipal (FUNCIONARIO, SUPERVISOR, CIUDADANO)
 * al arrancar, solo si app.bootstrap.default-users.enabled=true y el username no existe.
 * Nunca registra contraseñas en logs.
 */
@Component
public class DefaultUsersBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultUsersBootstrap.class);

    private final boolean habilitado;
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder passwordEncoder;

    private final String funcionarioUser;
    private final String funcionarioEmail;
    private final String funcionarioPass;
    private final String supervisorUser;
    private final String supervisorEmail;
    private final String supervisorPass;
    private final String ciudadanoUser;
    private final String ciudadanoEmail;
    private final String ciudadanoPass;

    public DefaultUsersBootstrap(
            @Value("${app.bootstrap.default-users.enabled:false}") boolean habilitado,
            @Value("${app.bootstrap.funcionario.username}") String funcionarioUser,
            @Value("${app.bootstrap.funcionario.email}") String funcionarioEmail,
            @Value("${app.bootstrap.funcionario.password}") String funcionarioPass,
            @Value("${app.bootstrap.supervisor.username}") String supervisorUser,
            @Value("${app.bootstrap.supervisor.email}") String supervisorEmail,
            @Value("${app.bootstrap.supervisor.password}") String supervisorPass,
            @Value("${app.bootstrap.ciudadano.username}") String ciudadanoUser,
            @Value("${app.bootstrap.ciudadano.email}") String ciudadanoEmail,
            @Value("${app.bootstrap.ciudadano.password}") String ciudadanoPass,
            UsuarioRepository usuarios,
            RolRepository roles,
            PasswordEncoder passwordEncoder) {
        this.habilitado = habilitado;
        this.usuarios = usuarios;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.funcionarioUser = funcionarioUser;
        this.funcionarioEmail = funcionarioEmail;
        this.funcionarioPass = funcionarioPass;
        this.supervisorUser = supervisorUser;
        this.supervisorEmail = supervisorEmail;
        this.supervisorPass = supervisorPass;
        this.ciudadanoUser = ciudadanoUser;
        this.ciudadanoEmail = ciudadanoEmail;
        this.ciudadanoPass = ciudadanoPass;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!habilitado) {
            return;
        }
        crearSiNoExiste(funcionarioUser, funcionarioEmail, funcionarioPass, "FUNCIONARIO", "Funcionario", "Municipal");
        crearSiNoExiste(supervisorUser, supervisorEmail, supervisorPass, "SUPERVISOR", "Supervisor", "Municipal");
        crearSiNoExiste(ciudadanoUser, ciudadanoEmail, ciudadanoPass, "CIUDADANO", "Ciudadano", "Demo");
    }

    private void crearSiNoExiste(String username, String email, String password,
            String nombreRol, String nombres, String apellidos) {
        if (usuarios.existsByUsername(username)) {
            log.info("Usuario por defecto '{}' ya existe; no se modifica.", username);
            return;
        }
        if (usuarios.existsByEmail(email.toLowerCase())) {
            log.warn("No se crea '{}': el correo ya está registrado por otro usuario.", username);
            return;
        }
        Rol rol = roles.findByNombre(nombreRol)
                .orElseThrow(() -> new IllegalStateException("Falta el rol " + nombreRol + "; ejecute las migraciones."));

        Usuario u = new Usuario();
        u.setUsername(username);
        u.setEmail(email.toLowerCase());
        u.setPassword(passwordEncoder.encode(password));
        u.setNombres(nombres);
        u.setApellidos(apellidos);
        u.setRoles(new HashSet<>(Set.of(rol)));
        usuarios.save(u);
        log.info("Usuario por defecto creado: '{}' con rol {}.", username, nombreRol);
    }
}
