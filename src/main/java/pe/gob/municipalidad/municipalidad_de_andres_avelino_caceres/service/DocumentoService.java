package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.DocumentoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoDocumento;

public interface DocumentoService {

    record ArchivoDescargable(String nombreOriginal, String tipoMime, byte[] contenido) {
    }

    List<DocumentoResponse> listarPorSolicitud(Long solicitudId);

    DocumentoResponse subir(Long solicitudId, Long requisitoId, String observaciones, MultipartFile archivo);

    DocumentoResponse obtener(Long id);

    ArchivoDescargable descargar(Long id);

    DocumentoResponse retirar(Long id);

    DocumentoResponse cambiarEstado(Long id, EstadoDocumento estado, String observaciones);
}
