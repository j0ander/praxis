package ec.edu.praxis.domain.modelo;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/** Modelo persistente correspondiente a la tabla requisito_estudiante.
 * Las claves foráneas se conservan como identificadores escalares para
 * evitar acoplar el dominio a consultas o cargas implícitas de Hibernate.
 */
@Entity
@Table(name = "requisito_estudiante")
public class RequisitoEstudiante {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "estudiante_id")
    public Long estudiante_id;

    @Column(name = "requisito_id")
    public Long requisito_id;

    @Column(name = "periodo_id")
    public Long periodo_id;

    @Column(name = "estado")
    public String estado;

    @Column(name = "fecha_verificacion")
    public OffsetDateTime fecha_verificacion;

    @Column(name = "verificado_por")
    public Long verificado_por;

    @Column(name = "observacion")
    public String observacion;

    @Column(name = "created_at")
    public OffsetDateTime created_at;

    @Column(name = "updated_at")
    public OffsetDateTime updated_at;

}
