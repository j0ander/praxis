package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.ActividadPractica;
import ec.edu.praxis.domain.repositorio.RepositorioActividadPractica;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/** Adaptador Panache para actividad_practica; el dominio solo conoce el contrato. */
@ApplicationScoped
public class RepositorioActividadPracticaPanache implements RepositorioActividadPractica, PanacheRepositoryBase<ActividadPractica, Long> {
    @Override public Optional<ActividadPractica> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(ActividadPractica entidad) { persist(entidad); }
    @Override public void eliminar(ActividadPractica entidad) { delete(entidad); }
}
