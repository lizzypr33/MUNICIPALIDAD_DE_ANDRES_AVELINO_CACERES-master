package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ExpedienteResponse;

/**
 * Servicio encargado de la generación transaccional de números únicos de expediente y su consulta.
 */
public interface ExpedienteService {

    /**
     * Genera un nuevo número de expediente único con formato provisional {@code EXP-YYYY-000001}.
     */
    String generarNumeroExpediente();

    /**
     * Consulta la información resumida de un expediente por su número único.
     */
    ExpedienteResponse obtenerPorNumero(String numeroExpediente);
}
