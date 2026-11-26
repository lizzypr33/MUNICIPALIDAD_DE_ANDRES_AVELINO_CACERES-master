package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ReporteFiltroRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse.FilaCiudadanoReporte;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse.FilaEstablecimientoReporte;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse.FilaInspeccionReporte;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse.FilaLicenciaReporte;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse.FilaPagoReporte;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ReporteResponse.FilaSolicitudReporte;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Ciudadano;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Establecimiento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Inspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Licencia;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Pago;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Solicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.CiudadanoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.EstablecimientoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.InspeccionRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.LicenciaRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.PagoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.SolicitudRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.security.CustomUserDetails;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.AuditoriaService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.ReporteService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.util.PdfGenerator;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.util.PdfGenerator.DatosReporteTabla;

@Service
public class ReporteServiceImpl implements ReporteService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SolicitudRepository solicitudes;
    private final LicenciaRepository licencias;
    private final InspeccionRepository inspecciones;
    private final PagoRepository pagos;
    private final EstablecimientoRepository establecimientos;
    private final CiudadanoRepository ciudadanos;
    private final PdfGenerator pdfGenerator;
    private final AuditoriaService auditoria;
    private final int maxExportRows;

    @Autowired
    public ReporteServiceImpl(
            SolicitudRepository solicitudes,
            LicenciaRepository licencias,
            InspeccionRepository inspecciones,
            PagoRepository pagos,
            EstablecimientoRepository establecimientos,
            CiudadanoRepository ciudadanos,
            PdfGenerator pdfGenerator,
            AuditoriaService auditoria,
            @Value("${app.reportes.max-export-rows:1000}") int maxExportRows) {
        this.solicitudes = solicitudes;
        this.licencias = licencias;
        this.inspecciones = inspecciones;
        this.pagos = pagos;
        this.establecimientos = establecimientos;
        this.ciudadanos = ciudadanos;
        this.pdfGenerator = pdfGenerator;
        this.auditoria = auditoria;
        this.maxExportRows = maxExportRows > 0 ? maxExportRows : 1000;
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteResponse<FilaSolicitudReporte> reporteSolicitudes(ReporteFiltroRequest filtro, Pageable pageable) {
        exigirRolPersonalMunicipal();
        validarFiltroFechas(filtro);
        Pageable pagSeguro = asegurarPageable(pageable);

        Page<Solicitud> pagina = solicitudes.findAll(specSolicitudes(filtro), pagSeguro);
        PaginaResponse<FilaSolicitudReporte> datos = PaginaResponse.from(pagina.map(this::mapearFilaSolicitud));

        return new ReporteResponse<>(
                "Reporte Municipal de Solicitudes",
                LocalDateTime.now(),
                mapaFiltrosSolicitudes(filtro),
                datos);
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteResponse<FilaLicenciaReporte> reporteLicencias(ReporteFiltroRequest filtro, Pageable pageable) {
        exigirRolPersonalMunicipal();
        validarFiltroFechas(filtro);
        Pageable pagSeguro = asegurarPageable(pageable);

        Page<Licencia> pagina = licencias.findAll(specLicencias(filtro), pagSeguro);
        PaginaResponse<FilaLicenciaReporte> datos = PaginaResponse.from(pagina.map(this::mapearFilaLicencia));

        return new ReporteResponse<>(
                "Reporte Municipal de Licencias de Funcionamiento",
                LocalDateTime.now(),
                mapaFiltrosLicencias(filtro),
                datos);
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteResponse<FilaInspeccionReporte> reporteInspecciones(ReporteFiltroRequest filtro, Pageable pageable) {
        exigirRolPersonalMunicipal();
        validarFiltroFechas(filtro);
        Pageable pagSeguro = asegurarPageable(pageable);

        Page<Inspeccion> pagina = inspecciones.findAll(specInspecciones(filtro), pagSeguro);
        PaginaResponse<FilaInspeccionReporte> datos = PaginaResponse.from(pagina.map(this::mapearFilaInspeccion));

        return new ReporteResponse<>(
                "Reporte Municipal de Inspecciones Técnicas",
                LocalDateTime.now(),
                mapaFiltrosInspecciones(filtro),
                datos);
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteResponse<FilaPagoReporte> reportePagos(ReporteFiltroRequest filtro, Pageable pageable) {
        exigirRolPersonalMunicipal();
        validarFiltroFechas(filtro);
        Pageable pagSeguro = asegurarPageable(pageable);

        Page<Pago> pagina = pagos.findAll(specPagos(filtro), pagSeguro);
        PaginaResponse<FilaPagoReporte> datos = PaginaResponse.from(pagina.map(this::mapearFilaPago));

        return new ReporteResponse<>(
                "Reporte Municipal de Pagos",
                LocalDateTime.now(),
                mapaFiltrosPagos(filtro),
                datos);
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteResponse<FilaEstablecimientoReporte> reporteEstablecimientos(
            ReporteFiltroRequest filtro, Pageable pageable) {
        exigirRolPersonalMunicipal();
        Pageable pagSeguro = asegurarPageable(pageable);
        boolean mostrarRucCompleto = puedeVerRucCompleto();

        Page<Establecimiento> pagina = establecimientos.findAll(specEstablecimientos(filtro), pagSeguro);
        PaginaResponse<FilaEstablecimientoReporte> datos = PaginaResponse.from(
                pagina.map(e -> mapearFilaEstablecimiento(e, mostrarRucCompleto)));

        return new ReporteResponse<>(
                "Reporte Municipal de Establecimientos Comerciales",
                LocalDateTime.now(),
                mapaFiltrosEstablecimientos(filtro),
                datos);
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteResponse<FilaCiudadanoReporte> reporteCiudadanos(ReporteFiltroRequest filtro, Pageable pageable) {
        exigirRolPersonalMunicipal();
        Pageable pagSeguro = asegurarPageable(pageable);

        Page<Ciudadano> pagina = ciudadanos.findAll(specCiudadanos(filtro), pagSeguro);
        PaginaResponse<FilaCiudadanoReporte> datos = PaginaResponse.from(pagina.map(this::mapearFilaCiudadano));

        return new ReporteResponse<>(
                "Reporte Municipal de Ciudadanos",
                LocalDateTime.now(),
                mapaFiltrosCiudadanos(filtro),
                datos);
    }

    @Override
    @Transactional
    public ArchivoReporte exportarSolicitudesPdf(ReporteFiltroRequest filtro) {
        exigirRolPersonalMunicipal();
        validarFiltroFechas(filtro);

        List<Solicitud> lista = consultarSolicitudesParaExportacion(filtro);
        List<String> encabezados = List.of(
                "N° Expediente", "Fecha Presentación", "Tipo de Trámite",
                "Establecimiento", "Estado", "Responsable");
        List<List<String>> filas = lista.stream()
                .map(this::mapearFilaSolicitud)
                .map(f -> List.of(
                        valorTexto(f.numeroExpediente(), "Borrador"),
                        f.fechaPresentacion() != null ? f.fechaPresentacion().format(FORMATO_FECHA_HORA) : "N/A",
                        valorTexto(f.tipoTramite(), "N/A"),
                        valorTexto(f.establecimiento(), "N/A"),
                        f.estado() != null ? f.estado().name() : "N/A",
                        valorTexto(f.responsable(), "Sin asignar")))
                .toList();

        byte[] pdf = pdfGenerator.generarReporteTablaPdf(new DatosReporteTabla(
                "Reporte Municipal de Solicitudes",
                LocalDateTime.now(),
                obtenerIdentificacionGenerador(),
                mapaFiltrosSolicitudes(filtro),
                encabezados,
                filas));

        auditoria.registrar("EXPORTAR_REPORTE_SOLICITUDES_PDF", "Reporte", null,
                "Reporte PDF de solicitudes exportado con " + filas.size() + " registros.");
        return new ArchivoReporte("reporte-solicitudes.pdf", "application/pdf", pdf);
    }

    @Override
    @Transactional
    public ArchivoReporte exportarLicenciasPdf(ReporteFiltroRequest filtro) {
        exigirRolPersonalMunicipal();
        validarFiltroFechas(filtro);

        List<Licencia> lista = consultarLicenciasParaExportacion(filtro);
        List<String> encabezados = List.of(
                "Código Licencia", "N° Expediente", "Establecimiento",
                "Giro Comercial", "Fecha Emisión", "Estado");
        List<List<String>> filas = lista.stream()
                .map(this::mapearFilaLicencia)
                .map(f -> List.of(
                        valorTexto(f.codigoLicencia(), "N/A"),
                        valorTexto(f.numeroExpediente(), "N/A"),
                        valorTexto(f.establecimiento(), "N/A"),
                        valorTexto(f.giro(), "N/A"),
                        f.fechaEmision() != null ? f.fechaEmision().format(FORMATO_FECHA) : "N/A",
                        f.estado() != null ? f.estado().name() : "N/A"))
                .toList();

        byte[] pdf = pdfGenerator.generarReporteTablaPdf(new DatosReporteTabla(
                "Reporte Municipal de Licencias de Funcionamiento",
                LocalDateTime.now(),
                obtenerIdentificacionGenerador(),
                mapaFiltrosLicencias(filtro),
                encabezados,
                filas));

        auditoria.registrar("EXPORTAR_REPORTE_LICENCIAS_PDF", "Reporte", null,
                "Reporte PDF de licencias exportado con " + filas.size() + " registros.");
        return new ArchivoReporte("reporte-licencias.pdf", "application/pdf", pdf);
    }

    @Override
    @Transactional
    public ArchivoReporte exportarInspeccionesPdf(ReporteFiltroRequest filtro) {
        exigirRolPersonalMunicipal();
        validarFiltroFechas(filtro);

        List<Inspeccion> lista = consultarInspeccionesParaExportacion(filtro);
        List<String> encabezados = List.of(
                "Expediente Solicitud", "Inspector", "Fecha Programada",
                "Fecha Realizada", "Estado", "Resultado");
        List<List<String>> filas = lista.stream()
                .map(this::mapearFilaInspeccion)
                .map(f -> List.of(
                        valorTexto(f.numeroExpedienteSolicitud(), "N/A"),
                        valorTexto(f.inspector(), "N/A"),
                        f.fechaProgramada() != null ? f.fechaProgramada().format(FORMATO_FECHA_HORA) : "N/A",
                        f.fechaRealizada() != null ? f.fechaRealizada().format(FORMATO_FECHA_HORA) : "Pendiente",
                        f.estado() != null ? f.estado().name() : "N/A",
                        f.resultado() != null ? f.resultado().name() : "Pendiente"))
                .toList();

        byte[] pdf = pdfGenerator.generarReporteTablaPdf(new DatosReporteTabla(
                "Reporte Municipal de Inspecciones Técnicas",
                LocalDateTime.now(),
                obtenerIdentificacionGenerador(),
                mapaFiltrosInspecciones(filtro),
                encabezados,
                filas));

        auditoria.registrar("EXPORTAR_REPORTE_INSPECCIONES_PDF", "Reporte", null,
                "Reporte PDF de inspecciones exportado con " + filas.size() + " registros.");
        return new ArchivoReporte("reporte-inspecciones.pdf", "application/pdf", pdf);
    }

    @Override
    @Transactional
    public ArchivoReporte exportarPagosPdf(ReporteFiltroRequest filtro) {
        exigirRolPersonalMunicipal();
        validarFiltroFechas(filtro);

        List<Pago> lista = consultarPagosParaExportacion(filtro);
        List<String> encabezados = List.of(
                "Código Operación", "N° Expediente", "Concepto",
                "Monto", "Moneda", "Fecha Pago", "Estado");
        List<List<String>> filas = lista.stream()
                .map(this::mapearFilaPago)
                .map(f -> List.of(
                        valorTexto(f.codigoOperacion(), "N/A"),
                        valorTexto(f.numeroExpediente(), "N/A"),
                        valorTexto(f.concepto(), "N/A"),
                        f.monto() != null ? f.monto().toPlainString() : "0.00",
                        valorTexto(f.moneda(), "PEN"),
                        f.fecha() != null ? f.fecha().format(FORMATO_FECHA_HORA) : "N/A",
                        f.estado() != null ? f.estado().name() : "N/A"))
                .toList();

        byte[] pdf = pdfGenerator.generarReporteTablaPdf(new DatosReporteTabla(
                "Reporte Municipal de Pagos",
                LocalDateTime.now(),
                obtenerIdentificacionGenerador(),
                mapaFiltrosPagos(filtro),
                encabezados,
                filas));

        auditoria.registrar("EXPORTAR_REPORTE_PAGOS_PDF", "Reporte", null,
                "Reporte PDF de pagos exportado con " + filas.size() + " registros.");
        return new ArchivoReporte("reporte-pagos.pdf", "application/pdf", pdf);
    }

    @Override
    @Transactional
    public ArchivoReporte exportarEstablecimientosPdf(ReporteFiltroRequest filtro) {
        exigirRolPersonalMunicipal();
        boolean mostrarRucCompleto = puedeVerRucCompleto();

        List<Establecimiento> lista = consultarEstablecimientosParaExportacion(filtro);
        List<String> encabezados = List.of(
                "Nombre Comercial", "Razón Social", "RUC",
                "Dirección", "Giro Comercial", "Estado");
        List<List<String>> filas = lista.stream()
                .map(e -> mapearFilaEstablecimiento(e, mostrarRucCompleto))
                .map(f -> List.of(
                        valorTexto(f.nombreComercial(), "N/A"),
                        valorTexto(f.razonSocial(), "No declarada"),
                        valorTexto(f.ruc(), "No declarado"),
                        valorTexto(f.direccion(), "N/A"),
                        valorTexto(f.giro(), "N/A"),
                        valorTexto(f.estado(), "ACTIVO")))
                .toList();

        byte[] pdf = pdfGenerator.generarReporteTablaPdf(new DatosReporteTabla(
                "Reporte Municipal de Establecimientos Comerciales",
                LocalDateTime.now(),
                obtenerIdentificacionGenerador(),
                mapaFiltrosEstablecimientos(filtro),
                encabezados,
                filas));

        auditoria.registrar("EXPORTAR_REPORTE_ESTABLECIMIENTOS_PDF", "Reporte", null,
                "Reporte PDF de establecimientos exportado con " + filas.size() + " registros.");
        return new ArchivoReporte("reporte-establecimientos.pdf", "application/pdf", pdf);
    }

    @Override
    @Transactional
    public ArchivoReporte exportarSolicitudesCsv(ReporteFiltroRequest filtro) {
        exigirRolPersonalMunicipal();
        validarFiltroFechas(filtro);

        List<Solicitud> lista = consultarSolicitudesParaExportacion(filtro);
        List<String> encabezados = List.of(
                "N° Expediente", "Fecha de Presentación", "Tipo de Trámite",
                "Establecimiento", "Estado", "Responsable");
        List<List<String>> filas = lista.stream()
                .map(this::mapearFilaSolicitud)
                .map(f -> List.of(
                        valorTexto(f.numeroExpediente(), "BORRADOR"),
                        f.fechaPresentacion() != null ? f.fechaPresentacion().format(FORMATO_FECHA_HORA) : "",
                        valorTexto(f.tipoTramite(), ""),
                        valorTexto(f.establecimiento(), ""),
                        f.estado() != null ? f.estado().name() : "",
                        valorTexto(f.responsable(), "Sin asignar")))
                .toList();

        byte[] csv = generarCsvSeguroUtf8(encabezados, filas);
        auditoria.registrar("EXPORTAR_REPORTE_SOLICITUDES_CSV", "Reporte", null,
                "Reporte CSV de solicitudes exportado con " + filas.size() + " registros.");
        return new ArchivoReporte("reporte-solicitudes.csv", "text/csv;charset=UTF-8", csv);
    }

    @Override
    @Transactional
    public ArchivoReporte exportarLicenciasCsv(ReporteFiltroRequest filtro) {
        exigirRolPersonalMunicipal();
        validarFiltroFechas(filtro);

        List<Licencia> lista = consultarLicenciasParaExportacion(filtro);
        List<String> encabezados = List.of(
                "Código de Licencia", "N° Expediente", "Establecimiento",
                "Giro Comercial", "Fecha de Emisión", "Estado");
        List<List<String>> filas = lista.stream()
                .map(this::mapearFilaLicencia)
                .map(f -> List.of(
                        valorTexto(f.codigoLicencia(), ""),
                        valorTexto(f.numeroExpediente(), ""),
                        valorTexto(f.establecimiento(), ""),
                        valorTexto(f.giro(), ""),
                        f.fechaEmision() != null ? f.fechaEmision().format(FORMATO_FECHA) : "",
                        f.estado() != null ? f.estado().name() : ""))
                .toList();

        byte[] csv = generarCsvSeguroUtf8(encabezados, filas);
        auditoria.registrar("EXPORTAR_REPORTE_LICENCIAS_CSV", "Reporte", null,
                "Reporte CSV de licencias exportado con " + filas.size() + " registros.");
        return new ArchivoReporte("reporte-licencias.csv", "text/csv;charset=UTF-8", csv);
    }

    @Override
    @Transactional
    public ArchivoReporte exportarInspeccionesCsv(ReporteFiltroRequest filtro) {
        exigirRolPersonalMunicipal();
        validarFiltroFechas(filtro);

        List<Inspeccion> lista = consultarInspeccionesParaExportacion(filtro);
        List<String> encabezados = List.of(
                "Solicitud / Expediente", "Inspector", "Fecha Programada",
                "Fecha Realizada", "Estado", "Resultado");
        List<List<String>> filas = lista.stream()
                .map(this::mapearFilaInspeccion)
                .map(f -> List.of(
                        valorTexto(f.numeroExpedienteSolicitud(), ""),
                        valorTexto(f.inspector(), ""),
                        f.fechaProgramada() != null ? f.fechaProgramada().format(FORMATO_FECHA_HORA) : "",
                        f.fechaRealizada() != null ? f.fechaRealizada().format(FORMATO_FECHA_HORA) : "",
                        f.estado() != null ? f.estado().name() : "",
                        f.resultado() != null ? f.resultado().name() : ""))
                .toList();

        byte[] csv = generarCsvSeguroUtf8(encabezados, filas);
        auditoria.registrar("EXPORTAR_REPORTE_INSPECCIONES_CSV", "Reporte", null,
                "Reporte CSV de inspecciones exportado con " + filas.size() + " registros.");
        return new ArchivoReporte("reporte-inspecciones.csv", "text/csv;charset=UTF-8", csv);
    }

    @Override
    @Transactional
    public ArchivoReporte exportarPagosCsv(ReporteFiltroRequest filtro) {
        exigirRolPersonalMunicipal();
        validarFiltroFechas(filtro);

        List<Pago> lista = consultarPagosParaExportacion(filtro);
        List<String> encabezados = List.of(
                "Código de Operación", "N° Expediente", "Concepto",
                "Monto", "Moneda", "Fecha", "Estado");
        List<List<String>> filas = lista.stream()
                .map(this::mapearFilaPago)
                .map(f -> List.of(
                        valorTexto(f.codigoOperacion(), ""),
                        valorTexto(f.numeroExpediente(), ""),
                        valorTexto(f.concepto(), ""),
                        f.monto() != null ? f.monto().toPlainString() : "0.00",
                        valorTexto(f.moneda(), "PEN"),
                        f.fecha() != null ? f.fecha().format(FORMATO_FECHA_HORA) : "",
                        f.estado() != null ? f.estado().name() : ""))
                .toList();

        byte[] csv = generarCsvSeguroUtf8(encabezados, filas);
        auditoria.registrar("EXPORTAR_REPORTE_PAGOS_CSV", "Reporte", null,
                "Reporte CSV de pagos exportado con " + filas.size() + " registros.");
        return new ArchivoReporte("reporte-pagos.csv", "text/csv;charset=UTF-8", csv);
    }

    @Override
    @Transactional
    public ArchivoReporte exportarEstablecimientosCsv(ReporteFiltroRequest filtro) {
        exigirRolPersonalMunicipal();
        boolean mostrarRucCompleto = puedeVerRucCompleto();

        List<Establecimiento> lista = consultarEstablecimientosParaExportacion(filtro);
        List<String> encabezados = List.of(
                "Nombre Comercial", "Razón Social", "RUC",
                "Dirección", "Giro Comercial", "Estado");
        List<List<String>> filas = lista.stream()
                .map(e -> mapearFilaEstablecimiento(e, mostrarRucCompleto))
                .map(f -> List.of(
                        valorTexto(f.nombreComercial(), ""),
                        valorTexto(f.razonSocial(), ""),
                        valorTexto(f.ruc(), ""),
                        valorTexto(f.direccion(), ""),
                        valorTexto(f.giro(), ""),
                        valorTexto(f.estado(), "")))
                .toList();

        byte[] csv = generarCsvSeguroUtf8(encabezados, filas);
        auditoria.registrar("EXPORTAR_REPORTE_ESTABLECIMIENTOS_CSV", "Reporte", null,
                "Reporte CSV de establecimientos exportado con " + filas.size() + " registros.");
        return new ArchivoReporte("reporte-establecimientos.csv", "text/csv;charset=UTF-8", csv);
    }

    /**
     * Genera contenido CSV en UTF-8 escapando comillas, comas, saltos de línea y
     * neutralizando vectores de inyección de fórmulas (CSV Injection: =, +, -, @, \t, \r).
     */
    public byte[] generarCsvSeguroUtf8(List<String> encabezados, List<List<String>> filas) {
        StringBuilder sb = new StringBuilder();
        sb.append(unirFilaCsv(encabezados)).append("\r\n");
        if (filas != null) {
            for (List<String> fila : filas) {
                sb.append(unirFilaCsv(fila)).append("\r\n");
            }
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String unirFilaCsv(List<String> valores) {
        if (valores == null || valores.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < valores.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escaparCeldaCsv(valores.get(i)));
        }
        return sb.toString();
    }

    private String escaparCeldaCsv(String valorOriginal) {
        if (valorOriginal == null || valorOriginal.isEmpty()) {
            return "";
        }
        String valor = valorOriginal;
        char primero = valor.charAt(0);
        if (primero == '=' || primero == '+' || primero == '-' || primero == '@'
                || primero == '\t' || primero == '\r') {
            valor = "'" + valor;
        }
        boolean requiereComillas = valor.indexOf(',') >= 0
                || valor.indexOf('"') >= 0
                || valor.indexOf('\n') >= 0
                || valor.indexOf('\r') >= 0
                || valor.indexOf(';') >= 0;
        if (requiereComillas) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }

    private List<Solicitud> consultarSolicitudesParaExportacion(ReporteFiltroRequest filtro) {
        List<Solicitud> lista = solicitudes.findAll(specSolicitudes(filtro));
        verificarLimiteExportacion(lista.size(), "solicitudes");
        return lista;
    }

    private List<Licencia> consultarLicenciasParaExportacion(ReporteFiltroRequest filtro) {
        List<Licencia> lista = licencias.findAll(specLicencias(filtro));
        verificarLimiteExportacion(lista.size(), "licencias");
        return lista;
    }

    private List<Inspeccion> consultarInspeccionesParaExportacion(ReporteFiltroRequest filtro) {
        List<Inspeccion> lista = inspecciones.findAll(specInspecciones(filtro));
        verificarLimiteExportacion(lista.size(), "inspecciones");
        return lista;
    }

    private List<Pago> consultarPagosParaExportacion(ReporteFiltroRequest filtro) {
        List<Pago> lista = pagos.findAll(specPagos(filtro));
        verificarLimiteExportacion(lista.size(), "pagos");
        return lista;
    }

    private List<Establecimiento> consultarEstablecimientosParaExportacion(ReporteFiltroRequest filtro) {
        List<Establecimiento> lista = establecimientos.findAll(specEstablecimientos(filtro));
        verificarLimiteExportacion(lista.size(), "establecimientos");
        return lista;
    }

    private void verificarLimiteExportacion(int cantidadRegistros, String modulo) {
        if (cantidadRegistros > maxExportRows) {
            throw new BusinessException("La consulta de " + modulo + " supera el límite máximo permitido de exportación ("
                    + maxExportRows + " registros; encontrados: " + cantidadRegistros
                    + "). Reduzca el rango de fechas o aplique filtros adicionales.");
        }
    }

    private Specification<Solicitud> specSolicitudes(ReporteFiltroRequest f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f != null) {
                if (f.estadoSolicitud() != null) {
                    p.add(cb.equal(root.get("estado"), f.estadoSolicitud()));
                }
                if (f.tipoTramiteId() != null) {
                    p.add(cb.equal(root.get("tipoTramite").get("id"), f.tipoTramiteId()));
                }
                if (f.funcionarioResponsableId() != null) {
                    p.add(cb.equal(root.get("usuarioResponsable").get("id"), f.funcionarioResponsableId()));
                }
                if (f.fechaDesde() != null) {
                    p.add(cb.greaterThanOrEqualTo(
                            cb.coalesce(root.get("fechaPresentacion"), root.get("fechaActualizacion")),
                            f.fechaDesde().atStartOfDay()));
                }
                if (f.fechaHasta() != null) {
                    p.add(cb.lessThanOrEqualTo(
                            cb.coalesce(root.get("fechaPresentacion"), root.get("fechaActualizacion")),
                            f.fechaHasta().atTime(LocalTime.MAX)));
                }
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    private Specification<Licencia> specLicencias(ReporteFiltroRequest f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f != null) {
                if (f.estadoLicencia() != null) {
                    p.add(cb.equal(root.get("estado"), f.estadoLicencia()));
                }
                if (StringUtils.hasText(f.giroComercial())) {
                    p.add(cb.like(cb.lower(root.get("solicitud").get("establecimiento").get("giroNegocio")),
                            "%" + f.giroComercial().trim().toLowerCase() + "%"));
                }
                if (f.fechaDesde() != null) {
                    p.add(cb.greaterThanOrEqualTo(root.get("fechaEmision"), f.fechaDesde()));
                }
                if (f.fechaHasta() != null) {
                    p.add(cb.lessThanOrEqualTo(root.get("fechaEmision"), f.fechaHasta()));
                }
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    private Specification<Inspeccion> specInspecciones(ReporteFiltroRequest f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f != null) {
                if (f.inspectorId() != null) {
                    p.add(cb.equal(root.get("inspector").get("id"), f.inspectorId()));
                }
                if (f.estadoInspeccion() != null) {
                    p.add(cb.equal(root.get("estado"), f.estadoInspeccion()));
                }
                if (f.resultadoInspeccion() != null) {
                    p.add(cb.equal(root.get("resultado"), f.resultadoInspeccion()));
                }
                if (f.fechaDesde() != null) {
                    p.add(cb.greaterThanOrEqualTo(root.get("fechaProgramada"), f.fechaDesde().atStartOfDay()));
                }
                if (f.fechaHasta() != null) {
                    p.add(cb.lessThanOrEqualTo(root.get("fechaProgramada"), f.fechaHasta().atTime(LocalTime.MAX)));
                }
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    private Specification<Pago> specPagos(ReporteFiltroRequest f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f != null) {
                if (f.estadoPago() != null) {
                    p.add(cb.equal(root.get("estado"), f.estadoPago()));
                }
                if (StringUtils.hasText(f.conceptoPago())) {
                    p.add(cb.like(cb.lower(root.get("concepto")),
                            "%" + f.conceptoPago().trim().toLowerCase() + "%"));
                }
                if (f.fechaDesde() != null) {
                    p.add(cb.greaterThanOrEqualTo(root.get("fechaPago"), f.fechaDesde().atStartOfDay()));
                }
                if (f.fechaHasta() != null) {
                    p.add(cb.lessThanOrEqualTo(root.get("fechaPago"), f.fechaHasta().atTime(LocalTime.MAX)));
                }
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    private Specification<Establecimiento> specEstablecimientos(ReporteFiltroRequest f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f != null) {
                if (StringUtils.hasText(f.nombreComercial())) {
                    p.add(cb.like(cb.lower(root.get("nombreComercial")),
                            "%" + f.nombreComercial().trim().toLowerCase() + "%"));
                }
                if (StringUtils.hasText(f.giroComercial())) {
                    p.add(cb.like(cb.lower(root.get("giroNegocio")),
                            "%" + f.giroComercial().trim().toLowerCase() + "%"));
                }
                if (f.activo() != null) {
                    p.add(cb.equal(root.get("activo"), f.activo()));
                }
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    private Specification<Ciudadano> specCiudadanos(ReporteFiltroRequest f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f != null) {
                if (f.tipoDocumento() != null) {
                    p.add(cb.equal(root.get("tipoDocumento"), f.tipoDocumento()));
                }
                if (StringUtils.hasText(f.nombreCiudadano())) {
                    String patron = "%" + f.nombreCiudadano().trim().toLowerCase() + "%";
                    p.add(cb.or(
                            cb.like(cb.lower(root.get("nombres")), patron),
                            cb.like(cb.lower(root.get("apellidos")), patron)));
                }
                if (f.activo() != null) {
                    p.add(cb.equal(root.get("activo"), f.activo()));
                }
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    private FilaSolicitudReporte mapearFilaSolicitud(Solicitud s) {
        String tramite = s.getTipoTramite() != null ? s.getTipoTramite().getNombre() : "N/A";
        String est = s.getEstablecimiento() != null ? s.getEstablecimiento().getNombreComercial() : "N/A";
        String resp = "Sin asignar";
        if (s.getUsuarioResponsable() != null) {
            resp = ((s.getUsuarioResponsable().getNombres() != null ? s.getUsuarioResponsable().getNombres() : "")
                    + " "
                    + (s.getUsuarioResponsable().getApellidos() != null ? s.getUsuarioResponsable().getApellidos() : "")).trim();
        }
        return new FilaSolicitudReporte(
                s.getId(),
                s.getNumeroExpediente() != null ? s.getNumeroExpediente() : "BORRADOR-" + s.getId(),
                s.getFechaPresentacion() != null ? s.getFechaPresentacion() : s.getFechaActualizacion(),
                tramite,
                est,
                s.getEstado(),
                resp);
    }

    private FilaLicenciaReporte mapearFilaLicencia(Licencia l) {
        String exp = l.getSolicitud() != null ? l.getSolicitud().getNumeroExpediente() : "N/A";
        String est = (l.getSolicitud() != null && l.getSolicitud().getEstablecimiento() != null)
                ? l.getSolicitud().getEstablecimiento().getNombreComercial()
                : "N/A";
        String giro = (l.getSolicitud() != null && l.getSolicitud().getEstablecimiento() != null)
                ? l.getSolicitud().getEstablecimiento().getGiroNegocio()
                : "N/A";
        return new FilaLicenciaReporte(
                l.getId(),
                l.getCodigoLicencia(),
                exp,
                est,
                giro,
                l.getFechaEmision(),
                l.getEstado());
    }

    private FilaInspeccionReporte mapearFilaInspeccion(Inspeccion i) {
        String exp = i.getSolicitud() != null
                ? (i.getSolicitud().getNumeroExpediente() != null
                        ? i.getSolicitud().getNumeroExpediente()
                        : "SOL-" + i.getSolicitud().getId())
                : "N/A";
        String inspector = "Sin asignar";
        if (i.getInspector() != null) {
            inspector = ((i.getInspector().getNombres() != null ? i.getInspector().getNombres() : "")
                    + " "
                    + (i.getInspector().getApellidos() != null ? i.getInspector().getApellidos() : "")).trim();
        }
        return new FilaInspeccionReporte(
                i.getId(),
                exp,
                inspector,
                i.getFechaProgramada(),
                i.getFechaRealizada(),
                i.getEstado(),
                i.getResultado());
    }

    private FilaPagoReporte mapearFilaPago(Pago p) {
        String exp = p.getSolicitud() != null
                ? (p.getSolicitud().getNumeroExpediente() != null
                        ? p.getSolicitud().getNumeroExpediente()
                        : "SOL-" + p.getSolicitud().getId())
                : "N/A";
        return new FilaPagoReporte(
                p.getId(),
                p.getCodigoOperacion(),
                exp,
                p.getConcepto(),
                p.getMonto(),
                p.getMoneda(),
                p.getFechaPago(),
                p.getEstado());
    }

    private FilaEstablecimientoReporte mapearFilaEstablecimiento(Establecimiento e, boolean mostrarRucCompleto) {
        String ruc = "No declarado";
        if (StringUtils.hasText(e.getRuc())) {
            String limpio = e.getRuc().trim();
            if (mostrarRucCompleto) {
                ruc = limpio;
            } else if (limpio.length() >= 4) {
                ruc = limpio.substring(0, 2) + "*******" + limpio.substring(limpio.length() - 2);
            } else {
                ruc = "RESERVADO";
            }
        }
        return new FilaEstablecimientoReporte(
                e.getId(),
                e.getNombreComercial(),
                e.getRazonSocial(),
                ruc,
                e.getDireccion(),
                e.getGiroNegocio(),
                e.isActivo() ? "ACTIVO" : "INACTIVO");
    }

    private FilaCiudadanoReporte mapearFilaCiudadano(Ciudadano c) {
        String nombreCompleto = ((c.getNombres() != null ? c.getNombres() : "")
                + " "
                + (c.getApellidos() != null ? c.getApellidos() : "")).trim();
        String docEnmascarado = enmascararDocumento(c.getNumeroDocumento());
        return new FilaCiudadanoReporte(
                c.getId(),
                nombreCompleto,
                c.getTipoDocumento(),
                docEnmascarado,
                c.isActivo() ? "ACTIVO" : "INACTIVO",
                c.getFechaRegistro());
    }

    private String enmascararDocumento(String numeroDocumento) {
        if (!StringUtils.hasText(numeroDocumento)) {
            return "****";
        }
        String limpio = numeroDocumento.trim();
        if (limpio.length() <= 4) {
            return "****";
        }
        return "****" + limpio.substring(limpio.length() - 4);
    }

    private Map<String, String> mapaFiltrosSolicitudes(ReporteFiltroRequest f) {
        Map<String, String> m = new LinkedHashMap<>();
        if (f != null) {
            if (f.fechaDesde() != null) m.put("fechaDesde", f.fechaDesde().toString());
            if (f.fechaHasta() != null) m.put("fechaHasta", f.fechaHasta().toString());
            if (f.estadoSolicitud() != null) m.put("estado", f.estadoSolicitud().name());
            if (f.tipoTramiteId() != null) m.put("tipoTramiteId", f.tipoTramiteId().toString());
            if (f.funcionarioResponsableId() != null) m.put("funcionarioResponsableId", f.funcionarioResponsableId().toString());
        }
        return m;
    }

    private Map<String, String> mapaFiltrosLicencias(ReporteFiltroRequest f) {
        Map<String, String> m = new LinkedHashMap<>();
        if (f != null) {
            if (f.fechaDesde() != null) m.put("fechaDesde", f.fechaDesde().toString());
            if (f.fechaHasta() != null) m.put("fechaHasta", f.fechaHasta().toString());
            if (f.estadoLicencia() != null) m.put("estado", f.estadoLicencia().name());
            if (StringUtils.hasText(f.giroComercial())) m.put("giroComercial", f.giroComercial().trim());
        }
        return m;
    }

    private Map<String, String> mapaFiltrosInspecciones(ReporteFiltroRequest f) {
        Map<String, String> m = new LinkedHashMap<>();
        if (f != null) {
            if (f.fechaDesde() != null) m.put("fechaDesde", f.fechaDesde().toString());
            if (f.fechaHasta() != null) m.put("fechaHasta", f.fechaHasta().toString());
            if (f.inspectorId() != null) m.put("inspectorId", f.inspectorId().toString());
            if (f.estadoInspeccion() != null) m.put("estado", f.estadoInspeccion().name());
            if (f.resultadoInspeccion() != null) m.put("resultado", f.resultadoInspeccion().name());
        }
        return m;
    }

    private Map<String, String> mapaFiltrosPagos(ReporteFiltroRequest f) {
        Map<String, String> m = new LinkedHashMap<>();
        if (f != null) {
            if (f.fechaDesde() != null) m.put("fechaDesde", f.fechaDesde().toString());
            if (f.fechaHasta() != null) m.put("fechaHasta", f.fechaHasta().toString());
            if (f.estadoPago() != null) m.put("estado", f.estadoPago().name());
            if (StringUtils.hasText(f.conceptoPago())) m.put("concepto", f.conceptoPago().trim());
        }
        return m;
    }

    private Map<String, String> mapaFiltrosEstablecimientos(ReporteFiltroRequest f) {
        Map<String, String> m = new LinkedHashMap<>();
        if (f != null) {
            if (StringUtils.hasText(f.nombreComercial())) m.put("nombreComercial", f.nombreComercial().trim());
            if (StringUtils.hasText(f.giroComercial())) m.put("giroComercial", f.giroComercial().trim());
            if (f.activo() != null) m.put("activo", f.activo().toString());
        }
        return m;
    }

    private Map<String, String> mapaFiltrosCiudadanos(ReporteFiltroRequest f) {
        Map<String, String> m = new LinkedHashMap<>();
        if (f != null) {
            if (f.tipoDocumento() != null) m.put("tipoDocumento", f.tipoDocumento().name());
            if (StringUtils.hasText(f.nombreCiudadano())) m.put("nombre", f.nombreCiudadano().trim());
            if (f.activo() != null) m.put("activo", f.activo().toString());
        }
        return m;
    }

    private Pageable asegurarPageable(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "id"));
        }
        int size = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "id");
        return PageRequest.of(Math.max(pageable.getPageNumber(), 0), size, sort);
    }

    private void validarFiltroFechas(ReporteFiltroRequest filtro) {
        if (filtro == null) {
            return;
        }
        LocalDate desde = filtro.fechaDesde();
        LocalDate hasta = filtro.fechaHasta();
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new BusinessException("La fecha inicial del reporte no puede ser posterior a la fecha final.");
        }
    }

    private void exigirRolPersonalMunicipal() {
        if (!tieneRol("ROLE_ADMINISTRADOR")
                && !tieneRol("ROLE_SUPERVISOR")
                && !tieneRol("ROLE_FUNCIONARIO")) {
            throw new AccessDeniedException(
                    "El acceso a los reportes municipales está reservado al personal autorizado.");
        }
    }

    private boolean puedeVerRucCompleto() {
        return tieneRol("ROLE_ADMINISTRADOR") || tieneRol("ROLE_SUPERVISOR");
    }

    private boolean tieneRol(String rolConPrefijo) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if (rolConPrefijo.equals(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private String obtenerIdentificacionGenerador() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "Sistema";
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails cud) {
            return cud.getUsername();
        }
        return auth.getName();
    }

    private String valorTexto(String valor, String porDefecto) {
        return StringUtils.hasText(valor) ? valor.trim() : porDefecto;
    }
}
