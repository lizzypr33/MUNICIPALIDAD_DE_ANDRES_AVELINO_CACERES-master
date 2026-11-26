package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.validation;

import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;

/**
 * Valida el formato y dígito verificador del RUC peruano (11 dígitos, módulo 11).
 * No realiza consultas externas a SUNAT.
 */
@Component
public class RucValidator {

    private static final Set<String> PREFIJOS_VALIDOS = Set.of("10", "15", "17", "20");
    private static final int[] FACTORES = { 5, 4, 3, 2, 7, 6, 5, 4, 3, 2 };

    /**
     * Normaliza y valida un RUC opcional: si es nulo o en blanco devuelve {@code null}.
     */
    public String validarYNormalizarOpcional(String rucRaw) {
        if (!StringUtils.hasText(rucRaw)) {
            return null;
        }
        return validarYNormalizar(rucRaw);
    }

    /**
     * Normaliza y valida un RUC obligatorio.
     *
     * @throws BusinessException si el RUC no tiene 11 dígitos, prefijo válido o dígito verificador correcto
     */
    public String validarYNormalizar(String rucRaw) {
        if (!StringUtils.hasText(rucRaw)) {
            throw new BusinessException("El RUC no puede estar vacío.");
        }
        String ruc = rucRaw.trim();
        if (!ruc.matches("^\\d{11}$")) {
            throw new BusinessException("El RUC debe contener exactamente 11 dígitos numéricos.");
        }
        String prefijo = ruc.substring(0, 2);
        if (!PREFIJOS_VALIDOS.contains(prefijo)) {
            throw new BusinessException("El RUC debe iniciar con un prefijo válido (10, 15, 17 o 20).");
        }
        if (!cumpleDigitoVerificador(ruc)) {
            throw new BusinessException("El RUC ingresado tiene un dígito verificador inválido.");
        }
        return ruc;
    }

    public boolean esValido(String rucRaw) {
        if (!StringUtils.hasText(rucRaw)) {
            return false;
        }
        String ruc = rucRaw.trim();
        return ruc.matches("^\\d{11}$")
                && PREFIJOS_VALIDOS.contains(ruc.substring(0, 2))
                && cumpleDigitoVerificador(ruc);
    }

    private boolean cumpleDigitoVerificador(String ruc) {
        int suma = 0;
        for (int i = 0; i < 10; i++) {
            suma += Character.getNumericValue(ruc.charAt(i)) * FACTORES[i];
        }
        int resto = 11 - (suma % 11);
        int verificadorEsperado = switch (resto) {
            case 10 -> 0;
            case 11 -> 1;
            default -> resto;
        };
        int verificadorReal = Character.getNumericValue(ruc.charAt(10));
        return verificadorEsperado == verificadorReal;
    }
}
