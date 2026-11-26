package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service;

import java.util.Set;

import org.springframework.data.domain.Pageable;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ActualizarUsuarioRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.CrearUsuarioRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.UsuarioResponse;

public interface UsuarioService {

    PaginaResponse<UsuarioResponse> listar(String username, String nombre, Boolean activo, String rol,
            Pageable pageable);

    UsuarioResponse obtener(Long id);

    UsuarioResponse crear(CrearUsuarioRequest request);

    UsuarioResponse actualizar(Long id, ActualizarUsuarioRequest request);

    UsuarioResponse cambiarEstado(Long id, boolean activo);

    UsuarioResponse asignarRoles(Long id, Set<Long> rolIds);
}
