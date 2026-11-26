package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.VerificacionLicenciaResponse;

public interface VerificacionLicenciaService {

    VerificacionLicenciaResponse verificarPorToken(String token);
}
