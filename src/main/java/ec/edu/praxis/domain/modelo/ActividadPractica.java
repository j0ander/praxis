package ec.edu.praxis.domain.modelo;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Modelo persistente correspondiente a la tabla actividad_practica.
 * Las claves foráneas se conservan como identificadores escalares para
 * evitar acoplar el dominio a consultas o cargas implícitas de Hibernate.
 */
@Entity
@Table(name = "actividad_practica")
public class ActividadPractica {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "practica_id")
    public Long practica_id;

    @Column(name = "fecha")
    public LocalDate fecha;

    @Column(name = "descripcion")
    public String descripcion;

    @Column(name = "registrada_por")
    public Long registrada_por;

    @Column(name = "created_at")
    public OffsetDateTime created_at;

    @Column(name = "updated_at")
    public OffsetDateTime updated_at;

}
