package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.TipoPractica;
import ec.edu.praxis.domain.repositorio.RepositorioTipoPractica;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/** Adaptador Panache para tipo_practica; el dominio solo conoce el contrato. */
@ApplicationScoped
public class RepositorioTipoPracticaPanache implements RepositorioTipoPractica, PanacheRepositoryBase<TipoPractica, Long> {
    @Override public Optional<TipoPractica> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(TipoPractica entidad) { persist(entidad); }
    @Override public void eliminar(TipoPractica entidad) { delete(entidad); }
}
