package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoInspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoLicencia;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoPago;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoSolicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.ResultadoInspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoDocumento;

/**
 * Envoltorio tipado para los reportes paginados municipales con metadatos y filas específicas por módulo.
 */
public record ReporteResponse<T>(
        String tituloReporte,
        LocalDateTime fechaGeneracion,
        Map<String, String> filtrosAplicados,
        PaginaResponse<T> datos) {

    public record FilaSolicitudReporte(
            Long id,
            String numeroExpediente,
            LocalDateTime fechaPresentacion,
            String tipoTramite,
            String establecimiento,
            EstadoSolicitud estado,
            String responsable) {
    }

    public record FilaLicenciaReporte(
            Long id,
            String codigoLicencia,
            String numeroExpediente,
            String establecimiento,
            String giro,
            LocalDate fechaEmision,
            EstadoLicencia estado) {
    }

    public record FilaInspeccionReporte(
            Long id,
            String numeroExpedienteSolicitud,
            String inspector,
            LocalDateTime fechaProgramada,
            LocalDateTime fechaRealizada,
            EstadoInspeccion estado,
            ResultadoInspeccion resultado) {
    }

    public record FilaPagoReporte(
            Long id,
            String codigoOperacion,
            String numeroExpediente,
            String concepto,
            BigDecimal monto,
            String moneda,
            LocalDateTime fecha,
            EstadoPago estado) {
    }

    public record FilaEstablecimientoReporte(
            Long id,
            String nombreComercial,
            String razonSocial,
            String ruc,
            String direccion,
            String giro,
            String estado) {
    }

    public record FilaCiudadanoReporte(
            Long id,
            String nombreCompleto,
            TipoDocumento tipoDocumento,
            String documentoEnmascarado,
            String estado,
            LocalDateTime fechaRegistro) {
    }
}
