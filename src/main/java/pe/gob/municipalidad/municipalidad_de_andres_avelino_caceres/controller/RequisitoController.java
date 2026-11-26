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
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.RequisitoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.RequisitoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.RequisitoService;

@RestController
@RequestMapping("/api/requisitos")
public class RequisitoController {

    private final RequisitoService requisitoService;

    public RequisitoController(RequisitoService requisitoService) {
        this.requisitoService = requisitoService;
    }

    @GetMapping
    public ResponseEntity<PaginaResponse<RequisitoResponse>> listar(
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Boolean activo,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(requisitoService.listar(codigo, nombre, activo, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RequisitoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(requisitoService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<RequisitoResponse> crear(@Valid @RequestBody RequisitoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(requisitoService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RequisitoResponse> actualizar(@PathVariable Long id,
            @Valid @RequestBody RequisitoRequest request) {
        return ResponseEntity.ok(requisitoService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<RequisitoResponse> cambiarEstado(@PathVariable Long id,
            @Valid @RequestBody CambiarEstadoUsuarioRequest request) {
        return ResponseEntity.ok(requisitoService.cambiarEstado(id, request.activo()));
    }
}
