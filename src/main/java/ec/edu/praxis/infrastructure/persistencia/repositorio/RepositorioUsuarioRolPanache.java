package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.UsuarioRol;
import ec.edu.praxis.domain.modelo.UsuarioRolId;
import ec.edu.praxis.domain.repositorio.RepositorioUsuarioRol;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RepositorioUsuarioRolPanache implements RepositorioUsuarioRol, PanacheRepositoryBase<UsuarioRol, UsuarioRolId> {
    @Override public Optional<UsuarioRol> buscarPorId(UsuarioRolId id) { return findByIdOptional(id); }
    @Override public void guardar(UsuarioRol entidad) { persist(entidad); }
    @Override public void eliminar(UsuarioRol entidad) { delete(entidad); }
    @Override public List<UsuarioRol> listarPorUsuario(Long usuarioId) { return find("usuario_id", usuarioId).list(); }
    @Override public List<UsuarioRol> listarPorRol(Long rolId) { return find("rol_id", rolId).list(); }
    @Override public void eliminarPorUsuario(Long usuarioId) { delete("usuario_id", usuarioId); }
}
