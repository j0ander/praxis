package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.InformeArchivo;
import ec.edu.praxis.domain.repositorio.RepositorioInformeArchivo;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class RepositorioInformeArchivoPanache implements RepositorioInformeArchivo, PanacheRepositoryBase<InformeArchivo, Long> {
    @Override public Optional<InformeArchivo> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(InformeArchivo entidad) { persist(entidad); }
    @Override public void eliminar(InformeArchivo entidad) { delete(entidad); }
}
