package ec.edu.praxis.domain.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Modelo persistente correspondiente a la tabla tipo_practica.
 * Las claves foráneas se conservan como identificadores escalares para
 * evitar acoplar el dominio a consultas o cargas implícitas de Hibernate.
 */
@Entity
@Table(name = "tipo_practica")
public class TipoPractica {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "nombre")
    public String nombre;

    @Column(name = "horas_requeridas", precision = 6, scale = 2)
    public BigDecimal horas_requeridas;

    @Column(name = "nota_minima", precision = 4, scale = 2)
    public BigDecimal nota_minima;

    @Column(name = "activo")
    public Boolean activo;

    @Column(name = "created_at")
    public OffsetDateTime created_at;

    @Column(name = "updated_at")
    public OffsetDateTime updated_at;

}
