package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

import java.time.LocalDateTime;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoSolicitud;

/** Respuesta resumida al consultar una solicitud por su número de expediente. */
public record ExpedienteResponse(
        Long solicitudId,
        String numeroExpediente,
        EstadoSolicitud estado,
        LocalDateTime fechaPresentacion,
        LocalDateTime fechaActualizacion,
        Long ciudadanoId,
        String ciudadanoNombreCompleto,
        String ciudadanoNumeroDocumento,
        Long establecimientoId,
        String establecimientoNombreComercial,
        Long tipoTramiteId,
        String tipoTramiteCodigo,
        String tipoTramiteNombre,
        Long usuarioResponsableId,
        String usuarioResponsableNombreCompleto,
        String observaciones,
        String motivoRechazo) {
}
