package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

import java.time.LocalDate;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoLicencia;

/**
 * Respuesta resumida de una licencia de funcionamiento municipal.
 */
public record LicenciaResponse(
        Long id,
        String codigoLicencia,
        Long solicitudId,
        String numeroExpediente,
        Long ciudadanoId,
        String nombreTitular,
        Long establecimientoId,
        String nombreComercial,
        String ruc,
        String giroNegocio,
        LocalDate fechaEmision,
        LocalDate fechaVencimiento,
        EstadoLicencia estado,
        Long usuarioEmisorId,
        String nombreUsuarioEmisor) {
}
