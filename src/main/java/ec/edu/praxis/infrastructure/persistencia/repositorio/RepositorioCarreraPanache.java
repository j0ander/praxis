package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.Carrera;
import ec.edu.praxis.domain.repositorio.RepositorioCarrera;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RepositorioCarreraPanache implements RepositorioCarrera, PanacheRepositoryBase<Carrera, Long> {
    @Override public Optional<Carrera> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(Carrera entidad) { persist(entidad); }
    @Override public void eliminar(Carrera entidad) { delete(entidad); }
    @Override public List<Carrera> listarTodos() { return findAll(Sort.by("nombre")).list(); }
}
