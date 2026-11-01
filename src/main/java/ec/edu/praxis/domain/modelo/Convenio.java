package ec.edu.praxis.domain.modelo;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Modelo persistente correspondiente a la tabla convenio.
 * Las claves foráneas se conservan como identificadores escalares para
 * evitar acoplar el dominio a consultas o cargas implícitas de Hibernate.
 */
@Entity
@Table(name = "convenio")
public class Convenio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "empresa_id")
    public Long empresa_id;

    @Column(name = "numero")
    public String numero;

    @Column(name = "fecha_inicio")
    public LocalDate fecha_inicio;

    @Column(name = "fecha_fin")
    public LocalDate fecha_fin;

    @Column(name = "estado")
    public String estado;

    @Column(name = "documento_url")
    public String documento_url;

    @Column(name = "created_at")
    public OffsetDateTime created_at;

    @Column(name = "updated_at")
    public OffsetDateTime updated_at;

}
