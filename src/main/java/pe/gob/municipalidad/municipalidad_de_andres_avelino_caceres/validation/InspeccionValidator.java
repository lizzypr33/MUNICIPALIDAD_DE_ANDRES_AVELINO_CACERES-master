package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.validation;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Inspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Solicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Usuario;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoInspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoSolicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.ResultadoInspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ConflictException;

/**
 * Validador de reglas operativas, estados y permisos para inspecciones municipales.
 */
@Component
public class InspeccionValidator {

    private static final Set<String> ROLES_INSPECTOR_AUTORIZADOS = Set.of(
            "FUNCIONARIO", "SUPERVISOR", "ADMINISTRADOR");

    private static final Set<EstadoSolicitud> ESTADOS_SOLICITUD_COMPATIBLES = EnumSet.of(
            EstadoSolicitud.PRESENTADA,
            EstadoSolicitud.EN_REVISION,
            EstadoSolicitud.OBSERVADA,
            EstadoSolicitud.SUBSANADA);

    private static final Map<EstadoInspeccion, Set<EstadoInspeccion>> TRANSICIONES_PERMITIDAS;

    static {
        Map<EstadoInspeccion, Set<EstadoInspeccion>> mapa = new EnumMap<>(EstadoInspeccion.class);
        mapa.put(EstadoInspeccion.PROGRAMADA,
                EnumSet.of(EstadoInspeccion.EN_PROCESO, EstadoInspeccion.FINALIZADA, EstadoInspeccion.CANCELADA));
        mapa.put(EstadoInspeccion.REPROGRAMADA,
                EnumSet.of(EstadoInspeccion.PROGRAMADA, EstadoInspeccion.EN_PROCESO, EstadoInspeccion.FINALIZADA,
                        EstadoInspeccion.CANCELADA));
        mapa.put(EstadoInspeccion.EN_PROCESO,
                EnumSet.of(EstadoInspeccion.FINALIZADA, EstadoInspeccion.CANCELADA));
        mapa.put(EstadoInspeccion.FINALIZADA, EnumSet.noneOf(EstadoInspeccion.class));
        mapa.put(EstadoInspeccion.REALIZADA, EnumSet.noneOf(EstadoInspeccion.class));
        mapa.put(EstadoInspeccion.CANCELADA, EnumSet.noneOf(EstadoInspeccion.class));
        TRANSICIONES_PERMITIDAS = Map.copyOf(mapa);
    }

    public void validarSolicitudParaProgramacion(Solicitud solicitud) {
        if (solicitud == null) {
            throw new BusinessException("La solicitud es obligatoria para programar una inspección.");
        }
        if (solicitud.getTipoTramite() != null && !solicitud.getTipoTramite().isRequiereInspeccion()) {
            throw new BusinessException(
                    "El tipo de trámite '" + solicitud.getTipoTramite().getCodigo()
                            + "' no requiere inspección técnica municipal.");
        }
        if (!ESTADOS_SOLICITUD_COMPATIBLES.contains(solicitud.getEstado())) {
            throw new ConflictException(
                    "No se puede programar una inspección para una solicitud en estado " + solicitud.getEstado() + ".");
        }
    }

    public void validarInspector(Usuario inspector) {
        if (inspector == null) {
            throw new BusinessException("El inspector asignado es obligatorio.");
        }
        if (!inspector.isActivo()) {
            throw new BusinessException("El usuario inspector seleccionado se encuentra inactivo.");
        }
        boolean autorizado = inspector.getRoles() != null && inspector.getRoles().stream()
                .anyMatch(r -> ROLES_INSPECTOR_AUTORIZADOS.contains(r.getNombre()));
        if (!autorizado) {
            throw new BusinessException(
                    "El usuario seleccionado no posee permisos ni rol autorizado para realizar inspecciones municipales.");
        }
    }

    public void validarFechaProgramada(LocalDateTime fechaProgramada) {
        if (fechaProgramada == null) {
            throw new BusinessException("La fecha programada de la inspección es obligatoria.");
        }
    }

    public void validarReprogramable(Inspeccion inspeccion, String motivo) {
        if (!StringUtils.hasText(motivo)) {
            throw new BusinessException("El motivo de reprogramación es obligatorio.");
        }
        EstadoInspeccion estado = inspeccion.getEstado();
        if (estado != EstadoInspeccion.PROGRAMADA && estado != EstadoInspeccion.REPROGRAMADA) {
            throw new ConflictException(
                    "Solo se pueden reprogramar inspecciones en estado PROGRAMADA (estado actual: " + estado + ").");
        }
    }

    public void validarTransicion(EstadoInspeccion estadoActual, EstadoInspeccion estadoNuevo) {
        if (estadoActual == null || estadoNuevo == null) {
            throw new BusinessException("El estado de la inspección es obligatorio.");
        }
        Set<EstadoInspeccion> permitidos = TRANSICIONES_PERMITIDAS.getOrDefault(estadoActual, Set.of());
        if (!permitidos.contains(estadoNuevo)) {
            throw new ConflictException(
                    "Transición de inspección no permitida: no se puede pasar de " + estadoActual + " a " + estadoNuevo
                            + ".");
        }
    }

    public void validarRegistroResultado(Inspeccion inspeccion,
            ResultadoInspeccion resultado,
            String observaciones,
            String justificacionRectificacion,
            boolean tieneAutoridadSupervision,
            boolean solicitudTieneLicencia) {

        if (resultado == null) {
            throw new BusinessException("El resultado técnico de la inspección es obligatorio.");
        }
        if (!StringUtils.hasText(observaciones)) {
            throw new BusinessException("Las observaciones técnicas del resultado de inspección son obligatorias.");
        }

        EstadoInspeccion estadoActual = inspeccion.getEstado();
        if (estadoActual == EstadoInspeccion.CANCELADA) {
            throw new ConflictException("No se puede registrar un resultado sobre una inspección CANCELADA.");
        }

        if (estadoActual == EstadoInspeccion.FINALIZADA || estadoActual == EstadoInspeccion.REALIZADA) {
            if (solicitudTieneLicencia) {
                throw new ConflictException(
                        "No se puede rectificar el resultado de una inspección cuya solicitud ya tiene licencia emitida.");
            }
            if (!tieneAutoridadSupervision || !StringUtils.hasText(justificacionRectificacion)) {
                throw new ConflictException(
                        "La inspección ya cuenta con un resultado definitivo registrado (" + inspeccion.getResultado()
                                + ") y no puede modificarse sin justificación administrativa autorizada.");
            }
        }
    }
}
