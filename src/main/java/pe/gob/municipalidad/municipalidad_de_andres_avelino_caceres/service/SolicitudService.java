package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ActualizarSolicitudRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.AsignarFuncionarioRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ObservarSolicitudRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.PresentarSolicitudRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.ResolverSolicitudRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.SolicitudRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.SubsanarSolicitudRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ExpedienteResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.HistorialSolicitudResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.SolicitudDetalleResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.SolicitudResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoSolicitud;

public interface SolicitudService {

    PaginaResponse<SolicitudResponse> listar(String numeroExpediente, Long ciudadanoId, Long establecimientoId,
            Long tipoTramiteId, EstadoSolicitud estado, LocalDate fechaInicial, LocalDate fechaFinal,
            Long usuarioResponsableId, Pageable pageable);

    SolicitudDetalleResponse obtener(Long id);

    ExpedienteResponse obtenerPorExpediente(String numeroExpediente);

    SolicitudResponse crear(SolicitudRequest request);

    SolicitudResponse actualizar(Long id, ActualizarSolicitudRequest request);

    SolicitudResponse presentar(Long id, PresentarSolicitudRequest request);

    SolicitudResponse asignarFuncionario(Long id, AsignarFuncionarioRequest request);

    SolicitudResponse iniciarRevision(Long id, String observaciones);

    SolicitudResponse observar(Long id, ObservarSolicitudRequest request);

    SolicitudResponse subsanar(Long id, SubsanarSolicitudRequest request);

    SolicitudResponse aprobar(Long id, ResolverSolicitudRequest request);

    SolicitudResponse rechazar(Long id, ResolverSolicitudRequest request);

    SolicitudResponse archivar(Long id, ResolverSolicitudRequest request);

    List<HistorialSolicitudResponse> listarHistorial(Long id);
}
