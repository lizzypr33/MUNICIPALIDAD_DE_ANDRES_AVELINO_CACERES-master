package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.controller;

import java.time.LocalDate;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.AuditoriaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.AuditoriaService;

@RestController
@RequestMapping("/api/auditorias")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public ResponseEntity<PaginaResponse<AuditoriaResponse>> listar(
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) Long entidadId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(auditoriaService.listar(
                usuarioId, accion, entidad, entidadId, fechaDesde, fechaHasta, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditoriaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(auditoriaService.obtener(id));
    }
}
