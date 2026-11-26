package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Estadísticas financieras de pagos municipales utilizando exclusivamente BigDecimal en moneda PEN.
 */
public record EstadisticaPagoResponse(
        long cantidadRegistrados,
        long cantidadVerificados,
        long cantidadPendientes,
        long cantidadRechazados,
        long cantidadAnulados,
        BigDecimal montoVerificadoPEN,
        String moneda,
        List<MontoMensual> porMes,
        List<MontoPorConcepto> porConcepto,
        LocalDate fechaDesde,
        LocalDate fechaHasta) {

    public record MontoMensual(
            String periodo,
            long cantidadVerificados,
            BigDecimal montoVerificadoPEN) {
    }

    public record MontoPorConcepto(
            String concepto,
            long cantidadVerificados,
            BigDecimal montoVerificadoPEN) {
    }
}
