package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.storage;

/**
 * Servicio reutilizable de almacenamiento seguro de documentos fuera del directorio público.
 */
public interface FileStorageService {

    /**
     * Almacena el contenido de un documento para una solicitud y devuelve el identificador relativo de almacenamiento.
     */
    String guardar(Long solicitudId, String nombreAlmacenado, byte[] contenido);

    /**
     * Recupera los bytes de un documento a partir de su identificador relativo de almacenamiento.
     */
    byte[] leer(String rutaAlmacenamiento);

    /**
     * Elimina físicamente un archivo de forma silenciosa (para compensación ante fallos de transacción).
     */
    void eliminarSilencioso(String rutaAlmacenamiento);
}
