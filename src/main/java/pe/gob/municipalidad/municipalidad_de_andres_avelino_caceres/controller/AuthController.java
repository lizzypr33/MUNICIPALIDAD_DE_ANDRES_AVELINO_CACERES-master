package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.CambiarPasswordRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.LoginRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.AuthenticatedUserResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.LoginResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Fuerza la emisión de la cookie XSRF-TOKEN y devuelve el nombre de cabecera a usar. */
    @GetMapping("/csrf")
    public Map<String, String> csrf(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return Map.of(
                "headerName", token.getHeaderName(),
                "parameterName", token.getParameterName(),
                "token", token.getToken());
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        return ResponseEntity.ok(authService.login(request, httpRequest, httpResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok(Map.of("message", "Sesión cerrada correctamente"));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthenticatedUserResponse> me() {
        return ResponseEntity.ok(authService.me());
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> cambiarPassword(@Valid @RequestBody CambiarPasswordRequest request,
            HttpServletRequest httpRequest) {
        authService.cambiarPassword(request, httpRequest);
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada. Las demás sesiones fueron cerradas."));
    }
}
