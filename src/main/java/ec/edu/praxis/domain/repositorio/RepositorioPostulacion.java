package ec.edu.praxis.domain.repositorio;

import ec.edu.praxis.domain.modelo.Postulacion;
import java.util.List;
import java.util.Optional;

public interface RepositorioPostulacion extends RepositorioBase<Postulacion, Long> {
    List<Postulacion> listarPorEstudiante(Long estudianteId);
    List<Postulacion> listarPorEstado(String estado);
    Optional<Postulacion> buscarActivaPorPerfilYEstudiante(Long perfilId, Long estudianteId);
    long contarPorPerfilYEstado(Long perfilId, String estado);
}
