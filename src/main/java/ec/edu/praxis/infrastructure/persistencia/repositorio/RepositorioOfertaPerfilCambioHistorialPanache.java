package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.OfertaPerfilCambioHistorial;
import ec.edu.praxis.domain.repositorio.RepositorioOfertaPerfilCambioHistorial;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/** Adaptador Panache para oferta_perfil_cambio_historial; el dominio solo conoce el contrato. */
@ApplicationScoped
public class RepositorioOfertaPerfilCambioHistorialPanache implements RepositorioOfertaPerfilCambioHistorial, PanacheRepositoryBase<OfertaPerfilCambioHistorial, Long> {
    @Override public Optional<OfertaPerfilCambioHistorial> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(OfertaPerfilCambioHistorial entidad) { persist(entidad); }
    @Override public void eliminar(OfertaPerfilCambioHistorial entidad) { delete(entidad); }
}
