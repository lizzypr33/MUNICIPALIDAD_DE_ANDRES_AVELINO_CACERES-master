package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.controller;

import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.CrearNotificacionRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.NotificacionResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.NotificacionService;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping
    public ResponseEntity<PaginaResponse<NotificacionResponse>> listarMisNotificaciones(
            @PageableDefault(size = 20, sort = "fechaCreacion") Pageable pageable) {
        return ResponseEntity.ok(notificacionService.listarMisNotificaciones(pageable));
    }

    @GetMapping("/no-leidas")
    public ResponseEntity<PaginaResponse<NotificacionResponse>> listarMisNoLeidas(
            @PageableDefault(size = 20, sort = "fechaCreacion") Pageable pageable) {
        return ResponseEntity.ok(notificacionService.listarMisNoLeidas(pageable));
    }

    @GetMapping("/contador")
    public ResponseEntity<Map<String, Long>> contarNoLeidas() {
        long cantidad = notificacionService.contarMisNoLeidas();
        return ResponseEntity.ok(Map.of("noLeidas", cantidad));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificacionResponse> obtenerMiNotificacion(@PathVariable Long id) {
        return ResponseEntity.ok(notificacionService.obtenerMiNotificacion(id));
    }

    @PatchMapping("/{id}/leer")
    public ResponseEntity<NotificacionResponse> marcarComoLeida(@PathVariable Long id) {
        return ResponseEntity.ok(notificacionService.marcarComoLeida(id));
    }

    @PatchMapping("/leer-todas")
    public ResponseEntity<Map<String, Integer>> marcarTodasComoLeidas() {
        int actualizadas = notificacionService.marcarTodasComoLeidas();
        return ResponseEntity.ok(Map.of("actualizadas", actualizadas));
    }

    @PostMapping
    public ResponseEntity<NotificacionResponse> crearNotificacionInterna(
            @Valid @RequestBody CrearNotificacionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificacionService.crearNotificacionInterna(request));
    }
}
