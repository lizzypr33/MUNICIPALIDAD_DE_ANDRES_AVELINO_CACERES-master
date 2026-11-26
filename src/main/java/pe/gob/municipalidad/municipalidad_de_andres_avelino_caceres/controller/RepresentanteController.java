package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.CambiarEstadoUsuarioRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.RepresentanteRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.RepresentanteResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoDocumento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.RepresentanteService;

@RestController
@RequestMapping("/api/representantes")
public class RepresentanteController {

    private final RepresentanteService representanteService;

    public RepresentanteController(RepresentanteService representanteService) {
        this.representanteService = representanteService;
    }

    @GetMapping
    public ResponseEntity<PaginaResponse<RepresentanteResponse>> listar(
            @RequestParam(required = false) TipoDocumento tipoDocumento,
            @RequestParam(required = false) String numeroDocumento,
            @RequestParam(required = false) String nombres,
            @RequestParam(required = false) String apellidos,
            @RequestParam(required = false) Boolean activo,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(representanteService.listar(tipoDocumento, numeroDocumento, nombres, apellidos, activo,
                pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RepresentanteResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(representanteService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<RepresentanteResponse> crear(@Valid @RequestBody RepresentanteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(representanteService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RepresentanteResponse> actualizar(@PathVariable Long id,
            @Valid @RequestBody RepresentanteRequest request) {
        return ResponseEntity.ok(representanteService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<RepresentanteResponse> cambiarEstado(@PathVariable Long id,
            @Valid @RequestBody CambiarEstadoUsuarioRequest request) {
        return ResponseEntity.ok(representanteService.cambiarEstado(id, request.activo()));
    }
}
