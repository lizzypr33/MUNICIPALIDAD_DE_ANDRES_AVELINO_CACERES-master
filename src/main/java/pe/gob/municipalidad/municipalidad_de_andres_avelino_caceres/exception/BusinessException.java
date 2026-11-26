package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception;

/** Se lanza cuando se incumple una regla de negocio. */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
