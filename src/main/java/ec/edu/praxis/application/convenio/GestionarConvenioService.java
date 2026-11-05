package ec.edu.praxis.application.convenio;

import ec.edu.praxis.application.excepcion.ConflictoException;
import ec.edu.praxis.application.excepcion.NoEncontradoException;
import ec.edu.praxis.application.excepcion.ReglaNegocioException;
import ec.edu.praxis.domain.modelo.Auditoria;
import ec.edu.praxis.domain.modelo.Convenio;
import ec.edu.praxis.domain.modelo.Empresa;
import ec.edu.praxis.domain.repositorio.RepositorioAuditoria;
import ec.edu.praxis.domain.repositorio.RepositorioConvenio;
import ec.edu.praxis.domain.repositorio.RepositorioEmpresa;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class GestionarConvenioService {

    private static final ZoneId ZONA_ECUADOR = ZoneId.of("America/Guayaquil");

    @Inject RepositorioConvenio convenios;
    @Inject RepositorioEmpresa empresas;
    @Inject RepositorioAuditoria auditoria;

    public List<Map<String, Object>> listarConvenios() {
        List<Map<String, Object>> resultado = new ArrayList<>();
        LocalDate hoy = LocalDate.now(ZONA_ECUADOR);

        for (Convenio convenio : convenios.listarTodos()) {
            String nombreEmpresa = empresas.buscarPorId(convenio.empresa_id)
                    .map(e -> e.razon_social).orElse("Empresa no disponible");
            String estadoEfectivo = "VIGENTE".equals(convenio.estado) && convenio.fecha_fin.isBefore(hoy)
                    ? "VENCIDO" : convenio.estado;

            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("id", convenio.id);
            fila.put("empresa_id", convenio.empresa_id);
            fila.put("empresa", nombreEmpresa);
            fila.put("numero", convenio.numero);
            fila.put("fecha_inicio", convenio.fecha_inicio);
            fila.put("fecha_fin", convenio.fecha_fin);
            fila.put("estado", convenio.estado);
            fila.put("estado_efectivo", estadoEfectivo);
            fila.put("documento_url", convenio.documento_url);
            resultado.add(fila);
        }
        return resultado;
    }

    @Transactional
    public long crearConvenio(Map<String, Object> datos, long actor) {
        long empresaId = numero(datos, "empresaId");
        Empresa empresa = empresas.buscarPorId(empresaId)
                .orElseThrow(() -> new NoEncontradoException("La empresa seleccionada no existe."));
        if (!Boolean.TRUE.equals(empresa.activa)) {
            throw new ReglaNegocioException("No se puede crear un convenio para una empresa inactiva.");
        }

        String numero = opcional(datos, "numero");
        if (numero != null && numero.length() > 50) {
            throw new IllegalArgumentException("El número del convenio no puede superar 50 caracteres.");
        }
        if (numero != null && convenios.existeNumero(numero)) {
            throw new ConflictoException("Ya existe un convenio con ese número.");
        }

        String documentoUrl = opcional(datos, "documentoUrl");
        if (documentoUrl != null && documentoUrl.length() > 500) {
            throw new IllegalArgumentException("El enlace del documento no puede superar 500 caracteres.");
        }

        LocalDate inicio = fecha(datos, "fechaInicio");
        LocalDate fin = fecha(datos, "fechaFin");
        if (fin.isBefore(inicio)) {
            throw new IllegalArgumentException("La fecha de finalización no puede ser anterior a la fecha de inicio.");
        }

        Convenio convenio = new Convenio();
        convenio.empresa_id = empresaId;
        convenio.numero = numero;
        convenio.fecha_inicio = inicio;
        convenio.fecha_fin = fin;
        convenio.estado = "VIGENTE";
        convenio.documento_url = documentoUrl;
        OffsetDateTime ahora = OffsetDateTime.now();
        convenio.created_at = ahora;
        convenio.updated_at = ahora;
        convenios.guardar(convenio);

        registrar(actor, "CREAR", convenio.id);
        return convenio.id;
    }

    @Transactional
    public void cambiarEstado(long id, String nuevoEstado, long actor) {
        Convenio convenio = convenios.buscarPorId(id)
                .orElseThrow(() -> new NoEncontradoException("Convenio no encontrado."));

        if (nuevoEstado == null || !List.of("VIGENTE", "SUSPENDIDO", "TERMINADO").contains(nuevoEstado)) {
            throw new IllegalArgumentException("El estado indicado no es válido.");
        }
        if ("TERMINADO".equals(convenio.estado)) {
            throw new ReglaNegocioException("Un convenio terminado no puede reactivarse ni modificarse.");
        }
        if ("VIGENTE".equals(nuevoEstado) && convenio.fecha_fin.isBefore(LocalDate.now(ZONA_ECUADOR))) {
            throw new ReglaNegocioException("No se puede reactivar un convenio cuya fecha de vigencia ya terminó.");
        }
        if (nuevoEstado.equals(convenio.estado)) {
            return;
        }

        convenio.estado = nuevoEstado;
        convenio.updated_at = OffsetDateTime.now();
        convenios.guardar(convenio);
        registrar(actor, nuevoEstado, id);
    }

    private long numero(Map<String, Object> datos, String campo) {
        Object valor = datos.get(campo);
        if (!(valor instanceof Number)) {
            throw new IllegalArgumentException("Debe seleccionar una empresa válida.");
        }
        return ((Number) valor).longValue();
    }

    private LocalDate fecha(Map<String, Object> datos, String campo) {
        String valor = requerido(datos, campo);
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("La fecha " + campo + " no tiene un formato válido (AAAA-MM-DD).");
        }
    }

    private String requerido(Map<String, Object> datos, String campo) {
        String valor = opcional(datos, campo);
        if (valor == null) throw new IllegalArgumentException("El campo " + campo + " es obligatorio.");
        return valor;
    }

    private String opcional(Map<String, Object> datos, String campo) {
        Object valor = datos.get(campo);
        if (valor == null) return null;
        String texto = valor.toString().trim();
        return texto.isEmpty() ? null : texto;
    }

    private void registrar(long actor, String accion, long convenioId) {
        Auditoria registro = new Auditoria();
        registro.usuario_id = actor;
        registro.accion = accion;
        registro.entidad = "convenio";
        registro.entidad_id = convenioId;
        registro.detalle = Map.of("modulo", "gestion_convenios");
        registro.fecha = OffsetDateTime.now();
        auditoria.guardar(registro);
    }
}
