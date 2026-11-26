package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.UpdateTimestamp;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoSolicitud;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "solicitudes", uniqueConstraints = @UniqueConstraint(name = "uk_solicitudes_expediente", columnNames = "numero_expediente"))
@Getter
@Setter
@NoArgsConstructor
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Se asigna al presentar la solicitud (null mientras permanece en BORRADOR). */
    @Column(length = 30)
    private String numeroExpediente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ciudadano_id", nullable = false)
    private Ciudadano ciudadano;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "establecimiento_id", nullable = false)
    private Establecimiento establecimiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_tramite_id", nullable = false)
    private TipoTramite tipoTramite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "representante_id")
    private Representante representante;

    /** Se completa al presentar la solicitud (null mientras es BORRADOR). */
    private LocalDateTime fechaPresentacion;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoSolicitud estado = EstadoSolicitud.BORRADOR;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(columnDefinition = "TEXT")
    private String motivoRechazo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_responsable_id")
    private Usuario usuarioResponsable;

    /** Fotografía de IDs de requisitos obligatorios vigentes al momento de presentar la solicitud. */
    @Column(name = "requisitos_snapshot", columnDefinition = "TEXT")
    private String requisitosSnapshot;
}
