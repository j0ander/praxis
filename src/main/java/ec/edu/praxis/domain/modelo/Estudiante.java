package ec.edu.praxis.domain.modelo;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Modelo persistente correspondiente a la tabla estudiante.
 * Las claves foráneas se conservan como identificadores escalares para
 * evitar acoplar el dominio a consultas o cargas implícitas de Hibernate.
 */
@Entity
@Table(name = "estudiante")
public class Estudiante {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "usuario_id")
    public Long usuario_id;

    @Column(name = "carrera_id")
    public Long carrera_id;

    @Column(name = "codigo_estudiante")
    public String codigo_estudiante;

    @Column(name = "fecha_nacimiento")
    public LocalDate fecha_nacimiento;

    @Column(name = "created_at")
    public OffsetDateTime created_at;

    @Column(name = "updated_at")
    public OffsetDateTime updated_at;

}
