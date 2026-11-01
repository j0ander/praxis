package ec.edu.praxis.domain.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Modelo persistente correspondiente a la tabla evaluacion.
 * Las claves foráneas se conservan como identificadores escalares para
 * evitar acoplar el dominio a consultas o cargas implícitas de Hibernate.
 */
@Entity
@Table(name = "evaluacion")
public class Evaluacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "practica_id")
    public Long practica_id;

    @Column(name = "evaluador_usuario_id")
    public Long evaluador_usuario_id;

    @Column(name = "rol_evaluador")
    public String rol_evaluador;

    @Column(name = "criterios", columnDefinition = "jsonb")
    public String criterios;

    @Column(name = "nota", precision = 4, scale = 2)
    public BigDecimal nota;

    @Column(name = "observacion")
    public String observacion;

    @Column(name = "fecha_evaluacion")
    public OffsetDateTime fecha_evaluacion;

    @Column(name = "created_at")
    public OffsetDateTime created_at;

    @Column(name = "updated_at")
    public OffsetDateTime updated_at;

}
