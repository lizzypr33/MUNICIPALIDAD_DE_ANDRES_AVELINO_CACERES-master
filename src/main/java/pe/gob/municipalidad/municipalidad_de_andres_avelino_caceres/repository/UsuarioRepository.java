package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    @EntityGraph(attributePaths = { "roles" })
    Optional<Usuario> findWithRolesById(Long id);

    Optional<Usuario> findByUsername(String username);

    Optional<Usuario> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /** Cuenta usuarios (activos o no) que tienen el rol indicado. */
    @Query("select count(u) from Usuario u join u.roles r where r.nombre = :rol")
    long countConRol(@Param("rol") String rol);

    /** Cuenta usuarios activos que tienen el rol indicado. */
    @Query("select count(u) from Usuario u join u.roles r where r.nombre = :rol and u.activo = true")
    long countActivosConRol(@Param("rol") String rol);
}
