package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.Convenio;
import ec.edu.praxis.domain.repositorio.RepositorioConvenio;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RepositorioConvenioPanache implements RepositorioConvenio, PanacheRepositoryBase<Convenio, Long> {
    @Override public Optional<Convenio> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(Convenio entidad) { persist(entidad); }
    @Override public void eliminar(Convenio entidad) { delete(entidad); }
    @Override public List<Convenio> listarTodos() { return findAll(Sort.descending("fecha_inicio")).list(); }
    @Override public boolean existeNumero(String numero) { return count("numero", numero) > 0; }
}
