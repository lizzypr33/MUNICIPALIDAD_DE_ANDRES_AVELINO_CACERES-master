package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper;

import org.springframework.stereotype.Component;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.EstablecimientoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Ciudadano;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Establecimiento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Representante;

/** Convierte entidades {@link Establecimiento} en DTO de respuesta. */
@Component
public class EstablecimientoMapper {

    public EstablecimientoResponse toResponse(Establecimiento e) {
        Ciudadano c = e.getCiudadano();
        Representante r = e.getRepresentante();

        Long ciudadanoId = c != null ? c.getId() : null;
        String ciudadanoNombre = c != null ? c.getNombres() + " " + c.getApellidos() : null;
        String ciudadanoDoc = c != null ? c.getNumeroDocumento() : null;

        Long representanteId = r != null ? r.getId() : null;
        String representanteNombre = r != null ? r.getNombres() + " " + r.getApellidos() : null;

        return new EstablecimientoResponse(
                e.getId(),
                e.getNombreComercial(),
                e.getRazonSocial(),
                e.getRuc(),
                e.getDireccion(),
                e.getReferencia(),
                e.getGiroNegocio(),
                e.getAreaMetrosCuadrados(),
                e.getNumeroPisos(),
                e.getTelefono(),
                e.getEmail(),
                e.isActivo(),
                e.getFechaRegistro(),
                ciudadanoId,
                ciudadanoNombre,
                ciudadanoDoc,
                representanteId,
                representanteNombre);
    }
}
