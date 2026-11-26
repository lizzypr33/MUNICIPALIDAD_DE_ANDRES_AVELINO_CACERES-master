package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.HistorialPago;

public interface HistorialPagoRepository extends JpaRepository<HistorialPago, Long> {

    @EntityGraph(attributePaths = { "usuarioResponsable" })
    List<HistorialPago> findByPagoIdOrderByFechaCambioAscIdAsc(Long pagoId);
}
