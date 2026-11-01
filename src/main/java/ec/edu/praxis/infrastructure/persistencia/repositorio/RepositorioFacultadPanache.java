package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.Facultad;
import ec.edu.praxis.domain.repositorio.RepositorioFacultad;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RepositorioFacultadPanache implements RepositorioFacultad, PanacheRepositoryBase<Facultad, Long> {
    @Override public Optional<Facultad> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(Facultad entidad) { persist(entidad); }
    @Override public void eliminar(Facultad entidad) { delete(entidad); }
    @Override public List<Facultad> listarTodos() { return findAll(Sort.by("nombre")).list(); }
}
