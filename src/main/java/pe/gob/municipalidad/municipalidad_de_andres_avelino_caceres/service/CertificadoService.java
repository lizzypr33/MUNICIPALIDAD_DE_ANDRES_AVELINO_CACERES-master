package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service;

public interface CertificadoService {

    record CertificadoDescarga(
            String nombreArchivo,
            String tipoMime,
            byte[] contenido) {
    }

    CertificadoDescarga generarCertificadoLicencia(Long licenciaId);
}
