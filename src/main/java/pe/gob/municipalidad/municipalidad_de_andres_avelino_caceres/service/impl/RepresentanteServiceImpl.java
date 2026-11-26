package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.RepresentanteRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.RepresentanteResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Representante;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoDocumento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ConflictException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ResourceNotFoundException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.RepresentanteMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.RepresentanteRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.SolicitudRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.AuditoriaService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.RepresentanteService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.validation.DocumentoIdentidadValidator;

@Service
public class RepresentanteServiceImpl implements RepresentanteService {

    private final RepresentanteRepository representantes;
    private final SolicitudRepository solicitudes;
    private final RepresentanteMapper mapper;
    private final DocumentoIdentidadValidator documentoValidator;
    private final AuditoriaService auditoria;

    public RepresentanteServiceImpl(RepresentanteRepository representantes, SolicitudRepository solicitudes,
            RepresentanteMapper mapper, DocumentoIdentidadValidator documentoValidator,
            AuditoriaService auditoria) {
        this.representantes = representantes;
        this.solicitudes = solicitudes;
        this.mapper = mapper;
        this.documentoValidator = documentoValidator;
        this.auditoria = auditoria;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<RepresentanteResponse> listar(TipoDocumento tipoDocumento, String numeroDocumento,
            String nombres, String apellidos, Boolean activo, Pageable pageable) {
        return PaginaResponse.from(
                representantes.findAll(filtros(tipoDocumento, numeroDocumento, nombres, apellidos, activo), pageable)
                        .map(mapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public RepresentanteResponse obtener(Long id) {
        return mapper.toResponse(buscar(id));
    }

    @Override
    @Transactional
    public RepresentanteResponse crear(RepresentanteRequest request) {
        String numNormalizado = documentoValidator.validarYNormalizar(request.tipoDocumento(),
                request.numeroDocumento());
        String nombres = normalizarTextoObligatorio(request.nombres(), "Los nombres son obligatorios.");
        String apellidos = normalizarTextoObligatorio(request.apellidos(), "Los apellidos son obligatorios.");

        if (representantes.existsByTipoDocumentoAndNumeroDocumento(request.tipoDocumento(), numNormalizado)) {
            throw new ConflictException(
                    "Ya existe un representante registrado con el documento " + request.tipoDocumento() + " "
                            + numNormalizado);
        }

        Representante r = new Representante();
        r.setTipoDocumento(request.tipoDocumento());
        r.setNumeroDocumento(numNormalizado);
        r.setNombres(nombres);
        r.setApellidos(apellidos);
        r.setCargo(normalizarOpcional(request.cargo()));
        r.setTelefono(normalizarOpcional(request.telefono()));
        r.setEmail(normalizarEmail(request.email()));
        representantes.save(r);

        auditoria.registrar("REGISTRAR_REPRESENTANTE", "Representante", r.getId(),
                "Representante registrado: " + r.getTipoDocumento() + " " + r.getNumeroDocumento());
        return mapper.toResponse(r);
    }

    @Override
    @Transactional
    public RepresentanteResponse actualizar(Long id, RepresentanteRequest request) {
        Representante r = buscar(id);
        String numNormalizado = documentoValidator.validarYNormalizar(request.tipoDocumento(),
                request.numeroDocumento());

        boolean cambiaDocumento = request.tipoDocumento() != r.getTipoDocumento()
                || !numNormalizado.equals(r.getNumeroDocumento());
        if (cambiaDocumento) {
            if (solicitudes.existsByRepresentanteId(r.getId())) {
                throw new ConflictException(
                        "No se puede modificar el documento de un representante que tiene expedientes vinculados.");
            }
            if (representantes.existsByTipoDocumentoAndNumeroDocumento(request.tipoDocumento(), numNormalizado)) {
                throw new ConflictException(
                        "Ya existe un representante registrado con el documento " + request.tipoDocumento() + " "
                                + numNormalizado);
            }
            r.setTipoDocumento(request.tipoDocumento());
            r.setNumeroDocumento(numNormalizado);
        }

        r.setNombres(normalizarTextoObligatorio(request.nombres(), "Los nombres son obligatorios."));
        r.setApellidos(normalizarTextoObligatorio(request.apellidos(), "Los apellidos son obligatorios."));
        r.setCargo(normalizarOpcional(request.cargo()));
        r.setTelefono(normalizarOpcional(request.telefono()));
        r.setEmail(normalizarEmail(request.email()));

        auditoria.registrar("ACTUALIZAR_REPRESENTANTE", "Representante", r.getId(),
                "Representante actualizado: " + r.getTipoDocumento() + " " + r.getNumeroDocumento());
        return mapper.toResponse(r);
    }

    @Override
    @Transactional
    public RepresentanteResponse cambiarEstado(Long id, boolean activo) {
        Representante r = buscar(id);
        if (r.isActivo() == activo) {
            return mapper.toResponse(r);
        }
        r.setActivo(activo);

        auditoria.registrar(activo ? "ACTIVAR_REPRESENTANTE" : "DESACTIVAR_REPRESENTANTE", "Representante", r.getId(),
                (activo ? "Representante activado: " : "Representante desactivado: ") + r.getTipoDocumento() + " "
                        + r.getNumeroDocumento());
        return mapper.toResponse(r);
    }

    private Representante buscar(Long id) {
        return representantes.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Representante no encontrado con id " + id));
    }

    private Specification<Representante> filtros(TipoDocumento tipoDocumento, String numeroDocumento,
            String nombres, String apellidos, Boolean activo) {
        return (Root<Representante> root, jakarta.persistence.criteria.CriteriaQuery<?> query,
                jakarta.persistence.criteria.CriteriaBuilder cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (tipoDocumento != null) {
                predicados.add(cb.equal(root.get("tipoDocumento"), tipoDocumento));
            }
            if (StringUtils.hasText(numeroDocumento)) {
                predicados.add(cb.like(cb.lower(root.get("numeroDocumento")), patron(numeroDocumento.trim())));
            }
            if (StringUtils.hasText(nombres)) {
                predicados.add(cb.like(cb.lower(root.get("nombres")), patron(nombres.trim())));
            }
            if (StringUtils.hasText(apellidos)) {
                predicados.add(cb.like(cb.lower(root.get("apellidos")), patron(apellidos.trim())));
            }
            if (activo != null) {
                predicados.add(cb.equal(root.get("activo"), activo));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }

    private static String patron(String texto) {
        return "%" + texto.toLowerCase() + "%";
    }

    private static String normalizarTextoObligatorio(String valor, String mensajeError) {
        if (!StringUtils.hasText(valor)) {
            throw new BusinessException(mensajeError);
        }
        return valor.trim().replaceAll("\\s+", " ");
    }

    private static String normalizarOpcional(String valor) {
        if (!StringUtils.hasText(valor)) {
            return null;
        }
        return valor.trim().replaceAll("\\s+", " ");
    }

    private static String normalizarEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return email.trim().toLowerCase();
    }
}
