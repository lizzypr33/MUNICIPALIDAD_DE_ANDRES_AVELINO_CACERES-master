package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service;

import org.springframework.data.domain.Pageable;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ReporteFiltroRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse.FilaCiudadanoReporte;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse.FilaEstablecimientoReporte;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse.FilaInspeccionReporte;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse.FilaLicenciaReporte;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse.FilaPagoReporte;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse.FilaSolicitudReporte;

public interface ReporteService {

    record ArchivoReporte(
            String nombreArchivo,
            String tipoMime,
            byte[] contenido) {
    }

    ReporteResponse<FilaSolicitudReporte> reporteSolicitudes(ReporteFiltroRequest filtro, Pageable pageable);

    ReporteResponse<FilaLicenciaReporte> reporteLicencias(ReporteFiltroRequest filtro, Pageable pageable);

    ReporteResponse<FilaInspeccionReporte> reporteInspecciones(ReporteFiltroRequest filtro, Pageable pageable);

    ReporteResponse<FilaPagoReporte> reportePagos(ReporteFiltroRequest filtro, Pageable pageable);

    ReporteResponse<FilaEstablecimientoReporte> reporteEstablecimientos(ReporteFiltroRequest filtro, Pageable pageable);

    ReporteResponse<FilaCiudadanoReporte> reporteCiudadanos(ReporteFiltroRequest filtro, Pageable pageable);

    ArchivoReporte exportarSolicitudesPdf(ReporteFiltroRequest filtro);

    ArchivoReporte exportarLicenciasPdf(ReporteFiltroRequest filtro);

    ArchivoReporte exportarInspeccionesPdf(ReporteFiltroRequest filtro);

    ArchivoReporte exportarPagosPdf(ReporteFiltroRequest filtro);

    ArchivoReporte exportarEstablecimientosPdf(ReporteFiltroRequest filtro);

    ArchivoReporte exportarSolicitudesCsv(ReporteFiltroRequest filtro);

    ArchivoReporte exportarLicenciasCsv(ReporteFiltroRequest filtro);

    ArchivoReporte exportarInspeccionesCsv(ReporteFiltroRequest filtro);

    ArchivoReporte exportarPagosCsv(ReporteFiltroRequest filtro);

    ArchivoReporte exportarEstablecimientosCsv(ReporteFiltroRequest filtro);
}
