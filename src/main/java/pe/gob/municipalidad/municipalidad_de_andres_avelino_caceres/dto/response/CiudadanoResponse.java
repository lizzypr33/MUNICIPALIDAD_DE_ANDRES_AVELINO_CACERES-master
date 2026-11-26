package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

import java.time.LocalDateTime;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoDocumento;

/** Respuesta con la información de un ciudadano registrado. */
public record CiudadanoResponse(
        Long id,
        TipoDocumento tipoDocumento,
        String numeroDocumento,
        String nombres,
        String apellidos,
        String email,
        String telefono,
        String direccion,
        boolean activo,
        LocalDateTime fechaRegistro,
        Long usuarioId) {
}
