package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoPago;

/**
 * Respuesta resumida de un registro de pago municipal.
 */
public record PagoResponse(
        Long id,
        Long solicitudId,
        String numeroExpediente,
        Long ciudadanoId,
        String nombreCiudadano,
        String codigoOperacion,
        String concepto,
        BigDecimal monto,
        String moneda,
        LocalDateTime fechaPago,
        String medioPago,
        EstadoPago estado,
        Long usuarioVerificadorId,
        String nombreUsuarioVerificador,
        LocalDateTime fechaVerificacion) {
}
