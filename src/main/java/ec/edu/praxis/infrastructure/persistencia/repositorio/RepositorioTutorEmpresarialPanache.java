package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.TutorEmpresarial;
import ec.edu.praxis.domain.repositorio.RepositorioTutorEmpresarial;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/** Adaptador Panache para tutor_empresarial; el dominio solo conoce el contrato. */
@ApplicationScoped
public class RepositorioTutorEmpresarialPanache implements RepositorioTutorEmpresarial, PanacheRepositoryBase<TutorEmpresarial, Long> {
    @Override public Optional<TutorEmpresarial> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(TutorEmpresarial entidad) { persist(entidad); }
    @Override public void eliminar(TutorEmpresarial entidad) { delete(entidad); }
}
