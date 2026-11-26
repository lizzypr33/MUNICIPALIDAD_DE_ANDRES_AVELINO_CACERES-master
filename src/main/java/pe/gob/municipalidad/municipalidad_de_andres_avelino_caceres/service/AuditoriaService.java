package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service;

import java.time.LocalDate;

import org.springframework.data.domain.Pageable;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.AuditoriaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;

/**
 * Registra y consulta operaciones relevantes de auditoría municipal.
 * El actor y la IP se toman del contexto de la petición.
 */
public interface AuditoriaService {

    /**
     * @param descripcion texto breve; nunca debe incluir contraseñas, tokens ni datos sensibles
     */
    void registrar(String accion, String entidad, Long entidadId, String descripcion);

    PaginaResponse<AuditoriaResponse> listar(
            Long usuarioId,
            String accion,
            String entidad,
            Long entidadId,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Pageable pageable);

    AuditoriaResponse obtener(Long id);
}
