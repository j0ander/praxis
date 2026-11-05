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
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Revisión académica de postulaciones y creación transaccional de prácticas. */
@ApplicationScoped
public class RevisarPostulacionService {

    private static final ZoneId ZONA_ECUADOR = ZoneId.of("America/Guayaquil");

    @Inject RepositorioPostulacion postulaciones;
    @Inject RepositorioEstudiante estudiantes;
    @Inject RepositorioUsuario usuarios;
    @Inject RepositorioCarrera carreras;
    @Inject RepositorioOfertaPerfil perfiles;
    @Inject RepositorioOfertaPractica ofertas;
    @Inject RepositorioEmpresa empresas;
    @Inject RepositorioConvenio convenios;
    @Inject RepositorioTipoPractica tipos;
    @Inject RepositorioPeriodoAcademico periodos;
    @Inject RepositorioOfertaPerfilRequisito requisitos;
    @Inject RepositorioPractica practicas;
    @Inject EntityManager entityManager;

    /** Lista únicamente postulaciones que todavía puede resolver coordinación. */
    public List<Map<String, Object>> listarPendientes() {
        return postulaciones.listarPorEstado("PENDIENTE").stream().map(this::resumen).toList();
    }

    /** Devuelve el expediente completo de una postulación pendiente. */
    public Map<String, Object> detalle(long postulacionId) {
        Postulacion postulacion = buscarPendiente(postulacionId);
        Map<String, Object> resultado = resumen(postulacion);

        Estudiante estudiante = estudiantes.buscarPorId(postulacion.estudiante_id).orElse(null);
        Usuario usuario = estudiante == null ? null : usuarios.buscarPorId(estudiante.usuario_id).orElse(null);
        Carrera carrera = estudiante == null || estudiante.carrera_id == null
                ? null : carreras.buscarPorId(estudiante.carrera_id).orElse(null);
        OfertaPerfil perfil = perfiles.buscarPorId(postulacion.perfil_id).orElse(null);
        OfertaPractica oferta = perfil == null ? null : ofertas.buscarPorId(perfil.oferta_id).orElse(null);
        Empresa empresa = oferta == null ? null : empresas.buscarPorId(oferta.empresa_id).orElse(null);
        Convenio convenio = oferta == null ? null : convenios.buscarPorId(oferta.convenio_id).orElse(null);
        TipoPractica tipo = oferta == null ? null : tipos.buscarPorId(oferta.tipo_practica_id).orElse(null);
        PeriodoAcademico periodo = oferta == null ? null : periodos.buscarPorId(oferta.periodo_id).orElse(null);

        resultado.put("estudiante_id", estudiante == null ? null : estudiante.id);
        resultado.put("estudiante_usuario_id", estudiante == null ? null : estudiante.usuario_id);
        resultado.put("estudiante", usuario == null ? "Estudiante #" + postulacion.estudiante_id
                : (usuario.nombres + " " + usuario.apellidos).trim());
        resultado.put("correo_estudiante", usuario == null ? null : usuario.correo);
        resultado.put("documento_estudiante", usuario == null ? null : usuario.numero_documento);
        resultado.put("telefono_estudiante", usuario == null ? null : usuario.telefono);
        resultado.put("codigo_estudiante", estudiante == null ? null : estudiante.codigo_estudiante);
        resultado.put("carrera_id", carrera == null ? null : carrera.id);
        resultado.put("carrera_estudiante", carrera == null ? "—" : carrera.nombre);

        resultado.put("oferta_id", oferta == null ? null : oferta.id);
        resultado.put("oferta", oferta == null ? "Oferta no disponible" : oferta.titulo);
        resultado.put("descripcion_oferta", oferta == null ? null : oferta.descripcion);
        resultado.put("empresa", empresa == null ? "—" : empresa.razon_social);
        resultado.put("empresa_nombre_comercial", empresa == null ? null : empresa.nombre_comercial);
        resultado.put("empresa_correo", empresa == null ? null : empresa.correo);
        resultado.put("perfil_id", perfil == null ? null : perfil.id);
        resultado.put("perfil", perfil == null ? "Perfil no disponible" : perfil.nombre);
        resultado.put("perfil_descripcion", perfil == null ? null : perfil.descripcion);
        resultado.put("plazas", perfil == null ? null : perfil.plazas);
        resultado.put("plazas_aceptadas", perfil == null ? 0 : postulaciones.contarPorPerfilYEstado(perfil.id, "ACEPTADA"));
        resultado.put("plazas_disponibles", perfil == null || perfil.plazas == null ? 0
                : Math.max(0, perfil.plazas - (int) postulaciones.contarPorPerfilYEstado(perfil.id, "ACEPTADA")));
        resultado.put("requisitos", perfil == null ? List.of() : requisitos.listarPorPerfil(perfil.id).stream().map(req -> {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("id", req.id);
            fila.put("descripcion", req.descripcion);
            fila.put("obligatorio", req.obligatorio);
            return fila;
        }).toList());
        resultado.put("convenio", convenio == null ? null : convenio.numero);
        resultado.put("convenio_estado", convenio == null ? null : convenio.estado);
        resultado.put("tipo_practica", tipo == null ? null : tipo.nombre);
        resultado.put("periodo", periodo == null ? null : periodo.nombre);
        resultado.put("fecha_inicio_actividades", oferta == null ? null : oferta.fecha_inicio_actividades);
        resultado.put("fecha_fin_actividades", oferta == null ? null : oferta.fecha_fin_actividades);
        resultado.put("horas_requeridas", oferta == null ? null : oferta.horas_requeridas_snapshot);
        resultado.put("nota_minima", oferta == null ? null : oferta.nota_minima_snapshot);
        resultado.put("recomendacion_empresa", postulacion.recomendacion_empresa);
        resultado.put("observacion_empresa", postulacion.observacion_empresa);
        resultado.put("mensaje", postulacion.mensaje);
        return resultado;
    }

