package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoDocumento;

/** Respuesta con la información de un representante autorizado. */
public record RepresentanteResponse(
        Long id,
        TipoDocumento tipoDocumento,
        String numeroDocumento,
        String nombres,
        String apellidos,
        String cargo,
        String telefono,
        String email,
        boolean activo) {
}
