package ec.edu.praxis.domain.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Modelo persistente correspondiente a la tabla registro_horas.
 * Las claves foráneas se conservan como identificadores escalares para
 * evitar acoplar el dominio a consultas o cargas implícitas de Hibernate.
 */
@Entity
@Table(name = "registro_horas")
public class RegistroHoras {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "practica_id")
    public Long practica_id;

    @Column(name = "fecha")
    public LocalDate fecha;

    @Column(name = "horas", precision = 4, scale = 2)
    public BigDecimal horas;

    @Column(name = "descripcion")
    public String descripcion;

    @Column(name = "validado")
    public Boolean validado;

    @Column(name = "validado_por")
    public Long validado_por;

    @Column(name = "fecha_validacion")
    public OffsetDateTime fecha_validacion;

    @Column(name = "version")
    public Long version;

    @Column(name = "created_at")
    public OffsetDateTime created_at;

    @Column(name = "updated_at")
    public OffsetDateTime updated_at;

}
