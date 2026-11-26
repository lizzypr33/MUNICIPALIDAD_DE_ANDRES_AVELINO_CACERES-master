package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud para suspender, reactivar o revocar una licencia de funcionamiento,
 * o cancelar una inspección programada, con motivo administrativo obligatorio.
 */
public record CambiarEstadoLicenciaRequest(
        @NotBlank(message = "El motivo administrativo es obligatorio") @Size(max = 2000) String motivo,
        @Size(max = 2000) String observaciones) {

    public CambiarEstadoLicenciaRequest(String motivo) {
        this(motivo, null);
    }
}
