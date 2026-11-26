package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Documento;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {

    @EntityGraph(attributePaths = { "solicitud", "requisito" })
    List<Documento> findBySolicitudId(Long solicitudId);

    @EntityGraph(attributePaths = { "solicitud", "requisito" })
    List<Documento> findBySolicitudIdAndActivoTrueOrderByFechaSubidaAscIdAsc(Long solicitudId);

    @EntityGraph(attributePaths = { "solicitud", "solicitud.ciudadano", "solicitud.ciudadano.usuario", "requisito" })
    Optional<Documento> findWithRelacionesById(Long id);

    boolean existsByRequisitoId(Long requisitoId);
}
