package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.util.JsonErrorWriter;

/** Responde 403 en JSON: sin permisos, o token CSRF ausente/inválido. */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final JsonErrorWriter errorWriter;

    public RestAccessDeniedHandler(JsonErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        String mensaje = ex instanceof CsrfException
                ? "Token CSRF ausente o inválido. Obtenga uno en GET /api/auth/csrf."
                : "No tiene permisos para realizar esta operación.";
        errorWriter.escribir(response, HttpStatus.FORBIDDEN, mensaje, request.getRequestURI());
    }
}
