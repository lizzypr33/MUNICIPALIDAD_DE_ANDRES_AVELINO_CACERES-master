package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de asignación de funcionario responsable para revisión del expediente.
 */
public record AsignarFuncionarioRequest(
        @NotNull(message = "El identificador del funcionario responsable es obligatorio") Long usuarioResponsableId,
        @Size(max = 2000, message = "Las observaciones no deben superar los 2000 caracteres") String observaciones) {
}
