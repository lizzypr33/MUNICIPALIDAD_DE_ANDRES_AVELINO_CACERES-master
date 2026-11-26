package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.EstadoInspeccion;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.ResultadoInspeccion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inspecciones")
@Getter
@Setter
@NoArgsConstructor
public class Inspeccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private Solicitud solicitud;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspector_id")
    private Usuario inspector;

    private LocalDateTime fechaProgramada;

    private LocalDateTime fechaRealizada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoInspeccion estado = EstadoInspeccion.PROGRAMADA;

    /** Null hasta que la inspección se realiza. */
    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private ResultadoInspeccion resultado;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(columnDefinition = "TEXT")
    private String recomendaciones;

    @Column(name = "motivo_reprogramacion", columnDefinition = "TEXT")
    private String motivoReprogramacion;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;
}
