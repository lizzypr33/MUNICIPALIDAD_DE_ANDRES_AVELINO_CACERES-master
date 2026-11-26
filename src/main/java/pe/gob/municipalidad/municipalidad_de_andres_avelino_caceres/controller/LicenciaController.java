package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.CambiarEstadoLicenciaRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.EmitirLicenciaRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.HistorialLicenciaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.LicenciaDetalleResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.LicenciaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoLicencia;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.CertificadoService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.CertificadoService.CertificadoDescarga;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.LicenciaService;

@RestController
@RequestMapping("/api/licencias")
public class LicenciaController {

    private final LicenciaService licenciaService;
    private final CertificadoService certificadoService;

    public LicenciaController(LicenciaService licenciaService, CertificadoService certificadoService) {
        this.licenciaService = licenciaService;
        this.certificadoService = certificadoService;
    }

    @GetMapping
    public ResponseEntity<PaginaResponse<LicenciaResponse>> listar(
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) Long solicitudId,
            @RequestParam(required = false) EstadoLicencia estado,
            @RequestParam(required = false) Long establecimientoId,
            @RequestParam(required = false) Long ciudadanoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(licenciaService.listar(
                codigo, solicitudId, estado, establecimientoId, ciudadanoId, fechaDesde, fechaHasta, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LicenciaDetalleResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(licenciaService.obtener(id));
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<LicenciaDetalleResponse> obtenerPorCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(licenciaService.obtenerPorCodigo(codigo));
    }

    @GetMapping("/solicitud/{solicitudId}")
    public ResponseEntity<LicenciaDetalleResponse> obtenerPorSolicitud(@PathVariable Long solicitudId) {
        return ResponseEntity.ok(licenciaService.obtenerPorSolicitud(solicitudId));
    }

    @PostMapping("/emitir")
    public ResponseEntity<LicenciaDetalleResponse> emitir(@Valid @RequestBody EmitirLicenciaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(licenciaService.emitir(request));
    }

    @PatchMapping("/{id}/suspender")
    public ResponseEntity<LicenciaDetalleResponse> suspender(
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoLicenciaRequest request) {
        return ResponseEntity.ok(licenciaService.suspender(id, request));
    }

    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<LicenciaDetalleResponse> reactivar(
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoLicenciaRequest request) {
        return ResponseEntity.ok(licenciaService.reactivar(id, request));
    }

    @PatchMapping("/{id}/revocar")
    public ResponseEntity<LicenciaDetalleResponse> revocar(
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoLicenciaRequest request) {
        return ResponseEntity.ok(licenciaService.revocar(id, request));
    }

    @GetMapping("/{id}/certificado")
    public ResponseEntity<byte[]> descargarCertificado(@PathVariable Long id) {
        CertificadoDescarga descarga = certificadoService.generarCertificadoLicencia(id);

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(descarga.nombreArchivo(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(descarga.contenido().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(descarga.contenido());
    }

    @GetMapping("/{id}/historial")
    public ResponseEntity<List<HistorialLicenciaResponse>> listarHistorial(@PathVariable Long id) {
        return ResponseEntity.ok(licenciaService.listarHistorial(id));
    }
}
