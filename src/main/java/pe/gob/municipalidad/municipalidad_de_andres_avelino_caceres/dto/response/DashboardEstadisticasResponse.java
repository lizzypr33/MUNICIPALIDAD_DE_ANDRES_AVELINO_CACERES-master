package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

/**
 * Agrupador general de estadísticas municipales del dashboard administrativo.
 */
public record DashboardEstadisticasResponse(
        EstadisticaSolicitudResponse solicitudes,
        EstadisticaLicenciaResponse licencias,
        EstadisticaInspeccionResponse inspecciones,
        EstadisticaPagoResponse pagos) {
}
