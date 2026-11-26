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
 * Crea el primer administrador al arrancar, solo si ADMIN_BOOTSTRAP_ENABLED=true
 * y todavía no existe ningún usuario con rol ADMINISTRADOR.
 * Nunca registra la contraseña en logs ni en mensajes de error.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private static final String ROL_ADMIN = "ADMINISTRADOR";

    private final boolean habilitado;
    private final String username;
    private final String email;
    private final String password;
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrap(
            @Value("${app.bootstrap.admin.enabled:false}") boolean habilitado,
        @Value("${app.bootstrap.admin.username:}") String username,
            @Value("${app.bootstrap.admin.email:}") String email,
            @Value("${app.bootstrap.admin.password:}") String password,
            UsuarioRepository usuarios,
            RolRepository roles,
            PasswordEncoder passwordEncoder) {
        this.habilitado = habilitado;
        this.username = username;
        this.email = email;
        this.password = password;
        this.usuarios = usuarios;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!habilitado) {
            return;
        }
        if (isBlank(username) || isBlank(email) || isBlank(password)) {
            throw new IllegalStateException(
                    "ADMIN_BOOTSTRAP_ENABLED=true requiere ADMIN_USERNAME, ADMIN_EMAIL y ADMIN_PASSWORD.");
        }
        if (password.length() < 12) {
            throw new IllegalStateException("ADMIN_PASSWORD debe tener al menos 12 caracteres.");
        }
        if (usuarios.countConRol(ROL_ADMIN) > 0) {
            log.info("Bootstrap omitido: ya existe un administrador. Desactive ADMIN_BOOTSTRAP_ENABLED.");
            return;
        }
        if (usuarios.existsByUsername(username) || usuarios.existsByEmail(email.toLowerCase())) {
            throw new IllegalStateException("ADMIN_USERNAME o ADMIN_EMAIL ya están registrados por otro usuario.");
        }
        Rol rolAdmin = roles.findByNombre(ROL_ADMIN)
                .orElseThrow(() -> new IllegalStateException("Falta el rol ADMINISTRADOR; ejecute las migraciones."));

        Usuario admin = new Usuario();
        admin.setUsername(username);
        admin.setEmail(email.toLowerCase());
        admin.setPassword(passwordEncoder.encode(password));
        admin.setNombres("Administrador");
        admin.setApellidos("Del sistema");
        admin.setRoles(new HashSet<>(Set.of(rolAdmin)));
        usuarios.save(admin);

        log.info("Administrador inicial creado. IMPORTANTE: establezca ADMIN_BOOTSTRAP_ENABLED=false.");
    }

    private static boolean isBlank(String valor) {
        return valor == null || valor.isBlank();
    }
}
