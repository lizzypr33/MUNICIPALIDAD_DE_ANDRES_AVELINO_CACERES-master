package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.util;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoLicencia;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoPago;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;

/**
 * Componente utilitario encargado de generar códigos QR (ZXing), certificados PDF
 * institucionales de licencias de funcionamiento y constancias de registro de pago (OpenPDF).
 */
@Component
public class PdfGenerator {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Datos estructurados requeridos para la emisión del certificado PDF de licencia.
     */
    public record DatosCertificadoLicencia(
            String codigoLicencia,
            String numeroExpediente,
            String nombreTitular,
            String nombreComercial,
            String razonSocial,
            String ruc,
            String direccionEstablecimiento,
            String giroAutorizado,
            BigDecimal areaMetrosCuadrados,
            LocalDate fechaEmision,
            LocalDate fechaVencimiento,
            EstadoLicencia estado,
            String urlVerificacionQr,
            String observaciones) {
    }

    /**
     * Datos estructurados requeridos para generar la constancia interna de registro de pago en PDF.
     */
    public record DatosConstanciaPago(
            Long pagoId,
            String codigoOperacion,
            String numeroExpediente,
            String nombreCiudadano,
            String concepto,
            BigDecimal monto,
            String moneda,
            String medioPago,
            java.time.LocalDateTime fechaPagoDeclarada,
            EstadoPago estadoVerificacion,
            java.time.LocalDateTime fechaVerificacion,
            String nombreUsuarioVerificador,
            java.time.LocalDateTime fechaEmisionConstancia,
            String observaciones) {
    }

    /**
     * Genera una imagen PNG con el código QR que representa la URL de verificación pública.
     */
    public byte[] generarQrPng(String contenidoVerificacion, int ancho, int alto) {
        if (!StringUtils.hasText(contenidoVerificacion)) {
            throw new BusinessException("El contenido para generar el código QR no puede estar vacío.");
        }
        if (ancho < 80 || alto < 80) {
            throw new BusinessException("Las dimensiones del código QR deben ser de al menos 80x80 píxeles.");
        }
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(contenidoVerificacion.trim(), BarcodeFormat.QR_CODE, ancho, alto, hints);

            BufferedImage image = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < ancho; x++) {
                for (int y = 0; y < alto; y++) {
                    image.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Error al generar el código QR de verificación.", e);
        }
    }

