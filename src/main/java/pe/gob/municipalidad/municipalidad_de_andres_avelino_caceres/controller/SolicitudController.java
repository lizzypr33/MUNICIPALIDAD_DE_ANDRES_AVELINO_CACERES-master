package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ActualizarSolicitudRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.AsignarFuncionarioRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ObservarSolicitudRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.PresentarSolicitudRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ResolverSolicitudRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.SolicitudRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.SubsanarSolicitudRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ExpedienteResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.HistorialSolicitudResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.SolicitudDetalleResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.SolicitudResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoSolicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.SolicitudService;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    private final SolicitudService solicitudService;

    public SolicitudController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    @GetMapping
    public ResponseEntity<PaginaResponse<SolicitudResponse>> listar(
            @RequestParam(required = false) String numeroExpediente,
            @RequestParam(required = false) Long ciudadanoId,
            @RequestParam(required = false) Long establecimientoId,
            @RequestParam(required = false) Long tipoTramiteId,
            @RequestParam(required = false) EstadoSolicitud estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicial,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFinal,
            @RequestParam(required = false) Long usuarioResponsableId,
            @PageableDefault(size = 20, sort = "fechaPresentacion", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(solicitudService.listar(
                numeroExpediente, ciudadanoId, establecimientoId, tipoTramiteId, estado,
                fechaInicial, fechaFinal, usuarioResponsableId, pageable));
    }

    @GetMapping("/expediente/{numero}")
    public ResponseEntity<ExpedienteResponse> obtenerPorExpediente(@PathVariable String numero) {
        return ResponseEntity.ok(solicitudService.obtenerPorExpediente(numero));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SolicitudDetalleResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(solicitudService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<SolicitudResponse> crear(@Valid @RequestBody SolicitudRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(solicitudService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SolicitudResponse> actualizar(@PathVariable Long id,
            @Valid @RequestBody ActualizarSolicitudRequest request) {
        return ResponseEntity.ok(solicitudService.actualizar(id, request));
    }

    @PostMapping("/{id}/presentar")
    public ResponseEntity<SolicitudResponse> presentar(@PathVariable Long id,
            @Valid @RequestBody(required = false) PresentarSolicitudRequest request) {
        return ResponseEntity.ok(solicitudService.presentar(id, request));
    }

    @PatchMapping("/{id}/asignar")
    public ResponseEntity<SolicitudResponse> asignarFuncionario(@PathVariable Long id,
            @Valid @RequestBody AsignarFuncionarioRequest request) {
        return ResponseEntity.ok(solicitudService.asignarFuncionario(id, request));
    }

    @PatchMapping("/{id}/iniciar-revision")
    public ResponseEntity<SolicitudResponse> iniciarRevision(@PathVariable Long id,
            @Valid @RequestBody(required = false) ResolverSolicitudRequest request) {
        String obs = request != null ? request.observaciones() : null;
        return ResponseEntity.ok(solicitudService.iniciarRevision(id, obs));
    }

    @PatchMapping("/{id}/observar")
    public ResponseEntity<SolicitudResponse> observar(@PathVariable Long id,
            @Valid @RequestBody ObservarSolicitudRequest request) {
        return ResponseEntity.ok(solicitudService.observar(id, request));
    }

    @PatchMapping("/{id}/subsanar")
    public ResponseEntity<SolicitudResponse> subsanar(@PathVariable Long id,
            @Valid @RequestBody SubsanarSolicitudRequest request) {
        return ResponseEntity.ok(solicitudService.subsanar(id, request));
    }

    @PatchMapping("/{id}/aprobar")
    public ResponseEntity<SolicitudResponse> aprobar(@PathVariable Long id,
            @Valid @RequestBody(required = false) ResolverSolicitudRequest request) {
        return ResponseEntity.ok(solicitudService.aprobar(id, request));
    }

    @PatchMapping("/{id}/rechazar")
    public ResponseEntity<SolicitudResponse> rechazar(@PathVariable Long id,
            @Valid @RequestBody ResolverSolicitudRequest request) {
        return ResponseEntity.ok(solicitudService.rechazar(id, request));
    }

    @PatchMapping("/{id}/archivar")
    public ResponseEntity<SolicitudResponse> archivar(@PathVariable Long id,
            @Valid @RequestBody ResolverSolicitudRequest request) {
        return ResponseEntity.ok(solicitudService.archivar(id, request));
    }

    @GetMapping("/{id}/historial")
    public ResponseEntity<List<HistorialSolicitudResponse>> listarHistorial(@PathVariable Long id) {
        return ResponseEntity.ok(solicitudService.listarHistorial(id));
    }
}
