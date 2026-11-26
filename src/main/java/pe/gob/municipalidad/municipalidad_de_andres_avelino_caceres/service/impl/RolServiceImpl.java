package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.RolResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ResourceNotFoundException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.RolMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.RolRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.RolService;

@Service
public class RolServiceImpl implements RolService {

    private final RolRepository roles;
    private final RolMapper mapper;

    public RolServiceImpl(RolRepository roles, RolMapper mapper) {
        this.roles = roles;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolResponse> listar() {
        return roles.findAll().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RolResponse obtener(Long id) {
        return roles.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con id " + id));
    }
}
