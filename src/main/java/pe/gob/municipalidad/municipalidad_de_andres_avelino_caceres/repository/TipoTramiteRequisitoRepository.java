package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.TipoTramiteRequisito;

public interface TipoTramiteRequisitoRepository extends JpaRepository<TipoTramiteRequisito, Long> {

    @EntityGraph(attributePaths = { "requisito", "tipoTramite" })
    List<TipoTramiteRequisito> findByTipoTramiteIdOrderByOrdenVisualizacionAsc(Long tipoTramiteId);

    boolean existsByRequisitoId(Long requisitoId);

    void deleteByTipoTramiteId(Long tipoTramiteId);
}
