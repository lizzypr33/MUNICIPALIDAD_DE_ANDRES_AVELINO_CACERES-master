package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de registro de un nuevo expediente en estado BORRADOR.
 * No permite definir id, número de expediente, estado ni funcionario responsable.
 */
public record SolicitudRequest(
        @NotNull(message = "El ciudadano titular es obligatorio") Long ciudadanoId,
        @NotNull(message = "El establecimiento comercial es obligatorio") Long establecimientoId,
        @NotNull(message = "El tipo de trámite es obligatorio") Long tipoTramiteId,
        Long representanteId,
        @Size(max = 2000, message = "Las observaciones no deben superar los 2000 caracteres") String observaciones) {
}