    /** Acepta y crea la práctica vinculada dentro de la misma transacción. */
    @Transactional
    public Map<String, Object> aceptar(long postulacionId, long actor) {
        establecerActor(actor);
        Postulacion postulacion = buscarPendiente(postulacionId);
        OfertaPerfil perfil = perfiles.buscarPorId(postulacion.perfil_id)
                .orElseThrow(() -> new NoEncontradoException("No se encontró el perfil de la postulación."));
        OfertaPractica oferta = ofertas.buscarPorId(perfil.oferta_id)
                .orElseThrow(() -> new NoEncontradoException("No se encontró la oferta de la postulación."));
        Estudiante estudiante = estudiantes.buscarPorId(postulacion.estudiante_id)
                .orElseThrow(() -> new NoEncontradoException("No se encontró el estudiante de la postulación."));

        if (oferta.horas_requeridas_snapshot == null || oferta.nota_minima_snapshot == null) {
            throw new ReglaNegocioException("La oferta no tiene configurados los valores académicos necesarios para generar la práctica.");
        }

        OffsetDateTime ahora = OffsetDateTime.now(ZONA_ECUADOR);
        postulacion.estado = "ACEPTADA";
        postulacion.resuelta_por = actor;
        postulacion.motivo_decision = null;
        postulacion.updated_at = ahora;
        postulaciones.guardar(postulacion);
        entityManager.flush();

        Practica practica = new Practica();
        practica.estudiante_id = estudiante.id;
        practica.carrera_id = estudiante.carrera_id;
        practica.empresa_id = oferta.empresa_id;
        practica.convenio_id = oferta.convenio_id;
        practica.tipo_practica_id = oferta.tipo_practica_id;
        practica.periodo_id = oferta.periodo_id;
        practica.estado = "APROBADA";
        practica.horas_requeridas_snapshot = oferta.horas_requeridas_snapshot;
        practica.nota_minima_snapshot = oferta.nota_minima_snapshot;
        practica.fecha_postulacion = postulacion.fecha_postulacion == null ? ahora : postulacion.fecha_postulacion;
        practica.fecha_aprobacion = ahora;
        practica.fecha_inicio = oferta.fecha_inicio_actividades;
        practica.fecha_fin = oferta.fecha_fin_actividades;
        practica.creado_por = actor;
        practica.postulacion_id = postulacion.id;
        practica.version = 0L;
        practica.created_at = ahora;
        practica.updated_at = ahora;
        practicas.guardar(practica);
        entityManager.flush();

        return Map.of("mensaje", "La postulación fue aceptada y se generó la práctica.",
                "postulacion_id", postulacion.id, "practica_id", practica.id,
                "estado_postulacion", postulacion.estado, "estado_practica", practica.estado);
    }

