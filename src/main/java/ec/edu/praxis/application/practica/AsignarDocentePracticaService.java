package ec.edu.praxis.application.practica;

import ec.edu.praxis.application.excepcion.NoEncontradoException;
import ec.edu.praxis.application.excepcion.ProhibidoException;
import ec.edu.praxis.application.excepcion.ReglaNegocioException;
import ec.edu.praxis.application.seguridad.PoliticaAutorizacionService;
import ec.edu.praxis.domain.modelo.*;
import ec.edu.praxis.domain.repositorio.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

/** Asignación de docente tutor a prácticas aprobadas, limitada por carrera. */
@ApplicationScoped
public class AsignarDocentePracticaService {
    private static final ZoneId ZONA_ECUADOR = ZoneId.of("America/Guayaquil");

    @Inject RepositorioPractica practicas;
    @Inject RepositorioDocente docentes;
    @Inject RepositorioUsuario usuarios;
    @Inject RepositorioEstudiante estudiantes;
    @Inject RepositorioCarrera carreras;
    @Inject RepositorioEmpresa empresas;
    @Inject RepositorioPeriodoAcademico periodos;
    @Inject PoliticaAutorizacionService politica;
    @Inject EntityManager entityManager;

    public List<Map<String, Object>> listarPendientes(long actor) {
        exigirCoordinador(actor);
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Practica practica : practicas.listarAprobadasSinDocente()) {
            if (politica.tieneAccesoCarrera(actor, practica.carrera_id)) {
                resultado.add(resumen(practica));
            }
        }
        return resultado;
    }

    public List<Map<String, Object>> listarDocentesDisponibles(long practicaId, long actor) {
        exigirCoordinador(actor);
        Practica practica = buscarPractica(practicaId);
        exigirAlcance(actor, practica.carrera_id);

        // Una sola consulta SQL comprueba las relaciones reales en PostgreSQL.
        // Esto evita falsos negativos al recorrer usuario_rol y usuario_carrera
        // mediante consultas separadas del repositorio.
        @SuppressWarnings("unchecked")
        List<Object[]> filas = entityManager.createNativeQuery("""
                SELECT d.id, u.id, u.nombres, u.apellidos, u.correo,
                       d.codigo_docente, d.dedicacion
                  FROM docente d
                  JOIN usuario u ON u.id = d.usuario_id
                 WHERE u.activo = TRUE
                   AND EXISTS (
                       SELECT 1
                         FROM usuario_rol ur
                         JOIN rol r ON r.id = ur.rol_id
                        WHERE ur.usuario_id = u.id
                          AND UPPER(BTRIM(r.codigo)) = 'DOCENTE'
                   )
                   AND (
                       u.alcance_global = TRUE
                       OR EXISTS (
                           SELECT 1
                             FROM usuario_carrera uc
                            WHERE uc.usuario_id = u.id
                              AND uc.carrera_id = :carreraId
                       )
                   )
                 ORDER BY LOWER(u.apellidos), LOWER(u.nombres), d.id
                """)
                .setParameter("carreraId", practica.carrera_id)
                .getResultList();

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Object[] filaDb : filas) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("id", ((Number) filaDb[0]).longValue());
            fila.put("usuario_id", ((Number) filaDb[1]).longValue());
            fila.put("nombre", (String.valueOf(filaDb[2]) + " " + String.valueOf(filaDb[3])).trim());
            fila.put("correo", filaDb[4]);
            fila.put("codigo_docente", filaDb[5]);
            fila.put("dedicacion", filaDb[6]);
            resultado.add(fila);
        }
        return resultado;
    }

    @Transactional
    public Map<String, Object> asignar(long practicaId, Long docenteId, long actor) {
        exigirCoordinador(actor);
        if (docenteId == null) throw new ReglaNegocioException("Debe seleccionar un docente.");

        Practica practica = buscarPractica(practicaId);
        exigirAlcance(actor, practica.carrera_id);
        if (!"APROBADA".equals(practica.estado)) {
            throw new ReglaNegocioException("Solo se puede asignar docente a una práctica en estado APROBADA.");
        }
        if (practica.docente_id != null) {
            throw new ReglaNegocioException("La práctica ya tiene un docente asignado.");
        }

        Docente docente = docentes.buscarPorId(docenteId)
                .orElseThrow(() -> new NoEncontradoException("No se encontró el docente seleccionado."));
        Usuario usuario = usuarios.buscarPorId(docente.usuario_id)
                .orElseThrow(() -> new NoEncontradoException("No se encontró el usuario del docente."));
        if (!docenteCumpleAlcance(docente.id, practica.carrera_id)) {
            throw new ReglaNegocioException(
                    "El docente debe tener un usuario activo, el rol DOCENTE y la carrera de esta práctica asignada.");
        }

        entityManager.createNativeQuery("select set_config('praxis.usuario_id', :actor, true)")
                .setParameter("actor", String.valueOf(actor)).getSingleResult();
        practica.docente_id = docente.id;
        practica.version = practica.version == null ? 1L : practica.version + 1L;
        practica.updated_at = OffsetDateTime.now(ZONA_ECUADOR);
        practicas.guardar(practica);
        entityManager.flush();

        return Map.of("mensaje", "Docente tutor asignado correctamente.",
                "practica_id", practica.id, "docente_id", docente.id,
                "docente", (usuario.nombres + " " + usuario.apellidos).trim());
    }

    private boolean docenteCumpleAlcance(Long docenteId, Long carreraId) {
        Number cantidad = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*)
                  FROM docente d
                  JOIN usuario u ON u.id = d.usuario_id
                 WHERE d.id = :docenteId
                   AND u.activo = TRUE
                   AND EXISTS (
                       SELECT 1
                         FROM usuario_rol ur
                         JOIN rol r ON r.id = ur.rol_id
                        WHERE ur.usuario_id = u.id
                          AND UPPER(BTRIM(r.codigo)) = 'DOCENTE'
                   )
                   AND (
                       u.alcance_global = TRUE
                       OR EXISTS (
                           SELECT 1
                             FROM usuario_carrera uc
                            WHERE uc.usuario_id = u.id
                              AND uc.carrera_id = :carreraId
                       )
                   )
                """)
                .setParameter("docenteId", docenteId)
                .setParameter("carreraId", carreraId)
                .getSingleResult();
        return cantidad.longValue() > 0;
    }

    private Practica buscarPractica(long id) {
        return practicas.buscarPorId(id)
                .orElseThrow(() -> new NoEncontradoException("No se encontró la práctica."));
    }

    private void exigirCoordinador(long actor) {
        if (!politica.tieneRol(actor, "COORDINADOR")) {
            throw new ProhibidoException("Se requiere el rol COORDINADOR.");
        }
    }

    private void exigirAlcance(long actor, Long carreraId) {
        if (!politica.tieneAccesoCarrera(actor, carreraId)) {
            throw new ProhibidoException("No tiene acceso a la carrera de esta práctica.");
        }
    }

    private Map<String, Object> resumen(Practica practica) {
        Estudiante estudiante = estudiantes.buscarPorId(practica.estudiante_id).orElse(null);
        Usuario usuario = estudiante == null ? null : usuarios.buscarPorId(estudiante.usuario_id).orElse(null);
        Carrera carrera = carreras.buscarPorId(practica.carrera_id).orElse(null);
        Empresa empresa = empresas.buscarPorId(practica.empresa_id).orElse(null);
        PeriodoAcademico periodo = periodos.buscarPorId(practica.periodo_id).orElse(null);

        Map<String, Object> fila = new LinkedHashMap<>();
        fila.put("id", practica.id);
        fila.put("estudiante", usuario == null ? "Estudiante #" + practica.estudiante_id
                : (usuario.nombres + " " + usuario.apellidos).trim());
        fila.put("correo_estudiante", usuario == null ? null : usuario.correo);
        fila.put("carrera", carrera == null ? "—" : carrera.nombre);
        fila.put("carrera_id", practica.carrera_id);
        fila.put("empresa", empresa == null ? "—" : empresa.razon_social);
        fila.put("periodo", periodo == null ? "—" : periodo.nombre);
        fila.put("fecha_aprobacion", practica.fecha_aprobacion);
        fila.put("fecha_inicio", practica.fecha_inicio);
        fila.put("fecha_fin", practica.fecha_fin);
        fila.put("estado", practica.estado);
        return fila;
    }
}
