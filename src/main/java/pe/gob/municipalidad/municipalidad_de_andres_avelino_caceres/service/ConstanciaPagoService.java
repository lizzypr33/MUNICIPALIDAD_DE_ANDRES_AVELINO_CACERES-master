package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service;

public interface ConstanciaPagoService {

    record ConstanciaPagoDescarga(
            String nombreArchivo,
            String tipoMime,
            byte[] contenido) {
    }

    ConstanciaPagoDescarga generarConstanciaPago(Long pagoId);
}
