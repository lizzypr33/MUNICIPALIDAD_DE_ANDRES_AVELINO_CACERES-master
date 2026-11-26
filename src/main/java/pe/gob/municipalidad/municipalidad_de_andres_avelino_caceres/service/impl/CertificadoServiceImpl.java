package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.LicenciaDetalleResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.AuditoriaService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.CertificadoService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.LicenciaService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.util.PdfGenerator;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.util.PdfGenerator.DatosCertificadoLicencia;

/**
 * Servicio de generación de certificados PDF de licencias de funcionamiento.
 * Obtiene los datos en una lectura corta y construye el documento PDF fuera de una transacción abierta.
 */
@Service
public class CertificadoServiceImpl implements CertificadoService {

    private final LicenciaService licenciaService;
    private final PdfGenerator pdfGenerator;
    private final AuditoriaService auditoria;
    private final String verificacionBaseUrl;

    public CertificadoServiceImpl(LicenciaService licenciaService,
            PdfGenerator pdfGenerator,
            AuditoriaService auditoria,
            @Value("${app.licencias.verificacion-base-url:http://localhost:8080}") String verificacionBaseUrl) {
        this.licenciaService = licenciaService;
        this.pdfGenerator = pdfGenerator;
        this.auditoria = auditoria;
        this.verificacionBaseUrl = limpiarBarraFinal(verificacionBaseUrl);
    }

    @Override
    public CertificadoDescarga generarCertificadoLicencia(Long licenciaId) {
        // Lectura transaccional de solo lectura y validación de titularidad/roles en LicenciaService
        LicenciaDetalleResponse detalle = licenciaService.obtener(licenciaId);

        String urlVerificacion = verificacionBaseUrl + "/api/public/licencias/verificar/"
                + detalle.tokenVerificacion();

        DatosCertificadoLicencia datos = new DatosCertificadoLicencia(
                detalle.codigoLicencia(),
                detalle.numeroExpediente(),
                detalle.nombreTitular(),
                detalle.nombreComercial(),
                detalle.razonSocial(),
                detalle.ruc(),
                detalle.direccionEstablecimiento(),
                detalle.giroNegocio(),
                detalle.areaMetrosCuadrados(),
                detalle.fechaEmision(),
                detalle.fechaVencimiento(),
                detalle.estado(),
                urlVerificacion,
                detalle.observaciones());

        // Generación en memoria fuera de transacción de base de datos
        byte[] pdfBytes = pdfGenerator.generarCertificadoPdf(datos);

        auditoria.registrar("GENERAR_CERTIFICADO_LICENCIA", "Licencia", detalle.id(),
                "Certificado PDF generado para licencia " + detalle.codigoLicencia());

        String nombreArchivo = "certificado-" + detalle.codigoLicencia() + ".pdf";
        return new CertificadoDescarga(nombreArchivo, "application/pdf", pdfBytes);
    }

    private static String limpiarBarraFinal(String url) {
        if (!StringUtils.hasText(url)) {
            return "http://localhost:8080";
        }
        String limpia = url.trim();
        while (limpia.endsWith("/")) {
            limpia = limpia.substring(0, limpia.length() - 1);
        }
        return limpia;
    }
}
