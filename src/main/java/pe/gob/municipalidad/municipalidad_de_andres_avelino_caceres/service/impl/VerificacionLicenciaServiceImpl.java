package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.VerificacionLicenciaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Licencia;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ResourceNotFoundException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.LicenciaMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.LicenciaRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.VerificacionLicenciaService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.util.CodigoLicenciaGenerator;

/**
 * Servicio de solo lectura para la verificación pública de licencias de funcionamiento mediante token QR.
 */
@Service
public class VerificacionLicenciaServiceImpl implements VerificacionLicenciaService {

    private final LicenciaRepository licencias;
    private final CodigoLicenciaGenerator codigoGenerator;
    private final LicenciaMapper mapper;

    public VerificacionLicenciaServiceImpl(LicenciaRepository licencias,
            CodigoLicenciaGenerator codigoGenerator,
            LicenciaMapper mapper) {
        this.licencias = licencias;
        this.codigoGenerator = codigoGenerator;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public VerificacionLicenciaResponse verificarPorToken(String token) {
        final String tokenLimpio;
        try {
            tokenLimpio = codigoGenerator.normalizarTokenVerificacion(token);
        } catch (BusinessException ex) {
            throw new ResourceNotFoundException("No se encontró ninguna licencia asociada al token de verificación.");
        }

        Licencia licencia = licencias.findByTokenVerificacion(tokenLimpio)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró ninguna licencia asociada al token de verificación."));

        return mapper.toVerificacionPublicaResponse(licencia);
    }
}
