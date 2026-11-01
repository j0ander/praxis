package ec.edu.praxis.domain.modelo;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Modelo persistente correspondiente a la tabla certificado.
 * Las claves foráneas se conservan como identificadores escalares para
 * evitar acoplar el dominio a consultas o cargas implícitas de Hibernate.
 */
@Entity
@Table(name = "certificado")
public class Certificado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "practica_id")
    public Long practica_id;

    @Column(name = "codigo_verificacion")
    public UUID codigo_verificacion;

    @Column(name = "datos_snapshot", columnDefinition = "jsonb")
    public String datos_snapshot;

    @Column(name = "estado")
    public String estado;

    @Column(name = "fecha_emision")
    public OffsetDateTime fecha_emision;

    @Column(name = "emitido_por")
    public Long emitido_por;

    @Column(name = "motivo_anulacion")
    public String motivo_anulacion;

    @Column(name = "fecha_anulacion")
    public OffsetDateTime fecha_anulacion;

    @Column(name = "anulado_por")
    public Long anulado_por;

    @Column(name = "created_at")
    public OffsetDateTime created_at;

}
