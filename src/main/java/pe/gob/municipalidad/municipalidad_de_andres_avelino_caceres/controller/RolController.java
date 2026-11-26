package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.RolResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.RolService;

/** Solo consulta. Los roles se asignan con PUT /api/usuarios/{id}/roles; no hay alta ni eliminación. */
@RestController
@RequestMapping("/api/roles")
public class RolController {

    private final RolService rolService;

    public RolController(RolService rolService) {
        this.rolService = rolService;
    }

    @GetMapping
    public ResponseEntity<List<RolResponse>> listar() {
        return ResponseEntity.ok(rolService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RolResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(rolService.obtener(id));
    }
}
