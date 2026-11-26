package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.impl;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response.DocumentoResponse;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Ciudadano;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Documento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Requisito;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity.Solicitud;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoDocumento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.ResourceNotFoundException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.mapper.DocumentoMapper;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.DocumentoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.RequisitoRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.repository.SolicitudRepository;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.security.CustomUserDetails;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.AuditoriaService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.service.DocumentoService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.storage.FileStorageService;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.util.CodigoGenerator;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.validation.DocumentoValidator;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.validation.SolicitudValidator;

@Service
public class DocumentoServiceImpl implements DocumentoService {

    private final DocumentoRepository documentos;
    private final SolicitudRepository solicitudes;
    private final RequisitoRepository requisitos;
    private final DocumentoValidator documentoValidator;
    private final SolicitudValidator solicitudValidator;
    private final FileStorageService fileStorage;
    private final CodigoGenerator codigoGenerator;
    private final DocumentoMapper mapper;
    private final AuditoriaService auditoria;

    public DocumentoServiceImpl(DocumentoRepository documentos,
            SolicitudRepository solicitudes,
            RequisitoRepository requisitos,
            DocumentoValidator documentoValidator,
            SolicitudValidator solicitudValidator,
            FileStorageService fileStorage,
            CodigoGenerator codigoGenerator,
            DocumentoMapper mapper,
            AuditoriaService auditoria) {
        this.documentos = documentos;
        this.solicitudes = solicitudes;
        this.requisitos = requisitos;
        this.documentoValidator = documentoValidator;
        this.solicitudValidator = solicitudValidator;
        this.fileStorage = fileStorage;
        this.codigoGenerator = codigoGenerator;
        this.mapper = mapper;
        this.auditoria = auditoria;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentoResponse> listarPorSolicitud(Long solicitudId) {
        Solicitud s = buscarSolicitud(solicitudId);
        verificarAccesoSolicitud(s);
        return documentos.findBySolicitudIdAndActivoTrueOrderByFechaSubidaAscIdAsc(solicitudId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public DocumentoResponse subir(Long solicitudId, Long requisitoId, String observaciones, MultipartFile archivo) {
        Solicitud solicitud = buscarSolicitud(solicitudId);
        verificarAccesoSolicitud(solicitud);
        solicitudValidator.validarPermiteCargaORetiroDeDocumentos(solicitud);

        Requisito requisito = null;
        if (requisitoId != null) {
            requisito = requisitos.findById(requisitoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Requisito no encontrado con id " + requisitoId));
            if (!requisito.isActivo()) {
                throw new BusinessException("El requisito indicado se encuentra inactivo.");
            }
        }

        DocumentoValidator.ArchivoValidado validado = documentoValidator.validar(archivo);
        String nombreInterno = codigoGenerator.generarNombreArchivoAlmacenado(validado.extension());
        String rutaRelativa = fileStorage.guardar(solicitud.getId(), nombreInterno, validado.contenido());

        try {
            Documento doc = new Documento();
            doc.setSolicitud(solicitud);
            doc.setRequisito(requisito);
            doc.setNombreOriginal(validado.nombreOriginal());
            doc.setNombreAlmacenado(nombreInterno);
            doc.setRutaAlmacenamiento(rutaRelativa);
            doc.setTipoMime(validado.tipoMime());
            doc.setTamanioBytes(validado.tamanioBytes());
            doc.setEstado(EstadoDocumento.PENDIENTE);
            doc.setObservaciones(normalizarOpcional(observaciones));
            doc.setActivo(true);
            documentos.save(doc);

            auditoria.registrar("SUBIR_DOCUMENTO", "Documento", doc.getId(),
                    "Documento subido para solicitud " + solicitud.getId()
                            + (requisito != null ? " (requisito " + requisito.getCodigo() + ")" : ""));
            return mapper.toResponse(doc);
        } catch (RuntimeException ex) {
            fileStorage.eliminarSilencioso(rutaRelativa);
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoResponse obtener(Long id) {
        Documento doc = buscarDocumento(id);
        verificarAccesoSolicitud(doc.getSolicitud());
        return mapper.toResponse(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public ArchivoDescargable descargar(Long id) {
        Documento doc = buscarDocumento(id);
        verificarAccesoSolicitud(doc.getSolicitud());
        if (!doc.isActivo()) {
            throw new ResourceNotFoundException("El documento solicitado fue retirado y no se encuentra activo.");
        }
        byte[] contenido = fileStorage.leer(doc.getRutaAlmacenamiento());
        return new ArchivoDescargable(doc.getNombreOriginal(), doc.getTipoMime(), contenido);
    }

    @Override
    @Transactional
    public DocumentoResponse retirar(Long id) {
        Documento doc = buscarDocumento(id);
        verificarAccesoSolicitud(doc.getSolicitud());
        solicitudValidator.validarPermiteCargaORetiroDeDocumentos(doc.getSolicitud());

        if (!doc.isActivo()) {
            return mapper.toResponse(doc);
        }
        doc.setActivo(false);

        auditoria.registrar("RETIRAR_DOCUMENTO", "Documento", doc.getId(),
                "Documento retirado lógicamente de la solicitud " + doc.getSolicitud().getId());
        return mapper.toResponse(doc);
    }

    @Override
    @Transactional
    public DocumentoResponse cambiarEstado(Long id, EstadoDocumento nuevoEstado, String observaciones) {
        if (nuevoEstado == null) {
            throw new BusinessException("El estado del documento es obligatorio.");
        }
        Documento doc = buscarDocumento(id);
        if (!doc.isActivo()) {
            throw new BusinessException("No se puede cambiar el estado de un documento retirado.");
        }
        doc.setEstado(nuevoEstado);
        if (StringUtils.hasText(observaciones)) {
            doc.setObservaciones(normalizarOpcional(observaciones));
        }

        auditoria.registrar("EVALUAR_DOCUMENTO", "Documento", doc.getId(),
                "Documento evaluado con estado " + nuevoEstado + " en solicitud " + doc.getSolicitud().getId());
        return mapper.toResponse(doc);
    }

    private Documento buscarDocumento(Long id) {
        return documentos.findWithRelacionesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado con id " + id));
    }

    private Solicitud buscarSolicitud(Long solicitudId) {
        return solicitudes.findWithRelacionesById(solicitudId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con id " + solicitudId));
    }

    private void verificarAccesoSolicitud(Solicitud s) {
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
            throw new AccessDeniedException(
                    "No tiene permisos para acceder o modificar documentos de expedientes ajenos.");
        }
    }

    private static String normalizarOpcional(String valor) {
        if (!StringUtils.hasText(valor)) {
            return null;
        }
        return valor.trim().replaceAll("\\s+", " ");
    }
}
