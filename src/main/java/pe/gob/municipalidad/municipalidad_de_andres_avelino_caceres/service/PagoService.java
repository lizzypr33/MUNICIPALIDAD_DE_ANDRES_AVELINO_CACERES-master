package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.AnularPagoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.RechazarPagoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.RegistrarPagoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.VerificarPagoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PagoDetalleResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PagoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ResumenPagosResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoPago;

public interface PagoService {

    PaginaResponse<PagoResponse> listar(
            Long solicitudId,
            EstadoPago estado,
            Long tipoTramiteId,
            String concepto,
            String codigoOperacion,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Pageable pageable);

    PagoDetalleResponse obtener(Long id);

    List<PagoResponse> listarPorSolicitud(Long solicitudId);

    PagoDetalleResponse registrar(RegistrarPagoRequest request);

    PagoDetalleResponse verificar(Long id, VerificarPagoRequest request);

    PagoDetalleResponse rechazar(Long id, RechazarPagoRequest request);

    PagoDetalleResponse anular(Long id, AnularPagoRequest request);

    ResumenPagosResponse obtenerResumen(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            EstadoPago estado,
            Long tipoTramiteId,
            String concepto);
}
