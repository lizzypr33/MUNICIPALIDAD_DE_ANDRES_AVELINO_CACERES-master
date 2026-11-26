package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

import java.math.BigDecimal;

/**
 * Respuesta consolidada del dashboard administrativo municipal (PROMPT 08 - Sección 5 y 6).
 */
public record DashboardResumenResponse(
        MetricasCiudadanos ciudadanos,
        MetricasEstablecimientos establecimientos,
        MetricasSolicitudes solicitudes,
        MetricasLicencias licencias,
        MetricasInspecciones inspecciones,
        MetricasPagos pagos) {

    public record MetricasCiudadanos(
            long total) {
    }

    public record MetricasEstablecimientos(
            long total) {
    }

    public record MetricasSolicitudes(
            long total,
            long pendientes,
            long observadas,
            long aprobadas,
            long rechazadas) {
    }

    public record MetricasLicencias(
            long total,
            long vigentes,
            long suspendidas) {
    }

    public record MetricasInspecciones(
            long programadas,
            long finalizadas) {
    }

    public record MetricasPagos(
            long pendientes,
            long verificados,
            BigDecimal montoVerificadoPEN) {
    }
}
