package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

import java.time.LocalDateTime;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoInspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.ResultadoInspeccion;

/**
 * Respuesta resumida de una inspección técnica municipal.
 */
public record InspeccionResponse(
        Long id,
        Long solicitudId,
        String numeroExpediente,
        String nombreComercialEstablecimiento,
        Long inspectorId,
        String nombreInspector,
        LocalDateTime fechaProgramada,
        LocalDateTime fechaRealizada,
        EstadoInspeccion estado,
        ResultadoInspeccion resultado) {
}
