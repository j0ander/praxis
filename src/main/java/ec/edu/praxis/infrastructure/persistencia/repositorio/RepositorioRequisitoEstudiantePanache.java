package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.RequisitoEstudiante;
import ec.edu.praxis.domain.repositorio.RepositorioRequisitoEstudiante;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/** Adaptador Panache para requisito_estudiante; el dominio solo conoce el contrato. */
@ApplicationScoped
public class RepositorioRequisitoEstudiantePanache implements RepositorioRequisitoEstudiante, PanacheRepositoryBase<RequisitoEstudiante, Long> {
    @Override public Optional<RequisitoEstudiante> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(RequisitoEstudiante entidad) { persist(entidad); }
    @Override public void eliminar(RequisitoEstudiante entidad) { delete(entidad); }
}
