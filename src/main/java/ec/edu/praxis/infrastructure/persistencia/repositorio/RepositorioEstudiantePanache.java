package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.Estudiante;
import ec.edu.praxis.domain.repositorio.RepositorioEstudiante;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/** Adaptador Panache para estudiante; el dominio solo conoce el contrato. */
@ApplicationScoped
public class RepositorioEstudiantePanache implements RepositorioEstudiante, PanacheRepositoryBase<Estudiante, Long> {
    @Override public Optional<Estudiante> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(Estudiante entidad) { persist(entidad); }
    @Override public void eliminar(Estudiante entidad) { delete(entidad); }
}
