package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

/** Respuesta del endpoint de diagnóstico. */
public record HealthResponse(String status, String application, String message) {
}
