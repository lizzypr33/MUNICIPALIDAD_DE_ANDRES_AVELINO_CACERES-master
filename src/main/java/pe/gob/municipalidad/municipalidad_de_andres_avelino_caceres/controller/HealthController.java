package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.HealthResponse;

/** Diagnóstico público. Solo confirma que la aplicación responde; no verifica PostgreSQL. */
@RestController
@RequestMapping("/api/public")
public class HealthController {

    private final String applicationName;

    public HealthController(@Value("${spring.application.name}") String applicationName) {
        this.applicationName = applicationName;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("UP", applicationName, "Backend municipal funcionando correctamente");
    }
}
