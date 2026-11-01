package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.RegistroHoras;
import ec.edu.praxis.domain.repositorio.RepositorioRegistroHoras;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/** Adaptador Panache para registro_horas; el dominio solo conoce el contrato. */
@ApplicationScoped
public class RepositorioRegistroHorasPanache implements RepositorioRegistroHoras, PanacheRepositoryBase<RegistroHoras, Long> {
    @Override public Optional<RegistroHoras> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(RegistroHoras entidad) { persist(entidad); }
    @Override public void eliminar(RegistroHoras entidad) { delete(entidad); }
}
