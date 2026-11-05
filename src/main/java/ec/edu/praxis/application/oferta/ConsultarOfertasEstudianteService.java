package ec.edu.praxis.application.oferta;

import ec.edu.praxis.application.excepcion.NoEncontradoException;
import ec.edu.praxis.application.excepcion.ReglaNegocioException;
import ec.edu.praxis.domain.modelo.*;
import ec.edu.praxis.domain.repositorio.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/** Consultas de ofertas publicadas disponibles para el perfil de estudiante autenticado. */
@ApplicationScoped
public class ConsultarOfertasEstudianteService {

    private static final ZoneId ZONA_ECUADOR = ZoneId.of("America/Guayaquil");

    @Inject RepositorioEstudiante estudiantes;
    @Inject RepositorioPostulacion postulaciones;
    @Inject RepositorioOfertaPractica ofertas;
    @Inject RepositorioOfertaPerfil perfiles;
    @Inject RepositorioOfertaPerfilRequisito requisitos;
    @Inject RepositorioOfertaPerfilCarrera perfilesCarrera;
    @Inject RepositorioCarrera carreras;
    @Inject RepositorioEmpresa empresas;
    @Inject RepositorioConvenio convenios;
    @Inject RepositorioTipoPractica tipos;
    @Inject RepositorioPeriodoAcademico periodos;

    public List<Map<String, Object>> listarPublicadas(long usuarioId) {
        Estudiante estudiante = estudianteAutenticado(usuarioId);
        return ofertas.listarTodas().stream()
                .filter(oferta -> "PUBLICADA".equals(oferta.estado))
                .map(oferta -> construirOferta(oferta, estudiante, false))
                .toList();
    }

    public Map<String, Object> detallePublicada(long ofertaId, long usuarioId) {
        Estudiante estudiante = estudianteAutenticado(usuarioId);
        OfertaPractica oferta = ofertas.buscarPorId(ofertaId)
                .filter(o -> "PUBLICADA".equals(o.estado))
                .orElseThrow(() -> new NoEncontradoException("Oferta publicada no encontrada."));
        return construirOferta(oferta, estudiante, true);
    }

