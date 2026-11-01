package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.Practica;
import ec.edu.praxis.domain.repositorio.RepositorioPractica;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/** Adaptador Panache para practica; el dominio solo conoce el contrato. */
@ApplicationScoped
public class RepositorioPracticaPanache implements RepositorioPractica, PanacheRepositoryBase<Practica, Long> {
    @Override public Optional<Practica> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(Practica entidad) { persist(entidad); }
    @Override public void eliminar(Practica entidad) { delete(entidad); }
}
