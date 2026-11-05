package ec.edu.praxis.application.seguridad;

import ec.edu.praxis.application.excepcion.ConflictoException;
import ec.edu.praxis.application.excepcion.NoAutenticadoException;
import ec.edu.praxis.application.excepcion.NoEncontradoException;
import ec.edu.praxis.domain.modelo.*;
import ec.edu.praxis.domain.repositorio.*;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

@ApplicationScoped
public class AdministrarSeguridadService {
    @Inject
    RepositorioUsuario usuarios;
    @Inject
    RepositorioRol rolesRepositorio;
    @Inject
    RepositorioUsuarioRol usuariosRoles;
    @Inject
    RepositorioUsuarioCarrera usuariosCarreras;
    @Inject
    RepositorioDocente docentes;
    @Inject
    RepositorioFacultad facultadesRepositorio;
    @Inject
    RepositorioCarrera carrerasRepositorio;
    @Inject
    RepositorioPeriodoAcademico periodosRepositorio;
    @Inject
    RepositorioAuditoria auditoriaRepositorio;

    @Transactional
    public long crearUsuario(Map<String, Object> d, long actor) {
        String correo = texto(d, "correo").toLowerCase(Locale.ROOT);
        if (usuarios.existeCorreo(correo))
            throw new ConflictoException("Ya existe un usuario con ese correo.");
        String password = texto(d, "password");
        if (password.length() < 12)
            throw new IllegalArgumentException("La contraseña inicial debe tener al menos 12 caracteres.");
        Usuario usuario = new Usuario();
        usuario.correo = correo;
        usuario.hash_password = BcryptUtil.bcryptHash(password);
        usuario.nombres = texto(d, "nombres");
        usuario.apellidos = texto(d, "apellidos");
        usuario.numero_documento = nuloSiVacio(d.get("numeroDocumento"));
        usuario.tipo_documento = Objects.toString(d.getOrDefault("tipoDocumento", "CEDULA"), "CEDULA");
        usuario.telefono = nuloSiVacio(d.get("telefono"));
        usuario.activo = true;
        usuario.alcance_global = false;
        usuario.debe_cambiar_password = true;
        usuario.intentos_fallidos = 0;

        // La tabla usuario exige estas fechas; Hibernate no aplica el DEFAULT de
        // PostgreSQL
        // cuando incluye explícitamente NULL en el INSERT.
        OffsetDateTime ahora = OffsetDateTime.now();
        usuario.created_at = ahora;
        usuario.updated_at = ahora;

        usuarios.guardar(usuario);
        registrar(actor, "CREAR", "usuario", usuario.id);
        return usuario.id;
    }

    @Transactional
    public void estadoUsuario(long id, boolean activo, long actor) {
        Usuario usuario = buscarUsuario(id);
        usuario.activo = activo;
        usuario.updated_at = OffsetDateTime.now();
        usuarios.guardar(usuario);
        registrar(actor, activo ? "ACTIVAR" : "DESACTIVAR", "usuario", id);
    }

    @Transactional
    public void asignarRoles(long id, List<String> codigos, long actor) {
        buscarUsuario(id);
        List<Rol> rolesNuevos = new ArrayList<>();
        for (String codigo : new LinkedHashSet<>(codigos == null ? List.of() : codigos)) {
            Rol rol = rolesRepositorio.buscarPorCodigo(codigo)
                    .orElseThrow(() -> new NoEncontradoException("Rol inexistente: " + codigo));
            rolesNuevos.add(rol);
        }
        usuariosRoles.eliminarPorUsuario(id);
        for (Rol rol : rolesNuevos) {
            UsuarioRol relacion = new UsuarioRol();
            relacion.usuario_id = id;
            relacion.rol_id = rol.id;
            relacion.created_at = OffsetDateTime.now();
            usuariosRoles.guardar(relacion);
        }

        // Un usuario con rol DOCENTE debe tener también su perfil en la tabla docente.
        // El alta de usuario y la asignación de roles son operaciones separadas en la UI;
        // por eso el perfil se crea al asignar el rol.
        if (rolesNuevos.stream().anyMatch(r -> "DOCENTE".equals(r.codigo))) {
            asegurarPerfilDocente(id);
        }
        registrar(actor, "ASIGNAR_ROLES", "usuario", id);
    }

    @Transactional
    public void asignarCarreras(long id, List<Long> carreras, boolean global, long actor) {
        Usuario usuario = buscarUsuario(id);
        List<Carrera> seleccionadas = new ArrayList<>();
        for (Long carreraId : new LinkedHashSet<>(carreras == null ? List.of() : carreras)) {
            Carrera carrera = carrerasRepositorio.buscarPorId(carreraId)
                    .orElseThrow(() -> new NoEncontradoException("Carrera inexistente: " + carreraId));
            seleccionadas.add(carrera);
        }
        usuariosCarreras.eliminarPorUsuario(id);
        for (Carrera carrera : seleccionadas) {
            UsuarioCarrera relacion = new UsuarioCarrera();
            relacion.usuario_id = id;
            relacion.carrera_id = carrera.id;
            relacion.created_at = OffsetDateTime.now();
            usuariosCarreras.guardar(relacion);
        }
        usuario.alcance_global = global;
        usuario.updated_at = OffsetDateTime.now();
        usuarios.guardar(usuario);
        registrar(actor, "ASIGNAR_CARRERAS", "usuario", id);
    }