    private Estudiante estudianteAutenticado(long usuarioId) {
        return estudiantes.buscarPorUsuario(usuarioId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "El usuario autenticado no tiene un perfil de estudiante registrado."));
    }

    private Map<String, Object> construirOferta(OfertaPractica oferta, Estudiante estudiante, boolean detalle) {
        Empresa empresa = empresas.buscarPorId(oferta.empresa_id).orElse(null);
        Convenio convenio = convenios.buscarPorId(oferta.convenio_id).orElse(null);
        TipoPractica tipo = tipos.buscarPorId(oferta.tipo_practica_id).orElse(null);
        PeriodoAcademico periodo = periodos.buscarPorId(oferta.periodo_id).orElse(null);
        Carrera carreraEstudiante = carreras.buscarPorId(estudiante.carrera_id).orElse(null);

        LocalDate hoy = LocalDate.now(ZONA_ECUADOR);
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("id", oferta.id);
        resultado.put("titulo", oferta.titulo);
        resultado.put("descripcion", oferta.descripcion);
        resultado.put("empresa", empresa == null ? "Empresa #" + oferta.empresa_id : empresa.razon_social);
        resultado.put("empresa_nombre_comercial", empresa == null ? null : empresa.nombre_comercial);
        resultado.put("empresa_ruc", empresa == null ? null : empresa.ruc);
        resultado.put("empresa_direccion", empresa == null ? null : empresa.direccion);
        resultado.put("empresa_telefono", empresa == null ? null : empresa.telefono);
        resultado.put("empresa_correo", empresa == null ? null : empresa.correo);
        resultado.put("convenio", convenio == null ? null : convenio.numero);
        resultado.put("convenio_fecha_inicio", convenio == null ? null : convenio.fecha_inicio);
        resultado.put("convenio_fecha_fin", convenio == null ? null : convenio.fecha_fin);
        resultado.put("tipo_practica", tipo == null ? null : tipo.nombre);
        resultado.put("periodo", periodo == null ? null : periodo.nombre);
        resultado.put("periodo_fecha_inicio", periodo == null ? null : periodo.fecha_inicio);
        resultado.put("periodo_fecha_fin", periodo == null ? null : periodo.fecha_fin);
        resultado.put("fecha_cierre", oferta.fecha_cierre);
        resultado.put("plazo_vigente", oferta.fecha_cierre != null && !oferta.fecha_cierre.isBefore(hoy));
        resultado.put("fecha_inicio_actividades", oferta.fecha_inicio_actividades);
        resultado.put("fecha_fin_actividades", oferta.fecha_fin_actividades);
        resultado.put("fecha_publicacion", oferta.fecha_publicacion);
        resultado.put("horas_requeridas", oferta.horas_requeridas_snapshot != null
                ? oferta.horas_requeridas_snapshot : tipo == null ? null : tipo.horas_requeridas);
        resultado.put("nota_minima", oferta.nota_minima_snapshot != null
                ? oferta.nota_minima_snapshot : tipo == null ? null : tipo.nota_minima);
        resultado.put("carrera_estudiante", carreraEstudiante == null ? null : carreraEstudiante.nombre);

        List<Map<String, Object>> listaPerfiles = perfiles.listarPorOferta(oferta.id).stream()
                .map(perfil -> construirPerfil(perfil, estudiante, detalle))
                .toList();
        resultado.put("perfiles", listaPerfiles);
        resultado.put("tiene_perfil_compatible", listaPerfiles.stream()
                .anyMatch(p -> Boolean.TRUE.equals(p.get("carrera_compatible"))));
        return resultado;
    }

    private Map<String, Object> construirPerfil(OfertaPerfil perfil, Estudiante estudiante, boolean incluirRequisitos) {
        List<OfertaPerfilCarrera> asignaciones = perfilesCarrera.listarPorPerfil(perfil.id);
        List<Map<String, Object>> listaCarreras = asignaciones.stream().map(asignacion -> {
            Carrera carrera = carreras.buscarPorId(asignacion.carrera_id).orElse(null);
            Map<String, Object> dato = new LinkedHashMap<>();
            dato.put("id", asignacion.carrera_id);
            dato.put("nombre", carrera == null ? "Carrera #" + asignacion.carrera_id : carrera.nombre);
            return dato;
        }).toList();

        boolean compatible = asignaciones.isEmpty()
                || asignaciones.stream().anyMatch(a -> Objects.equals(a.carrera_id, estudiante.carrera_id));

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("id", perfil.id);
        resultado.put("nombre", perfil.nombre);
        resultado.put("descripcion", perfil.descripcion);
        resultado.put("plazas", perfil.plazas);
        long aceptadas = postulaciones.contarPorPerfilYEstado(perfil.id, "ACEPTADA");
        resultado.put("plazas_disponibles", perfil.plazas == null ? 0 : Math.max(0, perfil.plazas - (int) aceptadas));
        Postulacion postulacionActiva = postulaciones
                .buscarActivaPorPerfilYEstudiante(perfil.id, estudiante.id).orElse(null);
        resultado.put("postulacion_estado", postulacionActiva == null ? null : postulacionActiva.estado);
        resultado.put("ya_postulado", postulacionActiva != null);
        resultado.put("carreras", listaCarreras);
        resultado.put("carrera_compatible", compatible);
        resultado.put("abierto_a_todas_las_carreras", asignaciones.isEmpty());

        if (incluirRequisitos) {
            resultado.put("requisitos", requisitos.listarPorPerfil(perfil.id).stream().map(req -> {
                Map<String, Object> requisito = new LinkedHashMap<>();
                requisito.put("id", req.id);
                requisito.put("descripcion", req.descripcion);
                requisito.put("obligatorio", req.obligatorio);
                return requisito;
            }).toList());
        }
        return resultado;
    }
}
