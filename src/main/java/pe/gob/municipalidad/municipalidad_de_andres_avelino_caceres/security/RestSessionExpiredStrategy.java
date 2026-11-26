package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.web.session.SessionInformationExpiredEvent;
import org.springframework.security.web.session.SessionInformationExpiredStrategy;
import org.springframework.stereotype.Component;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.util.JsonErrorWriter;

/**
 * Responde 401 en JSON cuando la sesión fue expirada: por desactivación del usuario,
 * cambio de contraseña o de roles. Reemplaza el mensaje de texto con estado 200 por defecto.
 */
@Component
public class RestSessionExpiredStrategy implements SessionInformationExpiredStrategy {

    private final JsonErrorWriter errorWriter;

    public RestSessionExpiredStrategy(JsonErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void onExpiredSessionDetected(SessionInformationExpiredEvent event) throws IOException {
        errorWriter.escribir(event.getResponse(), HttpStatus.UNAUTHORIZED,
                "La sesión ya no es válida. Inicie sesión nuevamente.",
                event.getRequest().getRequestURI());
    }
}
