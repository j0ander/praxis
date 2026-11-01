package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.UsuarioCarrera;
import ec.edu.praxis.domain.modelo.UsuarioCarreraId;
import ec.edu.praxis.domain.repositorio.RepositorioUsuarioCarrera;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RepositorioUsuarioCarreraPanache implements RepositorioUsuarioCarrera, PanacheRepositoryBase<UsuarioCarrera, UsuarioCarreraId> {
    @Override public Optional<UsuarioCarrera> buscarPorId(UsuarioCarreraId id) { return findByIdOptional(id); }
    @Override public void guardar(UsuarioCarrera entidad) { persist(entidad); }
    @Override public void eliminar(UsuarioCarrera entidad) { delete(entidad); }
    @Override public List<UsuarioCarrera> listarPorUsuario(Long usuarioId) { return find("usuario_id", usuarioId).list(); }
    @Override public void eliminarPorUsuario(Long usuarioId) { delete("usuario_id", usuarioId); }
}
