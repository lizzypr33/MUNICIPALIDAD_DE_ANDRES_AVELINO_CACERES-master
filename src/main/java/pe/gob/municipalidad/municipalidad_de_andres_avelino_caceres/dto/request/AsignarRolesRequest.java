package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record AsignarRolesRequest(@NotEmpty Set<@NotNull Long> rolIds) {
}
