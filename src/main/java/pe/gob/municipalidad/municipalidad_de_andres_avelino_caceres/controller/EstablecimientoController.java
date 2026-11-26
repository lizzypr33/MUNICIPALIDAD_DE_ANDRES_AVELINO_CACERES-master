package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.controller;

import java.util.List;

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
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ActualizarEstablecimientoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.CambiarEstadoUsuarioRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.EstablecimientoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.EstablecimientoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.EstablecimientoService;

@RestController
@RequestMapping("/api/establecimientos")
public class EstablecimientoController {

    private final EstablecimientoService establecimientoService;

    public EstablecimientoController(EstablecimientoService establecimientoService) {
        this.establecimientoService = establecimientoService;
    }

    @GetMapping
    public ResponseEntity<PaginaResponse<EstablecimientoResponse>> listar(
            @RequestParam(required = false) String nombreComercial,
            @RequestParam(required = false) String ruc,
            @RequestParam(required = false) Long ciudadanoId,
            @RequestParam(required = false) Boolean activo,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(
                establecimientoService.listar(nombreComercial, ruc, ciudadanoId, activo, pageable));
    }

    @GetMapping("/ruc/{ruc}")
    public ResponseEntity<List<EstablecimientoResponse>> obtenerPorRuc(@PathVariable String ruc) {
        return ResponseEntity.ok(establecimientoService.obtenerPorRuc(ruc));
    }

    @GetMapping("/ciudadano/{ciudadanoId}")
    public ResponseEntity<List<EstablecimientoResponse>> obtenerPorCiudadano(@PathVariable Long ciudadanoId) {
        return ResponseEntity.ok(establecimientoService.obtenerPorCiudadano(ciudadanoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EstablecimientoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(establecimientoService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<EstablecimientoResponse> crear(@Valid @RequestBody EstablecimientoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(establecimientoService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EstablecimientoResponse> actualizar(@PathVariable Long id,
            @Valid @RequestBody ActualizarEstablecimientoRequest request) {
        return ResponseEntity.ok(establecimientoService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<EstablecimientoResponse> cambiarEstado(@PathVariable Long id,
            @Valid @RequestBody CambiarEstadoUsuarioRequest request) {
        return ResponseEntity.ok(establecimientoService.cambiarEstado(id, request.activo()));
    }
}
