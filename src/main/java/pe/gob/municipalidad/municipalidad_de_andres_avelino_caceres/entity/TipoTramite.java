package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tipos_tramite", uniqueConstraints = @UniqueConstraint(name = "uk_tipos_tramite_codigo", columnNames = "codigo"))
@Getter
@Setter
@NoArgsConstructor
public class TipoTramite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private boolean requiereInspeccion = true;

    @Column(nullable = false)
    private boolean activo = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;
}
