package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.CambiarEstadoLicenciaRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ProgramarInspeccionRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ReprogramarInspeccionRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ResultadoInspeccionRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.InspeccionDetalleResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.InspeccionResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoInspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.ResultadoInspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.InspeccionService;

@RestController
@RequestMapping("/api/inspecciones")
public class InspeccionController {

    private final InspeccionService inspeccionService;

    public InspeccionController(InspeccionService inspeccionService) {
        this.inspeccionService = inspeccionService;
    }

    @GetMapping
    public ResponseEntity<PaginaResponse<InspeccionResponse>> listar(
            @RequestParam(required = false) Long solicitudId,
            @RequestParam(required = false) Long inspectorId,
            @RequestParam(required = false) EstadoInspeccion estado,
            @RequestParam(required = false) ResultadoInspeccion resultado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(inspeccionService.listar(
                solicitudId, inspectorId, estado, resultado, fechaDesde, fechaHasta, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InspeccionDetalleResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(inspeccionService.obtener(id));
    }

    @GetMapping("/solicitud/{solicitudId}")
    public ResponseEntity<List<InspeccionResponse>> listarPorSolicitud(@PathVariable Long solicitudId) {
        return ResponseEntity.ok(inspeccionService.listarPorSolicitud(solicitudId));
    }

    @PostMapping
    public ResponseEntity<InspeccionDetalleResponse> programar(
            @Valid @RequestBody ProgramarInspeccionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inspeccionService.programar(request));
    }

    @PutMapping("/{id}/reprogramar")
    public ResponseEntity<InspeccionDetalleResponse> reprogramar(
            @PathVariable Long id,
            @Valid @RequestBody ReprogramarInspeccionRequest request) {
        return ResponseEntity.ok(inspeccionService.reprogramar(id, request));
    }

    @PatchMapping("/{id}/iniciar")
    public ResponseEntity<InspeccionDetalleResponse> iniciar(@PathVariable Long id) {
        return ResponseEntity.ok(inspeccionService.iniciar(id));
    }

    @PatchMapping("/{id}/resultado")
    public ResponseEntity<InspeccionDetalleResponse> registrarResultado(
            @PathVariable Long id,
            @Valid @RequestBody ResultadoInspeccionRequest request) {
        return ResponseEntity.ok(inspeccionService.registrarResultado(id, request));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<InspeccionDetalleResponse> cancelar(
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoLicenciaRequest request) {
        return ResponseEntity.ok(inspeccionService.cancelar(id, request));
    }
}
