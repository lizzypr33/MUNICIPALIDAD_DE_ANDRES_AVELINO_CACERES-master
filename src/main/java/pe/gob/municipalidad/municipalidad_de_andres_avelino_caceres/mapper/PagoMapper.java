package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PagoDetalleResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PagoDetalleResponse.HistorialPagoItem;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PagoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Ciudadano;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Establecimiento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.HistorialPago;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Pago;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Solicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Usuario;

/**
 * Convierte entidades {@link Pago} y {@link HistorialPago} en DTO de respuesta.
 */
@Component
public class PagoMapper {

    public PagoResponse toResponse(Pago p) {
        if (p == null) {
            return null;
        }
        Solicitud s = p.getSolicitud();
        Ciudadano c = s != null ? s.getCiudadano() : null;
        Usuario verificador = p.getUsuarioVerificador();

        return new PagoResponse(
                p.getId(),
                s != null ? s.getId() : null,
                s != null ? s.getNumeroExpediente() : null,
                c != null ? c.getId() : null,
                c != null ? c.getNombres() + " " + c.getApellidos() : null,
                p.getCodigoOperacion(),
                p.getConcepto(),
                p.getMonto(),
                p.getMoneda(),
                p.getFechaPago(),
                p.getMedioPago(),
                p.getEstado(),
                verificador != null ? verificador.getId() : null,
                verificador != null ? verificador.getNombres() + " " + verificador.getApellidos() : null,
                p.getFechaVerificacion());
    }

    public PagoDetalleResponse toDetalleResponse(Pago p, List<HistorialPagoItem> historial) {
        if (p == null) {
            return null;
        }
        Solicitud s = p.getSolicitud();
        Ciudadano c = s != null ? s.getCiudadano() : null;
        Establecimiento e = s != null ? s.getEstablecimiento() : null;
        Usuario verificador = p.getUsuarioVerificador();

        return new PagoDetalleResponse(
                p.getId(),
                s != null ? s.getId() : null,
                s != null ? s.getNumeroExpediente() : null,
                c != null ? c.getId() : null,
                c != null ? c.getNombres() + " " + c.getApellidos() : null,
                e != null ? e.getId() : null,
                e != null ? e.getNombreComercial() : null,
                p.getCodigoOperacion(),
                p.getConcepto(),
                p.getMonto(),
                p.getMoneda(),
                p.getFechaPago(),
                p.getMedioPago(),
                p.getEstado(),
                p.getObservaciones(),
                p.getMotivoRechazoAnulacion(),
                verificador != null ? verificador.getId() : null,
                verificador != null ? verificador.getNombres() + " " + verificador.getApellidos() : null,
                p.getFechaVerificacion(),
                p.getFechaRegistro(),
                historial != null ? List.copyOf(historial) : List.of());
    }

    public HistorialPagoItem toHistorialItem(HistorialPago h) {
        if (h == null) {
            return null;
        }
        Usuario u = h.getUsuarioResponsable();
        return new HistorialPagoItem(
                h.getId(),
                h.getPago() != null ? h.getPago().getId() : null,
                h.getEstadoAnterior(),
                h.getEstadoNuevo(),
                h.getMotivo(),
                u != null ? u.getId() : null,
                u != null ? u.getNombres() + " " + u.getApellidos() : null,
                h.getFechaCambio());
    }
}
