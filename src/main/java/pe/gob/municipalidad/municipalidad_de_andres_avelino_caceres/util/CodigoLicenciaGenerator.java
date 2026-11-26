package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.util;

import java.security.SecureRandom;
import java.time.Year;
import java.util.HexFormat;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;

/**
 * Generador de códigos provisionales de licencia de funcionamiento ({@code LIC-YYYY-000001})
 * y tokens criptográficos de verificación pública para códigos QR.
 */
@Component
public class CodigoLicenciaGenerator {

    private static final String PREFIJO_LICENCIA = "LIC";
    private static final Pattern PATRON_CODIGO_LICENCIA = Pattern.compile("^LIC-\\d{4}-\\d{6,}$");
    private static final Pattern PATRON_TOKEN_VERIFICACION = Pattern.compile("^[a-fA-F0-9\\-]{16,64}$");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Formatea el código de licencia con el año en curso y el correlativo de secuencia PostgreSQL.
     */
    public String formatearCodigoLicencia(long correlativo) {
        return formatearCodigoLicencia(Year.now().getValue(), correlativo);
    }

    /**
     * Formatea el código de licencia para un año específico y correlativo positivo.
     */
    public String formatearCodigoLicencia(int anio, long correlativo) {
        if (anio < 2000 || anio > 9999) {
            throw new IllegalArgumentException("Año inválido para generación de código de licencia: " + anio);
        }
        if (correlativo <= 0) {
            throw new IllegalArgumentException("El correlativo de licencia debe ser positivo: " + correlativo);
        }
        return String.format("%s-%04d-%06d", PREFIJO_LICENCIA, anio, correlativo);
    }

    /**
     * Normaliza y valida el formato de un código de licencia en consultas.
     */
    public String normalizarCodigoLicencia(String codigo) {
        if (!StringUtils.hasText(codigo)) {
            throw new BusinessException("El código de licencia es obligatorio.");
        }
        String normalizado = codigo.trim().toUpperCase();
        if (!PATRON_CODIGO_LICENCIA.matcher(normalizado).matches()) {
            throw new BusinessException(
                    "El formato del código de licencia no es válido (ejemplo esperado: LIC-2026-000001).");
        }
        return normalizado;
    }

    /**
     * Genera un token aleatorio no predecible (160 bits en hexadecimal, 40 caracteres)
     * para la verificación pública mediante código QR.
     */
    public String generarTokenVerificacion() {
        byte[] bytes = new byte[20];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /**
     * Normaliza y comprueba el formato básico de un token público de verificación.
     */
    public String normalizarTokenVerificacion(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException("El token de verificación es obligatorio.");
        }
        String limpio = token.trim();
        if (!PATRON_TOKEN_VERIFICACION.matcher(limpio).matches()) {
            throw new BusinessException("El formato del token de verificación no es válido.");
        }
        return limpio;
    }
}