    private void asegurarPerfilDocente(long usuarioId) {
        boolean existe = docentes.listarTodos().stream()
                .anyMatch(d -> Objects.equals(d.usuario_id, usuarioId));
        if (existe) return;

        OffsetDateTime ahora = OffsetDateTime.now();
        Docente docente = new Docente();
        docente.usuario_id = usuarioId;
        docente.created_at = ahora;
        docente.updated_at = ahora;
        docentes.guardar(docente);
    }

    @Transactional
    public void cambiarPassword(long id, String actual, String nueva) {
        if (nueva == null || nueva.length() < 12)
            throw new IllegalArgumentException("La nueva contraseña debe tener al menos 12 caracteres.");
        Usuario usuario = usuarios.buscarPorId(id).filter(u -> Boolean.TRUE.equals(u.activo))
                .orElseThrow(NoAutenticadoException::new);
        if (actual == null || !BcryptUtil.matches(actual, usuario.hash_password))
            throw new NoAutenticadoException();
        usuario.hash_password = BcryptUtil.bcryptHash(nueva);
        usuario.debe_cambiar_password = false;
        usuario.updated_at = OffsetDateTime.now();
        usuarios.guardar(usuario);
        registrar(id, "CAMBIAR_PASSWORD", "usuario", id);
    }

    @Transactional
    public void restablecerPassword(long id, String temporal, long actor) {
        if (temporal == null || temporal.length() < 12)
            throw new IllegalArgumentException("La contraseña temporal debe tener al menos 12 caracteres.");
        Usuario usuario = buscarUsuario(id);
        usuario.hash_password = BcryptUtil.bcryptHash(temporal);
        usuario.debe_cambiar_password = true;
        usuario.intentos_fallidos = 0;
        usuario.bloqueado_hasta = null;
        usuario.updated_at = OffsetDateTime.now();
        usuarios.guardar(usuario);
        registrar(actor, "RESTABLECER_PASSWORD", "usuario", id);
    }

    @Transactional
    public long crearFacultad(Map<String, Object> d, long actor) {
        Facultad facultad = new Facultad();
        facultad.codigo = texto(d, "codigo");
        facultad.nombre = texto(d, "nombre");
        facultad.activa = true;
        facultadesRepositorio.guardar(facultad);
        registrar(actor, "CREAR", "facultad", facultad.id);
        return facultad.id;
    }

    @Transactional
    public long crearCarrera(Map<String, Object> d, long actor) {
        long facultadId = numero(d, "facultadId");
        facultadesRepositorio.buscarPorId(facultadId)
                .orElseThrow(() -> new NoEncontradoException("Facultad no encontrada."));
        Carrera carrera = new Carrera();
        carrera.facultad_id = facultadId;
        carrera.codigo = texto(d, "codigo");
        carrera.nombre = texto(d, "nombre");
        carrera.activa = true;
        carrerasRepositorio.guardar(carrera);
        registrar(actor, "CREAR", "carrera", carrera.id);
        return carrera.id;
    }

    @Transactional
    public long crearPeriodo(Map<String, Object> d, long actor) {
        PeriodoAcademico periodo = new PeriodoAcademico();
        periodo.codigo = texto(d, "codigo");
        periodo.nombre = texto(d, "nombre");
        periodo.fecha_inicio = LocalDate.parse(texto(d, "fechaInicio"));
        periodo.fecha_fin = LocalDate.parse(texto(d, "fechaFin"));
        if (periodo.fecha_fin.isBefore(periodo.fecha_inicio))
            throw new IllegalArgumentException("La fecha fin no puede ser anterior a la fecha inicio.");
        periodo.activo = false;
        periodosRepositorio.guardar(periodo);
        registrar(actor, "CREAR", "periodo_academico", periodo.id);
        return periodo.id;
    }

    @Transactional
    public void activarPeriodo(long id, long actor) {
        PeriodoAcademico periodo = periodosRepositorio.buscarPorId(id)
                .orElseThrow(() -> new NoEncontradoException("Periodo académico no encontrado."));
        periodosRepositorio.desactivarTodos();
        periodo.activo = true;
        periodosRepositorio.guardar(periodo);
        registrar(actor, "ACTIVAR", "periodo_academico", id);
    }

    @Transactional
    public void desactivar(String tabla, long id, long actor) {
        switch (tabla) {
            case "facultad" -> {
                Facultad facultad = facultadesRepositorio.buscarPorId(id)
                        .orElseThrow(() -> new NoEncontradoException("Facultad no encontrada."));
                facultad.activa = false;
                facultadesRepositorio.guardar(facultad);
            }
            case "carrera" -> {
                Carrera carrera = carrerasRepositorio.buscarPorId(id)
                        .orElseThrow(() -> new NoEncontradoException("Carrera no encontrada."));
                carrera.activa = false;
                carrerasRepositorio.guardar(carrera);
            }
            default -> throw new IllegalArgumentException("Catálogo no permitido.");
        }
        registrar(actor, "DESACTIVAR", tabla, id);
    }

