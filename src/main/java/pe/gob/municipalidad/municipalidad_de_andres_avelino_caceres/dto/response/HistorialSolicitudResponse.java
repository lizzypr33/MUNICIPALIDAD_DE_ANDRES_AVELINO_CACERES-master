package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

import java.time.LocalDateTime;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoSolicitud;

/** Respuesta de solo lectura con un registro del historial de transiciones de estado de una solicitud. */
public record HistorialSolicitudResponse(
        Long id,
        Long solicitudId,
        EstadoSolicitud estadoAnterior,
        EstadoSolicitud estadoNuevo,
        Long usuarioResponsableId,
        String usuarioResponsableUsername,
        String observaciones,
        LocalDateTime fechaCambio) {
}
