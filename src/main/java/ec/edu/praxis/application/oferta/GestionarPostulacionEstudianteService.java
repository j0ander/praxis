package ec.edu.praxis.application.oferta;

import ec.edu.praxis.application.excepcion.ConflictoException;
import ec.edu.praxis.application.excepcion.NoEncontradoException;
import ec.edu.praxis.application.excepcion.ReglaNegocioException;
import ec.edu.praxis.domain.modelo.*;
import ec.edu.praxis.domain.repositorio.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Gestiona las postulaciones creadas por el propio estudiante autenticado. */
@ApplicationScoped
public class GestionarPostulacionEstudianteService {

    private static final ZoneId ZONA_ECUADOR = ZoneId.of("America/Guayaquil");

    @Inject RepositorioEstudiante estudiantes;
    @Inject RepositorioOfertaPerfil perfiles;
    @Inject RepositorioOfertaPractica ofertas;
    @Inject RepositorioOfertaPerfilCarrera perfilesCarrera;
    @Inject RepositorioPostulacion postulaciones;
    @Inject RepositorioEmpresa empresas;
    @Inject EntityManager entityManager;

    /** Registra una postulación para el perfil elegido por el estudiante. */
    @Transactional
    public Map<String, Object> crear(Map<String, Object> datos, long usuarioId) {
        Estudiante estudiante = estudianteAutenticado(usuarioId);
        long perfilId = numero(datos, "perfilId");

        OfertaPerfil perfil = perfiles.buscarPorId(perfilId)
                .orElseThrow(() -> new NoEncontradoException("El perfil seleccionado no existe."));
        OfertaPractica oferta = ofertas.buscarPorId(perfil.oferta_id)
                .orElseThrow(() -> new NoEncontradoException("La oferta del perfil no existe."));

        if (!"PUBLICADA".equals(oferta.estado)) {
            throw new ReglaNegocioException("La oferta ya no está publicada y no acepta postulaciones.");
        }

        LocalDate hoy = LocalDate.now(ZONA_ECUADOR);
        if (oferta.fecha_cierre == null || oferta.fecha_cierre.isBefore(hoy)) {
            throw new ReglaNegocioException("El plazo de postulación de esta oferta ha finalizado.");
        }

        if (estudiante.carrera_id == null) {
            throw new ReglaNegocioException("Tu perfil de estudiante no tiene una carrera asignada.");
        }

        List<OfertaPerfilCarrera> carrerasPermitidas = perfilesCarrera.listarPorPerfil(perfil.id);
        boolean compatible = carrerasPermitidas.isEmpty()
                || carrerasPermitidas.stream().anyMatch(c -> Objects.equals(c.carrera_id, estudiante.carrera_id));
        if (!compatible) {
            throw new ReglaNegocioException("Tu carrera no está habilitada para el perfil seleccionado.");
        }

        long aceptadas = postulaciones.contarPorPerfilYEstado(perfil.id, "ACEPTADA");
        if (perfil.plazas == null || aceptadas >= perfil.plazas) {
            throw new ReglaNegocioException("El perfil ya completó sus plazas y no recibe nuevas postulaciones.");
        }

        if (postulaciones.buscarActivaPorPerfilYEstudiante(perfil.id, estudiante.id).isPresent()) {
            throw new ConflictoException("Ya tienes una postulación registrada para este perfil.");
        }

        String mensaje = opcional(datos, "mensaje");
        OffsetDateTime ahora = OffsetDateTime.now(ZONA_ECUADOR);

        // Los triggers de V3 verifican que el actor sea el propio estudiante y auditan el INSERT.
        establecerActor(usuarioId);

        Postulacion postulacion = new Postulacion();
        postulacion.perfil_id = perfil.id;
        postulacion.estudiante_id = estudiante.id;
        postulacion.estado = "PENDIENTE";
        postulacion.mensaje = mensaje;
        postulacion.fecha_postulacion = ahora;
        postulacion.recomendacion_empresa = "SIN_EVALUAR";
        postulacion.version = 0L;
        postulacion.created_at = ahora;
        postulacion.updated_at = ahora;

        postulaciones.guardar(postulacion);
        entityManager.flush();

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("id", postulacion.id);
        resultado.put("perfil_id", perfil.id);
        resultado.put("oferta_id", oferta.id);
        resultado.put("estado", postulacion.estado);
        resultado.put("fecha_postulacion", postulacion.fecha_postulacion);
        resultado.put("mensaje", "Tu postulación fue registrada correctamente.");
        return resultado;
    }

    /** Devuelve únicamente las postulaciones del estudiante autenticado. */
    public List<Map<String, Object>> listarPropias(long usuarioId) {
        Estudiante estudiante = estudianteAutenticado(usuarioId);
        return postulaciones.listarPorEstudiante(estudiante.id).stream().map(postulacion -> {
            OfertaPerfil perfil = perfiles.buscarPorId(postulacion.perfil_id).orElse(null);
            OfertaPractica oferta = perfil == null ? null : ofertas.buscarPorId(perfil.oferta_id).orElse(null);
            Empresa empresa = oferta == null ? null : empresas.buscarPorId(oferta.empresa_id).orElse(null);

            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("id", postulacion.id);
            fila.put("perfil_id", postulacion.perfil_id);
            fila.put("perfil", perfil == null ? "Perfil #" + postulacion.perfil_id : perfil.nombre);
            fila.put("oferta_id", oferta == null ? null : oferta.id);
            fila.put("oferta", oferta == null ? "Oferta no disponible" : oferta.titulo);
            fila.put("empresa", empresa == null ? "—" : empresa.razon_social);
            fila.put("estado", postulacion.estado);
            fila.put("fecha_postulacion", postulacion.fecha_postulacion);
            fila.put("mensaje", postulacion.mensaje);
            fila.put("recomendacion_empresa", postulacion.recomendacion_empresa);
            fila.put("observacion_empresa", postulacion.observacion_empresa);
            fila.put("motivo_decision", postulacion.motivo_decision);
            return fila;
        }).toList();
    }

    private Estudiante estudianteAutenticado(long usuarioId) {
        return estudiantes.buscarPorUsuario(usuarioId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "El usuario autenticado no tiene un perfil de estudiante registrado."));
    }

    private void establecerActor(long usuarioId) {
        entityManager.createNativeQuery("SELECT set_config('praxis.usuario_id', ?1, true)")
                .setParameter(1, Long.toString(usuarioId))
                .getSingleResult();
    }

    private long numero(Map<String, Object> datos, String campo) {
        Object valor = datos == null ? null : datos.get(campo);
        try {
            if (valor instanceof Number numero) return numero.longValue();
            return Long.parseLong(Objects.toString(valor, ""));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Seleccione un perfil válido para postularse.");
        }
    }

    private String opcional(Map<String, Object> datos, String campo) {
        Object valor = datos == null ? null : datos.get(campo);
        if (valor == null) return null;
        String texto = valor.toString().trim();
        return texto.isEmpty() ? null : texto;
    }
}
