package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper;

import org.springframework.stereotype.Component;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.CiudadanoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Ciudadano;

/** Convierte entidades {@link Ciudadano} en DTO de respuesta. */
@Component
public class CiudadanoMapper {

    public CiudadanoResponse toResponse(Ciudadano c) {
        Long usuarioId = c.getUsuario() != null ? c.getUsuario().getId() : null;
        return new CiudadanoResponse(
                c.getId(),
                c.getTipoDocumento(),
                c.getNumeroDocumento(),
                c.getNombres(),
                c.getApellidos(),
                c.getEmail(),
                c.getTelefono(),
                c.getDireccion(),
                c.isActivo(),
                c.getFechaRegistro(),
                usuarioId);
    }
}
