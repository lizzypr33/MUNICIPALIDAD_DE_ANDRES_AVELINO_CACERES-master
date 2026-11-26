package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoDocumento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Metadatos del archivo; el contenido se guardará en disco (prompt posterior). */
@Entity
@Table(name = "documentos")
@Getter
@Setter
@NoArgsConstructor
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private Solicitud solicitud;

    /** Null para documentos adicionales que no corresponden a un requisito. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requisito_id")
    private Requisito requisito;

    @Column(nullable = false, length = 255)
    private String nombreOriginal;

    /** Nombre generado por el servidor, nunca elegido por el cliente. */
    @Column(nullable = false, length = 255)
    private String nombreAlmacenado;

    @Column(nullable = false, length = 500)
    private String rutaAlmacenamiento;

    @Column(nullable = false, length = 100)
    private String tipoMime;

    @Column(nullable = false)
    private Long tamanioBytes;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaSubida;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoDocumento estado = EstadoDocumento.PENDIENTE;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(nullable = false)
    private boolean activo = true;
}
