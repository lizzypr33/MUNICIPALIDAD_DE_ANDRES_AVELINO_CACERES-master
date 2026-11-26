package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper;

import org.springframework.stereotype.Component;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.TipoTramiteResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.TipoTramite;

/** Convierte entidades {@link TipoTramite} en DTO de respuesta. */
@Component
public class TipoTramiteMapper {

    public TipoTramiteResponse toResponse(TipoTramite t) {
        if (t == null) {
            return null;
        }
        return new TipoTramiteResponse(
                t.getId(),
                t.getCodigo(),
                t.getNombre(),
                t.getDescripcion(),
                t.isActivo(),
                t.getFechaCreacion());
    }
}
