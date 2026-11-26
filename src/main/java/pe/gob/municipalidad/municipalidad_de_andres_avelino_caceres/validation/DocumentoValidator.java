package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.validation;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;

/**
 * Valida archivos subidos mediante {@link MultipartFile}: tamaño máximo, nombre original seguro,
 * extensión permitida (PDF, JPG, JPEG, PNG), tipo MIME y firma binaria real (magic bytes).
 */
@Component
public class DocumentoValidator {

    public static final long DEFAULT_MAX_BYTES = 10L * 1024L * 1024L; // 10 MB

    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of("pdf", "jpg", "jpeg", "png");

    private final long maxFileSizeBytes;

    public DocumentoValidator() {
        this(DEFAULT_MAX_BYTES);
    }

    @Autowired
    public DocumentoValidator(
            @Value("${app.storage.max-file-size-bytes:${DOCUMENT_MAX_SIZE_BYTES:10485760}}") long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes > 0 ? maxFileSizeBytes : DEFAULT_MAX_BYTES;
    }

    public record ArchivoValidado(
            String nombreOriginal,
            String extension,
            String tipoMime,
            byte[] contenido,
            long tamanioBytes) {
    }

    public ArchivoValidado validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty() || archivo.getSize() <= 0) {
            throw new BusinessException("El archivo adjunto es obligatorio y no puede estar vacío.");
        }
        if (archivo.getSize() > maxFileSizeBytes) {
            throw new BusinessException(
                    "El archivo excede el tamaño máximo permitido de " + (maxFileSizeBytes / (1024 * 1024)) + " MB.");
        }

        String nombreOriginalRaw = archivo.getOriginalFilename();
        String nombreLimpio = limpiarNombreOriginal(nombreOriginalRaw);
        String extension = extraerExtension(nombreLimpio);

        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new BusinessException(
                    "Extensión de archivo no permitida. Solo se aceptan archivos PDF, JPG, JPEG o PNG.");
        }

        byte[] bytes;
        try {
            bytes = archivo.getBytes();
        } catch (IOException ex) {
            throw new BusinessException("No se pudo leer el contenido del archivo subido.");
        }

        if (bytes == null || bytes.length == 0) {
            throw new BusinessException("El contenido del archivo está vacío.");
        }
        if (bytes.length > maxFileSizeBytes) {
            throw new BusinessException(
                    "El archivo excede el tamaño máximo permitido de " + (maxFileSizeBytes / (1024 * 1024)) + " MB.");
        }

        rechazarFirmasEjecutables(bytes);
        String mimeDetectado = verificarFirmaBinaria(extension, bytes);
        validarContentTypeCliente(archivo.getContentType(), mimeDetectado);

        return new ArchivoValidado(nombreLimpio, extension, mimeDetectado, bytes, bytes.length);
    }

    private String limpiarNombreOriginal(String nombreRaw) {
        if (!StringUtils.hasText(nombreRaw)) {
            throw new BusinessException("El archivo debe tener un nombre válido.");
        }
        if (nombreRaw.indexOf('\0') >= 0 || nombreRaw.contains("..")) {
            throw new BusinessException("El nombre del archivo contiene caracteres o secuencias no permitidas.");
        }
        // Extraer solo el nombre base en caso el navegador envíe ruta
        String base = nombreRaw.replace('\\', '/');
        int slashIdx = base.lastIndexOf('/');
        if (slashIdx >= 0) {
            base = base.substring(slashIdx + 1);
        }
        base = base.trim();
        if (base.isEmpty() || base.length() > 255 || !base.contains(".")) {
            throw new BusinessException("El nombre del archivo es inválido o no tiene extensión.");
        }
        return base.replaceAll("[\\r\\n\"]", "_");
    }

    private String extraerExtension(String nombre) {
        int punto = nombre.lastIndexOf('.');
        if (punto <= 0 || punto == nombre.length() - 1) {
            throw new BusinessException("El archivo debe incluir una extensión válida.");
        }
        return nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
    }

    private void rechazarFirmasEjecutables(byte[] b) {
        // Windows PE (MZ)
        if (b.length >= 2 && b[0] == 0x4D && b[1] == 0x5A) {
            throw new BusinessException("Archivo rechazado: se detectó contenido ejecutable no permitido.");
        }
        // Linux ELF (0x7F 'E' 'L' 'F')
        if (b.length >= 4 && b[0] == 0x7F && b[1] == 0x45 && b[2] == 0x4C && b[3] == 0x46) {
            throw new BusinessException("Archivo rechazado: se detectó contenido ejecutable no permitido.");
        }
        // Scripts (#!)
        if (b.length >= 2 && b[0] == '#' && b[1] == '!') {
            throw new BusinessException("Archivo rechazado: no se permiten scripts ejecutables.");
        }
    }

    private String verificarFirmaBinaria(String extension, byte[] b) {
        return switch (extension) {
            case "pdf" -> {
                // %PDF- -> 0x25 0x50 0x44 0x46 0x2D
                if (b.length < 5
                        || b[0] != 0x25
                        || b[1] != 0x50
                        || b[2] != 0x44
                        || b[3] != 0x46
                        || b[4] != 0x2D) {
                    throw new BusinessException(
                            "El contenido real del archivo no corresponde a un documento PDF válido.");
                }
                yield "application/pdf";
            }
            case "jpg", "jpeg" -> {
                // JPEG -> 0xFF 0xD8 0xFF
                if (b.length < 3
                        || (b[0] & 0xFF) != 0xFF
                        || (b[1] & 0xFF) != 0xD8
                        || (b[2] & 0xFF) != 0xFF) {
                    throw new BusinessException(
                            "El contenido real del archivo no corresponde a una imagen JPEG válida.");
                }
                yield "image/jpeg";
            }
            case "png" -> {
                // PNG -> 0x89 0x50 0x4E 0x47 0x0D 0x0A 0x1A 0x0A
                if (b.length < 8
                        || (b[0] & 0xFF) != 0x89
                        || b[1] != 0x50
                        || b[2] != 0x4E
                        || b[3] != 0x47
                        || b[4] != 0x0D
                        || b[5] != 0x0A
                        || b[6] != 0x1A
                        || b[7] != 0x0A) {
                    throw new BusinessException(
                            "El contenido real del archivo no corresponde a una imagen PNG válida.");
                }
                yield "image/png";
            }
            default -> throw new BusinessException("Formato de archivo no soportado.");
        };
    }

    private void validarContentTypeCliente(String contentTypeCliente, String mimeEsperado) {
        if (!StringUtils.hasText(contentTypeCliente)) {
            return;
        }
        String ct = contentTypeCliente.split(";")[0].trim().toLowerCase(Locale.ROOT);
        if ("application/octet-stream".equals(ct)) {
            return;
        }
        if ("image/jpg".equals(ct) && "image/jpeg".equals(mimeEsperado)) {
            return;
        }
        if (!ct.equals(mimeEsperado)) {
            throw new BusinessException(
                    "El tipo MIME declarado (" + ct + ") no coincide con el contenido del archivo (" + mimeEsperado + ").");
        }
    }
}
