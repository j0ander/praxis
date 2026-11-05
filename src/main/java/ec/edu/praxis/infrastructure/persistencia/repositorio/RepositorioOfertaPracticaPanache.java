package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.OfertaPractica;
import ec.edu.praxis.domain.repositorio.RepositorioOfertaPractica;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.List;
import io.quarkus.panache.common.Sort;

/** Adaptador Panache para oferta_practica; el dominio solo conoce el contrato. */
@ApplicationScoped
public class RepositorioOfertaPracticaPanache implements RepositorioOfertaPractica, PanacheRepositoryBase<OfertaPractica, Long> {
    @Override public Optional<OfertaPractica> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(OfertaPractica entidad) { persist(entidad); }
    @Override public void eliminar(OfertaPractica entidad) { delete(entidad); }
    @Override public List<OfertaPractica> listarTodas() { return findAll(Sort.descending("created_at")).list(); }
}
