package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.ExpedienteResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Ciudadano;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Solicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ConflictException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ResourceNotFoundException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.SolicitudMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.SolicitudRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.security.CustomUserDetails;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.ExpedienteService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.util.CodigoGenerator;

@Service
public class ExpedienteServiceImpl implements ExpedienteService {

    private final SolicitudRepository solicitudes;
    private final CodigoGenerator codigoGenerator;
    private final SolicitudMapper solicitudMapper;

    public ExpedienteServiceImpl(SolicitudRepository solicitudes,
            CodigoGenerator codigoGenerator,
            SolicitudMapper solicitudMapper) {
        this.solicitudes = solicitudes;
        this.codigoGenerator = codigoGenerator;
        this.solicitudMapper = solicitudMapper;
    }

    @Override
    @Transactional
    public String generarNumeroExpediente() {
        int anioActual = LocalDate.now().getYear();
        for (int intento = 0; intento < 5; intento++) {
            Long correlativo = solicitudes.obtenerSiguienteCorrelativoExpediente();
            if (correlativo == null || correlativo <= 0) {
                throw new IllegalStateException("No se pudo obtener el correlativo de la secuencia de expedientes.");
            }
            String candidato = codigoGenerator.generarNumeroExpediente(anioActual, correlativo);
            if (!solicitudes.existsByNumeroExpediente(candidato)) {
                return candidato;
            }
        }
        throw new ConflictException("No fue posible generar un número de expediente único.");
    }

    @Override
    @Transactional(readOnly = true)
    public ExpedienteResponse obtenerPorNumero(String numeroExpediente) {
        if (!StringUtils.hasText(numeroExpediente)) {
            throw new BusinessException("El número de expediente es obligatorio.");
        }
        String normalizado = numeroExpediente.trim().toUpperCase(Locale.ROOT);
        Solicitud s = solicitudes.findByNumeroExpediente(normalizado)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Expediente no encontrado con número " + normalizado));
        verificarAccesoCiudadano(s);
        return solicitudMapper.toExpedienteResponse(s);
    }

    private void verificarAccesoCiudadano(Solicitud s) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return;
        }
        List<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        boolean esPersonalMunicipal = authorities.contains("ROLE_ADMINISTRADOR")
                || authorities.contains("ROLE_FUNCIONARIO")
                || authorities.contains("ROLE_SUPERVISOR");
        if (esPersonalMunicipal || !authorities.contains("ROLE_CIUDADANO")) {
            return;
        }
        Long usuarioActualId = auth.getPrincipal() instanceof CustomUserDetails details ? details.getId() : null;
        Ciudadano titular = s.getCiudadano();
        if (usuarioActualId == null || titular == null || titular.getUsuario() == null
                || !usuarioActualId.equals(titular.getUsuario().getId())) {
            throw new AccessDeniedException("No tiene permisos para acceder a expedientes de otros ciudadanos.");
        }
    }
}
