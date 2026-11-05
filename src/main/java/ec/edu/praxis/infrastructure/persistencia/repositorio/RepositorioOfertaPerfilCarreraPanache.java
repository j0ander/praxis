package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.OfertaPerfilCarreraId;

import ec.edu.praxis.domain.modelo.OfertaPerfilCarrera;
import ec.edu.praxis.domain.repositorio.RepositorioOfertaPerfilCarrera;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/** Adaptador Panache para oferta_perfil_carrera; el dominio solo conoce el contrato. */
@ApplicationScoped
public class RepositorioOfertaPerfilCarreraPanache implements RepositorioOfertaPerfilCarrera, PanacheRepositoryBase<OfertaPerfilCarrera, OfertaPerfilCarreraId> {
    @Override public Optional<OfertaPerfilCarrera> buscarPorId(OfertaPerfilCarreraId id) { return findByIdOptional(id); }
    @Override public void guardar(OfertaPerfilCarrera entidad) { persist(entidad); }
    @Override public void eliminar(OfertaPerfilCarrera entidad) { delete(entidad); }
    @Override public java.util.List<OfertaPerfilCarrera> listarPorPerfil(Long perfilId) { return find("perfil_id", perfilId).list(); }
}
