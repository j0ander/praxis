package ec.edu.praxis.domain.modelo;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/** Modelo persistente correspondiente a la tabla informe.
 * Las claves foráneas se conservan como identificadores escalares para
 * evitar acoplar el dominio a consultas o cargas implícitas de Hibernate.
 */
@Entity
@Table(name = "informe")
public class Informe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "practica_id")
    public Long practica_id;

    @Column(name = "tipo")
    public String tipo;

    @Column(name = "estado")
    public String estado;

    @Column(name = "nombre_archivo")
    public String nombre_archivo;

    @Column(name = "nombre_interno")
    public String nombre_interno;

    @Column(name = "content_type")
    public String content_type;

    @Column(name = "subido_por")
    public Long subido_por;

    @Column(name = "fecha_subida")
    public OffsetDateTime fecha_subida;

    @Column(name = "observaciones")
    public String observaciones;

    @Column(name = "version")
    public Long version;

    @Column(name = "created_at")
    public OffsetDateTime created_at;

    @Column(name = "updated_at")
    public OffsetDateTime updated_at;

}
