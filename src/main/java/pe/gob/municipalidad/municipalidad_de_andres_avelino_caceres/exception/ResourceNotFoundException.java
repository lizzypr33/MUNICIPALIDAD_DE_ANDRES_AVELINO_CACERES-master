package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception;

/** Se lanza cuando un recurso solicitado no existe. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
