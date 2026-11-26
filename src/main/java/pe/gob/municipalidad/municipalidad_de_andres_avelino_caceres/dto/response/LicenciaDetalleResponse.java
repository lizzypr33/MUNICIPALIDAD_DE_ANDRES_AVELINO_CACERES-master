package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoLicencia;

/**
 * Respuesta detallada de una licencia de funcionamiento municipal con su historial de estados.
 */
public record LicenciaDetalleResponse(
        Long id,
        String codigoLicencia,
        String tokenVerificacion,
        String rutaVerificacionPublica,
        Long solicitudId,
        String numeroExpediente,
        Long ciudadanoId,
        String nombreTitular,
        String numeroDocumentoTitular,
        Long establecimientoId,
        String nombreComercial,
        String razonSocial,
        String ruc,
        String direccionEstablecimiento,
        String giroNegocio,
        BigDecimal areaMetrosCuadrados,
        Long tipoTramiteId,
        String codigoTipoTramite,
        String nombreTipoTramite,
        LocalDate fechaEmision,
        LocalDate fechaVencimiento,
        EstadoLicencia estado,
        String observaciones,
        Long usuarioEmisorId,
        String nombreUsuarioEmisor,
        List<HistorialLicenciaResponse> historial) {
}
