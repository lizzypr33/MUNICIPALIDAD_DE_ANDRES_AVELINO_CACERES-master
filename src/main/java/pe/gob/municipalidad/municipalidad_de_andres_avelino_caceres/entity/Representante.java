package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.entity;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoDocumento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "representantes", uniqueConstraints = @UniqueConstraint(name = "uk_representantes_documento",
        columnNames = { "tipo_documento", "numero_documento" }))
@Getter
@Setter
@NoArgsConstructor
public class Representante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoDocumento tipoDocumento;

    @Column(nullable = false, length = 20)
    private String numeroDocumento;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(length = 100)
    private String cargo;

    @Column(length = 20)
    private String telefono;

    @Column(length = 150)
    private String email;

    @Column(nullable = false)
    private boolean activo = true;
}
