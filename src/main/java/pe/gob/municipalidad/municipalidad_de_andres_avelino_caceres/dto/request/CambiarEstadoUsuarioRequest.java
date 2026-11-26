package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoUsuarioRequest(@NotNull Boolean activo) {
}
