package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

import java.time.LocalDateTime;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoSolicitud;

/** Respuesta general de una solicitud municipal para listados y operaciones de transición. */
public record SolicitudResponse(
        Long id,
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
        Long representanteId,
        String representanteNombreCompleto,
        Long usuarioResponsableId,
        String usuarioResponsableNombreCompleto,
        String observaciones,
        String motivoRechazo) {
}
