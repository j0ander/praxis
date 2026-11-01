package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.Usuario;
import ec.edu.praxis.domain.repositorio.RepositorioUsuario;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RepositorioUsuarioPanache implements RepositorioUsuario, PanacheRepositoryBase<Usuario, Long> {
    @Override public Optional<Usuario> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(Usuario entidad) { persist(entidad); }
    @Override public void eliminar(Usuario entidad) { delete(entidad); }
    @Override public Optional<Usuario> buscarPorCorreo(String correo) { return find("correo", correo).firstResultOptional(); }
    @Override public boolean existeCorreo(String correo) { return count("correo", correo) > 0; }
    @Override public List<Usuario> listarTodos() { return findAll(io.quarkus.panache.common.Sort.by("id")).list(); }
}
