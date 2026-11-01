package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.Docente;
import ec.edu.praxis.domain.repositorio.RepositorioDocente;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/** Adaptador Panache para docente; el dominio solo conoce el contrato. */
@ApplicationScoped
public class RepositorioDocentePanache implements RepositorioDocente, PanacheRepositoryBase<Docente, Long> {
    @Override public Optional<Docente> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(Docente entidad) { persist(entidad); }
    @Override public void eliminar(Docente entidad) { delete(entidad); }
}
