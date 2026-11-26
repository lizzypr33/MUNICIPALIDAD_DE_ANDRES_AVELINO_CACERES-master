package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request;

import java.time.LocalDate;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoInspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoLicencia;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoPago;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoSolicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.ResultadoInspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoDocumento;

/**
 * Criterios de filtrado reutilizables para los reportes y estadísticas municipales.
 */
public record ReporteFiltroRequest(
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        EstadoSolicitud estadoSolicitud,
        Long tipoTramiteId,
        Long funcionarioResponsableId,
        EstadoLicencia estadoLicencia,
        String giroComercial,
        Long inspectorId,
        EstadoInspeccion estadoInspeccion,
        ResultadoInspeccion resultadoInspeccion,
        EstadoPago estadoPago,
        String conceptoPago,
        String nombreComercial,
        Boolean activo,
        TipoDocumento tipoDocumento,
        String nombreCiudadano) {

    public static ReporteFiltroRequest paraSolicitudes(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            EstadoSolicitud estado,
            Long tipoTramiteId,
            Long funcionarioResponsableId) {
        return new ReporteFiltroRequest(
                fechaDesde, fechaHasta, estado, tipoTramiteId, funcionarioResponsableId,
                null, null, null, null, null, null, null, null, null, null, null);
    }

    public static ReporteFiltroRequest paraLicencias(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            EstadoLicencia estado,
            String giroComercial) {
        return new ReporteFiltroRequest(
                fechaDesde, fechaHasta, null, null, null,
                estado, giroComercial, null, null, null, null, null, null, null, null, null);
    }

    public static ReporteFiltroRequest paraInspecciones(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Long inspectorId,
            EstadoInspeccion estado,
            ResultadoInspeccion resultado) {
        return new ReporteFiltroRequest(
                fechaDesde, fechaHasta, null, null, null,
                null, null, inspectorId, estado, resultado, null, null, null, null, null, null);
    }

    public static ReporteFiltroRequest paraPagos(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            EstadoPago estado,
            String concepto) {
        return new ReporteFiltroRequest(
                fechaDesde, fechaHasta, null, null, null,
                null, null, null, null, null, estado, concepto, null, null, null, null);
    }

    public static ReporteFiltroRequest paraEstablecimientos(
            String nombreComercial,
            String giroComercial,
            Boolean activo) {
        return new ReporteFiltroRequest(
                null, null, null, null, null,
                null, giroComercial, null, null, null, null, null, nombreComercial, activo, null, null);
    }

    public static ReporteFiltroRequest paraCiudadanos(
            TipoDocumento tipoDocumento,
            String nombreCiudadano,
            Boolean activo) {
        return new ReporteFiltroRequest(
                null, null, null, null, null,
                null, null, null, null, null, null, null, null, activo, tipoDocumento, nombreCiudadano);
    }
}
