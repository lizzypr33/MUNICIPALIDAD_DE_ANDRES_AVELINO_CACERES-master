package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper;

import org.springframework.stereotype.Component;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.RequisitoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Requisito;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.TipoTramiteRequisito;

/** Convierte entidades {@link Requisito} y {@link TipoTramiteRequisito} en DTO de respuesta. */
@Component
public class RequisitoMapper {

    public RequisitoResponse toResponse(Requisito r) {
        if (r == null) {
            return null;
        }
        return new RequisitoResponse(
                r.getId(),
                r.getCodigo(),
                r.getNombre(),
                r.getDescripcion(),
                r.isActivo(),
                null,
                null);
    }

    public RequisitoResponse toResponse(TipoTramiteRequisito ttr) {
        if (ttr == null || ttr.getRequisito() == null) {
            return null;
        }
        Requisito r = ttr.getRequisito();
        return new RequisitoResponse(
                r.getId(),
                r.getCodigo(),
                r.getNombre(),
                r.getDescripcion(),
                r.isActivo(),
                ttr.isObligatorio(),
                ttr.getOrdenVisualizacion());
    }
}
