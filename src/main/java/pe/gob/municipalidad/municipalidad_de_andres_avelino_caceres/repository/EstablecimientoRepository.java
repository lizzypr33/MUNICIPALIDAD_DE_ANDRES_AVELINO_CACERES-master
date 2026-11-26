package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Establecimiento;

public interface EstablecimientoRepository
        extends JpaRepository<Establecimiento, Long>, JpaSpecificationExecutor<Establecimiento> {

    @Override
    @EntityGraph(attributePaths = { "ciudadano", "representante" })
    Page<Establecimiento> findAll(Specification<Establecimiento> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = { "ciudadano", "representante" })
    List<Establecimiento> findAll(Specification<Establecimiento> spec);

    long countByActivoTrue();

    @EntityGraph(attributePaths = { "ciudadano", "representante" })
    Optional<Establecimiento> findWithRelacionesById(Long id);

    @EntityGraph(attributePaths = { "ciudadano", "representante" })
    List<Establecimiento> findByRuc(String ruc);

    @EntityGraph(attributePaths = { "ciudadano", "representante" })
    List<Establecimiento> findByCiudadanoId(Long ciudadanoId);

    boolean existsByCiudadanoId(Long ciudadanoId);
}
