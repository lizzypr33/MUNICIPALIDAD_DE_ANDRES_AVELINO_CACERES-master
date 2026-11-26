package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.DocumentoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.DocumentoService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.DocumentoService.ArchivoDescargable;

@RestController
public class DocumentoController {

    private final DocumentoService documentoService;

    public DocumentoController(DocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    @GetMapping("/api/solicitudes/{id}/documentos")
    public ResponseEntity<List<DocumentoResponse>> listarPorSolicitud(@PathVariable Long id) {
        return ResponseEntity.ok(documentoService.listarPorSolicitud(id));
    }

    @PostMapping(value = "/api/solicitudes/{id}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoResponse> subir(@PathVariable Long id,
            @RequestParam(value = "archivo", required = false) MultipartFile archivo,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(required = false) Long requisitoId,
            @RequestParam(required = false) String observaciones) {
        MultipartFile adjunto = archivo != null ? archivo : file;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentoService.subir(id, requisitoId, observaciones, adjunto));
    }

    @GetMapping("/api/documentos/{id}")
    public ResponseEntity<DocumentoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(documentoService.obtener(id));
    }

    @GetMapping("/api/documentos/{id}/descargar")
    public ResponseEntity<byte[]> descargar(@PathVariable Long id) {
        ArchivoDescargable descargable = documentoService.descargar(id);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(descargable.nombreOriginal(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(descargable.tipoMime()))
                .contentLength(descargable.contenido().length)
                .body(descargable.contenido());
    }

    @DeleteMapping("/api/documentos/{id}")
    public ResponseEntity<DocumentoResponse> retirar(@PathVariable Long id) {
        return ResponseEntity.ok(documentoService.retirar(id));
    }
}
