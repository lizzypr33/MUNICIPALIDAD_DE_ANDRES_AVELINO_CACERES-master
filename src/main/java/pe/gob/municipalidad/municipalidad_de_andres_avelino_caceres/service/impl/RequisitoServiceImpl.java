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
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.RequisitoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.RequisitoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Requisito;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ConflictException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ResourceNotFoundException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.RequisitoMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.DocumentoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.RequisitoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.AuditoriaService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.RequisitoService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.util.CodigoGenerator;

@Service
public class RequisitoServiceImpl implements RequisitoService {

    private final RequisitoRepository requisitos;
    private final DocumentoRepository documentos;
    private final RequisitoMapper mapper;
    private final CodigoGenerator codigoGenerator;
    private final AuditoriaService auditoria;

    public RequisitoServiceImpl(RequisitoRepository requisitos,
            DocumentoRepository documentos,
            RequisitoMapper mapper,
            CodigoGenerator codigoGenerator,
            AuditoriaService auditoria) {
        this.requisitos = requisitos;
        this.documentos = documentos;
        this.mapper = mapper;
        this.codigoGenerator = codigoGenerator;
        this.auditoria = auditoria;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<RequisitoResponse> listar(String codigo, String nombre, Boolean activo, Pageable pageable) {
        return PaginaResponse.from(
                requisitos.findAll(filtros(codigo, nombre, activo), pageable).map(mapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public RequisitoResponse obtener(Long id) {
        return mapper.toResponse(buscar(id));
    }

    @Override
    @Transactional
    public RequisitoResponse crear(RequisitoRequest request) {
        String codigo = codigoGenerator.normalizarCodigo(request.codigo(), "requisito");
        String nombre = normalizarTextoObligatorio(request.nombre(), "El nombre del requisito es obligatorio.");

        if (requisitos.existsByCodigo(codigo)) {
            throw new ConflictException("Ya existe un requisito con el código " + codigo);
        }

        Requisito r = new Requisito();
        r.setCodigo(codigo);
        r.setNombre(nombre);
        r.setDescripcion(normalizarOpcional(request.descripcion()));
        r.setActivo(true);
        requisitos.save(r);

        auditoria.registrar("CREAR_REQUISITO", "Requisito", r.getId(),
                "Requisito registrado: " + r.getCodigo() + " - " + r.getNombre());
        return mapper.toResponse(r);
    }

    @Override
    @Transactional
    public RequisitoResponse actualizar(Long id, RequisitoRequest request) {
        Requisito r = buscar(id);
        String nuevoCodigo = codigoGenerator.normalizarCodigo(request.codigo(), "requisito");
        String nombre = normalizarTextoObligatorio(request.nombre(), "El nombre del requisito es obligatorio.");

        if (!nuevoCodigo.equals(r.getCodigo())) {
            if (documentos.existsByRequisitoId(r.getId())) {
                throw new ConflictException(
                        "No se puede modificar el código de un requisito que ya tiene documentos de expedientes vinculados.");
            }
            if (requisitos.existsByCodigo(nuevoCodigo)) {
                throw new ConflictException("Ya existe un requisito con el código " + nuevoCodigo);
            }
            r.setCodigo(nuevoCodigo);
        }

        r.setNombre(nombre);
        r.setDescripcion(normalizarOpcional(request.descripcion()));

        auditoria.registrar("ACTUALIZAR_REQUISITO", "Requisito", r.getId(),
                "Requisito actualizado: " + r.getCodigo());
        return mapper.toResponse(r);
    }

    @Override
    @Transactional
    public RequisitoResponse cambiarEstado(Long id, boolean activo) {
        Requisito r = buscar(id);
        if (r.isActivo() == activo) {
            return mapper.toResponse(r);
        }
        r.setActivo(activo);

        auditoria.registrar(activo ? "ACTIVAR_REQUISITO" : "DESACTIVAR_REQUISITO", "Requisito", r.getId(),
                (activo ? "Requisito activado: " : "Requisito desactivado: ") + r.getCodigo());
        return mapper.toResponse(r);
    }

    private Requisito buscar(Long id) {
        return requisitos.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requisito no encontrado con id " + id));
    }

    private Specification<Requisito> filtros(String codigo, String nombre, Boolean activo) {
        return (Root<Requisito> root, jakarta.persistence.criteria.CriteriaQuery<?> query,
                jakarta.persistence.criteria.CriteriaBuilder cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (StringUtils.hasText(codigo)) {
                predicados.add(cb.like(cb.lower(root.get("codigo")), "%" + codigo.trim().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(nombre)) {
                predicados.add(cb.like(cb.lower(root.get("nombre")), "%" + nombre.trim().toLowerCase() + "%"));
            }
            if (activo != null) {
                predicados.add(cb.equal(root.get("activo"), activo));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
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
}
