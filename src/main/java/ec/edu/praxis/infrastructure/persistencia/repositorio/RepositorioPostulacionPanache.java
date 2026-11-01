package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.Postulacion;
import ec.edu.praxis.domain.repositorio.RepositorioPostulacion;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/** Adaptador Panache para postulacion; el dominio solo conoce el contrato. */
@ApplicationScoped
public class RepositorioPostulacionPanache implements RepositorioPostulacion, PanacheRepositoryBase<Postulacion, Long> {
    @Override public Optional<Postulacion> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(Postulacion entidad) { persist(entidad); }
    @Override public void eliminar(Postulacion entidad) { delete(entidad); }
}
