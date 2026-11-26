package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper;

import org.springframework.stereotype.Component;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.RolResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Rol;

@Component
public class RolMapper {

    public RolResponse toResponse(Rol rol) {
        return new RolResponse(rol.getId(), rol.getNombre(), rol.getDescripcion(), rol.isActivo());
    }
}
