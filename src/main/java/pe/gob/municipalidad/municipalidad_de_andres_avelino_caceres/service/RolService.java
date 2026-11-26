package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service;

import java.util.List;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.RolResponse;

public interface RolService {

    List<RolResponse> listar();

    RolResponse obtener(Long id);
}
