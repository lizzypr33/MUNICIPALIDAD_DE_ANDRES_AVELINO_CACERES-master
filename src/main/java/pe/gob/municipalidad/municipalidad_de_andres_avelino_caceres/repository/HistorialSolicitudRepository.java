package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.HistorialSolicitud;

public interface HistorialSolicitudRepository extends JpaRepository<HistorialSolicitud, Long> {

    @EntityGraph(attributePaths = { "usuarioResponsable" })
    List<HistorialSolicitud> findBySolicitudIdOrderByFechaCambioAscIdAsc(Long solicitudId);
}
