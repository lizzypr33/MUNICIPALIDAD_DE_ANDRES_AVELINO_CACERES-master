package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ActualizarCiudadanoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.CambiarEstadoUsuarioRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.CiudadanoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.CiudadanoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoDocumento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.CiudadanoService;

@RestController
@RequestMapping("/api/ciudadanos")
public class CiudadanoController {

    private final CiudadanoService ciudadanoService;

    public CiudadanoController(CiudadanoService ciudadanoService) {
        this.ciudadanoService = ciudadanoService;
    }

    @GetMapping
    public ResponseEntity<PaginaResponse<CiudadanoResponse>> listar(
            @RequestParam(required = false) TipoDocumento tipoDocumento,
            @RequestParam(required = false) String numeroDocumento,
            @RequestParam(required = false) String nombres,
            @RequestParam(required = false) String apellidos,
            @RequestParam(required = false) Boolean activo,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(ciudadanoService.listar(tipoDocumento, numeroDocumento, nombres, apellidos, activo,
                pageable));
    }

    @GetMapping("/documento")
    public ResponseEntity<CiudadanoResponse> obtenerPorDocumento(
            @RequestParam TipoDocumento tipoDocumento,
            @RequestParam String numeroDocumento) {
        return ResponseEntity.ok(ciudadanoService.obtenerPorDocumento(tipoDocumento, numeroDocumento));
    }

    /** Ficha de ciudadano asociada a la cuenta autenticada (solo para el rol CIUDADANO). */
    @GetMapping("/mi-perfil")
    public ResponseEntity<CiudadanoResponse> miPerfil() {
        return ResponseEntity.ok(ciudadanoService.obtenerMiPerfil());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CiudadanoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ciudadanoService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<CiudadanoResponse> crear(@Valid @RequestBody CiudadanoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ciudadanoService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CiudadanoResponse> actualizar(@PathVariable Long id,
            @Valid @RequestBody ActualizarCiudadanoRequest request) {
        return ResponseEntity.ok(ciudadanoService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<CiudadanoResponse> cambiarEstado(@PathVariable Long id,
            @Valid @RequestBody CambiarEstadoUsuarioRequest request) {
        return ResponseEntity.ok(ciudadanoService.cambiarEstado(id, request.activo()));
    }
}
