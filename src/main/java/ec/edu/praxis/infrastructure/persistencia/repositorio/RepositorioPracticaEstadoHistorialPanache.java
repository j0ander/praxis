package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.PracticaEstadoHistorial;
import ec.edu.praxis.domain.repositorio.RepositorioPracticaEstadoHistorial;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/** Adaptador Panache para practica_estado_historial; el dominio solo conoce el contrato. */
@ApplicationScoped
public class RepositorioPracticaEstadoHistorialPanache implements RepositorioPracticaEstadoHistorial, PanacheRepositoryBase<PracticaEstadoHistorial, Long> {
    @Override public Optional<PracticaEstadoHistorial> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(PracticaEstadoHistorial entidad) { persist(entidad); }
    @Override public void eliminar(PracticaEstadoHistorial entidad) { delete(entidad); }
}
