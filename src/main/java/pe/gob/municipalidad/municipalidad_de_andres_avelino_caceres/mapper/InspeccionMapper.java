package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper;

import org.springframework.stereotype.Component;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.InspeccionDetalleResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.InspeccionResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Ciudadano;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Establecimiento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Inspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Solicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Usuario;

/**
 * Convierte entidades {@link Inspeccion} en DTO de respuesta.
 */
@Component
public class InspeccionMapper {

    public InspeccionResponse toResponse(Inspeccion i) {
        if (i == null) {
            return null;
        }
        Solicitud s = i.getSolicitud();
        Establecimiento e = s != null ? s.getEstablecimiento() : null;
        Usuario inspector = i.getInspector();

        return new InspeccionResponse(
                i.getId(),
                s != null ? s.getId() : null,
                s != null ? s.getNumeroExpediente() : null,
                e != null ? e.getNombreComercial() : null,
                inspector != null ? inspector.getId() : null,
                inspector != null ? inspector.getNombres() + " " + inspector.getApellidos() : null,
                i.getFechaProgramada(),
                i.getFechaRealizada(),
                i.getEstado(),
                i.getResultado());
    }

    public InspeccionDetalleResponse toDetalleResponse(Inspeccion i) {
        if (i == null) {
            return null;
        }
        Solicitud s = i.getSolicitud();
        Ciudadano c = s != null ? s.getCiudadano() : null;
        Establecimiento e = s != null ? s.getEstablecimiento() : null;
        Usuario inspector = i.getInspector();

        return new InspeccionDetalleResponse(
                i.getId(),
                s != null ? s.getId() : null,
                s != null ? s.getNumeroExpediente() : null,
                c != null ? c.getId() : null,
                c != null ? c.getNombres() + " " + c.getApellidos() : null,
                e != null ? e.getId() : null,
                e != null ? e.getNombreComercial() : null,
                e != null ? e.getDireccion() : null,
                inspector != null ? inspector.getId() : null,
                inspector != null ? inspector.getNombres() + " " + inspector.getApellidos() : null,
                i.getFechaProgramada(),
                i.getFechaRealizada(),
                i.getEstado(),
                i.getResultado(),
                i.getObservaciones(),
                i.getRecomendaciones(),
                i.getMotivoReprogramacion(),
                i.getFechaCreacion(),
                i.getFechaActualizacion());
    }
}
