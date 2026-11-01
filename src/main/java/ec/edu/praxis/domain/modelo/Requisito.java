package ec.edu.praxis.domain.modelo;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/** Modelo persistente correspondiente a la tabla requisito.
 * Las claves foráneas se conservan como identificadores escalares para
 * evitar acoplar el dominio a consultas o cargas implícitas de Hibernate.
 */
@Entity
@Table(name = "requisito")
public class Requisito {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "nombre")
    public String nombre;

    @Column(name = "descripcion")
    public String descripcion;

    @Column(name = "obligatorio")
    public Boolean obligatorio;

    @Column(name = "carrera_id")
    public Long carrera_id;

    @Column(name = "activo")
    public Boolean activo;

    @Column(name = "created_at")
    public OffsetDateTime created_at;

    @Column(name = "updated_at")
    public OffsetDateTime updated_at;

}
