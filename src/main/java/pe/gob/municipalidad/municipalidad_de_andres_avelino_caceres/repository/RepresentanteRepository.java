package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Representante;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoDocumento;

public interface RepresentanteRepository
        extends JpaRepository<Representante, Long>, JpaSpecificationExecutor<Representante> {

    Optional<Representante> findByTipoDocumentoAndNumeroDocumento(TipoDocumento tipoDocumento, String numeroDocumento);

    boolean existsByTipoDocumentoAndNumeroDocumento(TipoDocumento tipoDocumento, String numeroDocumento);
}
