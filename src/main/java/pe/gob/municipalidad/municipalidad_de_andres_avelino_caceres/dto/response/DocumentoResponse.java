package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

import java.time.LocalDateTime;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoDocumento;

/**
 * Respuesta con los metadatos de un documento adjunto.
 * Nunca expone la ruta física del servidor ni el nombre interno de almacenamiento.
 */
public record DocumentoResponse(
        Long id,
        Long solicitudId,
        Long requisitoId,
        String requisitoCodigo,
        String requisitoNombre,
        String nombreOriginal,
        String tipoMime,
        Long tamanioBytes,
        LocalDateTime fechaSubida,
        EstadoDocumento estado,
        String observaciones,
        boolean activo) {
}
