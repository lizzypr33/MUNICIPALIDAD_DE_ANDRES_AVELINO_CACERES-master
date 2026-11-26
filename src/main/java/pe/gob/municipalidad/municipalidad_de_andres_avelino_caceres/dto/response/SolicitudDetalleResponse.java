package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoSolicitud;

/** Respuesta detallada de una solicitud municipal incluyendo entidades vinculadas, documentos e historial. */
public record SolicitudDetalleResponse(
        Long id,
        String numeroExpediente,
        EstadoSolicitud estado,
        LocalDateTime fechaPresentacion,
        LocalDateTime fechaActualizacion,
        CiudadanoResponse ciudadano,
        EstablecimientoResponse establecimiento,
        TipoTramiteResponse tipoTramite,
        RepresentanteResponse representante,
        Long usuarioResponsableId,
        String usuarioResponsableNombreCompleto,
        String observaciones,
        String motivoRechazo,
        List<DocumentoResponse> documentos,
        List<HistorialSolicitudResponse> historial) {
}
