package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.storage.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ResourceNotFoundException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.storage.FileStorageService;

/**
 * Implementación local en disco fuera del directorio público {@code static}.
 * Protege contra ataques de path traversal y nunca expone rutas absolutas del servidor.
 */
@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);

    private final Path rootPath;

    public LocalFileStorageService(
            @Value("${app.storage.documentos-path:${DOCUMENT_STORAGE_PATH:./data/documentos}}") String storageDir) {
        if (!StringUtils.hasText(storageDir)) {
            throw new IllegalStateException("La ruta de almacenamiento de documentos no puede estar vacía.");
        }
        this.rootPath = Paths.get(storageDir).toAbsolutePath().normalize();
    }

    @Override
    public String guardar(Long solicitudId, String nombreAlmacenado, byte[] contenido) {
        if (solicitudId == null || solicitudId <= 0) {
            throw new BusinessException("El identificador de solicitud para almacenamiento es inválido.");
        }
        if (!StringUtils.hasText(nombreAlmacenado)
                || nombreAlmacenado.contains("..")
                || nombreAlmacenado.contains("/")
                || nombreAlmacenado.contains("\\")) {
            throw new BusinessException("Nombre interno de archivo inválido.");
        }
        if (contenido == null || contenido.length == 0) {
            throw new BusinessException("No se puede almacenar un archivo vacío.");
        }

        String claveRelativa = solicitudId + "/" + nombreAlmacenado;
        Path destino = resolverSeguro(claveRelativa);

        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, contenido, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            destino.toFile().setExecutable(false, false);
            return claveRelativa;
        } catch (IOException ex) {
            log.error("No se pudo almacenar el documento para la solicitud {}", solicitudId, ex);
            throw new IllegalStateException("Error al almacenar el documento adjunto.", ex);
        }
    }

    @Override
    public byte[] leer(String rutaAlmacenamiento) {
        Path archivo = resolverSeguro(rutaAlmacenamiento);
        if (!Files.exists(archivo) || !Files.isRegularFile(archivo)) {
            throw new ResourceNotFoundException("El archivo solicitado no se encuentra disponible en el almacenamiento.");
        }
        try {
            return Files.readAllBytes(archivo);
        } catch (IOException ex) {
            log.error("No se pudo leer el documento almacenado", ex);
            throw new IllegalStateException("Error al leer el documento almacenado.", ex);
        }
    }

    @Override
    public void eliminarSilencioso(String rutaAlmacenamiento) {
        if (!StringUtils.hasText(rutaAlmacenamiento)) {
            return;
        }
        try {
            Path archivo = resolverSeguro(rutaAlmacenamiento);
            Files.deleteIfExists(archivo);
        } catch (Exception ex) {
            log.warn("No se pudo eliminar archivo huérfano tras compensación");
        }
    }

    private Path resolverSeguro(String rutaRelativa) {
        if (!StringUtils.hasText(rutaRelativa)
                || rutaRelativa.contains("..")
                || rutaRelativa.contains("\\")
                || rutaRelativa.startsWith("/")) {
            throw new BusinessException("Identificador de almacenamiento inválido.");
        }
        Path resuelto = rootPath.resolve(rutaRelativa).normalize();
        if (!resuelto.startsWith(rootPath)) {
            throw new BusinessException("Acceso fuera del directorio de almacenamiento no permitido.");
        }
        return resuelto;
    }
}
