package ec.edu.praxis.application.empresa;

import ec.edu.praxis.application.excepcion.ConflictoException;
import ec.edu.praxis.application.excepcion.NoEncontradoException;
import ec.edu.praxis.domain.modelo.Auditoria;
import ec.edu.praxis.domain.modelo.Empresa;
import ec.edu.praxis.domain.repositorio.RepositorioAuditoria;
import ec.edu.praxis.domain.repositorio.RepositorioEmpresa;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class GestionarEmpresaService {

    @Inject
    RepositorioEmpresa empresas;
    @Inject
    RepositorioAuditoria auditoria;

    public List<Map<String, Object>> listarEmpresas() {
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Empresa empresa : empresas.listarTodas()) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("id", empresa.id);
            fila.put("ruc", empresa.ruc);
            fila.put("razon_social", empresa.razon_social);
            fila.put("nombre_comercial", empresa.nombre_comercial);
            fila.put("telefono", empresa.telefono);
            fila.put("correo", empresa.correo);
            fila.put("activa", empresa.activa);
            resultado.add(fila);
        }
        return resultado;
    }

    @Transactional
    public long crearEmpresa(Map<String, Object> datos, long actor) {
        String ruc = texto(datos, "ruc");
        if (!ruc.matches("[0-9]{13}")) {
            throw new IllegalArgumentException("El RUC debe contener exactamente 13 dígitos.");
        }
        if (empresas.existeRuc(ruc)) {
            throw new ConflictoException("Ya existe una empresa registrada con ese RUC.");
        }

        Empresa empresa = new Empresa();
        empresa.ruc = ruc;
        empresa.razon_social = texto(datos, "razonSocial");
        empresa.nombre_comercial = opcional(datos, "nombreComercial");
        empresa.direccion = opcional(datos, "direccion");
        empresa.telefono = opcional(datos, "telefono");
        empresa.correo = opcional(datos, "correo");
        empresa.activa = true;
        OffsetDateTime ahora = OffsetDateTime.now();
        empresa.created_at = ahora;
        empresa.updated_at = ahora;
        empresas.guardar(empresa);
        registrar(actor, "CREAR", empresa.id);
        return empresa.id;
    }

    @Transactional
    public void cambiarEstado(long id, boolean activa, long actor) {
        Empresa empresa = empresas.buscarPorId(id)
                .orElseThrow(() -> new NoEncontradoException("Empresa no encontrada."));
        empresa.activa = activa;
        empresa.updated_at = OffsetDateTime.now();
        empresas.guardar(empresa);
        registrar(actor, activa ? "ACTIVAR" : "DESACTIVAR", id);
    }

    private String texto(Map<String, Object> datos, String campo) {
        Object valor = datos.get(campo);
        String texto = valor == null ? "" : valor.toString().trim();
        if (texto.isEmpty()) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio.");
        }
        return texto;
    }

    private String opcional(Map<String, Object> datos, String campo) {
        Object valor = datos.get(campo);
        if (valor == null)
            return null;
        String texto = valor.toString().trim();
        return texto.isEmpty() ? null : texto;
    }

    private void registrar(long actor, String accion, long empresaId) {
        Auditoria registro = new Auditoria();
        registro.usuario_id = actor;
        registro.accion = accion;
        registro.entidad = "empresa";
        registro.entidad_id = empresaId;
        registro.detalle = Map.of("modulo", "gestion_empresas");
        registro.fecha = OffsetDateTime.now();
        auditoria.guardar(registro);
    }
}
