package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoLicencia;

/**
 * Respuesta pública de verificación de autenticidad de licencia mediante código QR.
 * Excluye deliberadamente DNI, teléfonos, correos personales, documentos adjuntos y auditoría interna.
 */
public record VerificacionLicenciaResponse(
        boolean autentica,
        boolean vigente,
        String codigoLicencia,
        EstadoLicencia estado,
        String nombreComercial,
        String direccionEstablecimiento,
        String giroNegocio,
        LocalDate fechaEmision,
        LocalDate fechaVencimiento,
        String vigencia,
        String resultadoVerificacion,
        LocalDateTime fechaConsulta) {
}
