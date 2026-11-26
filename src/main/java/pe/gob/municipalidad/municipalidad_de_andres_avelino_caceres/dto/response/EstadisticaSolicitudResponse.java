package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Estadísticas consolidadas de solicitudes municipales para gráficos del dashboard.
 */
public record EstadisticaSolicitudResponse(
        long total,
        Map<String, Long> porEstado,
        List<ConteoMensual> porMes,
        List<ConteoPorTipoTramite> porTipoTramite,
        LocalDate fechaDesde,
        LocalDate fechaHasta) {

    public record ConteoMensual(
            String periodo,
            long cantidad) {
    }

    public record ConteoPorTipoTramite(
            Long tipoTramiteId,
            String codigoTramite,
            String nombreTramite,
            long cantidad) {
    }
}
