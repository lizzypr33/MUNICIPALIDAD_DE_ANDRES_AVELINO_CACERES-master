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
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ActualizarUsuarioRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.AsignarRolesRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.CambiarEstadoUsuarioRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.CrearUsuarioRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.UsuarioResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.UsuarioService;

/** Solo ADMINISTRADOR (regla definida en SecurityConfig). */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<PaginaResponse<UsuarioResponse>> listar(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String rol,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(usuarioService.listar(username, nombre, activo, rol, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> actualizar(@PathVariable Long id,
            @Valid @RequestBody ActualizarUsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<UsuarioResponse> cambiarEstado(@PathVariable Long id,
            @Valid @RequestBody CambiarEstadoUsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.cambiarEstado(id, request.activo()));
    }

    @PutMapping("/{id}/roles")
    public ResponseEntity<UsuarioResponse> asignarRoles(@PathVariable Long id,
            @Valid @RequestBody AsignarRolesRequest request) {
        return ResponseEntity.ok(usuarioService.asignarRoles(id, request.rolIds()));
    }
}
