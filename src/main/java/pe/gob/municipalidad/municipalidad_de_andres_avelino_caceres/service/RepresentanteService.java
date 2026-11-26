package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service;

import org.springframework.data.domain.Pageable;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.RepresentanteRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.RepresentanteResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoDocumento;

public interface RepresentanteService {

    PaginaResponse<RepresentanteResponse> listar(TipoDocumento tipoDocumento, String numeroDocumento,
            String nombres, String apellidos, Boolean activo, Pageable pageable);

    RepresentanteResponse obtener(Long id);

    RepresentanteResponse crear(RepresentanteRequest request);

    RepresentanteResponse actualizar(Long id, RepresentanteRequest request);

    RepresentanteResponse cambiarEstado(Long id, boolean activo);
}
