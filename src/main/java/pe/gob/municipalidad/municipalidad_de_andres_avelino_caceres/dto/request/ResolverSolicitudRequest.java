package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Solicitud de resolución administrativa (aprobación, rechazo o archivo) de una solicitud.
 */
public record ResolverSolicitudRequest(
        @Size(max = 2000, message = "Las observaciones no deben superar los 2000 caracteres") String observaciones,
        @Size(max = 2000, message = "El motivo de rechazo no debe superar los 2000 caracteres") String motivoRechazo) {
}
