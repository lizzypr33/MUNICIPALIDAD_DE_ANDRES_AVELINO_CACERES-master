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
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.AnularPagoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.RechazarPagoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.RegistrarPagoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.VerificarPagoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PagoDetalleResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PagoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ResumenPagosResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoPago;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.ConstanciaPagoService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.ConstanciaPagoService.ConstanciaPagoDescarga;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.PagoService;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;
    private final ConstanciaPagoService constanciaPagoService;

    public PagoController(PagoService pagoService, ConstanciaPagoService constanciaPagoService) {
        this.pagoService = pagoService;
        this.constanciaPagoService = constanciaPagoService;
    }

    @GetMapping
    public ResponseEntity<PaginaResponse<PagoResponse>> listar(
            @RequestParam(required = false) Long solicitudId,
            @RequestParam(required = false) EstadoPago estado,
            @RequestParam(required = false) Long tipoTramiteId,
            @RequestParam(required = false) String concepto,
            @RequestParam(required = false) String codigoOperacion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagoService.listar(
                solicitudId, estado, tipoTramiteId, concepto, codigoOperacion, fechaDesde, fechaHasta, pageable));
    }

    @GetMapping("/resumen")
    public ResponseEntity<ResumenPagosResponse> obtenerResumen(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) EstadoPago estado,
            @RequestParam(required = false) Long tipoTramiteId,
            @RequestParam(required = false) String concepto) {
        return ResponseEntity.ok(pagoService.obtenerResumen(
                fechaDesde, fechaHasta, estado, tipoTramiteId, concepto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PagoDetalleResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(pagoService.obtener(id));
    }

    @GetMapping("/solicitud/{solicitudId}")
    public ResponseEntity<List<PagoResponse>> listarPorSolicitud(@PathVariable Long solicitudId) {
        return ResponseEntity.ok(pagoService.listarPorSolicitud(solicitudId));
    }

    @PostMapping
    public ResponseEntity<PagoDetalleResponse> registrar(@Valid @RequestBody RegistrarPagoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pagoService.registrar(request));
    }

    @PatchMapping("/{id}/verificar")
    public ResponseEntity<PagoDetalleResponse> verificar(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) VerificarPagoRequest request) {
        return ResponseEntity.ok(pagoService.verificar(id, request));
    }

    @PatchMapping("/{id}/rechazar")
    public ResponseEntity<PagoDetalleResponse> rechazar(
            @PathVariable Long id,
            @Valid @RequestBody RechazarPagoRequest request) {
        return ResponseEntity.ok(pagoService.rechazar(id, request));
    }

    @PatchMapping("/{id}/anular")
    public ResponseEntity<PagoDetalleResponse> anular(
            @PathVariable Long id,
            @Valid @RequestBody AnularPagoRequest request) {
        return ResponseEntity.ok(pagoService.anular(id, request));
    }

    @GetMapping("/{id}/constancia")
    public ResponseEntity<byte[]> descargarConstancia(@PathVariable Long id) {
        ConstanciaPagoDescarga descarga = constanciaPagoService.generarConstanciaPago(id);

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
}
