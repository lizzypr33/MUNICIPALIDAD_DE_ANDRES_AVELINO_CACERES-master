package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Rol;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Usuario;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.RolRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.UsuarioRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.security.AdminBootstrap;

/** Lógica del bootstrap sin contexto de Spring: no requiere PostgreSQL. */
class AdminBootstrapTests {

    private static final String PASS = "Clave-admin-12345";

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final RolRepository roles = mock(RolRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private AdminBootstrap bootstrap(boolean habilitado, String user, String email, String pass) {
        return new AdminBootstrap(habilitado, user, email, pass, usuarios, roles, encoder);
    }

    @Test
    void deshabilitadoNoHaceNada() {
        bootstrap(false, "admin", "admin@test.pe", PASS).run(null);
        verifyNoInteractions(usuarios, roles);
    }

    @Test
    void faltanVariablesFallaSinRevelarLaContrasena() {
        assertThatThrownBy(() -> bootstrap(true, "admin", "", PASS).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_EMAIL")
                .hasMessageNotContaining(PASS);
    }

    @Test
    void contrasenaCortaSeRechaza() {
        assertThatThrownBy(() -> bootstrap(true, "admin", "admin@test.pe", "corta").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("12 caracteres")
                .hasMessageNotContaining("corta");
    }

    @Test
    void noSobrescribeUnAdministradorExistente() {
        when(usuarios.countConRol("ADMINISTRADOR")).thenReturn(1L);
        bootstrap(true, "admin", "admin@test.pe", PASS).run(null);
        verify(usuarios, never()).save(any(Usuario.class));
    }

    @Test
    void creaAdministradorConContrasenaHasheada() {
        Rol rolAdmin = new Rol();
        rolAdmin.setNombre("ADMINISTRADOR");
        when(usuarios.countConRol("ADMINISTRADOR")).thenReturn(0L);
        when(roles.findByNombre("ADMINISTRADOR")).thenReturn(Optional.of(rolAdmin));

        bootstrap(true, "admin", "Admin@Test.pe", PASS).run(null);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(captor.capture());
        Usuario creado = captor.getValue();
        assertThat(creado.getUsername()).isEqualTo("admin");
        assertThat(creado.getEmail()).isEqualTo("admin@test.pe");
        assertThat(creado.getPassword()).isNotEqualTo(PASS).startsWith("$2");
        assertThat(encoder.matches(PASS, creado.getPassword())).isTrue();
        assertThat(creado.getRoles()).extracting(Rol::getNombre).containsExactly("ADMINISTRADOR");
    }
}
