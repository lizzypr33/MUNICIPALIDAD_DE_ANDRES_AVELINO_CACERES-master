package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.RequisitoRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.request.TipoTramiteRequest;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.PaginaResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.RequisitoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.TipoTramiteResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Requisito;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.TipoTramite;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.TipoTramiteRequisito;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ConflictException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ResourceNotFoundException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.RequisitoMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.TipoTramiteMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.RequisitoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.SolicitudRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.TipoTramiteRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.TipoTramiteRequisitoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.AuditoriaService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.TipoTramiteService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.util.CodigoGenerator;

@Service
public class TipoTramiteServiceImpl implements TipoTramiteService {

    private final TipoTramiteRepository tiposTramite;
    private final RequisitoRepository requisitos;
    private final TipoTramiteRequisitoRepository tipoTramiteRequisitos;
    private final SolicitudRepository solicitudes;
    private final TipoTramiteMapper mapper;
    private final RequisitoMapper requisitoMapper;
    private final CodigoGenerator codigoGenerator;
    private final AuditoriaService auditoria;

    public TipoTramiteServiceImpl(TipoTramiteRepository tiposTramite,
            RequisitoRepository requisitos,
            TipoTramiteRequisitoRepository tipoTramiteRequisitos,
            SolicitudRepository solicitudes,
            TipoTramiteMapper mapper,
            RequisitoMapper requisitoMapper,
            CodigoGenerator codigoGenerator,
            AuditoriaService auditoria) {
        this.tiposTramite = tiposTramite;
        this.requisitos = requisitos;
        this.tipoTramiteRequisitos = tipoTramiteRequisitos;
        this.solicitudes = solicitudes;
        this.mapper = mapper;
        this.requisitoMapper = requisitoMapper;
        this.codigoGenerator = codigoGenerator;
        this.auditoria = auditoria;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<TipoTramiteResponse> listar(String codigo, String nombre, Boolean activo,
            Pageable pageable) {
        return PaginaResponse.from(
                tiposTramite.findAll(filtros(codigo, nombre, activo), pageable).map(mapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public TipoTramiteResponse obtener(Long id) {
        return mapper.toResponse(buscar(id));
    }

    @Override
    @Transactional
    public TipoTramiteResponse crear(TipoTramiteRequest request) {
        String codigo = codigoGenerator.normalizarCodigo(request.codigo(), "tipo de trámite");
        String nombre = normalizarTextoObligatorio(request.nombre(), "El nombre del tipo de trámite es obligatorio.");

        if (tiposTramite.existsByCodigo(codigo)) {
            throw new ConflictException("Ya existe un tipo de trámite con el código " + codigo);
        }

        TipoTramite t = new TipoTramite();
        t.setCodigo(codigo);
        t.setNombre(nombre);
        t.setDescripcion(normalizarOpcional(request.descripcion()));
        if (request.requiereInspeccion() != null) {
            t.setRequiereInspeccion(request.requiereInspeccion());
        }
        t.setRequiereInspeccion(request.requiereInspeccion() == null || request.requiereInspeccion());
        t.setActivo(true);
        tiposTramite.save(t);

        auditoria.registrar("CREAR_TIPO_TRAMITE", "TipoTramite", t.getId(),
                "Tipo de trámite registrado: " + t.getCodigo() + " - " + t.getNombre());
        return mapper.toResponse(t);
    }

    @Override
    @Transactional
    public TipoTramiteResponse actualizar(Long id, TipoTramiteRequest request) {
        TipoTramite t = buscar(id);
        String nuevoCodigo = codigoGenerator.normalizarCodigo(request.codigo(), "tipo de trámite");
        String nombre = normalizarTextoObligatorio(request.nombre(), "El nombre del tipo de trámite es obligatorio.");

        if (!nuevoCodigo.equals(t.getCodigo())) {
            if (solicitudes.existsByTipoTramiteId(t.getId())) {
                throw new ConflictException(
                        "No se puede modificar el código de un tipo de trámite que ya cuenta con solicitudes vinculadas.");
            }
            if (tiposTramite.existsByCodigo(nuevoCodigo)) {
                throw new ConflictException("Ya existe un tipo de trámite con el código " + nuevoCodigo);
            }
            t.setCodigo(nuevoCodigo);
        }

        t.setNombre(nombre);
        t.setDescripcion(normalizarOpcional(request.descripcion()));
        if (request.requiereInspeccion() != null) {
            t.setRequiereInspeccion(request.requiereInspeccion());
        }

        auditoria.registrar("ACTUALIZAR_TIPO_TRAMITE", "TipoTramite", t.getId(),
                "Tipo de trámite actualizado: " + t.getCodigo());
        return mapper.toResponse(t);
    }

    @Override
    @Transactional
    public TipoTramiteResponse cambiarEstado(Long id, boolean activo) {
        TipoTramite t = buscar(id);
        if (t.isActivo() == activo) {
            return mapper.toResponse(t);
        }
        t.setActivo(activo);

        auditoria.registrar(activo ? "ACTIVAR_TIPO_TRAMITE" : "DESACTIVAR_TIPO_TRAMITE", "TipoTramite", t.getId(),
                (activo ? "Tipo de trámite activado: " : "Tipo de trámite desactivado: ") + t.getCodigo());
        return mapper.toResponse(t);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RequisitoResponse> listarRequisitos(Long tipoTramiteId) {
        buscar(tipoTramiteId);
        return tipoTramiteRequisitos.findByTipoTramiteIdOrderByOrdenVisualizacionAsc(tipoTramiteId).stream()
                .map(requisitoMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<RequisitoResponse> configurarRequisitos(Long tipoTramiteId,
            List<RequisitoRequest.AsignacionTramiteItem> items) {
        TipoTramite tramite = buscar(tipoTramiteId);
        if (items == null) {
            throw new BusinessException("La lista de requisitos a configurar no puede ser nula.");
        }

        Set<Long> vistos = new HashSet<>();
        List<TipoTramiteRequisito> nuevos = new ArrayList<>();
        int indiceDefecto = 1;

        for (RequisitoRequest.AsignacionTramiteItem item : items) {
            if (item == null || item.requisitoId() == null) {
                throw new BusinessException("Cada elemento debe indicar el identificador del requisito.");
            }
            if (!vistos.add(item.requisitoId())) {
                throw new ConflictException(
                        "No se puede duplicar el requisito con id " + item.requisitoId() + " en el mismo tipo de trámite.");
            }
            Requisito req = requisitos.findById(item.requisitoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Requisito no encontrado con id " + item.requisitoId()));
            if (!req.isActivo()) {
                throw new BusinessException("El requisito '" + req.getCodigo() + "' se encuentra inactivo.");
            }

            TipoTramiteRequisito ttr = new TipoTramiteRequisito();
            ttr.setTipoTramite(tramite);
            ttr.setRequisito(req);
            ttr.setObligatorio(item.obligatorio() == null || item.obligatorio());
            ttr.setOrdenVisualizacion(
                    item.ordenVisualizacion() != null ? item.ordenVisualizacion() : indiceDefecto);
            nuevos.add(ttr);
            indiceDefecto++;
        }

        // Las solicitudes ya presentadas conservan su fotografía (requisitosSnapshot) en la tabla solicitudes.
        tipoTramiteRequisitos.deleteByTipoTramiteId(tipoTramiteId);
        tipoTramiteRequisitos.flush();
        tipoTramiteRequisitos.saveAll(nuevos);

        auditoria.registrar("CONFIGURAR_REQUISITOS_TRAMITE", "TipoTramite", tramite.getId(),
                "Configurados " + nuevos.size() + " requisitos para el trámite " + tramite.getCodigo());

        return tipoTramiteRequisitos.findByTipoTramiteIdOrderByOrdenVisualizacionAsc(tipoTramiteId).stream()
                .map(requisitoMapper::toResponse)
                .toList();
    }

    private TipoTramite buscar(Long id) {
        return tiposTramite.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de trámite no encontrado con id " + id));
    }

    private Specification<TipoTramite> filtros(String codigo, String nombre, Boolean activo) {
        return (Root<TipoTramite> root, jakarta.persistence.criteria.CriteriaQuery<?> query,
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
