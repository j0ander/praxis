package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.PostulacionRecomendacionHistorial;
import ec.edu.praxis.domain.repositorio.RepositorioPostulacionRecomendacionHistorial;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/** Adaptador Panache para postulacion_recomendacion_historial; el dominio solo conoce el contrato. */
@ApplicationScoped
public class RepositorioPostulacionRecomendacionHistorialPanache implements RepositorioPostulacionRecomendacionHistorial, PanacheRepositoryBase<PostulacionRecomendacionHistorial, Long> {
    @Override public Optional<PostulacionRecomendacionHistorial> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(PostulacionRecomendacionHistorial entidad) { persist(entidad); }
    @Override public void eliminar(PostulacionRecomendacionHistorial entidad) { delete(entidad); }
}
