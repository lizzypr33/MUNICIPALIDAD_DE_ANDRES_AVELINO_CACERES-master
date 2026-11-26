package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "requisitos", uniqueConstraints = @UniqueConstraint(name = "uk_requisitos_codigo", columnNames = "codigo"))
@Getter
@Setter
@NoArgsConstructor
public class Requisito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private boolean activo = true;
}
