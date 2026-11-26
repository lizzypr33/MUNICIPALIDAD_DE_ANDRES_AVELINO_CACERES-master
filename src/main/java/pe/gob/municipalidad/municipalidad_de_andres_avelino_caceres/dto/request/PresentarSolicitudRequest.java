package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Datos opcionales al presentar una solicitud en estado BORRADOR.
 */
public record PresentarSolicitudRequest(
        @Size(max = 2000, message = "Las observaciones no deben superar los 2000 caracteres") String observaciones) {
}
