package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository;

import java.time.LocalDate;
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

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Licencia;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoLicencia;

public interface LicenciaRepository extends JpaRepository<Licencia, Long>, JpaSpecificationExecutor<Licencia> {

    @Override
    @EntityGraph(attributePaths = { "solicitud", "solicitud.ciudadano", "solicitud.establecimiento",
            "solicitud.tipoTramite", "solicitud.representante", "usuarioEmisor" })
    Page<Licencia> findAll(Specification<Licencia> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = { "solicitud", "solicitud.ciudadano", "solicitud.establecimiento",
            "solicitud.tipoTramite", "solicitud.representante", "usuarioEmisor" })
    List<Licencia> findAll(Specification<Licencia> spec);

    long countByEstado(EstadoLicencia estado);

    @EntityGraph(attributePaths = { "solicitud", "solicitud.ciudadano", "solicitud.establecimiento",
            "solicitud.tipoTramite", "solicitud.representante", "usuarioEmisor" })
    Optional<Licencia> findWithRelacionesById(Long id);

    @EntityGraph(attributePaths = { "solicitud", "solicitud.ciudadano", "solicitud.establecimiento",
            "solicitud.tipoTramite", "solicitud.representante", "usuarioEmisor" })
    Optional<Licencia> findByCodigoLicencia(String codigoLicencia);

    @EntityGraph(attributePaths = { "solicitud", "solicitud.ciudadano", "solicitud.establecimiento",
            "solicitud.tipoTramite", "solicitud.representante", "usuarioEmisor" })
    Optional<Licencia> findBySolicitudId(Long solicitudId);

    @EntityGraph(attributePaths = { "solicitud", "solicitud.ciudadano", "solicitud.establecimiento",
            "solicitud.tipoTramite", "usuarioEmisor" })
    Optional<Licencia> findByTokenVerificacion(String tokenVerificacion);

    @EntityGraph(attributePaths = { "solicitud", "solicitud.ciudadano", "solicitud.establecimiento" })
    List<Licencia> findByEstado(EstadoLicencia estado);

    @EntityGraph(attributePaths = { "solicitud", "solicitud.ciudadano", "solicitud.establecimiento" })
    List<Licencia> findBySolicitudEstablecimientoId(Long establecimientoId);

    @EntityGraph(attributePaths = { "solicitud", "solicitud.ciudadano", "solicitud.establecimiento" })
    List<Licencia> findBySolicitudCiudadanoId(Long ciudadanoId);

    @EntityGraph(attributePaths = { "solicitud", "solicitud.ciudadano", "solicitud.establecimiento" })
    List<Licencia> findByFechaEmisionBetween(LocalDate desde, LocalDate hasta);

    boolean existsBySolicitudId(Long solicitudId);

    boolean existsByCodigoLicencia(String codigoLicencia);

    @Modifying
    @Query(value = "INSERT INTO seq_codigo_licencia () VALUES ()", nativeQuery = true)
    void avanzar_seq_codigo_licencia();

    @Query(value = "SELECT LAST_INSERT_ID()", nativeQuery = true)
    Long ultimoId_seq_codigo_licencia();

    /** Emula una secuencia en MySQL; llamar dentro de una transaccion (misma conexion). */
    default Long obtenerSiguienteCorrelativoLicencia() {
        avanzar_seq_codigo_licencia();
        return ultimoId_seq_codigo_licencia();
    }
}
