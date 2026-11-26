package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service;

import org.springframework.data.domain.Pageable;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.RequisitoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.RequisitoResponse;

public interface RequisitoService {

    PaginaResponse<RequisitoResponse> listar(String codigo, String nombre, Boolean activo, Pageable pageable);

    RequisitoResponse obtener(Long id);

    RequisitoResponse crear(RequisitoRequest request);

    RequisitoResponse actualizar(Long id, RequisitoRequest request);

    RequisitoResponse cambiarEstado(Long id, boolean activo);
}
