package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Respuesta con la información de un establecimiento comercial y sus personas vinculadas. */
public record EstablecimientoResponse(
        Long id,
        String nombreComercial,
        String razonSocial,
        String ruc,
        String direccion,
        String referencia,
        String giroNegocio,
        BigDecimal areaMetrosCuadrados,
        Integer numeroPisos,
        String telefono,
        String email,
        boolean activo,
        LocalDateTime fechaRegistro,
        Long ciudadanoId,
        String ciudadanoNombreCompleto,
        String ciudadanoNumeroDocumento,
        Long representanteId,
        String representanteNombreCompleto) {
}
