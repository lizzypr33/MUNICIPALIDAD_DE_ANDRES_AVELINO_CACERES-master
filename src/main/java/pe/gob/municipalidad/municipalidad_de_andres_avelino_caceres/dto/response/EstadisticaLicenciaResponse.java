package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Estadísticas consolidadas de licencias de funcionamiento emitidas.
 */
public record EstadisticaLicenciaResponse(
        long totalEmitidas,
        long totalVigentes,
        long totalSuspendidas,
        long totalRevocadas,
        List<ConteoMensual> porMesEmision,
        List<ConteoPorGiro> porGiroNegocio,
        LocalDate fechaDesde,
        LocalDate fechaHasta) {

    public record ConteoMensual(
            String periodo,
            long cantidad) {
    }

    public record ConteoPorGiro(
            String giroNegocio,
            long cantidad) {
    }
}
