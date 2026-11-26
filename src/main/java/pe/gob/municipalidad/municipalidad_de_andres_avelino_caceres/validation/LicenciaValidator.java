package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.validation;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Ciudadano;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Establecimiento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Inspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Licencia;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Solicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.TipoTramite;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoInspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoLicencia;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoSolicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.ResultadoInspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ConflictException;

/**
 * Validador de condiciones previas de emisión y transiciones de estado para {@link Licencia}.
 */
@Component
public class LicenciaValidator {

    private static final Map<EstadoLicencia, Set<EstadoLicencia>> TRANSICIONES_PERMITIDAS;

    static {
        Map<EstadoLicencia, Set<EstadoLicencia>> mapa = new EnumMap<>(EstadoLicencia.class);
        mapa.put(EstadoLicencia.VIGENTE,
                EnumSet.of(EstadoLicencia.SUSPENDIDA, EstadoLicencia.REVOCADA,
                        EstadoLicencia.VENCIDA, EstadoLicencia.ANULADA));
        mapa.put(EstadoLicencia.SUSPENDIDA,
                EnumSet.of(EstadoLicencia.VIGENTE, EstadoLicencia.REVOCADA, EstadoLicencia.ANULADA));
        mapa.put(EstadoLicencia.VENCIDA,
                EnumSet.of(EstadoLicencia.REVOCADA, EstadoLicencia.ANULADA));
        mapa.put(EstadoLicencia.REVOCADA, EnumSet.noneOf(EstadoLicencia.class));
        mapa.put(EstadoLicencia.ANULADA, EnumSet.noneOf(EstadoLicencia.class));
        TRANSICIONES_PERMITIDAS = Map.copyOf(mapa);
    }

    /**
     * Verifica todas las condiciones previas exigidas antes de emitir una licencia de funcionamiento.
     */
    public void validarCondicionesPreviasEmision(Solicitud solicitud,
            List<Inspeccion> inspeccionesSolicitud,
            boolean licenciaYaEmitida) {

        if (solicitud == null) {
            throw new BusinessException("La solicitud es obligatoria para emitir una licencia.");
        }
        if (licenciaYaEmitida) {
            throw new ConflictException(
                    "Ya existe una licencia de funcionamiento emitida para la solicitud con id " + solicitud.getId()
                            + ".");
        }
        if (solicitud.getEstado() != EstadoSolicitud.APROBADA) {
            throw new ConflictException(
                    "No se puede emitir una licencia para una solicitud que no se encuentra APROBADA (estado actual: "
                            + solicitud.getEstado() + ").");
        }

        Ciudadano titular = solicitud.getCiudadano();
        if (titular == null || !titular.isActivo()) {
            throw new BusinessException("El ciudadano titular de la solicitud no es válido o se encuentra inactivo.");
        }

        Establecimiento establecimiento = solicitud.getEstablecimiento();
        if (establecimiento == null || !establecimiento.isActivo()) {
            throw new BusinessException(
                    "El establecimiento comercial de la solicitud no es válido o se encuentra inactivo.");
        }

        TipoTramite tipoTramite = solicitud.getTipoTramite();
        if (tipoTramite == null || !tipoTramite.isActivo()) {
            throw new BusinessException("El tipo de trámite asociado a la solicitud no es válido o está inactivo.");
        }

        // Validar inspección únicamente cuando el tipo de trámite la requiera
        if (tipoTramite.isRequiereInspeccion()) {
            List<Inspeccion> lista = inspeccionesSolicitud != null ? inspeccionesSolicitud : List.of();
            boolean tienePendiente = lista.stream()
                    .anyMatch(i -> i.getEstado() == EstadoInspeccion.PROGRAMADA
                            || i.getEstado() == EstadoInspeccion.REPROGRAMADA
                            || i.getEstado() == EstadoInspeccion.EN_PROCESO);
            if (tienePendiente) {
                throw new ConflictException(
                        "La solicitud tiene una inspección técnica en curso o pendiente de finalización.");
            }

            Inspeccion ultimaFinalizada = lista.stream()
                    .filter(i -> i.getEstado() == EstadoInspeccion.FINALIZADA
                            || i.getEstado() == EstadoInspeccion.REALIZADA)
                    .max(Comparator.comparing(Inspeccion::getId, Comparator.nullsFirst(Comparator.naturalOrder())))
                    .orElse(null);

            if (ultimaFinalizada == null || ultimaFinalizada.getResultado() != ResultadoInspeccion.FAVORABLE) {
                throw new ConflictException(
                        "El procedimiento requiere una inspección técnica finalizada con resultado FAVORABLE para emitir la licencia.");
            }
        }
    }

    public void validarFechasVigencia(LocalDate fechaEmision, LocalDate fechaVencimiento) {
        if (fechaEmision == null) {
            throw new BusinessException("La fecha de emisión de la licencia es obligatoria.");
        }
        if (fechaVencimiento != null && fechaVencimiento.isBefore(fechaEmision)) {
            throw new BusinessException(
                    "La fecha de vencimiento de la licencia no puede ser anterior a la fecha de emisión.");
        }
    }

    public void validarCambioEstado(Licencia licencia, EstadoLicencia estadoNuevo, String motivo) {
        if (licencia == null || estadoNuevo == null) {
            throw new BusinessException("La licencia y el nuevo estado son obligatorios.");
        }
        if (!StringUtils.hasText(motivo)) {
            throw new BusinessException("El motivo administrativo para el cambio de estado de la licencia es obligatorio.");
        }

        EstadoLicencia estadoActual = licencia.getEstado();
        Set<EstadoLicencia> permitidos = TRANSICIONES_PERMITIDAS.getOrDefault(estadoActual, Set.of());
        if (!permitidos.contains(estadoNuevo)) {
            throw new ConflictException(
                    "No se permite cambiar el estado de la licencia de " + estadoActual + " a " + estadoNuevo + ".");
        }

        if (estadoNuevo == EstadoLicencia.VIGENTE
                && licencia.getFechaVencimiento() != null
                && licencia.getFechaVencimiento().isBefore(LocalDate.now())) {
            throw new ConflictException(
                    "No se puede reactivar una licencia cuya fecha de vencimiento (" + licencia.getFechaVencimiento()
                            + ") ya ha expirado.");
        }
    }
}
