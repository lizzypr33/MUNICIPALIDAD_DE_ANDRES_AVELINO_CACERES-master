package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service;

import org.springframework.data.domain.Pageable;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ActualizarCiudadanoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.CiudadanoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.CiudadanoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoDocumento;

public interface CiudadanoService {

    PaginaResponse<CiudadanoResponse> listar(TipoDocumento tipoDocumento, String numeroDocumento,
            String nombres, String apellidos, Boolean activo, Pageable pageable);

    CiudadanoResponse obtener(Long id);

    CiudadanoResponse obtenerMiPerfil();

    CiudadanoResponse obtenerPorDocumento(TipoDocumento tipoDocumento, String numeroDocumento);

    CiudadanoResponse crear(CiudadanoRequest request);

    CiudadanoResponse actualizar(Long id, ActualizarCiudadanoRequest request);

    CiudadanoResponse cambiarEstado(Long id, boolean activo);
}