    /** Rechaza la postulación registrando obligatoriamente el motivo. */
    @Transactional
    public Map<String, Object> rechazar(long postulacionId, String motivo, long actor) {
        String motivoLimpio = motivo == null ? "" : motivo.trim();
        if (motivoLimpio.isEmpty()) throw new ReglaNegocioException("Debe indicar el motivo del rechazo.");
        if (motivoLimpio.length() > 2000) throw new ReglaNegocioException("El motivo no puede superar los 2000 caracteres.");

        establecerActor(actor);
        Postulacion postulacion = buscarPendiente(postulacionId);
        postulacion.estado = "RECHAZADA";
        postulacion.resuelta_por = actor;
        postulacion.motivo_decision = motivoLimpio;
        postulacion.updated_at = OffsetDateTime.now(ZONA_ECUADOR);
        postulaciones.guardar(postulacion);
        entityManager.flush();
        return Map.of("mensaje", "La postulación fue rechazada.", "postulacion_id", postulacion.id,
                "estado", postulacion.estado);
    }

    private Postulacion buscarPendiente(long id) {
        Postulacion postulacion = postulaciones.buscarPorId(id)
                .orElseThrow(() -> new NoEncontradoException("Postulación no encontrada."));
        if (!"PENDIENTE".equals(postulacion.estado)) {
            throw new ReglaNegocioException("La postulación ya fue resuelta y no está pendiente.");
        }
        return postulacion;
    }

    private Map<String, Object> resumen(Postulacion postulacion) {
        Estudiante estudiante = estudiantes.buscarPorId(postulacion.estudiante_id).orElse(null);
        Usuario usuario = estudiante == null ? null : usuarios.buscarPorId(estudiante.usuario_id).orElse(null);
        OfertaPerfil perfil = perfiles.buscarPorId(postulacion.perfil_id).orElse(null);
        OfertaPractica oferta = perfil == null ? null : ofertas.buscarPorId(perfil.oferta_id).orElse(null);
        Empresa empresa = oferta == null ? null : empresas.buscarPorId(oferta.empresa_id).orElse(null);
        long aceptadas = perfil == null ? 0 : postulaciones.contarPorPerfilYEstado(perfil.id, "ACEPTADA");

        Map<String, Object> fila = new LinkedHashMap<>();
        fila.put("id", postulacion.id);
        fila.put("estudiante", usuario == null ? "Estudiante #" + postulacion.estudiante_id
                : (usuario.nombres + " " + usuario.apellidos).trim());
        fila.put("correo_estudiante", usuario == null ? null : usuario.correo);
        fila.put("oferta", oferta == null ? "Oferta no disponible" : oferta.titulo);
        fila.put("empresa", empresa == null ? "—" : empresa.razon_social);
        fila.put("perfil", perfil == null ? "Perfil no disponible" : perfil.nombre);
        fila.put("plazas_disponibles", perfil == null || perfil.plazas == null ? 0 : Math.max(0, perfil.plazas - (int) aceptadas));
        fila.put("fecha_postulacion", postulacion.fecha_postulacion);
        fila.put("recomendacion_empresa", postulacion.recomendacion_empresa);
        fila.put("estado", postulacion.estado);
        return fila;
    }

    private void establecerActor(long actor) {
        entityManager.createNativeQuery("SELECT set_config('praxis.usuario_id', ?1, true)")
                .setParameter(1, Long.toString(actor)).getSingleResult();
    }
}
