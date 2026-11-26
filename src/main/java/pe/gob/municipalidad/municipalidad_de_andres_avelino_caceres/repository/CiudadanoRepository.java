package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Ciudadano;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoDocumento;

public interface CiudadanoRepository extends JpaRepository<Ciudadano, Long>, JpaSpecificationExecutor<Ciudadano> {

    Optional<Ciudadano> findByTipoDocumentoAndNumeroDocumento(TipoDocumento tipoDocumento, String numeroDocumento);

    boolean existsByTipoDocumentoAndNumeroDocumento(TipoDocumento tipoDocumento, String numeroDocumento);

    Optional<Ciudadano> findByUsuarioId(Long usuarioId);

    long countByActivoTrue();
}
