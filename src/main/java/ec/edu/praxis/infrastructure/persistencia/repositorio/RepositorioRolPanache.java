package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.Rol;
import ec.edu.praxis.domain.repositorio.RepositorioRol;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RepositorioRolPanache implements RepositorioRol, PanacheRepositoryBase<Rol, Long> {
    @Override public Optional<Rol> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(Rol entidad) { persist(entidad); }
    @Override public void eliminar(Rol entidad) { delete(entidad); }
    @Override public Optional<Rol> buscarPorCodigo(String codigo) { return find("codigo", codigo).firstResultOptional(); }
    @Override public List<Rol> listarTodos() { return findAll(Sort.by("id")).list(); }
}
