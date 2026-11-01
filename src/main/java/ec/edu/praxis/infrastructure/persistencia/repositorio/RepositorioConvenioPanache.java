package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.Convenio;
import ec.edu.praxis.domain.repositorio.RepositorioConvenio;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/** Adaptador Panache para convenio; el dominio solo conoce el contrato. */
@ApplicationScoped
public class RepositorioConvenioPanache implements RepositorioConvenio, PanacheRepositoryBase<Convenio, Long> {
    @Override public Optional<Convenio> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(Convenio entidad) { persist(entidad); }
    @Override public void eliminar(Convenio entidad) { delete(entidad); }
}
