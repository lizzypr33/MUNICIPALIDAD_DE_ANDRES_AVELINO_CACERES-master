package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.DocumentoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ExpedienteResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.HistorialSolicitudResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.SolicitudDetalleResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.SolicitudResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Ciudadano;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Establecimiento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.HistorialSolicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Representante;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Solicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.TipoTramite;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Usuario;

/**
 * Convierte entidades {@link Solicitud} y {@link HistorialSolicitud} en sus DTO de respuesta.
 */
@Component
public class SolicitudMapper {

    private final CiudadanoMapper ciudadanoMapper;
    private final EstablecimientoMapper establecimientoMapper;
    private final TipoTramiteMapper tipoTramiteMapper;
    private final RepresentanteMapper representanteMapper;

    public SolicitudMapper(CiudadanoMapper ciudadanoMapper,
            EstablecimientoMapper establecimientoMapper,
            TipoTramiteMapper tipoTramiteMapper,
            RepresentanteMapper representanteMapper) {
        this.ciudadanoMapper = ciudadanoMapper;
        this.establecimientoMapper = establecimientoMapper;
        this.tipoTramiteMapper = tipoTramiteMapper;
        this.representanteMapper = representanteMapper;
    }

    public SolicitudResponse toResponse(Solicitud s) {
        Ciudadano c = s.getCiudadano();
        Establecimiento e = s.getEstablecimiento();
        TipoTramite t = s.getTipoTramite();
        Representante r = s.getRepresentante();
        Usuario u = s.getUsuarioResponsable();

        return new SolicitudResponse(
                s.getId(),
                s.getNumeroExpediente(),
                s.getEstado(),
                s.getFechaPresentacion(),
                s.getFechaActualizacion(),
                c != null ? c.getId() : null,
                c != null ? c.getNombres() + " " + c.getApellidos() : null,
                c != null ? c.getNumeroDocumento() : null,
                e != null ? e.getId() : null,
                e != null ? e.getNombreComercial() : null,
                t != null ? t.getId() : null,
                t != null ? t.getCodigo() : null,
                t != null ? t.getNombre() : null,
                r != null ? r.getId() : null,
                r != null ? r.getNombres() + " " + r.getApellidos() : null,
                u != null ? u.getId() : null,
                u != null ? u.getNombres() + " " + u.getApellidos() : null,
                s.getObservaciones(),
                s.getMotivoRechazo());
    }

    public ExpedienteResponse toExpedienteResponse(Solicitud s) {
        Ciudadano c = s.getCiudadano();
        Establecimiento e = s.getEstablecimiento();
        TipoTramite t = s.getTipoTramite();
        Usuario u = s.getUsuarioResponsable();

        return new ExpedienteResponse(
                s.getId(),
                s.getNumeroExpediente(),
                s.getEstado(),
                s.getFechaPresentacion(),
                s.getFechaActualizacion(),
                c != null ? c.getId() : null,
                c != null ? c.getNombres() + " " + c.getApellidos() : null,
                c != null ? c.getNumeroDocumento() : null,
                e != null ? e.getId() : null,
                e != null ? e.getNombreComercial() : null,
                t != null ? t.getId() : null,
                t != null ? t.getCodigo() : null,
                t != null ? t.getNombre() : null,
                u != null ? u.getId() : null,
                u != null ? u.getNombres() + " " + u.getApellidos() : null,
                s.getObservaciones(),
                s.getMotivoRechazo());
    }

    public SolicitudDetalleResponse toDetalleResponse(Solicitud s,
            List<DocumentoResponse> documentos,
            List<HistorialSolicitudResponse> historial) {
        Usuario u = s.getUsuarioResponsable();
        return new SolicitudDetalleResponse(
                s.getId(),
                s.getNumeroExpediente(),
                s.getEstado(),
                s.getFechaPresentacion(),
                s.getFechaActualizacion(),
                s.getCiudadano() != null ? ciudadanoMapper.toResponse(s.getCiudadano()) : null,
                s.getEstablecimiento() != null ? establecimientoMapper.toResponse(s.getEstablecimiento()) : null,
                s.getTipoTramite() != null ? tipoTramiteMapper.toResponse(s.getTipoTramite()) : null,
                s.getRepresentante() != null ? representanteMapper.toResponse(s.getRepresentante()) : null,
                u != null ? u.getId() : null,
                u != null ? u.getNombres() + " " + u.getApellidos() : null,
                s.getObservaciones(),
                s.getMotivoRechazo(),
                documentos,
                historial);
    }

    public HistorialSolicitudResponse toHistorialResponse(HistorialSolicitud h) {
        Usuario u = h.getUsuarioResponsable();
        return new HistorialSolicitudResponse(
                h.getId(),
                h.getSolicitud() != null ? h.getSolicitud().getId() : null,
                h.getEstadoAnterior(),
                h.getEstadoNuevo(),
                u != null ? u.getId() : null,
                u != null ? u.getUsername() : null,
                h.getObservaciones(),
                h.getFechaCambio());
    }
}
