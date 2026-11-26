package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Estadísticas consolidadas de inspecciones técnicas municipales, separando estados y resultados.
 */
public record EstadisticaInspeccionResponse(
        long totalProgramadas,
        long totalEnProceso,
        long totalFinalizadas,
        long totalCanceladas,
        long totalFavorables,
        long totalDesfavorables,
        long totalConObservaciones,
        List<ConteoPorInspector> porInspector,
        List<ConteoPorPeriodo> porPeriodo,
        LocalDate fechaDesde,
        LocalDate fechaHasta) {

    public record ConteoPorInspector(
            Long inspectorId,
            String nombreInspector,
            long cantidad) {
    }

    public record ConteoPorPeriodo(
            String periodo,
            long cantidad) {
    }
}
