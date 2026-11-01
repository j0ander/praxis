package ec.edu.praxis.domain.modelo;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/** Modelo persistente correspondiente a la tabla incidencia.
 * Las claves foráneas se conservan como identificadores escalares para
 * evitar acoplar el dominio a consultas o cargas implícitas de Hibernate.
 */
@Entity
@Table(name = "incidencia")
public class Incidencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "practica_id")
    public Long practica_id;

    @Column(name = "reportada_por")
    public Long reportada_por;

    @Column(name = "descripcion")
    public String descripcion;

    @Column(name = "estado")
    public String estado;

    @Column(name = "resuelta_por")
    public Long resuelta_por;

    @Column(name = "fecha_resolucion")
    public OffsetDateTime fecha_resolucion;

    @Column(name = "created_at")
    public OffsetDateTime created_at;

    @Column(name = "updated_at")
    public OffsetDateTime updated_at;

}
