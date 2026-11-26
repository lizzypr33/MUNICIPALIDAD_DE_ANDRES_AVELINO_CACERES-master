package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.validation;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoDocumento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;

/**
 * Valida y normaliza números de documento de identidad según {@link TipoDocumento}.
 * No realiza consultas externas a RENIEC ni Migraciones.
 */
@Component
public class DocumentoIdentidadValidator {

    private final RucValidator rucValidator;

    public DocumentoIdentidadValidator(RucValidator rucValidator) {
        this.rucValidator = rucValidator;
    }

    /**
     * Normaliza (trim y mayúsculas cuando aplica) y valida el número de documento.
     *
     * @throws BusinessException si el tipo o el número son inválidos
     */
    public String validarYNormalizar(TipoDocumento tipo, String numeroRaw) {
        if (tipo == null) {
            throw new BusinessException("El tipo de documento es obligatorio.");
        }
        if (!StringUtils.hasText(numeroRaw)) {
            throw new BusinessException("El número de documento es obligatorio.");
        }
        String numero = numeroRaw.trim().toUpperCase();

        switch (tipo) {
            case DNI -> {
                if (!numero.matches("^\\d{8}$")) {
                    throw new BusinessException("El DNI debe contener exactamente 8 dígitos numéricos.");
                }
            }
            case CARNE_EXTRANJERIA -> {
                if (!numero.matches("^[A-Z0-9]{9,12}$")) {
                    throw new BusinessException(
                            "El carné de extranjería debe tener entre 9 y 12 caracteres alfanuméricos.");
                }
            }
            case PASAPORTE -> {
                if (!numero.matches("^[A-Z0-9]{6,15}$")) {
                    throw new BusinessException(
                            "El pasaporte debe tener entre 6 y 15 caracteres alfanuméricos.");
                }
            }
            case RUC -> {
                return rucValidator.validarYNormalizar(numero);
            }
            case OTRO -> {
                if (!numero.matches("^[A-Z0-9-]{4,20}$")) {
                    throw new BusinessException(
                            "El número de documento debe tener entre 4 y 20 caracteres válidos.");
                }
            }
        }
        return numero;
    }
}
