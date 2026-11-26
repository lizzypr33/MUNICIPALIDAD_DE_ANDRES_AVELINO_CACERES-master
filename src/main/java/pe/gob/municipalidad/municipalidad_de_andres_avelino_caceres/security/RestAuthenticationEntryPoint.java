package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.util.JsonErrorWriter;

/** Responde 401 en JSON cuando la petición no está autenticada. */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final JsonErrorWriter errorWriter;

    public RestAuthenticationEntryPoint(JsonErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        errorWriter.escribir(response, HttpStatus.UNAUTHORIZED,
                "No autenticado. Inicie sesión para continuar.", request.getRequestURI());
    }
}
