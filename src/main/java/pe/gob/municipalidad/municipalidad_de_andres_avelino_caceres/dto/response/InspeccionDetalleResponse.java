package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

import java.time.LocalDateTime;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoInspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.ResultadoInspeccion;

/**
 * Respuesta detallada de una inspección técnica municipal.
 */
public record InspeccionDetalleResponse(
        Long id,
        Long solicitudId,
        String numeroExpediente,
        Long ciudadanoId,
        String nombreCiudadano,
        Long establecimientoId,
        String nombreComercialEstablecimiento,
        String direccionEstablecimiento,
        Long inspectorId,
        String nombreInspector,
        LocalDateTime fechaProgramada,
        LocalDateTime fechaRealizada,
        EstadoInspeccion estado,
        ResultadoInspeccion resultado,
        String observaciones,
        String recomendaciones,
        String motivoReprogramacion,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion) {
}
