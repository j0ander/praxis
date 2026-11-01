package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.Incidencia;
import ec.edu.praxis.domain.repositorio.RepositorioIncidencia;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/** Adaptador Panache para incidencia; el dominio solo conoce el contrato. */
@ApplicationScoped
public class RepositorioIncidenciaPanache implements RepositorioIncidencia, PanacheRepositoryBase<Incidencia, Long> {
    @Override public Optional<Incidencia> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(Incidencia entidad) { persist(entidad); }
    @Override public void eliminar(Incidencia entidad) { delete(entidad); }
}
