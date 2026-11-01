package ec.edu.praxis.infrastructure.persistencia.repositorio;

import ec.edu.praxis.domain.modelo.Certificado;
import ec.edu.praxis.domain.repositorio.RepositorioCertificado;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/** Adaptador Panache para certificado; el dominio solo conoce el contrato. */
@ApplicationScoped
public class RepositorioCertificadoPanache implements RepositorioCertificado, PanacheRepositoryBase<Certificado, Long> {
    @Override public Optional<Certificado> buscarPorId(Long id) { return findByIdOptional(id); }
    @Override public void guardar(Certificado entidad) { persist(entidad); }
    @Override public void eliminar(Certificado entidad) { delete(entidad); }
}