    public List<Map<String, Object>> listarUsuarios() {
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Usuario usuario : usuarios.listarTodos()) {
            List<String> codigos = usuariosRoles.listarPorUsuario(usuario.id).stream()
                    .map(r -> r.rol_id).map(rolesRepositorio::buscarPorId)
                    .flatMap(Optional::stream).map(r -> r.codigo).sorted().toList();
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("id", usuario.id);
            fila.put("correo", usuario.correo);
            fila.put("nombres", usuario.nombres);
            fila.put("apellidos", usuario.apellidos);
            fila.put("numero_documento", usuario.numero_documento);
            fila.put("telefono", usuario.telefono);
            fila.put("activo", usuario.activo);
            fila.put("alcance_global", usuario.alcance_global);
            fila.put("debe_cambiar_password", usuario.debe_cambiar_password);
            fila.put("roles", codigos);
            fila.put("carreras", usuariosCarreras.listarPorUsuario(usuario.id).stream()
                    .map(relacion -> relacion.carrera_id).toList());
            resultado.add(fila);
        }
        return resultado;
    }

    public List<Map<String, Object>> roles() {
        return rolesRepositorio.listarTodos().stream().map(r -> {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("id", r.id);
            fila.put("codigo", r.codigo);
            fila.put("nombre", r.nombre);
            fila.put("descripcion", r.descripcion);
            return fila;
        }).toList();
    }

    public List<Map<String, Object>> facultades() {
        return facultadesRepositorio.listarTodos().stream().map(f -> {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("id", f.id);
            fila.put("codigo", f.codigo);
            fila.put("nombre", f.nombre);
            fila.put("activa", f.activa);
            return fila;
        }).toList();
    }

    public List<Map<String, Object>> carreras() {
        Map<Long, Facultad> facultades = new HashMap<>();
        facultadesRepositorio.listarTodos().forEach(f -> facultades.put(f.id, f));
        return carrerasRepositorio.listarTodos().stream().map(c -> {
            Facultad f = facultades.get(c.facultad_id);
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("id", c.id);
            fila.put("facultad_id", c.facultad_id);
            fila.put("facultad", f == null ? null : f.nombre);
            fila.put("codigo", c.codigo);
            fila.put("nombre", c.nombre);
            fila.put("activa", c.activa);
            return fila;
        }).toList();
    }

    public List<Map<String, Object>> periodos() {
        return periodosRepositorio.listarTodos().stream().map(this::mapPeriodo).toList();
    }

    public List<Map<String, Object>> periodoActivo() {
        return periodosRepositorio.buscarActivo().map(p -> List.of(mapPeriodo(p))).orElseGet(List::of);
    }

    public List<Map<String, Object>> auditoria(int limite) {
        int cantidad = Math.min(Math.max(limite, 1), 200);
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Auditoria a : auditoriaRepositorio.listarRecientes(cantidad)) {
            Usuario usuario = a.usuario_id == null ? null : usuarios.buscarPorId(a.usuario_id).orElse(null);
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("id", a.id);
            fila.put("usuario_id", a.usuario_id);
            fila.put("correo", usuario == null ? null : usuario.correo);
            fila.put("accion", a.accion);
            fila.put("entidad", a.entidad);
            fila.put("entidad_id", a.entidad_id);
            fila.put("detalle", a.detalle);
            fila.put("fecha", a.fecha);
            resultado.add(fila);
        }
        return resultado;
    }

    private Usuario buscarUsuario(long id) {
        return usuarios.buscarPorId(id).orElseThrow(() -> new NoEncontradoException("Usuario no encontrado."));
    }

    private Map<String, Object> mapPeriodo(PeriodoAcademico p) {
        Map<String, Object> fila = new LinkedHashMap<>();
        fila.put("id", p.id);
        fila.put("codigo", p.codigo);
        fila.put("nombre", p.nombre);
        fila.put("fecha_inicio", p.fecha_inicio);
        fila.put("fecha_fin", p.fecha_fin);
        fila.put("activo", p.activo);
        return fila;
    }

    private void registrar(long actor, String accion, String entidad, long id) {
        Auditoria a = new Auditoria();
        a.usuario_id = actor;
        a.accion = accion;
        a.entidad = entidad;
        a.entidad_id = id;
        a.detalle = Map.of();
        a.fecha = OffsetDateTime.now();

        auditoriaRepositorio.guardar(a);
    }

    private static String texto(Map<String, Object> d, String k) {
        Object v = d == null ? null : d.get(k);
        if (v == null || v.toString().isBlank())
            throw new IllegalArgumentException("El campo " + k + " es obligatorio.");
        return v.toString().trim();
    }

    private static long numero(Map<String, Object> d, String k) {
        return Long.parseLong(texto(d, k));
    }

    private static String nuloSiVacio(Object valor) {
        return valor == null || valor.toString().isBlank() ? null : valor.toString().trim();
    }
}
