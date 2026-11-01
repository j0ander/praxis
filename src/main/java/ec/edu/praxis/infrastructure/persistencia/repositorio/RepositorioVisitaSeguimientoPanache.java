package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.VisitaSeguimiento;
import ec.edu.praxis.domain.repositorio.RepositorioVisitaSeguimiento;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/** Adaptador Panache para visita_seguimiento; el dominio solo conoce el contrato. */
@ApplicationScoped
public class RepositorioVisitaSeguimientoPanache implements RepositorioVisitaSeguimiento, PanacheRepositoryBase<VisitaSeguimiento, Long> {
    @Override public Optional<VisitaSeguimiento> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(VisitaSeguimiento entidad) { persist(entidad); }
    @Override public void eliminar(VisitaSeguimiento entidad) { delete(entidad); }
}
