package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.security;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.userdetails.UserDetails;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Rol;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Usuario;

/**
 * Principal de la sesión. Copia los datos necesarios al momento del login para no
 * depender de la sesión de Hibernate. La contraseña se borra tras autenticar.
 */
public class CustomUserDetails implements UserDetails, CredentialsContainer, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String username;
    private String password;
    private final String nombres;
    private final String apellidos;
    private final String email;
    private final boolean activo;
    private final List<String> roles;
    private final List<GrantedAuthority> authorities;

    public CustomUserDetails(Usuario usuario) {
        this.id = usuario.getId();
        this.username = usuario.getUsername();
        this.password = usuario.getPassword();
        this.nombres = usuario.getNombres();
        this.apellidos = usuario.getApellidos();
        this.email = usuario.getEmail();
        this.activo = usuario.isActivo();
        this.roles = usuario.getRoles().stream().map(Rol::getNombre).sorted().toList();
        this.authorities = roles.stream()
                .<GrantedAuthority>map(rol -> new SimpleGrantedAuthority("ROLE_" + rol))
                .toList();
    }

    public Long getId() {
        return id;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String getEmail() {
        return email;
    }

    public List<String> getRoles() {
        return roles;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }

    @Override
    public void eraseCredentials() {
        this.password = null;
    }
}
