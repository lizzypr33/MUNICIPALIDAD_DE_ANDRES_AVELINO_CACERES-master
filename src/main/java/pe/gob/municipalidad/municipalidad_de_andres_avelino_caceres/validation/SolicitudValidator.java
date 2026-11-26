package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.validation;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Ciudadano;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Establecimiento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Representante;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Solicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.TipoTramite;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoSolicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ConflictException;

/**
 * Centraliza las reglas de transición de estados de {@link Solicitud} y las validaciones de coherencia
 * entre ciudadano titular, establecimiento, representante y tipo de trámite.
 */
@Component
public class SolicitudValidator {

    private static final Map<EstadoSolicitud, Set<EstadoSolicitud>> TRANSICIONES_PERMITIDAS;

    static {
        Map<EstadoSolicitud, Set<EstadoSolicitud>> mapa = new EnumMap<>(EstadoSolicitud.class);
        mapa.put(EstadoSolicitud.BORRADOR,
                EnumSet.of(EstadoSolicitud.PRESENTADA, EstadoSolicitud.ARCHIVADA));
        mapa.put(EstadoSolicitud.PRESENTADA,
                EnumSet.of(EstadoSolicitud.EN_REVISION, EstadoSolicitud.ARCHIVADA));
        mapa.put(EstadoSolicitud.EN_REVISION,
                EnumSet.of(EstadoSolicitud.OBSERVADA, EstadoSolicitud.APROBADA,
                        EstadoSolicitud.RECHAZADA, EstadoSolicitud.ARCHIVADA));
        mapa.put(EstadoSolicitud.OBSERVADA,
                EnumSet.of(EstadoSolicitud.SUBSANADA, EstadoSolicitud.ARCHIVADA));
        mapa.put(EstadoSolicitud.SUBSANADA,
                EnumSet.of(EstadoSolicitud.EN_REVISION, EstadoSolicitud.ARCHIVADA));
        mapa.put(EstadoSolicitud.APROBADA, EnumSet.noneOf(EstadoSolicitud.class));
        mapa.put(EstadoSolicitud.RECHAZADA, EnumSet.of(EstadoSolicitud.ARCHIVADA));
        mapa.put(EstadoSolicitud.ARCHIVADA, EnumSet.noneOf(EstadoSolicitud.class));
        TRANSICIONES_PERMITIDAS = Map.copyOf(mapa);
    }

    /**
     * Verifica que la transición desde {@code estadoActual} hacia {@code estadoNuevo} sea válida.
     *
     * @throws ConflictException si la transición no está permitida
     */
    public void validarTransicion(EstadoSolicitud estadoActual, EstadoSolicitud estadoNuevo) {
        if (estadoActual == null || estadoNuevo == null) {
            throw new BusinessException("El estado de la solicitud es obligatorio.");
        }
        Set<EstadoSolicitud> permitidos = TRANSICIONES_PERMITIDAS.getOrDefault(estadoActual, Set.of());
        if (!permitidos.contains(estadoNuevo)) {
            throw new ConflictException(
                    "Transición de estado no permitida: no se puede pasar de " + estadoActual + " a " + estadoNuevo + ".");
        }
    }

    /**
     * Verifica que las entidades vinculadas a la solicitud estén activas y guarden relación válida.
     */
    public void validarEntidadesRelacionadas(Ciudadano ciudadano, Establecimiento establecimiento,
            TipoTramite tipoTramite, Representante representante) {
        if (!ciudadano.isActivo()) {
            throw new BusinessException("El ciudadano titular se encuentra inactivo.");
        }
        if (!establecimiento.isActivo()) {
            throw new BusinessException("El establecimiento comercial se encuentra inactivo.");
        }
        if (!tipoTramite.isActivo()) {
            throw new BusinessException("El tipo de trámite seleccionado se encuentra inactivo.");
        }
        if (establecimiento.getCiudadano() == null
                || !Objects.equals(establecimiento.getCiudadano().getId(), ciudadano.getId())) {
            throw new ConflictException(
                    "El establecimiento comercial no pertenece al ciudadano titular indicado.");
        }
        if (representante != null) {
            if (!representante.isActivo()) {
                throw new BusinessException("El representante indicado se encuentra inactivo.");
            }
            if (establecimiento.getRepresentante() == null
                    || !Objects.equals(establecimiento.getRepresentante().getId(), representante.getId())) {
                throw new ConflictException(
                        "El representante no se encuentra vinculado ni autorizado para el establecimiento indicado.");
            }
        }
    }

    public void validarModificableEnBorrador(Solicitud solicitud) {
        if (solicitud.getEstado() != EstadoSolicitud.BORRADOR) {
            throw new ConflictException(
                    "Solo se pueden modificar los datos generales de solicitudes en estado BORRADOR (estado actual: "
                            + solicitud.getEstado() + ").");
        }
    }

    public void validarPermiteCargaORetiroDeDocumentos(Solicitud solicitud) {
        EstadoSolicitud estado = solicitud.getEstado();
        if (estado != EstadoSolicitud.BORRADOR && estado != EstadoSolicitud.OBSERVADA) {
            throw new ConflictException(
                    "Solo se pueden adjuntar o retirar documentos cuando la solicitud está en estado BORRADOR u OBSERVADA (estado actual: "
                            + estado + ").");
        }
    }

    public void validarEstadoCompatibleConAsignacion(Solicitud solicitud) {
        EstadoSolicitud estado = solicitud.getEstado();
        if (estado == EstadoSolicitud.BORRADOR
                || estado == EstadoSolicitud.APROBADA
                || estado == EstadoSolicitud.RECHAZADA
                || estado == EstadoSolicitud.ARCHIVADA) {
            throw new ConflictException(
                    "No se puede asignar un funcionario a una solicitud en estado " + estado + ".");
        }
    }
}
