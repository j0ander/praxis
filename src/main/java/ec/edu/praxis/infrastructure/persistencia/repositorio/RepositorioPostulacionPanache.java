package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.Postulacion;
import ec.edu.praxis.domain.repositorio.RepositorioPostulacion;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

/** Adaptador Panache para postulacion; el dominio solo conoce el contrato. */
@ApplicationScoped
public class RepositorioPostulacionPanache implements RepositorioPostulacion, PanacheRepositoryBase<Postulacion, Long> {
    @Override public Optional<Postulacion> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(Postulacion entidad) { persist(entidad); }
    @Override public void eliminar(Postulacion entidad) { delete(entidad); }
    @Override public List<Postulacion> listarPorEstado(String estado) {
        return find("estado", Sort.descending("fecha_postulacion"), estado).list();
    }
    @Override public List<Postulacion> listarPorEstudiante(Long estudianteId) {
        return find("estudiante_id", Sort.descending("fecha_postulacion"), estudianteId).list();
    }
    @Override public Optional<Postulacion> buscarActivaPorPerfilYEstudiante(Long perfilId, Long estudianteId) {
        return find("perfil_id = ?1 and estudiante_id = ?2 and estado <> 'RETIRADA'", perfilId, estudianteId)
                .firstResultOptional();
    }
    @Override public long contarPorPerfilYEstado(Long perfilId, String estado) {
        return count("perfil_id = ?1 and estado = ?2", perfilId, estado);
    }
}
