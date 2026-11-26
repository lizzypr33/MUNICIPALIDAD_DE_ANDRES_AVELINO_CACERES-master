package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.UsuarioRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarios;

    public CustomUserDetailsService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    /** Transaccional para cargar los roles (LAZY) antes de construir las authorities. */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        return usuarios.findByUsername(username)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
    }
}
