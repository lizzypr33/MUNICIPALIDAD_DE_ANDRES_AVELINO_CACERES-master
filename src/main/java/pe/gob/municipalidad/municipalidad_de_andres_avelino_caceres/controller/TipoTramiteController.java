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
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.CambiarEstadoUsuarioRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.RequisitoRequest.AsignacionTramiteItem;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.TipoTramiteRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.RequisitoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.TipoTramiteResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.TipoTramiteService;

@RestController
@RequestMapping("/api/tipos-tramite")
public class TipoTramiteController {

    private final TipoTramiteService tipoTramiteService;

    public TipoTramiteController(TipoTramiteService tipoTramiteService) {
        this.tipoTramiteService = tipoTramiteService;
    }

    @GetMapping
    public ResponseEntity<PaginaResponse<TipoTramiteResponse>> listar(
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Boolean activo,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(tipoTramiteService.listar(codigo, nombre, activo, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoTramiteResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(tipoTramiteService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<TipoTramiteResponse> crear(@Valid @RequestBody TipoTramiteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tipoTramiteService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoTramiteResponse> actualizar(@PathVariable Long id,
            @Valid @RequestBody TipoTramiteRequest request) {
        return ResponseEntity.ok(tipoTramiteService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<TipoTramiteResponse> cambiarEstado(@PathVariable Long id,
            @Valid @RequestBody CambiarEstadoUsuarioRequest request) {
        return ResponseEntity.ok(tipoTramiteService.cambiarEstado(id, request.activo()));
    }

    @GetMapping("/{id}/requisitos")
    public ResponseEntity<List<RequisitoResponse>> listarRequisitos(@PathVariable Long id) {
        return ResponseEntity.ok(tipoTramiteService.listarRequisitos(id));
    }

    @PutMapping("/{id}/requisitos")
    public ResponseEntity<List<RequisitoResponse>> configurarRequisitos(@PathVariable Long id,
            @RequestBody List<@Valid AsignacionTramiteItem> requisitos) {
        return ResponseEntity.ok(tipoTramiteService.configurarRequisitos(id, requisitos));
    }
}
