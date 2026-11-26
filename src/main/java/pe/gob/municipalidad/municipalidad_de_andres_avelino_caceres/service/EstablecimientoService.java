package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ActualizarEstablecimientoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.EstablecimientoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.EstablecimientoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;

public interface EstablecimientoService {

    PaginaResponse<EstablecimientoResponse> listar(String nombreComercial, String ruc, Long ciudadanoId,
            Boolean activo, Pageable pageable);

    EstablecimientoResponse obtener(Long id);

    List<EstablecimientoResponse> obtenerPorRuc(String ruc);

    List<EstablecimientoResponse> obtenerPorCiudadano(Long ciudadanoId);

    EstablecimientoResponse crear(EstablecimientoRequest request);

    EstablecimientoResponse actualizar(Long id, ActualizarEstablecimientoRequest request);

    EstablecimientoResponse cambiarEstado(Long id, boolean activo);
}
