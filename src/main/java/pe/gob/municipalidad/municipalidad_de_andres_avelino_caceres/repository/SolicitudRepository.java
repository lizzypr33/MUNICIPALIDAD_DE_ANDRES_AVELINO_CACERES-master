package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Solicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoSolicitud;

public interface SolicitudRepository
        extends JpaRepository<Solicitud, Long>, JpaSpecificationExecutor<Solicitud> {

    @Override
    @EntityGraph(attributePaths = { "ciudadano", "establecimiento", "tipoTramite", "representante", "usuarioResponsable" })
    Page<Solicitud> findAll(Specification<Solicitud> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = { "ciudadano", "establecimiento", "tipoTramite", "representante", "usuarioResponsable" })
    List<Solicitud> findAll(Specification<Solicitud> spec);

    long countByEstado(EstadoSolicitud estado);

    long countByEstadoIn(Collection<EstadoSolicitud> estados);

    @EntityGraph(attributePaths = {
            "ciudadano", "ciudadano.usuario",
            "establecimiento", "establecimiento.ciudadano", "establecimiento.representante",
            "tipoTramite", "representante", "usuarioResponsable"
    })
    Optional<Solicitud> findWithRelacionesById(Long id);

    @EntityGraph(attributePaths = { "ciudadano", "ciudadano.usuario", "establecimiento", "tipoTramite", "representante", "usuarioResponsable" })
    Optional<Solicitud> findByNumeroExpediente(String numeroExpediente);

    boolean existsByNumeroExpediente(String numeroExpediente);

    List<Solicitud> findByCiudadanoId(Long ciudadanoId);

    boolean existsByCiudadanoId(Long ciudadanoId);

    boolean existsByEstablecimientoId(Long establecimientoId);

    boolean existsByRepresentanteId(Long representanteId);

    boolean existsByTipoTramiteId(Long tipoTramiteId);

    List<Solicitud> findByEstado(EstadoSolicitud estado);

    @Modifying
    @Query(value = "INSERT INTO seq_numero_expediente () VALUES ()", nativeQuery = true)
    void avanzar_seq_numero_expediente();

    @Query(value = "SELECT LAST_INSERT_ID()", nativeQuery = true)
    Long ultimoId_seq_numero_expediente();

    /** Emula una secuencia en MySQL; llamar dentro de una transaccion (misma conexion). */
    default Long obtenerSiguienteCorrelativoExpediente() {
        avanzar_seq_numero_expediente();
        return ultimoId_seq_numero_expediente();
    }
}
