package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.RequisitoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.TipoTramiteRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.RequisitoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.TipoTramiteResponse;

public interface TipoTramiteService {

    PaginaResponse<TipoTramiteResponse> listar(String codigo, String nombre, Boolean activo, Pageable pageable);

    TipoTramiteResponse obtener(Long id);

    TipoTramiteResponse crear(TipoTramiteRequest request);

    TipoTramiteResponse actualizar(Long id, TipoTramiteRequest request);

    TipoTramiteResponse cambiarEstado(Long id, boolean activo);

    List<RequisitoResponse> listarRequisitos(Long tipoTramiteId);

    List<RequisitoResponse> configurarRequisitos(Long tipoTramiteId,
            List<RequisitoRequest.AsignacionTramiteItem> requisitos);
}
