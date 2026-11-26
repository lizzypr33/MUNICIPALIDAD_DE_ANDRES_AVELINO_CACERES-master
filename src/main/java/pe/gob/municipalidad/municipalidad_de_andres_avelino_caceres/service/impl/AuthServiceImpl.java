package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl;

import java.time.LocalDateTime;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.CambiarPasswordRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.LoginRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.AuthenticatedUserResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.LoginResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Usuario;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ResourceNotFoundException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.UsuarioMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.UsuarioRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.security.CustomUserDetails;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.security.SesionesUsuarioManager;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.AuditoriaService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.AuthService;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SessionRegistry sessionRegistry;
    private final SesionesUsuarioManager sesiones;
    private final UsuarioRepository usuarios;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoria;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            SessionRegistry sessionRegistry,
            SesionesUsuarioManager sesiones,
            UsuarioRepository usuarios,
            UsuarioMapper usuarioMapper,
            PasswordEncoder passwordEncoder,
            AuditoriaService auditoria) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.sessionRegistry = sessionRegistry;
        this.sesiones = sesiones;
        this.usuarios = usuarios;
        this.usuarioMapper = usuarioMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditoria = auditoria;
    }

    /**
     * Credenciales incorrectas, cuenta desactivada o usuario inexistente lanzan AuthenticationException,
     * que GlobalExceptionHandler convierte en 401 con un mensaje genérico.
     */
    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        Authentication auth = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));

        // Protección contra fijación de sesión: nuevo ID de sesión antes de autenticar.
        httpRequest.getSession(true);
        httpRequest.changeSessionId();

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        sessionRegistry.registerNewSession(httpRequest.getSession().getId(), auth.getPrincipal());

        CustomUserDetails principal = (CustomUserDetails) auth.getPrincipal();
        usuarios.findById(principal.getId()).ifPresent(u -> u.setUltimoAcceso(LocalDateTime.now()));
        auditoria.registrar("LOGIN_EXITOSO", "Usuario", principal.getId(), "Inicio de sesión exitoso");

        return new LoginResponse("Inicio de sesión exitoso", principal.getUsername(), principal.getRoles());
    }

    @Override
    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        CustomUserDetails principal = principalActual();
        if (principal != null) {
            auditoria.registrar("LOGOUT", "Usuario", principal.getId(), "Cierre de sesión");
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            sessionRegistry.removeSessionInformation(session.getId());
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        ResponseCookie borrarCookie = ResponseCookie.from("JSESSIONID", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, borrarCookie.toString());
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticatedUserResponse me() {
        CustomUserDetails principal = principalActual();
        return usuarios.findById(principal.getId())
                .map(usuarioMapper::toAuthenticated)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    @Override
    @Transactional
    public void cambiarPassword(CambiarPasswordRequest request, HttpServletRequest httpRequest) {
        CustomUserDetails principal = principalActual();
        Usuario usuario = usuarios.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.currentPassword(), usuario.getPassword())) {
            throw new BusinessException("La contraseña actual no es correcta.");
        }
        if (passwordEncoder.matches(request.newPassword(), usuario.getPassword())) {
            throw new BusinessException("La nueva contraseña debe ser diferente de la actual.");
        }

        usuario.setPassword(passwordEncoder.encode(request.newPassword()));
        // Se cierran las demás sesiones del usuario; la actual continúa activa.
        sesiones.cerrarSesiones(usuario.getUsername(), true);
        auditoria.registrar("CAMBIO_PASSWORD", "Usuario", usuario.getId(), "Cambio de contraseña");
    }

    private CustomUserDetails principalActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails principal) {
            return principal;
        }
        throw new BusinessException("Debe iniciar sesión para realizar esta operación.");
    }
}
