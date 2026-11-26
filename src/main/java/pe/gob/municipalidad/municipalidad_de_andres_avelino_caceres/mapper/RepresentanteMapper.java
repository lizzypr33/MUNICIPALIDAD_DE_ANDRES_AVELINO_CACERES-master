package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper;

import org.springframework.stereotype.Component;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.RepresentanteResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Representante;

/** Convierte entidades {@link Representante} en DTO de respuesta. */
@Component
public class RepresentanteMapper {

    public RepresentanteResponse toResponse(Representante r) {
        return new RepresentanteResponse(
                r.getId(),
                r.getTipoDocumento(),
                r.getNumeroDocumento(),
                r.getNombres(),
                r.getApellidos(),
                r.getCargo(),
                r.getTelefono(),
                r.getEmail(),
                r.isActivo());
    }
}
