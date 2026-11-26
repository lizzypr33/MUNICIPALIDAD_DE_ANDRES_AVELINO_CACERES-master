package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ActividadRecienteResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.DashboardEstadisticasResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.DashboardResumenResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.EstadisticaInspeccionResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.EstadisticaLicenciaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.EstadisticaPagoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.EstadisticaSolicitudResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoSolicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/resumen")
    public ResponseEntity<DashboardResumenResponse> obtenerResumen() {
        return ResponseEntity.ok(dashboardService.obtenerResumen());
    }

    @GetMapping("/estadisticas")
    public ResponseEntity<DashboardEstadisticasResponse> obtenerEstadisticasGenerales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {
        return ResponseEntity.ok(dashboardService.obtenerEstadisticasGenerales(fechaDesde, fechaHasta));
    }

    @GetMapping("/solicitudes/estadisticas")
    public ResponseEntity<EstadisticaSolicitudResponse> obtenerEstadisticasSolicitudes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) Long tipoTramiteId,
            @RequestParam(required = false) EstadoSolicitud estado) {
        return ResponseEntity.ok(dashboardService.obtenerEstadisticasSolicitudes(
                fechaDesde, fechaHasta, tipoTramiteId, estado));
    }

    @GetMapping("/licencias/estadisticas")
    public ResponseEntity<EstadisticaLicenciaResponse> obtenerEstadisticasLicencias(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {
        return ResponseEntity.ok(dashboardService.obtenerEstadisticasLicencias(fechaDesde, fechaHasta));
    }

    @GetMapping("/inspecciones/estadisticas")
    public ResponseEntity<EstadisticaInspeccionResponse> obtenerEstadisticasInspecciones(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {
        return ResponseEntity.ok(dashboardService.obtenerEstadisticasInspecciones(fechaDesde, fechaHasta));
    }

    @GetMapping("/pagos/estadisticas")
    public ResponseEntity<EstadisticaPagoResponse> obtenerEstadisticasPagos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {
        return ResponseEntity.ok(dashboardService.obtenerEstadisticasPagos(fechaDesde, fechaHasta));
    }

    @GetMapping("/actividad-reciente")
    public ResponseEntity<List<ActividadRecienteResponse>> obtenerActividadReciente(
            @RequestParam(required = false) Integer limite) {
        return ResponseEntity.ok(dashboardService.obtenerActividadReciente(limite));
    }
}
