package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Inspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoInspeccion;

public interface InspeccionRepository extends JpaRepository<Inspeccion, Long>, JpaSpecificationExecutor<Inspeccion> {

    @Override
    @EntityGraph(attributePaths = { "solicitud", "solicitud.ciudadano", "solicitud.establecimiento",
            "solicitud.tipoTramite", "inspector" })
    Page<Inspeccion> findAll(Specification<Inspeccion> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = { "solicitud", "solicitud.ciudadano", "solicitud.establecimiento",
            "solicitud.tipoTramite", "inspector" })
    List<Inspeccion> findAll(Specification<Inspeccion> spec);

    long countByEstado(EstadoInspeccion estado);

    @EntityGraph(attributePaths = { "solicitud", "solicitud.ciudadano", "solicitud.establecimiento",
            "solicitud.tipoTramite", "inspector" })
    Optional<Inspeccion> findWithRelacionesById(Long id);

    @EntityGraph(attributePaths = { "solicitud", "solicitud.ciudadano", "solicitud.establecimiento",
            "solicitud.tipoTramite", "inspector" })
    List<Inspeccion> findBySolicitudIdOrderByIdDesc(Long solicitudId);

    @EntityGraph(attributePaths = { "solicitud", "inspector" })
    List<Inspeccion> findBySolicitudId(Long solicitudId);

    @EntityGraph(attributePaths = { "solicitud", "inspector" })
    List<Inspeccion> findByInspectorIdOrderByFechaProgramadaDesc(Long inspectorId);

    @EntityGraph(attributePaths = { "solicitud", "inspector" })
    List<Inspeccion> findByEstado(EstadoInspeccion estado);

    @EntityGraph(attributePaths = { "solicitud", "inspector" })
    List<Inspeccion> findByFechaProgramadaBetween(LocalDateTime desde, LocalDateTime hasta);

    boolean existsBySolicitudIdAndEstadoIn(Long solicitudId, Collection<EstadoInspeccion> estados);

    boolean existsByInspectorIdAndFechaProgramadaAndEstadoIn(Long inspectorId, LocalDateTime fechaProgramada,
            Collection<EstadoInspeccion> estados);

    boolean existsByInspectorIdAndFechaProgramadaAndEstadoInAndIdNot(Long inspectorId, LocalDateTime fechaProgramada,
            Collection<EstadoInspeccion> estados, Long id);
}
