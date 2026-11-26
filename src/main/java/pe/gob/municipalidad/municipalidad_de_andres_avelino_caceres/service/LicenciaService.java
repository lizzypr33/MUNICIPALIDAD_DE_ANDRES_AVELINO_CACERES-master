package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.CambiarEstadoLicenciaRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.EmitirLicenciaRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.HistorialLicenciaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.LicenciaDetalleResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.LicenciaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoLicencia;

public interface LicenciaService {

    PaginaResponse<LicenciaResponse> listar(
            String codigo,
            Long solicitudId,
            EstadoLicencia estado,
            Long establecimientoId,
            Long ciudadanoId,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Pageable pageable);

    LicenciaDetalleResponse obtener(Long id);

    LicenciaDetalleResponse obtenerPorCodigo(String codigo);

    LicenciaDetalleResponse obtenerPorSolicitud(Long solicitudId);

    LicenciaDetalleResponse emitir(EmitirLicenciaRequest request);

    LicenciaDetalleResponse suspender(Long id, CambiarEstadoLicenciaRequest request);

    LicenciaDetalleResponse reactivar(Long id, CambiarEstadoLicenciaRequest request);

    LicenciaDetalleResponse revocar(Long id, CambiarEstadoLicenciaRequest request);

    List<HistorialLicenciaResponse> listarHistorial(Long id);
}
