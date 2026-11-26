package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PagoDetalleResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.AuditoriaService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.ConstanciaPagoService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.PagoService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.util.PdfGenerator;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.util.PdfGenerator.DatosConstanciaPago;

/**
 * Servicio para la generación de constancias internas de registro de pago en PDF.
 */
@Service
public class ConstanciaPagoServiceImpl implements ConstanciaPagoService {

    private final PagoService pagoService;
    private final PdfGenerator pdfGenerator;
    private final AuditoriaService auditoria;

    public ConstanciaPagoServiceImpl(PagoService pagoService,
            PdfGenerator pdfGenerator,
            AuditoriaService auditoria) {
        this.pagoService = pagoService;
        this.pdfGenerator = pdfGenerator;
        this.auditoria = auditoria;
    }

    @Override
    public ConstanciaPagoDescarga generarConstanciaPago(Long pagoId) {
        PagoDetalleResponse detalle = pagoService.obtener(pagoId);

        DatosConstanciaPago datos = new DatosConstanciaPago(
                detalle.id(),
                detalle.codigoOperacion(),
                detalle.numeroExpediente(),
                detalle.nombreCiudadano(),
                detalle.concepto(),
                detalle.monto(),
                detalle.moneda(),
                detalle.medioPago(),
                detalle.fechaPago(),
                detalle.estado(),
                detalle.fechaVerificacion(),
                detalle.nombreUsuarioVerificador(),
                LocalDateTime.now(),
                detalle.observaciones());

        byte[] pdfBytes = pdfGenerator.generarConstanciaPagoPdf(datos);

        auditoria.registrar("GENERAR_CONSTANCIA_PAGO", "Pago", detalle.id(),
                "Constancia interna PDF generada para el registro de pago id=" + detalle.id());

        String nombreArchivo = "constancia-pago-" + detalle.id() + ".pdf";
        return new ConstanciaPagoDescarga(nombreArchivo, "application/pdf", pdfBytes);
    }
}
