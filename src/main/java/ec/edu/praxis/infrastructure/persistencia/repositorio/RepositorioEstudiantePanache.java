package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.Estudiante;
import ec.edu.praxis.domain.repositorio.RepositorioEstudiante;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

/** Adaptador Panache para los perfiles académicos de estudiantes. */
@ApplicationScoped
public class RepositorioEstudiantePanache implements RepositorioEstudiante, PanacheRepositoryBase<Estudiante, Long> {
    @Override public Optional<Estudiante> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(Estudiante entidad) { persist(entidad); }
    @Override public void eliminar(Estudiante entidad) { delete(entidad); }
    @Override public List<Estudiante> listarTodos() { return findAll(Sort.by("id")).list(); }
    @Override public boolean existePorUsuario(Long usuarioId) { return count("usuario_id", usuarioId) > 0; }
    @Override public Optional<Estudiante> buscarPorUsuario(Long usuarioId) { return find("usuario_id", usuarioId).firstResultOptional(); }
}
