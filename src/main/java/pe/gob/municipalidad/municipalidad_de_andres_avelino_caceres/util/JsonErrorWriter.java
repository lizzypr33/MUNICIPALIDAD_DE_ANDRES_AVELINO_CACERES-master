package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ErrorResponse;
import tools.jackson.databind.ObjectMapper;

/** Escribe errores JSON con la misma forma que GlobalExceptionHandler, para filtros de seguridad. */
@Component
public class JsonErrorWriter {

    private final ObjectMapper objectMapper;

    public JsonErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void escribir(HttpServletResponse response, HttpStatus status, String mensaje, String ruta)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(),
                new ErrorResponse(LocalDateTime.now(), status.value(), mensaje, ruta));
    }
}
