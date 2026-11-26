package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service;

import java.time.LocalDate;
import java.util.List;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ActividadRecienteResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.DashboardEstadisticasResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.DashboardResumenResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.EstadisticaInspeccionResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.EstadisticaLicenciaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.EstadisticaPagoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.EstadisticaSolicitudResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoSolicitud;

public interface DashboardService {

    DashboardResumenResponse obtenerResumen();

    EstadisticaSolicitudResponse obtenerEstadisticasSolicitudes(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Long tipoTramiteId,
            EstadoSolicitud estado);

    EstadisticaLicenciaResponse obtenerEstadisticasLicencias(
            LocalDate fechaDesde,
            LocalDate fechaHasta);

    EstadisticaInspeccionResponse obtenerEstadisticasInspecciones(
            LocalDate fechaDesde,
            LocalDate fechaHasta);

    EstadisticaPagoResponse obtenerEstadisticasPagos(
            LocalDate fechaDesde,
            LocalDate fechaHasta);

    DashboardEstadisticasResponse obtenerEstadisticasGenerales(
            LocalDate fechaDesde,
            LocalDate fechaHasta);

    List<ActividadRecienteResponse> obtenerActividadReciente(Integer limite);
}
