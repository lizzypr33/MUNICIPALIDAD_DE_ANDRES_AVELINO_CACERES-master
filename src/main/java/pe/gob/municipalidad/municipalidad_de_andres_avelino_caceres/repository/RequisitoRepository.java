package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Requisito;

public interface RequisitoRepository
        extends JpaRepository<Requisito, Long>, JpaSpecificationExecutor<Requisito> {

    Optional<Requisito> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);
}
