package ec.edu.praxis.domain.modelo;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Modelo persistente correspondiente a la tabla auditoria.
 * Las claves foráneas se conservan como identificadores escalares para
 * evitar acoplar el dominio a consultas o cargas implícitas de Hibernate.
 */
@Entity
@Table(name = "auditoria")
public class Auditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "usuario_id")
    public Long usuario_id;

    @Column(name = "accion")
    public String accion;

    @Column(name = "entidad")
    public String entidad;

    @Column(name = "entidad_id")
    public Long entidad_id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detalle", columnDefinition = "jsonb")
    public Map<String, Object> detalle;

    @Column(name = "fecha", nullable = false)
    public OffsetDateTime fecha;

    @PrePersist
    protected void antesDePersistir() {
        if (fecha == null) {
            fecha = OffsetDateTime.now();
        }
    }
}
