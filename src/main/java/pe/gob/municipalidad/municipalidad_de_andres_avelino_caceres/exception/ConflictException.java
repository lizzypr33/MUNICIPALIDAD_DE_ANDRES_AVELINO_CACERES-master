package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception;

/** Se lanza cuando la operación entra en conflicto con datos existentes (HTTP 409). */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
