package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoNotificacion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notificaciones")
@Getter
@Setter
@NoArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoNotificacion tipo = TipoNotificacion.INFORMATIVA;

    @Column(nullable = false)
    private boolean leida = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaLectura;
}
