package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.Requisito;
import ec.edu.praxis.domain.repositorio.RepositorioRequisito;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/** Adaptador Panache para requisito; el dominio solo conoce el contrato. */
@ApplicationScoped
public class RepositorioRequisitoPanache implements RepositorioRequisito, PanacheRepositoryBase<Requisito, Long> {
    @Override public Optional<Requisito> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(Requisito entidad) { persist(entidad); }
    @Override public void eliminar(Requisito entidad) { delete(entidad); }
}
