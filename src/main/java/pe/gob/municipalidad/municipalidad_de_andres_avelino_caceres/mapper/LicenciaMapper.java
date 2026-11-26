package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.HistorialLicenciaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.LicenciaDetalleResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.LicenciaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.VerificacionLicenciaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Ciudadano;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Establecimiento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.HistorialLicencia;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Licencia;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Solicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.TipoTramite;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Usuario;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoLicencia;

/**
 * Convierte entidades {@link Licencia} y {@link HistorialLicencia} en DTO de respuesta.
 */
@Component
public class LicenciaMapper {

    public LicenciaResponse toResponse(Licencia l) {
        if (l == null) {
            return null;
        }
        Solicitud s = l.getSolicitud();
        Ciudadano c = s != null ? s.getCiudadano() : null;
        Establecimiento e = s != null ? s.getEstablecimiento() : null;
        Usuario emisor = l.getUsuarioEmisor();

        return new LicenciaResponse(
                l.getId(),
                l.getCodigoLicencia(),
                s != null ? s.getId() : null,
                s != null ? s.getNumeroExpediente() : null,
                c != null ? c.getId() : null,
                c != null ? c.getNombres() + " " + c.getApellidos() : null,
                e != null ? e.getId() : null,
                e != null ? e.getNombreComercial() : null,
                e != null ? e.getRuc() : null,
                e != null ? e.getGiroNegocio() : null,
                l.getFechaEmision(),
                l.getFechaVencimiento(),
                l.getEstado(),
                emisor != null ? emisor.getId() : null,
                emisor != null ? emisor.getNombres() + " " + emisor.getApellidos() : null);
    }

    public LicenciaDetalleResponse toDetalleResponse(Licencia l, List<HistorialLicenciaResponse> historial) {
        if (l == null) {
            return null;
        }
        Solicitud s = l.getSolicitud();
        Ciudadano c = s != null ? s.getCiudadano() : null;
        Establecimiento e = s != null ? s.getEstablecimiento() : null;
        TipoTramite t = s != null ? s.getTipoTramite() : null;
        Usuario emisor = l.getUsuarioEmisor();
        String rutaVerificacion = "/api/public/licencias/verificar/" + l.getTokenVerificacion();

        return new LicenciaDetalleResponse(
                l.getId(),
                l.getCodigoLicencia(),
                l.getTokenVerificacion(),
                rutaVerificacion,
                s != null ? s.getId() : null,
                s != null ? s.getNumeroExpediente() : null,
                c != null ? c.getId() : null,
                c != null ? c.getNombres() + " " + c.getApellidos() : null,
                c != null ? c.getNumeroDocumento() : null,
                e != null ? e.getId() : null,
                e != null ? e.getNombreComercial() : null,
                e != null ? e.getRazonSocial() : null,
                e != null ? e.getRuc() : null,
                e != null ? e.getDireccion() : null,
                e != null ? e.getGiroNegocio() : null,
                e != null ? e.getAreaMetrosCuadrados() : null,
                t != null ? t.getId() : null,
                t != null ? t.getCodigo() : null,
                t != null ? t.getNombre() : null,
                l.getFechaEmision(),
                l.getFechaVencimiento(),
                l.getEstado(),
                l.getObservaciones(),
                emisor != null ? emisor.getId() : null,
                emisor != null ? emisor.getNombres() + " " + emisor.getApellidos() : null,
                historial != null ? List.copyOf(historial) : List.of());
    }

    public HistorialLicenciaResponse toHistorialResponse(HistorialLicencia h) {
        if (h == null) {
            return null;
        }
        Usuario u = h.getUsuarioResponsable();
        return new HistorialLicenciaResponse(
                h.getId(),
                h.getLicencia() != null ? h.getLicencia().getId() : null,
                h.getEstadoAnterior(),
                h.getEstadoNuevo(),
                h.getMotivo(),
                u != null ? u.getId() : null,
                u != null ? u.getNombres() + " " + u.getApellidos() : null,
                h.getFechaCambio());
    }

    public VerificacionLicenciaResponse toVerificacionPublicaResponse(Licencia l) {
        if (l == null) {
            return null;
        }
        Solicitud s = l.getSolicitud();
        Establecimiento e = s != null ? s.getEstablecimiento() : null;

        boolean expiradaPorFecha = l.getFechaVencimiento() != null
                && l.getFechaVencimiento().isBefore(LocalDate.now());
        boolean vigente = l.getEstado() == EstadoLicencia.VIGENTE && !expiradaPorFecha;

        String vigencia = l.getFechaVencimiento() != null
                ? "HASTA " + l.getFechaVencimiento()
                : "INDETERMINADA";

        String resultadoVerificacion;
        if (vigente) {
            resultadoVerificacion = "Licencia de funcionamiento auténtica y VIGENTE.";
        } else if (expiradaPorFecha) {
            resultadoVerificacion = "Licencia registrada con vigencia expirada (" + l.getFechaVencimiento() + ").";
        } else {
            resultadoVerificacion = "Licencia registrada en estado " + l.getEstado() + ".";
        }

        return new VerificacionLicenciaResponse(
                true,
                vigente,
                l.getCodigoLicencia(),
                expiradaPorFecha && l.getEstado() == EstadoLicencia.VIGENTE ? EstadoLicencia.VENCIDA : l.getEstado(),
                e != null ? e.getNombreComercial() : null,
                e != null ? e.getDireccion() : null,
                e != null ? e.getGiroNegocio() : null,
                l.getFechaEmision(),
                l.getFechaVencimiento(),
                vigencia,
                resultadoVerificacion,
                LocalDateTime.now());
    }
}
