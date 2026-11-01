package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.PeriodoAcademico;
import ec.edu.praxis.domain.repositorio.RepositorioPeriodoAcademico;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RepositorioPeriodoAcademicoPanache implements RepositorioPeriodoAcademico, PanacheRepositoryBase<PeriodoAcademico, Long> {
    @Override public Optional<PeriodoAcademico> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(PeriodoAcademico entidad) { persist(entidad); }
    @Override public void eliminar(PeriodoAcademico entidad) { delete(entidad); }
    @Override public List<PeriodoAcademico> listarTodos() { return findAll(Sort.by("fecha_inicio").descending()).list(); }
    @Override public Optional<PeriodoAcademico> buscarActivo() { return find("activo", true).firstResultOptional(); }
    @Override public void desactivarTodos() { update("activo = false where activo = true"); }
}
