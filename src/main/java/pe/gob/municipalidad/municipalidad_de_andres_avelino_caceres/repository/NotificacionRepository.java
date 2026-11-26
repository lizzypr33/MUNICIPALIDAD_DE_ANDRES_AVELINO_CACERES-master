package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Notificacion;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    @EntityGraph(attributePaths = { "usuario" })
    Optional<Notificacion> findWithUsuarioById(Long id);

    Page<Notificacion> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId, Pageable pageable);

    Page<Notificacion> findByUsuarioIdAndLeidaFalseOrderByFechaCreacionDesc(Long usuarioId, Pageable pageable);

    List<Notificacion> findByUsuarioIdAndLeidaFalse(Long usuarioId);

    List<Notificacion> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    long countByUsuarioIdAndLeidaFalse(Long usuarioId);
}
