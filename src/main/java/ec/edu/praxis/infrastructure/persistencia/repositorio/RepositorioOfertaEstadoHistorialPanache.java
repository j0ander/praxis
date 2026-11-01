package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.OfertaEstadoHistorial;
import ec.edu.praxis.domain.repositorio.RepositorioOfertaEstadoHistorial;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/** Adaptador Panache para oferta_estado_historial; el dominio solo conoce el contrato. */
@ApplicationScoped
public class RepositorioOfertaEstadoHistorialPanache implements RepositorioOfertaEstadoHistorial, PanacheRepositoryBase<OfertaEstadoHistorial, Long> {
    @Override public Optional<OfertaEstadoHistorial> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(OfertaEstadoHistorial entidad) { persist(entidad); }
    @Override public void eliminar(OfertaEstadoHistorial entidad) { delete(entidad); }
}
