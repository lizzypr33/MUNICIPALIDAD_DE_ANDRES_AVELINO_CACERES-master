package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.CambiarEstadoLicenciaRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ProgramarInspeccionRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ReprogramarInspeccionRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ResultadoInspeccionRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.InspeccionDetalleResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.InspeccionResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoInspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.ResultadoInspeccion;

public interface InspeccionService {

    PaginaResponse<InspeccionResponse> listar(
            Long solicitudId,
            Long inspectorId,
            EstadoInspeccion estado,
            ResultadoInspeccion resultado,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Pageable pageable);

    InspeccionDetalleResponse obtener(Long id);

    List<InspeccionResponse> listarPorSolicitud(Long solicitudId);

    InspeccionDetalleResponse programar(ProgramarInspeccionRequest request);

    InspeccionDetalleResponse reprogramar(Long id, ReprogramarInspeccionRequest request);

    InspeccionDetalleResponse iniciar(Long id);

    InspeccionDetalleResponse registrarResultado(Long id, ResultadoInspeccionRequest request);

    InspeccionDetalleResponse cancelar(Long id, CambiarEstadoLicenciaRequest request);
}
