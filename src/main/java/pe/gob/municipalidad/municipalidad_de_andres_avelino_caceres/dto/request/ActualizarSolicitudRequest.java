package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Solicitud de actualización de una solicitud en estado BORRADOR.
 * No permite modificar id, número de expediente, estado, fecha de presentación ni usuario responsable.
 */
public record ActualizarSolicitudRequest(
        Long establecimientoId,
        Long tipoTramiteId,
        Long representanteId,
        @Size(max = 2000, message = "Las observaciones no deben superar los 2000 caracteres") String observaciones) {
}
