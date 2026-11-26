package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Rol;

public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByNombre(String nombre);
}
