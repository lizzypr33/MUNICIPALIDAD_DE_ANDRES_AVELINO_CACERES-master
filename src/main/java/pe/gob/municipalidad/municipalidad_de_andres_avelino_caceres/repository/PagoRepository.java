package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Pago;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoPago;

public interface PagoRepository extends JpaRepository<Pago, Long>, JpaSpecificationExecutor<Pago> {

    @Override
    @EntityGraph(attributePaths = { "solicitud", "solicitud.ciudadano", "solicitud.establecimiento",
            "solicitud.tipoTramite", "usuarioVerificador" })
    Page<Pago> findAll(Specification<Pago> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = { "solicitud", "solicitud.ciudadano", "solicitud.establecimiento",
            "solicitud.tipoTramite", "usuarioVerificador" })
    List<Pago> findAll(Specification<Pago> spec);

    @EntityGraph(attributePaths = { "solicitud", "solicitud.ciudadano", "solicitud.establecimiento",
            "solicitud.tipoTramite", "usuarioVerificador" })
    Optional<Pago> findWithRelacionesById(Long id);

    @EntityGraph(attributePaths = { "solicitud", "solicitud.ciudadano", "solicitud.establecimiento",
            "solicitud.tipoTramite", "usuarioVerificador" })
    List<Pago> findBySolicitudIdOrderByIdDesc(Long solicitudId);

    @EntityGraph(attributePaths = { "solicitud", "usuarioVerificador" })
    List<Pago> findBySolicitudId(Long solicitudId);

    boolean existsByCodigoOperacionIgnoreCaseAndMedioPagoIgnoreCaseAndEstadoNotIn(
            String codigoOperacion,
            String medioPago,
            Collection<EstadoPago> estadosExcluidos);

    long countByEstado(EstadoPago estado);

    long countByEstadoIn(Collection<EstadoPago> estados);

    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.estado IN :estados AND UPPER(p.moneda) = 'PEN'")
    BigDecimal sumarMontoPorEstadosPen(@Param("estados") Collection<EstadoPago> estados);
}
