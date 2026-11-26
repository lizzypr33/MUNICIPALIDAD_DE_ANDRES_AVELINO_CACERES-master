package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ReporteFiltroRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse.FilaCiudadanoReporte;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse.FilaEstablecimientoReporte;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse.FilaInspeccionReporte;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse.FilaLicenciaReporte;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse.FilaPagoReporte;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse.FilaSolicitudReporte;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoInspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoLicencia;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoPago;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoSolicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.ResultadoInspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoDocumento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.ReporteService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.ReporteService.ArchivoReporte;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/solicitudes")
    public ResponseEntity<ReporteResponse<FilaSolicitudReporte>> reporteSolicitudes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) EstadoSolicitud estado,
            @RequestParam(required = false) Long tipoTramiteId,
            @RequestParam(required = false) Long funcionarioResponsableId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(reporteService.reporteSolicitudes(
                ReporteFiltroRequest.paraSolicitudes(fechaDesde, fechaHasta, estado, tipoTramiteId, funcionarioResponsableId),
                pageable));
    }

    @GetMapping("/licencias")
    public ResponseEntity<ReporteResponse<FilaLicenciaReporte>> reporteLicencias(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) EstadoLicencia estado,
            @RequestParam(required = false) String giro,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(reporteService.reporteLicencias(
                ReporteFiltroRequest.paraLicencias(fechaDesde, fechaHasta, estado, giro),
                pageable));
    }

    @GetMapping("/inspecciones")
    public ResponseEntity<ReporteResponse<FilaInspeccionReporte>> reporteInspecciones(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) Long inspectorId,
            @RequestParam(required = false) EstadoInspeccion estado,
            @RequestParam(required = false) ResultadoInspeccion resultado,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(reporteService.reporteInspecciones(
                ReporteFiltroRequest.paraInspecciones(fechaDesde, fechaHasta, inspectorId, estado, resultado),
                pageable));
    }

    @GetMapping("/pagos")
    public ResponseEntity<ReporteResponse<FilaPagoReporte>> reportePagos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) EstadoPago estado,
            @RequestParam(required = false) String concepto,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(reporteService.reportePagos(
                ReporteFiltroRequest.paraPagos(fechaDesde, fechaHasta, estado, concepto),
                pageable));
    }

    @GetMapping("/establecimientos")
    public ResponseEntity<ReporteResponse<FilaEstablecimientoReporte>> reporteEstablecimientos(
            @RequestParam(required = false) String nombreComercial,
            @RequestParam(required = false) String giro,
            @RequestParam(required = false) Boolean activo,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(reporteService.reporteEstablecimientos(
                ReporteFiltroRequest.paraEstablecimientos(nombreComercial, giro, activo),
                pageable));
    }

    @GetMapping("/ciudadanos")
    public ResponseEntity<ReporteResponse<FilaCiudadanoReporte>> reporteCiudadanos(
            @RequestParam(required = false) TipoDocumento tipoDocumento,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Boolean activo,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(reporteService.reporteCiudadanos(
                ReporteFiltroRequest.paraCiudadanos(tipoDocumento, nombre, activo),
                pageable));
    }

    @GetMapping("/solicitudes/pdf")
    public ResponseEntity<byte[]> exportarSolicitudesPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) EstadoSolicitud estado,
            @RequestParam(required = false) Long tipoTramiteId,
            @RequestParam(required = false) Long funcionarioResponsableId) {
        return construirRespuestaArchivo(reporteService.exportarSolicitudesPdf(
                ReporteFiltroRequest.paraSolicitudes(fechaDesde, fechaHasta, estado, tipoTramiteId, funcionarioResponsableId)),
                MediaType.APPLICATION_PDF);
    }

    @GetMapping("/licencias/pdf")
    public ResponseEntity<byte[]> exportarLicenciasPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) EstadoLicencia estado,
            @RequestParam(required = false) String giro) {
        return construirRespuestaArchivo(reporteService.exportarLicenciasPdf(
                ReporteFiltroRequest.paraLicencias(fechaDesde, fechaHasta, estado, giro)),
                MediaType.APPLICATION_PDF);
    }

    @GetMapping("/inspecciones/pdf")
    public ResponseEntity<byte[]> exportarInspeccionesPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) Long inspectorId,
            @RequestParam(required = false) EstadoInspeccion estado,
            @RequestParam(required = false) ResultadoInspeccion resultado) {
        return construirRespuestaArchivo(reporteService.exportarInspeccionesPdf(
                ReporteFiltroRequest.paraInspecciones(fechaDesde, fechaHasta, inspectorId, estado, resultado)),
                MediaType.APPLICATION_PDF);
    }

    @GetMapping("/pagos/pdf")
    public ResponseEntity<byte[]> exportarPagosPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) EstadoPago estado,
            @RequestParam(required = false) String concepto) {
        return construirRespuestaArchivo(reporteService.exportarPagosPdf(
                ReporteFiltroRequest.paraPagos(fechaDesde, fechaHasta, estado, concepto)),
                MediaType.APPLICATION_PDF);
    }

    @GetMapping("/establecimientos/pdf")
    public ResponseEntity<byte[]> exportarEstablecimientosPdf(
            @RequestParam(required = false) String nombreComercial,
            @RequestParam(required = false) String giro,
            @RequestParam(required = false) Boolean activo) {
        return construirRespuestaArchivo(reporteService.exportarEstablecimientosPdf(
                ReporteFiltroRequest.paraEstablecimientos(nombreComercial, giro, activo)),
                MediaType.APPLICATION_PDF);
    }

    @GetMapping("/solicitudes/csv")
    public ResponseEntity<byte[]> exportarSolicitudesCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) EstadoSolicitud estado,
            @RequestParam(required = false) Long tipoTramiteId,
            @RequestParam(required = false) Long funcionarioResponsableId) {
        return construirRespuestaCsv(reporteService.exportarSolicitudesCsv(
                ReporteFiltroRequest.paraSolicitudes(fechaDesde, fechaHasta, estado, tipoTramiteId, funcionarioResponsableId)));
    }

    @GetMapping("/licencias/csv")
    public ResponseEntity<byte[]> exportarLicenciasCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) EstadoLicencia estado,
            @RequestParam(required = false) String giro) {
        return construirRespuestaCsv(reporteService.exportarLicenciasCsv(
                ReporteFiltroRequest.paraLicencias(fechaDesde, fechaHasta, estado, giro)));
    }

    @GetMapping("/inspecciones/csv")
    public ResponseEntity<byte[]> exportarInspeccionesCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) Long inspectorId,
            @RequestParam(required = false) EstadoInspeccion estado,
            @RequestParam(required = false) ResultadoInspeccion resultado) {
        return construirRespuestaCsv(reporteService.exportarInspeccionesCsv(
                ReporteFiltroRequest.paraInspecciones(fechaDesde, fechaHasta, inspectorId, estado, resultado)));
    }

    @GetMapping("/pagos/csv")
    public ResponseEntity<byte[]> exportarPagosCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) EstadoPago estado,
            @RequestParam(required = false) String concepto) {
        return construirRespuestaCsv(reporteService.exportarPagosCsv(
                ReporteFiltroRequest.paraPagos(fechaDesde, fechaHasta, estado, concepto)));
    }

    @GetMapping("/establecimientos/csv")
    public ResponseEntity<byte[]> exportarEstablecimientosCsv(
            @RequestParam(required = false) String nombreComercial,
            @RequestParam(required = false) String giro,
            @RequestParam(required = false) Boolean activo) {
        return construirRespuestaCsv(reporteService.exportarEstablecimientosCsv(
                ReporteFiltroRequest.paraEstablecimientos(nombreComercial, giro, activo)));
    }

    private ResponseEntity<byte[]> construirRespuestaArchivo(ArchivoReporte archivo, MediaType mediaType) {
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(archivo.nombreArchivo(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(archivo.contenido().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(archivo.contenido());
    }

    private ResponseEntity<byte[]> construirRespuestaCsv(ArchivoReporte archivo) {
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(archivo.nombreArchivo(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8")
                .contentLength(archivo.contenido().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(archivo.contenido());
    }
}