    /**
     * Genera el certificado institucional de licencia de funcionamiento en formato PDF.
     */
    public byte[] generarCertificadoPdf(DatosCertificadoLicencia datos) {
        if (datos == null || !StringUtils.hasText(datos.codigoLicencia())) {
            throw new BusinessException("Los datos de la licencia son obligatorios para generar el certificado PDF.");
        }

        byte[] qrBytes = generarQrPng(datos.urlVerificacionQr(), 160, 160);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Document document = new Document(PageSize.A4, 42f, 42f, 42f, 42f);
        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            Font fuenteInstitucion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(20, 45, 76));
            Font fuenteSubtitulo = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(60, 60, 60));
            Font fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(15, 35, 65));
            Font fuenteEtiqueta = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(30, 30, 30));
            Font fuenteValor = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font fuenteNota = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, new Color(80, 80, 80));

            // Encabezado institucional textual (sin inventar escudos ni autoridades)
            Paragraph encabezado1 = new Paragraph(
                    "MUNICIPALIDAD DISTRITAL DE ANDRÉS AVELINO CÁCERES DORREGARAY", fuenteInstitucion);
            encabezado1.setAlignment(Element.ALIGN_CENTER);
            document.add(encabezado1);

            Paragraph encabezado2 = new Paragraph(
                    "Provincia de Huamanga - Departamento de Ayacucho - República del Perú", fuenteSubtitulo);
            encabezado2.setAlignment(Element.ALIGN_CENTER);
            encabezado2.setSpacingAfter(14f);
            document.add(encabezado2);

            Paragraph titulo = new Paragraph("LICENCIA DE FUNCIONAMIENTO", fuenteTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(14f);
            document.add(titulo);

            // Tabla superior: Código de licencia, Expediente y Estado
            PdfPTable tablaCabecera = new PdfPTable(new float[] { 35f, 35f, 30f });
            tablaCabecera.setWidthPercentage(100f);
            tablaCabecera.setSpacingAfter(14f);
            agregarCeldaDestacada(tablaCabecera, "CÓDIGO DE LICENCIA", datos.codigoLicencia(), fuenteEtiqueta, fuenteValor);
            agregarCeldaDestacada(tablaCabecera, "N° DE EXPEDIENTE", valorSeguro(datos.numeroExpediente(), "N/A"),
                    fuenteEtiqueta, fuenteValor);
            agregarCeldaDestacada(tablaCabecera, "ESTADO ACTUAL",
                    datos.estado() != null ? datos.estado().name() : "VIGENTE", fuenteEtiqueta, fuenteValor);
            document.add(tablaCabecera);

            // Tabla de datos principales del establecimiento y titular
            PdfPTable tablaDatos = new PdfPTable(new float[] { 34f, 66f });
            tablaDatos.setWidthPercentage(100f);
            tablaDatos.setSpacingAfter(16f);

            String titularTexto = StringUtils.hasText(datos.razonSocial())
                    ? datos.razonSocial() + " (" + valorSeguro(datos.nombreTitular(), "") + ")"
                    : valorSeguro(datos.nombreTitular(), "No registrado");

            agregarFilaDato(tablaDatos, "Titular / Razón Social:", titularTexto, fuenteEtiqueta, fuenteValor);
            agregarFilaDato(tablaDatos, "Nombre Comercial:", valorSeguro(datos.nombreComercial(), "No registrado"),
                    fuenteEtiqueta, fuenteValor);
            agregarFilaDato(tablaDatos, "RUC:", valorSeguro(datos.ruc(), "No declarado"),
                    fuenteEtiqueta, fuenteValor);
            agregarFilaDato(tablaDatos, "Dirección del Establecimiento:",
                    valorSeguro(datos.direccionEstablecimiento(), "No registrada"), fuenteEtiqueta, fuenteValor);
            agregarFilaDato(tablaDatos, "Giro Comercial Autorizado:",
                    valorSeguro(datos.giroAutorizado(), "No especificado"), fuenteEtiqueta, fuenteValor);
            agregarFilaDato(tablaDatos, "Área Declarada:",
                    datos.areaMetrosCuadrados() != null ? datos.areaMetrosCuadrados().toPlainString() + " m²" : "No declarada",
                    fuenteEtiqueta, fuenteValor);
            agregarFilaDato(tablaDatos, "Fecha de Emisión:",
                    datos.fechaEmision() != null ? datos.fechaEmision().format(FORMATO_FECHA) : "N/A",
                    fuenteEtiqueta, fuenteValor);

            String textoVigencia = datos.fechaVencimiento() != null
                    ? "Temporal — Vence el " + datos.fechaVencimiento().format(FORMATO_FECHA)
                    : "Indeterminada";
            agregarFilaDato(tablaDatos, "Vigencia:", textoVigencia, fuenteEtiqueta, fuenteValor);

            if (StringUtils.hasText(datos.observaciones())) {
                agregarFilaDato(tablaDatos, "Observaciones:", datos.observaciones(), fuenteEtiqueta, fuenteValor);
            }

            document.add(tablaDatos);

            // Sección inferior: Código QR de verificación y espacio para firma/validación institucional
            PdfPTable tablaPie = new PdfPTable(new float[] { 40f, 60f });
            tablaPie.setWidthPercentage(100f);
            tablaPie.setSpacingAfter(12f);

            Image qrImage = Image.getInstance(qrBytes);
            qrImage.scaleToFit(120f, 120f);
            PdfPCell celdaQr = new PdfPCell();
            celdaQr.setBorder(Rectangle.BOX);
            celdaQr.setPadding(8f);
            celdaQr.setHorizontalAlignment(Element.ALIGN_CENTER);
            celdaQr.addElement(qrImage);
            Paragraph pQr = new Paragraph("Verificación por Código QR", fuenteNota);
            pQr.setAlignment(Element.ALIGN_CENTER);
            celdaQr.addElement(pQr);
            tablaPie.addCell(celdaQr);

            PdfPCell celdaFirma = new PdfPCell();
            celdaFirma.setBorder(Rectangle.BOX);
            celdaFirma.setPadding(12f);
            Paragraph espacioFirma = new Paragraph("\n\n\n\n____________________________________________", fuenteValor);
            espacioFirma.setAlignment(Element.ALIGN_CENTER);
            celdaFirma.addElement(espacioFirma);
            Paragraph leyendaFirma = new Paragraph(
                    "ESPACIO PARA FIRMA Y SELLO DE VALIDACIÓN INSTITUCIONAL\n"
                            + "Municipalidad Distrital de Andrés Avelino Cáceres Dorregaray",
                    fuenteEtiqueta);
            leyendaFirma.setAlignment(Element.ALIGN_CENTER);
            celdaFirma.addElement(leyendaFirma);
            tablaPie.addCell(celdaFirma);

            document.add(tablaPie);

            Paragraph notaLegal = new Paragraph(
                    "Nota de verificación: La autenticidad y el estado vigente de esta licencia pueden comprobarse "
                            + "consultando la ruta institucional de verificación contenida en el código QR ("
                            + datos.urlVerificacionQr() + "). "
                            + "La validez administrativa está sujeta a las autorizaciones y procedimientos municipales correspondientes.",
                    fuenteNota);
            notaLegal.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(notaLegal);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Error al generar el documento PDF del certificado de licencia.", e);
        }
    }

    private static void agregarCeldaDestacada(PdfPTable tabla, String titulo, String valor, Font fTitulo, Font fValor) {
        PdfPCell celda = new PdfPCell();
        celda.setBackgroundColor(new Color(242, 246, 250));
        celda.setPadding(8f);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph p1 = new Paragraph(titulo, fTitulo);
        p1.setAlignment(Element.ALIGN_CENTER);
        Paragraph p2 = new Paragraph(valor, fValor);
        p2.setAlignment(Element.ALIGN_CENTER);
        celda.addElement(p1);
        celda.addElement(p2);
        tabla.addCell(celda);
    }

    private static void agregarFilaDato(PdfPTable tabla, String etiqueta, String valor, Font fEtiqueta, Font fValor) {
        PdfPCell cEtiqueta = new PdfPCell(new Phrase(etiqueta, fEtiqueta));
        cEtiqueta.setBackgroundColor(new Color(248, 249, 250));
        cEtiqueta.setPadding(7f);
        tabla.addCell(cEtiqueta);

        PdfPCell cValor = new PdfPCell(new Phrase(valor, fValor));
        cValor.setPadding(7f);
        tabla.addCell(cValor);
    }

    private static String valorSeguro(String valor, String porDefecto) {
        return StringUtils.hasText(valor) ? valor.trim() : porDefecto;
    }

    /**
     * Datos estructurados requeridos para generar un reporte tabular institucional en PDF.
     */
    public record DatosReporteTabla(
            String tituloReporte,
            LocalDateTime fechaGeneracion,
            String usuarioGenerador,
            Map<String, String> filtrosAplicados,
            List<String> encabezados,
            List<List<String>> filas) {
    }

    /**
     * Genera la constancia interna de registro de pago en formato PDF.
     */
    public byte[] generarConstanciaPagoPdf(DatosConstanciaPago datos) {
        if (datos == null || datos.pagoId() == null) {
            throw new BusinessException("Los datos del registro de pago son obligatorios para generar la constancia PDF.");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 42f, 42f, 42f, 42f);
        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            Font fuenteInstitucion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(20, 45, 76));
            Font fuenteSubtitulo = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(60, 60, 60));
            Font fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 17, new Color(15, 35, 65));
            Font fuenteEtiqueta = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(30, 30, 30));
            Font fuenteValor = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font fuenteAdvertencia = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(140, 40, 20));
            Font fuenteNota = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, new Color(80, 80, 80));

            Paragraph encabezado1 = new Paragraph(
                    "MUNICIPALIDAD DISTRITAL DE ANDRÉS AVELINO CÁCERES DORREGARAY", fuenteInstitucion);
            encabezado1.setAlignment(Element.ALIGN_CENTER);
            document.add(encabezado1);

            Paragraph encabezado2 = new Paragraph(
                    "Provincia de Huamanga - Departamento de Ayacucho - República del Perú", fuenteSubtitulo);
            encabezado2.setAlignment(Element.ALIGN_CENTER);
            encabezado2.setSpacingAfter(14f);
            document.add(encabezado2);

            Paragraph titulo = new Paragraph("CONSTANCIA DE REGISTRO DE PAGO", fuenteTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(14f);
            document.add(titulo);

            boolean verificado = datos.estadoVerificacion() == EstadoPago.VERIFICADO
                    || datos.estadoVerificacion() == EstadoPago.PAGADO;
            String textoEstado = verificado ? "VERIFICADO" : String.valueOf(datos.estadoVerificacion());

            PdfPTable tablaCabecera = new PdfPTable(new float[] { 30f, 35f, 35f });
            tablaCabecera.setWidthPercentage(100f);
            tablaCabecera.setSpacingAfter(14f);
            agregarCeldaDestacada(tablaCabecera, "ID DE REGISTRO INTERNO", "REG-PAGO-" + datos.pagoId(),
                    fuenteEtiqueta, fuenteValor);
            agregarCeldaDestacada(tablaCabecera, "CÓDIGO DE OPERACIÓN",
                    valorSeguro(datos.codigoOperacion(), "N/A"), fuenteEtiqueta, fuenteValor);
            agregarCeldaDestacada(tablaCabecera, "ESTADO DE VERIFICACIÓN", textoEstado,
                    fuenteEtiqueta, fuenteValor);
            document.add(tablaCabecera);

            PdfPTable tablaDatos = new PdfPTable(new float[] { 36f, 64f });
            tablaDatos.setWidthPercentage(100f);
            tablaDatos.setSpacingAfter(16f);

            agregarFilaDato(tablaDatos, "Número de Expediente:",
                    valorSeguro(datos.numeroExpediente(), "En trámite / Borrador"), fuenteEtiqueta, fuenteValor);
            agregarFilaDato(tablaDatos, "Administrado / Titular:",
                    valorSeguro(datos.nombreCiudadano(), "No registrado"), fuenteEtiqueta, fuenteValor);
            agregarFilaDato(tablaDatos, "Concepto Declarado:",
                    valorSeguro(datos.concepto(), "No especificado"), fuenteEtiqueta, fuenteValor);
            agregarFilaDato(tablaDatos, "Monto y Moneda:",
                    (datos.monto() != null ? datos.monto().toPlainString() : "0.00") + " "
                            + valorSeguro(datos.moneda(), "PEN"),
                    fuenteEtiqueta, fuenteValor);
            agregarFilaDato(tablaDatos, "Medio de Pago Declarado:",
                    valorSeguro(datos.medioPago(), "No especificado"), fuenteEtiqueta, fuenteValor);
            agregarFilaDato(tablaDatos, "Fecha Declarada del Pago:",
                    datos.fechaPagoDeclarada() != null ? datos.fechaPagoDeclarada().format(FORMATO_FECHA_HORA) : "N/A",
                    fuenteEtiqueta, fuenteValor);
            agregarFilaDato(tablaDatos, "Estado de Verificación:", textoEstado, fuenteEtiqueta, fuenteValor);

            if (verificado && datos.fechaVerificacion() != null) {
                agregarFilaDato(tablaDatos, "Fecha de Verificación:",
                        datos.fechaVerificacion().format(FORMATO_FECHA_HORA), fuenteEtiqueta, fuenteValor);
            }
            if (StringUtils.hasText(datos.nombreUsuarioVerificador())) {
                agregarFilaDato(tablaDatos, "Verificado por:", datos.nombreUsuarioVerificador(),
                        fuenteEtiqueta, fuenteValor);
            }
            agregarFilaDato(tablaDatos, "Fecha de Emisión de Constancia:",
                    (datos.fechaEmisionConstancia() != null
                            ? datos.fechaEmisionConstancia()
                            : java.time.LocalDateTime.now()).format(FORMATO_FECHA_HORA),
                    fuenteEtiqueta, fuenteValor);

            if (StringUtils.hasText(datos.observaciones())) {
                agregarFilaDato(tablaDatos, "Observaciones:", datos.observaciones(), fuenteEtiqueta, fuenteValor);
            }

            document.add(tablaDatos);

            if (verificado) {
                Paragraph pVerificado = new Paragraph(
                        "ESTADO: VERIFICADO — El registro de pago ha sido contrastado administrativamente por el área competente.",
                        fuenteEtiqueta);
                pVerificado.setSpacingAfter(10f);
                document.add(pVerificado);
            } else {
                Paragraph pPendiente = new Paragraph(
                        "AVISO IMPORTANTE: Este pago se encuentra en estado " + textoEstado
                                + " (registro pendiente de verificación administrativa) y NO constituye un comprobante de recepción efectiva de fondos.",
                        fuenteAdvertencia);
                pPendiente.setSpacingAfter(10f);
                document.add(pPendiente);
            }

            Paragraph notaLegal = new Paragraph(
                    "Nota administrativa: Este documento es una CONSTANCIA DE REGISTRO DE PAGO de uso interno administrativo "
                            + "en el Sistema de Gestión de Licencias Municipales. No constituye comprobante tributario electrónico, "
                            + "factura ni boleta de venta.",
                    fuenteNota);
            notaLegal.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(notaLegal);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Error al generar la constancia PDF del registro de pago.", e);
        }
    }

    /**
     * Genera un reporte tabular institucional en formato PDF con paginación automática y numeración de páginas.
     */
    public byte[] generarReporteTablaPdf(DatosReporteTabla datos) {
        if (datos == null || !StringUtils.hasText(datos.tituloReporte())
                || datos.encabezados() == null || datos.encabezados().isEmpty()) {
            throw new BusinessException("El título y los encabezados del reporte PDF son obligatorios.");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 36f, 36f, 36f, 44f);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            Font fuentePiePagina = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(90, 90, 90));
            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter w, Document doc) {
                    Phrase pie = new Phrase(
                            "Municipalidad Distrital de Andrés Avelino Cáceres Dorregaray — Reporte Administrativo Interno | Página "
                                    + w.getPageNumber(),
                            fuentePiePagina);
                    ColumnText.showTextAligned(
                            w.getDirectContent(),
                            Element.ALIGN_CENTER,
                            pie,
                            (doc.right() + doc.left()) / 2,
                            doc.bottom() - 18f,
                            0);
                }
            });

            document.open();

            Font fuenteInstitucion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(20, 45, 76));
            Font fuenteSubtitulo = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(60, 60, 60));
            Font fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(15, 35, 65));
            Font fuenteMetaEtiqueta = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(30, 30, 30));
            Font fuenteMetaValor = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
            Font fuenteCabeceraTabla = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font fuenteCeldaTabla = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
            Font fuenteNota = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, new Color(80, 80, 80));

            Paragraph encabezado1 = new Paragraph(
                    "MUNICIPALIDAD DISTRITAL DE ANDRÉS AVELINO CÁCERES DORREGARAY", fuenteInstitucion);
            encabezado1.setAlignment(Element.ALIGN_CENTER);
            document.add(encabezado1);

            Paragraph encabezado2 = new Paragraph(
                    "Provincia de Huamanga - Departamento de Ayacucho - Sistema de Gestión de Licencias Municipales",
                    fuenteSubtitulo);
            encabezado2.setAlignment(Element.ALIGN_CENTER);
            encabezado2.setSpacingAfter(10f);
            document.add(encabezado2);

            Paragraph titulo = new Paragraph(datos.tituloReporte().trim().toUpperCase(), fuenteTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(10f);
            document.add(titulo);

            List<List<String>> filas = datos.filas() != null ? datos.filas() : List.of();
            LocalDateTime fechaGen = datos.fechaGeneracion() != null ? datos.fechaGeneracion() : LocalDateTime.now();

            String resumenFiltros = "Sin filtros específicos (consulta general)";
            if (datos.filtrosAplicados() != null && !datos.filtrosAplicados().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, String> entry : datos.filtrosAplicados().entrySet()) {
                    if (StringUtils.hasText(entry.getValue())) {
                        if (sb.length() > 0) {
                            sb.append(" | ");
                        }
                        sb.append(entry.getKey()).append(": ").append(entry.getValue());
                    }
                }
                if (sb.length() > 0) {
                    resumenFiltros = sb.toString();
                }
            }

            PdfPTable tablaMeta = new PdfPTable(new float[] { 22f, 78f });
            tablaMeta.setWidthPercentage(100f);
            tablaMeta.setSpacingAfter(12f);
            agregarFilaDato(tablaMeta, "Fecha y Hora de Generación:", fechaGen.format(FORMATO_FECHA_HORA),
                    fuenteMetaEtiqueta, fuenteMetaValor);
            agregarFilaDato(tablaMeta, "Generado por:", valorSeguro(datos.usuarioGenerador(), "Sistema"),
                    fuenteMetaEtiqueta, fuenteMetaValor);
            agregarFilaDato(tablaMeta, "Filtros Aplicados:", resumenFiltros,
                    fuenteMetaEtiqueta, fuenteMetaValor);
            agregarFilaDato(tablaMeta, "Total de Registros Incluidos:", String.valueOf(filas.size()),
                    fuenteMetaEtiqueta, fuenteMetaValor);
            document.add(tablaMeta);

            int numColumnas = datos.encabezados().size();
            PdfPTable tablaResultados = new PdfPTable(numColumnas);
            tablaResultados.setWidthPercentage(100f);
            tablaResultados.setHeaderRows(1);
            tablaResultados.setSpacingAfter(12f);

            Color colorCabecera = new Color(24, 54, 92);
            for (String col : datos.encabezados()) {
                PdfPCell c = new PdfPCell(new Phrase(valorSeguro(col, ""), fuenteCabeceraTabla));
                c.setBackgroundColor(colorCabecera);
                c.setPadding(6f);
                c.setHorizontalAlignment(Element.ALIGN_LEFT);
                tablaResultados.addCell(c);
            }

            if (filas.isEmpty()) {
                PdfPCell vacia = new PdfPCell(new Phrase("No se encontraron registros para los filtros seleccionados.", fuenteCeldaTabla));
                vacia.setColspan(numColumnas);
                vacia.setPadding(8f);
                vacia.setHorizontalAlignment(Element.ALIGN_CENTER);
                tablaResultados.addCell(vacia);
            } else {
                Color colorPar = new Color(248, 250, 252);
                for (int i = 0; i < filas.size(); i++) {
                    List<String> fila = filas.get(i);
                    Color bg = (i % 2 == 1) ? colorPar : Color.WHITE;
                    for (int cIdx = 0; cIdx < numColumnas; cIdx++) {
                        String val = (fila != null && cIdx < fila.size() && fila.get(cIdx) != null)
                                ? fila.get(cIdx)
                                : "";
                        PdfPCell celda = new PdfPCell(new Phrase(val, fuenteCeldaTabla));
                        celda.setBackgroundColor(bg);
                        celda.setPadding(5f);
                        tablaResultados.addCell(celda);
                    }
                }
            }

            document.add(tablaResultados);

            Paragraph notaLegal = new Paragraph(
                    "Nota administrativa: Este documento es un reporte administrativo interno generado por el Sistema de Gestión de Licencias Municipales "
                            + "de la Municipalidad Distrital de Andrés Avelino Cáceres Dorregaray. No constituye una certificación administrativa oficial.",
                    fuenteNota);
            notaLegal.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(notaLegal);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Error al generar el reporte PDF municipal.", e);
        }
    }
}
