package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper;

import org.springframework.stereotype.Component;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.DocumentoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Documento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Requisito;

/**
 * Convierte entidades {@link Documento} en DTO de respuesta sin exponer rutas internas del servidor.
 */
@Component
public class DocumentoMapper {

    public DocumentoResponse toResponse(Documento d) {
        if (d == null) {
            return null;
        }
        Requisito r = d.getRequisito();
        Long solicitudId = d.getSolicitud() != null ? d.getSolicitud().getId() : null;
        return new DocumentoResponse(
                d.getId(),
                solicitudId,
                r != null ? r.getId() : null,
                r != null ? r.getCodigo() : null,
                r != null ? r.getNombre() : null,
                d.getNombreOriginal(),
                d.getTipoMime(),
                d.getTamanioBytes(),
                d.getFechaSubida(),
                d.getEstado(),
                d.getObservaciones(),
                d.isActivo());
    }
}
