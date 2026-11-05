package ec.edu.praxis.application.estudiante;

import ec.edu.praxis.application.excepcion.ConflictoException;
import ec.edu.praxis.application.excepcion.NoEncontradoException;
import ec.edu.praxis.application.excepcion.ReglaNegocioException;
import ec.edu.praxis.domain.modelo.Auditoria;
import ec.edu.praxis.domain.modelo.Carrera;
import ec.edu.praxis.domain.modelo.Estudiante;
import ec.edu.praxis.domain.modelo.Rol;
import ec.edu.praxis.domain.modelo.Usuario;
import ec.edu.praxis.domain.modelo.UsuarioRol;
import ec.edu.praxis.domain.repositorio.RepositorioAuditoria;
import ec.edu.praxis.domain.repositorio.RepositorioCarrera;
import ec.edu.praxis.domain.repositorio.RepositorioEstudiante;
import ec.edu.praxis.domain.repositorio.RepositorioRol;
import ec.edu.praxis.domain.repositorio.RepositorioUsuario;
import ec.edu.praxis.domain.repositorio.RepositorioUsuarioRol;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Casos de uso para crear y consultar perfiles académicos de estudiantes. */
@ApplicationScoped
public class GestionarEstudianteService {

    @Inject RepositorioEstudiante estudiantes;
    @Inject RepositorioUsuario usuarios;
    @Inject RepositorioUsuarioRol usuariosRoles;
    @Inject RepositorioRol roles;
    @Inject RepositorioCarrera carreras;
    @Inject RepositorioAuditoria auditoria;

    public List<Map<String, Object>> listarEstudiantes() {
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Estudiante estudiante : estudiantes.listarTodos()) {
            Usuario usuario = usuarios.buscarPorId(estudiante.usuario_id).orElse(null);
            Carrera carrera = carreras.buscarPorId(estudiante.carrera_id).orElse(null);
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("id", estudiante.id);
            fila.put("usuario_id", estudiante.usuario_id);
            fila.put("estudiante", usuario == null ? "Usuario no disponible" : usuario.nombres + " " + usuario.apellidos);
            fila.put("correo", usuario == null ? "—" : usuario.correo);
            fila.put("carrera_id", estudiante.carrera_id);
            fila.put("carrera", carrera == null ? "Carrera no disponible" : carrera.nombre);
            fila.put("codigo_estudiante", estudiante.codigo_estudiante);
            fila.put("fecha_nacimiento", estudiante.fecha_nacimiento);
            resultado.add(fila);
        }
        return resultado;
    }

    public Map<String, Object> opciones() {
        List<Map<String, Object>> usuariosDisponibles = new ArrayList<>();
        Rol rolEstudiante = roles.buscarPorCodigo("ESTUDIANTE").orElse(null);
        if (rolEstudiante != null) {
            for (Usuario usuario : usuarios.listarTodos()) {
                if (!Boolean.TRUE.equals(usuario.activo) || estudiantes.existePorUsuario(usuario.id)) continue;
                boolean tieneRol = usuariosRoles.listarPorUsuario(usuario.id).stream()
                        .anyMatch(relacion -> relacion.rol_id.equals(rolEstudiante.id));
                if (!tieneRol) continue;
                Map<String, Object> fila = new LinkedHashMap<>();
                fila.put("id", usuario.id);
                fila.put("nombres", usuario.nombres);
                fila.put("apellidos", usuario.apellidos);
                fila.put("correo", usuario.correo);
                usuariosDisponibles.add(fila);
            }
        }

        List<Map<String, Object>> carrerasDisponibles = carreras.listarTodos().stream()
                .filter(carrera -> Boolean.TRUE.equals(carrera.activa))
                .map(carrera -> Map.<String, Object>of("id", carrera.id, "nombre", carrera.nombre, "codigo", carrera.codigo))
                .toList();

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("usuarios", usuariosDisponibles);
        resultado.put("carreras", carrerasDisponibles);
        return resultado;
    }

    @Transactional
    public long crearEstudiante(Map<String, Object> datos, long actor) {
        long usuarioId = numero(datos, "usuarioId");
        long carreraId = numero(datos, "carreraId");

        Usuario usuario = usuarios.buscarPorId(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("El usuario seleccionado no existe."));
        if (!Boolean.TRUE.equals(usuario.activo)) {
            throw new ReglaNegocioException("No se puede crear el perfil de un usuario inactivo.");
        }

        Rol rolEstudiante = roles.buscarPorCodigo("ESTUDIANTE")
                .orElseThrow(() -> new ReglaNegocioException("No está configurado el rol ESTUDIANTE."));
        boolean tieneRol = usuariosRoles.listarPorUsuario(usuarioId).stream()
                .anyMatch(relacion -> relacion.rol_id.equals(rolEstudiante.id));
        if (!tieneRol) {
            throw new ReglaNegocioException("El usuario debe tener asignado el rol ESTUDIANTE.");
        }
        if (estudiantes.existePorUsuario(usuarioId)) {
            throw new ConflictoException("Este usuario ya tiene un perfil de estudiante.");
        }

        Carrera carrera = carreras.buscarPorId(carreraId)
                .orElseThrow(() -> new NoEncontradoException("La carrera seleccionada no existe."));
        if (!Boolean.TRUE.equals(carrera.activa)) {
            throw new ReglaNegocioException("No se puede asignar una carrera inactiva.");
        }

        String codigo = opcional(datos, "codigoEstudiante");
        if (codigo != null && codigo.length() > 30) {
            throw new IllegalArgumentException("El código de estudiante no puede superar 30 caracteres.");
        }
        if (codigo != null && estudiantes.listarTodos().stream()
                .anyMatch(e -> codigo.equalsIgnoreCase(e.codigo_estudiante))) {
            throw new ConflictoException("Ya existe un estudiante con ese código.");
        }

        Estudiante estudiante = new Estudiante();
        estudiante.usuario_id = usuarioId;
        estudiante.carrera_id = carreraId;
        estudiante.codigo_estudiante = codigo;
        estudiante.fecha_nacimiento = fechaOpcional(datos, "fechaNacimiento");
        OffsetDateTime ahora = OffsetDateTime.now();
        estudiante.created_at = ahora;
        estudiante.updated_at = ahora;
        estudiantes.guardar(estudiante);

        registrar(actor, estudiante.id, "CREAR");
        return estudiante.id;
    }

    private long numero(Map<String, Object> datos, String campo) {
        Object valor = datos.get(campo);
        if (!(valor instanceof Number)) throw new IllegalArgumentException("Debe seleccionar " + campo + ".");
        return ((Number) valor).longValue();
    }

    private String opcional(Map<String, Object> datos, String campo) {
        Object valor = datos.get(campo);
        if (valor == null) return null;
        String texto = valor.toString().trim();
        return texto.isEmpty() ? null : texto;
    }

    private LocalDate fechaOpcional(Map<String, Object> datos, String campo) {
        String valor = opcional(datos, campo);
        if (valor == null) return null;
        try {
            LocalDate fecha = LocalDate.parse(valor);
            if (fecha.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura.");
            }
            return fecha;
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("La fecha de nacimiento no tiene un formato válido.");
        }
    }

    private void registrar(long actor, long estudianteId, String accion) {
        Auditoria registro = new Auditoria();
        registro.usuario_id = actor;
        registro.accion = accion;
        registro.entidad = "estudiante";
        registro.entidad_id = estudianteId;
        registro.detalle = Map.of("modulo", "gestion_estudiantes");
        registro.fecha = OffsetDateTime.now();
        auditoria.guardar(registro);
    }
}
