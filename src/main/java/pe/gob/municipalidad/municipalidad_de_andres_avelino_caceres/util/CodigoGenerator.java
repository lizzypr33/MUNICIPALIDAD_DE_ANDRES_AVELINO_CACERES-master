package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.util;

import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;

/**
 * Generador y normalizador de códigos internos (expedientes, trámites, requisitos y nombres físicos de archivos).
 */
@Component
public class CodigoGenerator {

    /**
     * Genera el número de expediente con el formato provisional {@code EXP-YYYY-000001}.
     */
    public String generarNumeroExpediente(int anio, long correlativo) {
        if (anio < 2000 || anio > 9999) {
            throw new BusinessException("El año para generar el expediente es inválido.");
        }
        if (correlativo <= 0) {
            throw new BusinessException("El correlativo del expediente debe ser mayor que cero.");
        }
        return String.format(Locale.ROOT, "EXP-%04d-%06d", anio, correlativo);
    }

    /**
     * Normaliza y valida un código de catálogo (TipoTramite o Requisito).
     */
    public String normalizarCodigo(String codigoRaw, String nombreCampo) {
        if (!StringUtils.hasText(codigoRaw)) {
            throw new BusinessException("El código de " + nombreCampo + " es obligatorio.");
        }
        String normalizado = codigoRaw.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "-");
        if (!normalizado.matches("^[A-Z0-9_-]{2,50}$")) {
            throw new BusinessException("El código de " + nombreCampo
                    + " debe tener entre 2 y 50 caracteres alfanuméricos, guiones o guiones bajos.");
        }
        return normalizado;
    }

    /**
     * Genera un nombre interno único basado en UUID para almacenar archivos sin usar el nombre del cliente.
     */
    public String generarNombreArchivoAlmacenado(String extension) {
        if (!StringUtils.hasText(extension)) {
            throw new BusinessException("La extensión del archivo es obligatoria.");
        }
        String ext = extension.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return UUID.randomUUID().toString() + "." + ext;
    }
}
