package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Resumen consolidado de pagos municipales para control administrativo y futuro dashboard.
 * El monto total confirmado incluye exclusivamente pagos en estado VERIFICADO en moneda PEN.
 */
public record ResumenPagosResponse(
        long cantidadTotalPagos,
        long totalPagosVerificados,
        BigDecimal montoTotalVerificado,
        String moneda,
        long cantidadPagosPendientes,
        long cantidadPagosRechazados,
        long cantidadPagosAnulados,
        LocalDate fechaDesde,
        LocalDate fechaHasta) {
}
