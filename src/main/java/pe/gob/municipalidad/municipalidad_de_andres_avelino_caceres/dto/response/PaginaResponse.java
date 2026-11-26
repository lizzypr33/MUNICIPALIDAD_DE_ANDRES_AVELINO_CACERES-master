package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.dto.response;

import java.util.List;

import org.springframework.data.domain.Page;

/** Envoltorio estable de paginación; evita serializar PageImpl directamente. */
public record PaginaResponse<T>(
        List<T> contenido,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas) {

    public static <T> PaginaResponse<T> from(Page<T> page) {
        return new PaginaResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
