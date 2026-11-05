package ec.edu.praxis.application.oferta;

import ec.edu.praxis.application.excepcion.NoEncontradoException;
import ec.edu.praxis.application.excepcion.ReglaNegocioException;
import ec.edu.praxis.domain.modelo.*;
import ec.edu.praxis.domain.repositorio.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.*;

/** Casos de uso para la revisión académica de ofertas enviadas por las empresas. */
@ApplicationScoped
public class RevisarOfertaPracticaService {

    @Inject RepositorioOfertaPractica ofertas;
    @Inject RepositorioOfertaPerfil perfiles;
    @Inject RepositorioOfertaPerfilRequisito requisitos;
    @Inject RepositorioEmpresa empresas;
    @Inject RepositorioConvenio convenios;
    @Inject RepositorioTipoPractica tipos;
    @Inject RepositorioPeriodoAcademico periodos;
    @Inject EntityManager entityManager;

    public List<Map<String, Object>> listarPendientes() {
        return ofertas.listarTodas().stream()
                .filter(o -> "PENDIENTE_REVISION".equals(o.estado))
                .map(this::resumen)
                .toList();
    }

    public Map<String, Object> detalle(long ofertaId) {
        OfertaPractica oferta = buscarPendiente(ofertaId);
        Map<String, Object> resultado = resumen(oferta);
        Empresa empresa = empresas.buscarPorId(oferta.empresa_id).orElse(null);
        Convenio convenio = convenios.buscarPorId(oferta.convenio_id).orElse(null);
        TipoPractica tipo = tipos.buscarPorId(oferta.tipo_practica_id).orElse(null);
        PeriodoAcademico periodo = periodos.buscarPorId(oferta.periodo_id).orElse(null);

        resultado.put("descripcion", oferta.descripcion);
        resultado.put("empresa_ruc", empresa == null ? null : empresa.ruc);
        resultado.put("empresa_nombre_comercial", empresa == null ? null : empresa.nombre_comercial);
        resultado.put("empresa_correo", empresa == null ? null : empresa.correo);
        resultado.put("empresa_telefono", empresa == null ? null : empresa.telefono);
        resultado.put("convenio_id", oferta.convenio_id);
        resultado.put("convenio_estado", convenio == null ? null : convenio.estado);
        resultado.put("convenio_fecha_inicio", convenio == null ? null : convenio.fecha_inicio);
        resultado.put("convenio_fecha_fin", convenio == null ? null : convenio.fecha_fin);
        resultado.put("tipo_practica_id", oferta.tipo_practica_id);
        resultado.put("tipo_horas_requeridas", tipo == null ? null : tipo.horas_requeridas);
        resultado.put("tipo_nota_minima", tipo == null ? null : tipo.nota_minima);
        resultado.put("horas_requeridas_snapshot", oferta.horas_requeridas_snapshot);
        resultado.put("nota_minima_snapshot", oferta.nota_minima_snapshot);
        resultado.put("periodo_id", oferta.periodo_id);
        resultado.put("periodo_fecha_inicio", periodo == null ? null : periodo.fecha_inicio);
        resultado.put("periodo_fecha_fin", periodo == null ? null : periodo.fecha_fin);
        resultado.put("fecha_inicio_actividades", oferta.fecha_inicio_actividades);
        resultado.put("fecha_fin_actividades", oferta.fecha_fin_actividades);
        resultado.put("fecha_publicacion", oferta.fecha_publicacion);
        resultado.put("perfiles", perfiles.listarPorOferta(oferta.id).stream().map(perfil -> {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("id", perfil.id);
            p.put("nombre", perfil.nombre);
            p.put("descripcion", perfil.descripcion);
            p.put("plazas", perfil.plazas);
            p.put("requisitos", requisitos.listarPorPerfil(perfil.id).stream().map(req -> {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("id", req.id);
                r.put("descripcion", req.descripcion);
                r.put("obligatorio", req.obligatorio);
                return r;
            }).toList());
            return p;
        }).toList());
        return resultado;
    }

    @Transactional
    public void aprobar(long ofertaId, long actor) {
        establecerActor(actor);
        OfertaPractica oferta = buscarPendiente(ofertaId);
        oferta.estado = "PUBLICADA";
        oferta.motivo_decision = null;
        oferta.updated_at = OffsetDateTime.now();
        ofertas.guardar(oferta);
        entityManager.flush();
    }

    @Transactional
    public void rechazar(long ofertaId, String motivo, long actor) {
        String motivoLimpio = motivo == null ? "" : motivo.trim();
        if (motivoLimpio.isEmpty()) {
            throw new ReglaNegocioException("Debe indicar el motivo del rechazo.");
        }
        if (motivoLimpio.length() > 2000) {
            throw new ReglaNegocioException("El motivo no puede superar los 2000 caracteres.");
        }
        establecerActor(actor);
        OfertaPractica oferta = buscarPendiente(ofertaId);
        oferta.estado = "RECHAZADA";
        oferta.motivo_decision = motivoLimpio;
        oferta.updated_at = OffsetDateTime.now();
        ofertas.guardar(oferta);
        entityManager.flush();
    }

    private OfertaPractica buscarPendiente(long ofertaId) {
        OfertaPractica oferta = ofertas.buscarPorId(ofertaId)
                .orElseThrow(() -> new NoEncontradoException("Oferta no encontrada."));
        if (!"PENDIENTE_REVISION".equals(oferta.estado)) {
            throw new ReglaNegocioException("La oferta ya no está pendiente de revisión.");
        }
        return oferta;
    }

    private Map<String, Object> resumen(OfertaPractica oferta) {
        Empresa empresa = empresas.buscarPorId(oferta.empresa_id).orElse(null);
        Convenio convenio = convenios.buscarPorId(oferta.convenio_id).orElse(null);
        TipoPractica tipo = tipos.buscarPorId(oferta.tipo_practica_id).orElse(null);
        PeriodoAcademico periodo = periodos.buscarPorId(oferta.periodo_id).orElse(null);
        Map<String, Object> fila = new LinkedHashMap<>();
        fila.put("id", oferta.id);
        fila.put("empresa", empresa == null ? "Empresa #" + oferta.empresa_id : empresa.razon_social);
        fila.put("titulo", oferta.titulo);
        fila.put("convenio", convenio == null ? "—" : convenio.numero);
        fila.put("tipo_practica", tipo == null ? "—" : tipo.nombre);
        fila.put("periodo", periodo == null ? "—" : periodo.nombre);
        fila.put("fecha_cierre", oferta.fecha_cierre);
        fila.put("fecha_inicio_actividades", oferta.fecha_inicio_actividades);
        fila.put("fecha_fin_actividades", oferta.fecha_fin_actividades);
        fila.put("estado", oferta.estado);
        fila.put("creada_por_usuario_id", oferta.creada_por_usuario_id);
        fila.put("created_at", oferta.created_at);
        fila.put("perfiles", perfiles.listarPorOferta(oferta.id).stream().map(perfil -> {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("id", perfil.id);
            p.put("nombre", perfil.nombre);
            p.put("plazas", perfil.plazas);
            return p;
        }).toList());
        return fila;
    }

    private void establecerActor(long actor) {
        entityManager.createNativeQuery("SELECT set_config('praxis.usuario_id', ?1, true)")
                .setParameter(1, Long.toString(actor))
                .getSingleResult();
    }
}
