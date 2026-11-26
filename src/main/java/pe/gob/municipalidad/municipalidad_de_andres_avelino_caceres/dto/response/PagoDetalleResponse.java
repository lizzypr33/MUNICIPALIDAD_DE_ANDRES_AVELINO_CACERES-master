package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoPago;

/**
 * Respuesta detallada de un pago municipal con su historial de cambios de estado.
 */
public record PagoDetalleResponse(
        Long id,
        Long solicitudId,
        String numeroExpediente,
        Long ciudadanoId,
        String nombreCiudadano,
        Long establecimientoId,
        String nombreComercialEstablecimiento,
        String codigoOperacion,
        String concepto,
        BigDecimal monto,
        String moneda,
        LocalDateTime fechaPago,
        String medioPago,
        EstadoPago estado,
        String observaciones,
        String motivoRechazoAnulacion,
        Long usuarioVerificadorId,
        String nombreUsuarioVerificador,
        LocalDateTime fechaVerificacion,
        LocalDateTime fechaRegistro,
        List<HistorialPagoItem> historial) {

    public record HistorialPagoItem(
            Long id,
            Long pagoId,
            EstadoPago estadoAnterior,
            EstadoPago estadoNuevo,
            String motivo,
            Long usuarioResponsableId,
            String nombreUsuarioResponsable,
            LocalDateTime fechaCambio) {
    }
}
