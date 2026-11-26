package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Auditoria;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long>, JpaSpecificationExecutor<Auditoria> {

    @Override
    @EntityGraph(attributePaths = { "usuario" })
    Page<Auditoria> findAll(Specification<Auditoria> spec, Pageable pageable);

    @EntityGraph(attributePaths = { "usuario" })
    Optional<Auditoria> findWithUsuarioById(Long id);

    @EntityGraph(attributePaths = { "usuario" })
    List<Auditoria> findByUsuarioId(Long usuarioId);
}
