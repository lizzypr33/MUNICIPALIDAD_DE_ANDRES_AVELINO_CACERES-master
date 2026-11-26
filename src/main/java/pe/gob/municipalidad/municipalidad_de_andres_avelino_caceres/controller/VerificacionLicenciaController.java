package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.VerificacionLicenciaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.VerificacionLicenciaService;

@RestController
@RequestMapping("/api/public/licencias")
public class VerificacionLicenciaController {

    private final VerificacionLicenciaService verificacionLicenciaService;

    public VerificacionLicenciaController(VerificacionLicenciaService verificacionLicenciaService) {
        this.verificacionLicenciaService = verificacionLicenciaService;
    }

    @GetMapping("/verificar/{token}")
    public ResponseEntity<VerificacionLicenciaResponse> verificar(@PathVariable String token) {
        return ResponseEntity.ok(verificacionLicenciaService.verificarPorToken(token));
    }
}
