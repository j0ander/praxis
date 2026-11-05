package ec.edu.praxis.application.empresa;

import ec.edu.praxis.application.excepcion.NoEncontradoException;
import ec.edu.praxis.domain.modelo.Empresa;
import ec.edu.praxis.domain.modelo.EmpresaUsuario;
import ec.edu.praxis.domain.modelo.Rol;
import ec.edu.praxis.domain.modelo.Usuario;
import ec.edu.praxis.domain.modelo.UsuarioRol;
import ec.edu.praxis.domain.repositorio.RepositorioEmpresa;
import ec.edu.praxis.domain.repositorio.RepositorioEmpresaUsuario;
import ec.edu.praxis.domain.repositorio.RepositorioRol;
import ec.edu.praxis.domain.repositorio.RepositorioUsuario;
import ec.edu.praxis.domain.repositorio.RepositorioUsuarioRol;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class GestionarRepresentanteEmpresaService {

    @Inject
    RepositorioEmpresaUsuario miembros;
    @Inject
    RepositorioEmpresa empresas;
    @Inject
    RepositorioUsuario usuarios;
    @Inject
    RepositorioRol roles;
    @Inject
    RepositorioUsuarioRol usuariosRoles;
    @Inject
    EntityManager entityManager;

    public List<Map<String, Object>> listar() {
        Map<Long, Empresa> empresasPorId = new LinkedHashMap<>();
        empresas.listarTodas().forEach(e -> empresasPorId.put(e.id, e));

        Map<Long, Usuario> usuariosPorId = new LinkedHashMap<>();
        usuarios.listarTodos().forEach(u -> usuariosPorId.put(u.id, u));

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (EmpresaUsuario miembro : miembros.listarTodas()) {
            Empresa empresa = empresasPorId.get(miembro.empresa_id);
            Usuario usuario = usuariosPorId.get(miembro.usuario_id);

            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("id", miembro.id);
            fila.put("empresa_id", miembro.empresa_id);
            fila.put("empresa", empresa == null ? "Empresa #" + miembro.empresa_id : empresa.razon_social);
            fila.put("usuario_id", miembro.usuario_id);
            fila.put("usuario", usuario == null ? "Usuario #" + miembro.usuario_id
                    : usuario.nombres + " " + usuario.apellidos);
            fila.put("correo", usuario == null ? null : usuario.correo);
            fila.put("rol_empresa", miembro.rol_empresa);
            fila.put("activo", miembro.activo);
            resultado.add(fila);
        }
        return resultado;
    }

    public Map<String, Object> opciones() {
        Rol rolRepresentante = roles.buscarPorCodigo("REPRESENTANTE_EMPRESA").orElse(null);
        List<Map<String, Object>> usuariosDisponibles = new ArrayList<>();

        if (rolRepresentante != null) {
            for (Usuario usuario : usuarios.listarTodos()) {
                if (!Boolean.TRUE.equals(usuario.activo)) {
                    continue;
                }
                boolean tieneRol = usuariosRoles.listarPorUsuario(usuario.id).stream()
                        .anyMatch(relacion -> relacion.rol_id.equals(rolRepresentante.id));
                if (tieneRol) {
                    Map<String, Object> fila = new LinkedHashMap<>();
                    fila.put("id", usuario.id);
                    fila.put("nombres", usuario.nombres);
                    fila.put("apellidos", usuario.apellidos);
                    fila.put("correo", usuario.correo);
                    usuariosDisponibles.add(fila);
                }
            }
        }

        List<Map<String, Object>> empresasDisponibles = empresas.listarTodas().stream()
                .filter(e -> Boolean.TRUE.equals(e.activa))
                .map(e -> {
                    Map<String, Object> fila = new LinkedHashMap<>();
                    fila.put("id", e.id);
                    fila.put("razon_social", e.razon_social);
                    fila.put("ruc", e.ruc);
                    return fila;
                }).toList();

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("empresas", empresasDisponibles);
        resultado.put("usuarios", usuariosDisponibles);
        return resultado;
    }

    @Transactional
    public long asignar(Map<String, Object> datos, long actor) {
        establecerActor(actor);

        long empresaId = numero(datos, "empresaId");
        long usuarioId = numero(datos, "usuarioId");
        String rolEmpresa = texto(datos, "rolEmpresa");

        if (!rolEmpresa.equals("ADMIN_EMPRESA") && !rolEmpresa.equals("PUBLICADOR")) {
            throw new IllegalArgumentException("El rol de empresa debe ser ADMIN_EMPRESA o PUBLICADOR.");
        }

        Empresa empresa = empresas.buscarPorId(empresaId)
                .orElseThrow(() -> new NoEncontradoException("Empresa no encontrada."));
        if (!Boolean.TRUE.equals(empresa.activa)) {
            throw new IllegalArgumentException("No se pueden asignar usuarios a una empresa inactiva.");
        }

        Usuario usuario = usuarios.buscarPorId(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("Usuario no encontrado."));
        if (!Boolean.TRUE.equals(usuario.activo)) {
            throw new IllegalArgumentException("El usuario debe estar activo.");
        }
        if (!tieneRolRepresentante(usuarioId)) {
            throw new IllegalArgumentException(
                    "El usuario debe tener asignado el rol global REPRESENTANTE_EMPRESA.");
        }

        EmpresaUsuario miembro = miembros.buscarPorEmpresaYUsuario(empresaId, usuarioId).orElse(null);
        validarAdministrador(empresaId, miembro, rolEmpresa);

        OffsetDateTime ahora = OffsetDateTime.now();
        if (miembro == null) {
            miembro = new EmpresaUsuario();
            miembro.empresa_id = empresaId;
            miembro.usuario_id = usuarioId;
            miembro.created_at = ahora;
        }
        miembro.rol_empresa = rolEmpresa;
        miembro.activo = true;
        miembro.updated_at = ahora;
        miembros.guardar(miembro);
        return miembro.id;
    }

    @Transactional
    public void cambiarEstado(long id, boolean activo, long actor) {
        establecerActor(actor);

        EmpresaUsuario miembro = miembros.buscarPorId(id)
                .orElseThrow(() -> new NoEncontradoException("Asignación de empresa no encontrada."));

        Empresa empresa = empresas.buscarPorId(miembro.empresa_id)
                .orElseThrow(() -> new NoEncontradoException("Empresa no encontrada."));

        if (activo && !Boolean.TRUE.equals(empresa.activa)) {
            throw new IllegalArgumentException("No se puede activar un representante en una empresa inactiva.");
        }

        if (activo) {
            validarAdministrador(miembro.empresa_id, miembro, miembro.rol_empresa);
        }

        if (!activo && Boolean.TRUE.equals(empresa.activa)
                && "ADMIN_EMPRESA".equals(miembro.rol_empresa)
                && !miembros.existeOtroAdministradorActivo(miembro.empresa_id, miembro.id)) {
            throw new IllegalArgumentException(
                    "No se puede desactivar al único ADMIN_EMPRESA activo de la empresa.");
        }

        miembro.activo = activo;
        miembro.updated_at = OffsetDateTime.now();
        miembros.guardar(miembro);
    }

    private void validarAdministrador(Long empresaId, EmpresaUsuario actual, String rolNuevo) {
        if (actual == null || !Boolean.TRUE.equals(actual.activo)) {
            if (!miembros.existeMiembroActivo(empresaId) && !rolNuevo.equals("ADMIN_EMPRESA")) {
                throw new IllegalArgumentException(
                        "El primer representante activo de una empresa debe ser ADMIN_EMPRESA.");
            }
        }

        if (actual != null && Boolean.TRUE.equals(actual.activo)
                && "ADMIN_EMPRESA".equals(actual.rol_empresa)
                && !rolNuevo.equals("ADMIN_EMPRESA")
                && !miembros.existeOtroAdministradorActivo(empresaId, actual.id)) {
            throw new IllegalArgumentException(
                    "La empresa debe conservar al menos un ADMIN_EMPRESA activo.");
        }
    }

    private boolean tieneRolRepresentante(long usuarioId) {
        Rol rol = roles.buscarPorCodigo("REPRESENTANTE_EMPRESA").orElse(null);
        if (rol == null) {
            return false;
        }
        return usuariosRoles.listarPorUsuario(usuarioId).stream()
                .map(relacion -> relacion.rol_id)
                .anyMatch(id -> id.equals(rol.id));
    }

    private void establecerActor(long actor) {
        entityManager.createNativeQuery("SELECT set_config('praxis.usuario_id', ?1, true)")
                .setParameter(1, Long.toString(actor))
                .getSingleResult();
    }

    private long numero(Map<String, Object> datos, String campo) {
        Object valor = datos == null ? null : datos.get(campo);
        if (valor == null || valor.toString().isBlank()) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio.");
        }
        return Long.parseLong(valor.toString());
    }

    private String texto(Map<String, Object> datos, String campo) {
        Object valor = datos == null ? null : datos.get(campo);
        if (valor == null || valor.toString().isBlank()) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio.");
        }
        return valor.toString().trim();
    }
}
