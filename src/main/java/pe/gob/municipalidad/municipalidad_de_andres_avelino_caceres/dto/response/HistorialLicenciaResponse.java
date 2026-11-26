package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

import java.time.LocalDateTime;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoLicencia;

/**
 * Registro histórico de transición de estado de una licencia de funcionamiento.
 */
public record HistorialLicenciaResponse(
        Long id,
        Long licenciaId,
        EstadoLicencia estadoAnterior,
        EstadoLicencia estadoNuevo,
        String motivo,
        Long usuarioResponsableId,
        String nombreUsuarioResponsable,
        LocalDateTime fechaCambio) {
}
