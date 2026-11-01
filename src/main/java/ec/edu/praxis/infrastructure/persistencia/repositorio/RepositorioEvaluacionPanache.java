package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.Evaluacion;
import ec.edu.praxis.domain.repositorio.RepositorioEvaluacion;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/** Adaptador Panache para evaluacion; el dominio solo conoce el contrato. */
@ApplicationScoped
public class RepositorioEvaluacionPanache implements RepositorioEvaluacion, PanacheRepositoryBase<Evaluacion, Long> {
    @Override public Optional<Evaluacion> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(Evaluacion entidad) { persist(entidad); }
    @Override public void eliminar(Evaluacion entidad) { delete(entidad); }
}
