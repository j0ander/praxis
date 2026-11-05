import { useCallback, useEffect, useState } from 'react';
import './style.css';

/* =========================================================
   CONFIGURACIÓN
========================================================= */

const RUTAS = {
  usuarios: '/admin/usuarios',
  facultades: '/admin/facultades',
  carreras: '/admin/carreras',
  periodos: '/admin/periodos',
  auditoria: '/admin/auditoria?limite=100',
  empresas: '/admin/empresas',
  convenios: '/admin/convenios',
  estudiantes: '/admin/estudiantes',
  representantes: '/admin/representantes',
  ofertas: '/empresa/ofertas',
  revisionOfertas: '/coordinacion/ofertas/pendientes',
  revisionPostulaciones: '/coordinacion/postulaciones/pendientes',
  asignacionDocente: '/coordinacion/practicas/pendientes-docente',
  ofertasEstudiante: '/estudiante/ofertas',
};

const COLUMNAS = {
  usuarios: [
    'ID',
    'Correo',
    'Nombres',
    'Apellidos',
    'Activo',
    'Roles',
  ],
  facultades: [
    'ID',
    'Código',
    'Nombre',
    'Activa',
  ],
  carreras: [
    'ID',
    'Facultad',
    'Código',
    'Nombre',
    'Activa',
  ],
  periodos: [
    'ID',
    'Código',
    'Nombre',
    'Inicio',
    'Fin',
    'Activo',
  ],
  auditoria: [
    'ID',
    'Usuario',
    'Acción',
    'Entidad',
    'Fecha',
  ],
  empresas: ['ID', 'RUC', 'Razón social', 'Nombre comercial', 'Teléfono', 'Correo', 'Activa'],
  convenios: ['ID', 'Empresa', 'Número', 'Fecha inicio', 'Fecha fin', 'Estado'],
  estudiantes: ['ID', 'Estudiante', 'Correo', 'Carrera', 'Código'],
  representantes: ['ID', 'Empresa', 'Representante', 'Correo', 'Permiso', 'Activo'],
  ofertas: ['ID', 'Empresa', 'Título', 'Convenio', 'Tipo de práctica', 'Periodo', 'Cierre', 'Estado', 'Motivo de decisión'],
  revisionOfertas: ['ID', 'Empresa', 'Oferta', 'Convenio', 'Tipo de práctica', 'Periodo', 'Cierre', 'Perfiles'],
  revisionPostulaciones: ['ID', 'Estudiante', 'Correo', 'Oferta', 'Empresa', 'Perfil', 'Fecha', 'Recomendación', 'Plazas disponibles'],
};

/* =========================================================
   FUNCIONES GENERALES
========================================================= */

const obtenerSalud = async () => {
  const respuesta = await fetch('/api/salud');

  if (!respuesta.ok) {
    throw new Error('No se pudo consultar el estado del servidor.');
  }

  return respuesta.json();
};

/* =========================================================
   COMPONENTE PRINCIPAL
========================================================= */

export default function App({ obtener }) {
  // Conservamos la inyección del Incremento 0 para sus pruebas.
  if (obtener) {
    return <EstadoTecnico obtener={obtener} />;
  }

  return <Aplicacion />;
}

/* =========================================================
   ESTADO TÉCNICO DEL SERVIDOR
========================================================= */

function EstadoTecnico({ obtener }) {
  const [estado, setEstado] = useState(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    obtener()
      .then(setEstado)
      .catch(() => setError(true));
  }, [obtener]);

  return (
    <main className="technical">
      <h1>PRAXIS</h1>

      <p>Sistema de prácticas preprofesionales</p>

      {error && (
        <p role="alert">
          No se pudo conectar con el servidor.
        </p>
      )}

      {estado && (
        <p data-testid="estado">
          Servidor: {estado.baseDatos} · esquema v{estado.versionEsquema}
        </p>
      )}
    </main>
  );
}

/* =========================================================
   APLICACIÓN Y SESIÓN
========================================================= */

function Aplicacion() {
  const [sesion, setSesion] = useState(() => {
    try {
      return JSON.parse(sessionStorage.getItem('praxisSesion'));
    } catch {
      return null;
    }
  });

  const [vista, setVista] = useState(() => {
    try {
      const guardada = JSON.parse(sessionStorage.getItem('praxisSesion'));
      const roles = guardada?.roles || [];
      if (roles.includes('ESTUDIANTE') && !roles.includes('COORDINADOR') && !roles.includes('ADMINISTRADOR')) {
        return 'ofertasEstudiante';
      }
    } catch {
      // Si no existe una sesión válida, se muestra la vista administrativa inicial.
    }
    return 'usuarios';
  });
  const [error, setError] = useState('');

  /*
   * Función central para realizar peticiones al backend.
   * Agrega automáticamente el token de autenticación.
   */
  const api = useCallback(
    async (ruta, opciones = {}) => {
      const respuesta = await fetch(`/api${ruta}`, {
        ...opciones,

        headers: {
          'Content-Type': 'application/json',

          ...(sesion?.token
            ? { Authorization: `Bearer ${sesion.token}` }
            : {}),

          ...opciones.headers,
        },
      });

      const datos = await respuesta.json().catch(() => ({}));

      if (!respuesta.ok) {
        throw new Error(
          datos?.mensaje || 'No se pudo completar la operación.'
        );
      }

      return datos;
    },
    [sesion?.token]
  );

  /* Iniciar sesión */
  const entrar = async (correo, password) => {
    const datos = await api('/auth/login', {
      method: 'POST',
      body: JSON.stringify({
        correo,
        password,
      }),
    });

    sessionStorage.setItem('praxisSesion', JSON.stringify(datos));

    setSesion(datos);
    if (datos.roles?.includes('ESTUDIANTE')
      && !datos.roles.includes('COORDINADOR')
      && !datos.roles.includes('ADMINISTRADOR')) {
      setVista('ofertasEstudiante');
    } else {
      setVista('usuarios');
    }
    setError('');
  };

  /* Cerrar sesión */
  const salir = () => {
    sessionStorage.removeItem('praxisSesion');

    setSesion(null);
    setVista('usuarios');
    setError('');
  };

  const esCoordinador = sesion?.roles?.includes('COORDINADOR');
  const esEstudiante = sesion?.roles?.includes('ESTUDIANTE');
  const esAdministrador = sesion?.roles?.includes('ADMINISTRADOR');

  /* Mostrar inicio de sesión */
  if (!sesion) {
    return (
      <Login
        entrar={entrar}
        error={error}
        setError={setError}
      />
    );
  }

  /* Obligar al usuario a cambiar su contraseña temporal */
  if (sesion.debeCambiarPassword) {
    return (
      <CambiarPassword
        api={api}
        salir={salir}
      />
    );
  }

  return (
    <main className="layout">

      {/* MENÚ LATERAL */}
      <aside>
        <div className="marca">
          PRAXIS
          <span>Gestión de prácticas</span>
        </div>

        <nav>
          {Object.entries({
            usuarios: 'Usuarios',
            facultades: 'Facultades',
            carreras: 'Carreras',
            periodos: 'Periodos',
            auditoria: 'Auditoría',
            empresas: 'Empresas',
            convenios: 'Convenios',
            estudiantes: 'Estudiantes',
            representantes: 'Representantes',
            ofertas: 'Ofertas de prácticas',
            revisionOfertas: 'Revisión de ofertas',
            revisionPostulaciones: 'Revisión de postulaciones',
            asignacionDocente: 'Asignación de docentes',
            ofertasEstudiante: 'Ofertas disponibles',
          }).filter(([id]) => {
            if (esEstudiante && !esCoordinador && !esAdministrador) return id === 'ofertasEstudiante';
            if (id === 'revisionOfertas' || id === 'revisionPostulaciones' || id === 'asignacionDocente') return esCoordinador;
            if (id === 'ofertasEstudiante') return esEstudiante;
            return true;
          }).map(([id, nombre]) => (
            <button
              key={id}
              className={vista === id ? 'seleccionado' : ''}
              data-vista={id}
              onClick={() => {
                setVista(id);
                setError('');
              }}
            >
              {nombre}
            </button>
          ))}
        </nav>

        <button
          className="salir"
          onClick={salir}
        >
          Cerrar sesión
        </button>
      </aside>

      {/* CONTENIDO PRINCIPAL */}
      <section className="contenido">

        <header>
          <div>
            <small>
              {vista === 'revisionOfertas' || vista === 'revisionPostulaciones' || vista === 'asignacionDocente' ? 'COORDINACIÓN' : vista === 'ofertasEstudiante' ? 'ESTUDIANTE' : 'ADMINISTRACIÓN'}
            </small>

            <h1>
              {vista === 'revisionOfertas' ? 'Revisión de ofertas' : vista === 'revisionPostulaciones' ? 'Revisión de postulaciones' : vista === 'asignacionDocente' ? 'Asignación de docentes' : vista === 'ofertasEstudiante' ? 'Ofertas disponibles' : vista.charAt(0).toUpperCase() + vista.slice(1)}
            </h1>
          </div>

          <span className="usuario-sesion">
            <span className="usuario-avatar" aria-hidden="true">
              {(sesion.nombres || 'U').charAt(0).toUpperCase()}
            </span>
            <span className="usuario-datos">
              <strong>{sesion.nombres} {sesion.apellidos}</strong>
              <small>{(sesion.roles || []).join(' · ')}</small>
            </span>
          </span>
        </header>

        {error && (
          <p className="error" role="alert">
            {error}
          </p>
        )}

        <Panel
          vista={vista}
          api={api}
          setError={setError}
          esCoordinador={esCoordinador}
          esEstudiante={esEstudiante}
        />

      </section>
    </main>
  );
}

/* =========================================================
   INICIO DE SESIÓN
========================================================= */

function Login({ entrar, error, setError }) {
  const [correo, setCorreo] = useState('');
  const [password, setPassword] = useState('');
  const [cargando, setCargando] = useState(false);

  const enviar = async (e) => {
    e.preventDefault();

    setCargando(true);
    setError('');

    try {
      await entrar(correo, password);
    } catch (err) {
      setError(err.message);
    } finally {
      setCargando(false);
    }
  };

  return (
    <main className="login-fondo">
      <form
        className="login"
        onSubmit={enviar}
      >
        <div className="marca oscuro">
          PRAXIS
          <span>Sistema de prácticas preprofesionales</span>
        </div>

        <h1>Iniciar sesión</h1>

        <p>
          Ingresa con tus credenciales institucionales.
        </p>

        <label>
          Correo electrónico

          <input
            type="email"
            autoComplete="username"
            required
            value={correo}
            onChange={(e) => setCorreo(e.target.value)}
          />
        </label>

        <label>
          Contraseña

          <input
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </label>

        {error && (
          <p className="error" role="alert">
            {error}
          </p>
        )}

        <button
          className="primario"
          type="submit"
          disabled={cargando}
        >
          {cargando ? 'Ingresando…' : 'Ingresar'}
        </button>
      </form>
    </main>
  );
}

/* =========================================================
   PANEL DE ADMINISTRACIÓN
========================================================= */

function Panel({ vista, api, setError, esCoordinador, esEstudiante }) {
  const [datos, setDatos] = useState([]);
  const [roles, setRoles] = useState([]);
  const [carrerasDisponibles, setCarrerasDisponibles] = useState([]);
  const [facultadesDisponibles, setFacultadesDisponibles] = useState([]);
  const [empresasDisponibles, setEmpresasDisponibles] = useState([]);
  const [estudiantesDisponibles, setEstudiantesDisponibles] = useState([]);
  const [representantesDisponibles, setRepresentantesDisponibles] = useState([]);
  const [conveniosOfertaDisponibles, setConveniosOfertaDisponibles] = useState([]);
  const [tiposPracticaDisponibles, setTiposPracticaDisponibles] = useState([]);
  const [periodosOfertaDisponibles, setPeriodosOfertaDisponibles] = useState([]);
  const [usuarioEditando, setUsuarioEditando] = useState(null);
  const [carrerasElegidas, setCarrerasElegidas] = useState([]);
  const [alcanceGlobal, setAlcanceGlobal] = useState(false);
  const [ofertaDetalle, setOfertaDetalle] = useState(null);
  const [postulacionDetalle, setPostulacionDetalle] = useState(null);
  const [motivoRechazoPostulacion, setMotivoRechazoPostulacion] = useState('');
  const [ofertaDetalleEstudiante, setOfertaDetalleEstudiante] = useState(null);
  const [misPostulaciones, setMisPostulaciones] = useState([]);
  const [mensajePostulacion, setMensajePostulacion] = useState('');
  const [postulandoPerfilId, setPostulandoPerfilId] = useState(null);
  const [motivoRechazo, setMotivoRechazo] = useState('');
  const [ofertaEditandoId, setOfertaEditandoId] = useState(null);

  const [form, setForm] = useState({});
  const [rolesElegidos, setRolesElegidos] = useState([]);

  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);

  const rutaActual = RUTAS[vista];

  /* -------------------------------------------------------
     CARGAR INFORMACIÓN
  ------------------------------------------------------- */

  const cargar = useCallback(async () => {
    setCargando(true);

    try {
      const respuesta = await api(rutaActual);

      // Evitamos que una respuesta inesperada rompa la tabla.
      setDatos(
        Array.isArray(respuesta) ? respuesta : []
      );
    } catch (err) {
      setDatos([]);
      setError(err.message);
    } finally {
      setCargando(false);
    }
  }, [api, rutaActual, setError]);

  useEffect(() => {
    setForm({});
    setRolesElegidos([]);
    setOfertaDetalle(null);
    setPostulacionDetalle(null);
    setMotivoRechazoPostulacion('');
    setOfertaDetalleEstudiante(null);
    setMensajePostulacion('');
    setMotivoRechazo('');
    setOfertaEditandoId(null);
    setError('');

    cargar();

    if (vista === 'ofertasEstudiante') {
      api('/estudiante/postulaciones')
        .then((respuesta) => setMisPostulaciones(Array.isArray(respuesta) ? respuesta : []))
        .catch((err) => {
          setMisPostulaciones([]);
          setError(err.message);
        });
    } else {
      setMisPostulaciones([]);
    }

    if (vista === 'usuarios') {
      Promise.all([api('/admin/roles'), api('/admin/carreras')])
        .then(([listaRoles, listaCarreras]) => {
          setRoles(Array.isArray(listaRoles) ? listaRoles : []);
          setCarrerasDisponibles(Array.isArray(listaCarreras) ? listaCarreras : []);
        })
        .catch((err) => {
          setRoles([]);
          setCarrerasDisponibles([]);
          setError(err.message);
        });
    } else {
      setRoles([]);
      setUsuarioEditando(null);
    }

    if (vista === 'carreras') {
      api('/admin/facultades')
        .then((respuesta) => setFacultadesDisponibles(Array.isArray(respuesta) ? respuesta : []))
        .catch((err) => setError(err.message));
    }

    if (vista === 'convenios') {
      api('/admin/empresas')
        .then((respuesta) => setEmpresasDisponibles(Array.isArray(respuesta) ? respuesta : []))
        .catch((err) => setError(err.message));
    }

    if (vista === 'estudiantes') {
      api('/admin/estudiantes/opciones')
        .then((respuesta) => {
          setEstudiantesDisponibles(Array.isArray(respuesta.usuarios) ? respuesta.usuarios : []);
          setCarrerasDisponibles(Array.isArray(respuesta.carreras) ? respuesta.carreras : []);
        })
        .catch((err) => {
          setEstudiantesDisponibles([]);
          setCarrerasDisponibles([]);
          setError(err.message);
        });
    }

    if (vista === 'representantes') {
      api('/admin/representantes/opciones')
        .then((respuesta) => {
          setEmpresasDisponibles(Array.isArray(respuesta.empresas) ? respuesta.empresas : []);
          setRepresentantesDisponibles(Array.isArray(respuesta.usuarios) ? respuesta.usuarios : []);
        })
        .catch((err) => {
          setEmpresasDisponibles([]);
          setRepresentantesDisponibles([]);
          setError(err.message);
        });
    }

    if (vista === 'ofertas') {
      api('/empresa/ofertas/opciones')
        .then((respuesta) => {
          setEmpresasDisponibles(Array.isArray(respuesta.empresas) ? respuesta.empresas : []);
          setConveniosOfertaDisponibles(Array.isArray(respuesta.convenios) ? respuesta.convenios : []);
          setTiposPracticaDisponibles(Array.isArray(respuesta.tiposPractica) ? respuesta.tiposPractica : []);
          setPeriodosOfertaDisponibles(Array.isArray(respuesta.periodos) ? respuesta.periodos : []);
        })
        .catch((err) => {
          setEmpresasDisponibles([]);
          setConveniosOfertaDisponibles([]);
          setTiposPracticaDisponibles([]);
          setPeriodosOfertaDisponibles([]);
          setError(err.message);
        });
    }
  }, [vista, cargar, api, setError]);

  /* -------------------------------------------------------
     CAMPOS DINÁMICOS DE LOS FORMULARIOS
  ------------------------------------------------------- */

  const campo = (nombre, placeholder, tipo = 'text', requerido = true) => {
    const autocompletado = {
      email: 'email',
      password: 'new-password',
      text: 'off',
      number: 'off',
      date: 'off',
    };

    return (
      <input
        key={nombre}
        required={requerido}
        type={tipo}
        autoComplete={autocompletado[tipo] || 'off'}
        placeholder={placeholder}
        value={form[nombre] || ''}
        onChange={(e) => {
          setForm((anterior) => ({
            ...anterior,
            [nombre]: e.target.value,
          }));
        }}
      />
    );
  };

  /* -------------------------------------------------------
     CREAR REGISTROS
  ------------------------------------------------------- */

  const enviar = async (e) => {
    e.preventDefault();

    setGuardando(true);
    setError('');

    try {
      if (vista === 'usuarios') {
        const creado = await api('/admin/usuarios', {
          method: 'POST',
          body: JSON.stringify(form),
        });

        if (rolesElegidos.length > 0) {
          await api(`/admin/usuarios/${creado.id}/roles`, {
            method: 'PUT',
            body: JSON.stringify({
              roles: rolesElegidos,
            }),
          });
        }
      }

      if (vista === 'facultades') {
        await api('/admin/facultades', {
          method: 'POST',
          body: JSON.stringify(form),
        });
      }

      if (vista === 'carreras') {
        await api('/admin/carreras', {
          method: 'POST',
          body: JSON.stringify({
            ...form,
            facultadId: Number(form.facultadId),
          }),
        });
      }

      if (vista === 'periodos') {
        await api('/admin/periodos', {
          method: 'POST',
          body: JSON.stringify(form),
        });
      }

      if (vista === 'empresas') {
        await api('/admin/empresas', {
          method: 'POST',
          body: JSON.stringify(form),
        });
      }

      if (vista === 'convenios') {
        await api('/admin/convenios', {
          method: 'POST',
          body: JSON.stringify({
            ...form,
            empresaId: Number(form.empresaId),
          }),
        });
      }

      if (vista === 'estudiantes') {
        await api('/admin/estudiantes', {
          method: 'POST',
          body: JSON.stringify({
            ...form,
            usuarioId: Number(form.usuarioId),
            carreraId: Number(form.carreraId),
          }),
        });
      }

      if (vista === 'representantes') {
        await api('/admin/representantes', {
          method: 'POST',
          body: JSON.stringify({
            ...form,
            empresaId: Number(form.empresaId),
            usuarioId: Number(form.usuarioId),
          }),
        });
      }

      if (vista === 'ofertas') {
        const datosOferta = {
          ...form,
          empresaId: Number(form.empresaId),
          convenioId: Number(form.convenioId),
          tipoPracticaId: Number(form.tipoPracticaId),
          periodoId: Number(form.periodoId),
          plazas: Number(form.plazas),
          requisitos: (form.requisitosTexto || '')
            .split(/\r?\n/)
            .map((texto) => texto.trim())
            .filter(Boolean),
        };
        delete datosOferta.requisitosTexto;

        await api(ofertaEditandoId ? `/empresa/ofertas/${ofertaEditandoId}` : '/empresa/ofertas', {
          method: ofertaEditandoId ? 'PUT' : 'POST',
          body: JSON.stringify(datosOferta),
        });
      }

      setForm({});
      setOfertaEditandoId(null);
      setRolesElegidos([]);

      await cargar();
    } catch (err) {
      setError(err.message);
    } finally {
      setGuardando(false);
    }
  };

  /* -------------------------------------------------------
     DESACTIVAR REGISTROS
  ------------------------------------------------------- */

  const desactivar = async (id) => {
    try {
      await api(
        `/admin/${vista}/${id}${vista === 'usuarios' ? '/estado' : ''}`,
        {
          method: 'PATCH',
          body: JSON.stringify({
            activo: false,
          }),
        }
      );

      await cargar();
    } catch (err) {
      setError(err.message);
    }
  };

  /* -------------------------------------------------------
     ACTIVAR / DESACTIVAR USUARIOS
  ------------------------------------------------------- */

  const cambiarEstadoUsuario = async (usuario) => {
    try {
      await api(`/admin/usuarios/${usuario.id}/estado`, {
        method: 'PATCH',
        body: JSON.stringify({ activo: !usuario.activo }),
      });
      await cargar();
    } catch (err) {
      setError(err.message);
    }
  };

  const editarCarrerasUsuario = (usuario) => {
    setUsuarioEditando(usuario);
    setCarrerasElegidas((usuario.carreras || []).map(String));
    setAlcanceGlobal(Boolean(usuario.alcance_global));
    setError('');
  };

  const guardarCarrerasUsuario = async (e) => {
    e.preventDefault();
    if (!usuarioEditando) return;
    try {
      await api(`/admin/usuarios/${usuarioEditando.id}/carreras`, {
        method: 'PUT',
        body: JSON.stringify({
          carreras: carrerasElegidas.map(Number),
          alcanceGlobal,
        }),
      });
      setUsuarioEditando(null);
      await cargar();
    } catch (err) {
      setError(err.message);
    }
  };

  /* -------------------------------------------------------
     ACTIVAR PERIODOS
  ------------------------------------------------------- */

  const cambiarEstadoConvenio = async (convenio, estado) => {
    try {
      await api(`/admin/convenios/${convenio.id}/estado`, {
        method: 'PATCH',
        body: JSON.stringify({ estado }),
      });
      await cargar();
    } catch (err) {
      setError(err.message);
    }
  };

  const cambiarEstadoEmpresa = async (empresa) => {
    try {
      await api(`/admin/empresas/${empresa.id}/estado`, {
        method: 'PATCH',
        body: JSON.stringify({ activa: !empresa.activa }),
      });
      await cargar();
    } catch (err) {
      setError(err.message);
    }
  };

  const cambiarEstadoRepresentante = async (representante) => {
    try {
      await api(`/admin/representantes/${representante.id}/estado`, {
        method: 'PATCH',
        body: JSON.stringify({ activo: !representante.activo }),
      });
      await cargar();
    } catch (err) {
      setError(err.message);
    }
  };

  const editarOferta = (oferta) => {
    const perfil = oferta.perfiles?.[0] || {};
    setOfertaEditandoId(oferta.id);
    setForm({
      empresaId: String(oferta.empresa_id || ''),
      convenioId: String(oferta.convenio_id || ''),
      tipoPracticaId: String(oferta.tipo_practica_id || ''),
      periodoId: String(oferta.periodo_id || ''),
      titulo: oferta.titulo || '',
      descripcion: oferta.descripcion || '',
      fechaCierre: oferta.fecha_cierre || '',
      fechaInicioActividades: oferta.fecha_inicio_actividades || '',
      fechaFinActividades: oferta.fecha_fin_actividades || '',
      perfilNombre: perfil.nombre || '',
      perfilDescripcion: perfil.descripcion || '',
      plazas: String(perfil.plazas || ''),
      requisitosTexto: (perfil.requisitos || []).join('\n'),
    });
    setError('');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const cancelarEdicionOferta = () => {
    setOfertaEditandoId(null);
    setForm({});
  };

  const enviarOfertaRevision = async (oferta) => {
    try {
      await api(`/empresa/ofertas/${oferta.id}/enviar-revision`, {
        method: 'PATCH',
      });
      await cargar();
    } catch (err) {
      setError(err.message);
    }
  };

  const abrirDetalleOferta = async (oferta) => {
    try {
      setError('');
      setMotivoRechazo('');
      const detalle = await api(`/coordinacion/ofertas/${oferta.id}`);
      setOfertaDetalle(detalle);
    } catch (err) {
      setError(err.message);
    }
  };

  const aprobarOferta = async () => {
    if (!ofertaDetalle) return;
    try {
      await api(`/coordinacion/ofertas/${ofertaDetalle.id}/aprobar`, { method: 'PATCH' });
      setOfertaDetalle(null);
      await cargar();
    } catch (err) {
      setError(err.message);
    }
  };

  const rechazarOferta = async (e) => {
    e.preventDefault();
    if (!ofertaDetalle) return;
    try {
      await api(`/coordinacion/ofertas/${ofertaDetalle.id}/rechazar`, {
        method: 'PATCH',
        body: JSON.stringify({ motivo: motivoRechazo }),
      });
      setOfertaDetalle(null);
      setMotivoRechazo('');
      await cargar();
    } catch (err) {
      setError(err.message);
    }
  };

  const abrirDetallePostulacion = async (postulacion) => {
    try {
      setError('');
      setMotivoRechazoPostulacion('');
      const detalle = await api(`/coordinacion/postulaciones/${postulacion.id}`);
      setPostulacionDetalle(detalle);
    } catch (err) {
      setError(err.message);
    }
  };

  const aceptarPostulacion = async () => {
    if (!postulacionDetalle) return;
    try {
      await api(`/coordinacion/postulaciones/${postulacionDetalle.id}/aceptar`, { method: 'PATCH' });
      setPostulacionDetalle(null);
      await cargar();
    } catch (err) {
      setError(err.message);
    }
  };

  const rechazarPostulacion = async (e) => {
    e.preventDefault();
    if (!postulacionDetalle) return;
    try {
      await api(`/coordinacion/postulaciones/${postulacionDetalle.id}/rechazar`, {
        method: 'PATCH',
        body: JSON.stringify({ motivo: motivoRechazoPostulacion }),
      });
      setPostulacionDetalle(null);
      setMotivoRechazoPostulacion('');
      await cargar();
    } catch (err) {
      setError(err.message);
    }
  };

  const abrirDetalleOfertaEstudiante = async (oferta) => {
    try {
      setError('');
      const detalle = await api(`/estudiante/ofertas/${oferta.id}`);
      setOfertaDetalleEstudiante(detalle);
    } catch (err) {
      setError(err.message);
    }
  };

  const postularse = async (perfilId) => {
    try {
      setError('');
      setPostulandoPerfilId(perfilId);
      await api('/estudiante/postulaciones', {
        method: 'POST',
        body: JSON.stringify({ perfilId, mensaje: mensajePostulacion }),
      });
      setMensajePostulacion('');
      const [detalle, lista] = await Promise.all([
        api(`/estudiante/ofertas/${ofertaDetalleEstudiante.id}`),
        api('/estudiante/postulaciones'),
      ]);
      setOfertaDetalleEstudiante(detalle);
      setMisPostulaciones(Array.isArray(lista) ? lista : []);
      await cargar();
    } catch (err) {
      setError(err.message);
    } finally {
      setPostulandoPerfilId(null);
    }
  };

  const activar = async (id) => {
    try {
      await api(`/admin/periodos/${id}/activar`, {
        method: 'POST',
      });

      await cargar();
    } catch (err) {
      setError(err.message);
    }
  };

  /* -------------------------------------------------------
     DATOS QUE SE MOSTRARÁN EN CADA FILA
  ------------------------------------------------------- */

  const obtenerCeldas = (registro) => {
    switch (vista) {
      case 'usuarios':
        return [
          registro.id,
          registro.correo,
          registro.nombres,
          registro.apellidos,
          registro.activo ? 'Sí' : 'No',
          registro.roles?.join(', ') || 'Sin roles',
        ];

      case 'facultades':
        return [
          registro.id,
          registro.codigo,
          registro.nombre,
          registro.activa ? 'Sí' : 'No',
        ];

      case 'carreras':
        return [
          registro.id,
          registro.facultad || registro.facultad_id,
          registro.codigo,
          registro.nombre,
          registro.activa ? 'Sí' : 'No',
        ];

      case 'periodos':
        return [
          registro.id,
          registro.codigo,
          registro.nombre,
          registro.fecha_inicio,
          registro.fecha_fin,
          registro.activo ? 'Sí' : 'No',
        ];

      case 'auditoria':
        return [
          registro.id,
          registro.correo ||
          (registro.usuario_id
            ? `Usuario #${registro.usuario_id}`
            : 'Sistema'),
          registro.accion,
          registro.entidad,
          registro.fecha,
        ];

      case 'empresas':
        return [registro.id, registro.ruc, registro.razon_social, registro.nombre_comercial, registro.telefono, registro.correo, registro.activa ? 'Sí' : 'No'];

      case 'convenios':
        return [registro.id, registro.empresa, registro.numero || '—', registro.fecha_inicio, registro.fecha_fin, registro.estado_efectivo];

      case 'estudiantes':
        return [registro.id, registro.estudiante, registro.correo, registro.carrera, registro.codigo_estudiante || '—'];

      case 'representantes':
        return [
          registro.id,
          registro.empresa,
          registro.usuario,
          registro.correo,
          registro.rol_empresa,
          registro.activo ? 'Sí' : 'No',
        ];

      case 'ofertas':
        return [
          registro.id,
          registro.empresa,
          registro.titulo,
          registro.convenio || '—',
          registro.tipo_practica || '—',
          registro.periodo || '—',
          registro.fecha_cierre,
          registro.estado,
          registro.motivo_decision || '—',
        ];

      case 'revisionPostulaciones':
        return [
          registro.id, registro.estudiante, registro.correo_estudiante, registro.oferta,
          registro.empresa, registro.perfil, registro.fecha_postulacion ? new Date(registro.fecha_postulacion).toLocaleDateString('es-EC') : '—',
          registro.recomendacion_empresa || 'SIN_EVALUAR', registro.plazas_disponibles,
        ];

      case 'revisionOfertas':
        return [
          registro.id,
          registro.empresa,
          registro.titulo,
          registro.convenio || '—',
          registro.tipo_practica || '—',
          registro.periodo || '—',
          registro.fecha_cierre || '—',
          (registro.perfiles || []).map((p) => `${p.nombre} (${p.plazas} plazas)`).join(', ') || '—',
        ];

      default:
        return [];
    }
  };

  /* -------------------------------------------------------
     TÍTULO DEL FORMULARIO
  ------------------------------------------------------- */

  const titulosFormulario = {
    usuarios: 'Crear usuario',
    facultades: 'Crear facultad',
    carreras: 'Crear carrera',
    periodos: 'Crear periodo',
    empresas: 'Registrar empresa',
    convenios: 'Registrar convenio',
    estudiantes: 'Registrar perfil de estudiante',
    representantes: 'Asignar representante a empresa',
    ofertas: 'Crear oferta de prácticas',
    revisionOfertas: 'Revisión de ofertas',
    revisionPostulaciones: 'Revisión de postulaciones',
  };

  /* -------------------------------------------------------
     INTERFAZ DEL PANEL
  ------------------------------------------------------- */

  if (vista === 'asignacionDocente') {
    return <AsignacionDocentePanel api={api} setError={setError} />;
  }

  return (
    <>
      {/* CONSULTA DE OFERTAS PARA ESTUDIANTES */}
      {vista === 'ofertasEstudiante' ? (
        <div className="ofertas-estudiante">
          <div className="panel ofertas-intro">
            <h2>Convocatorias publicadas</h2>
            <p>Consulta las oportunidades aprobadas por la coordinación. Solo se muestran ofertas publicadas.</p>
          </div>

          <section className="panel mis-postulaciones">
            <h2>Mis postulaciones</h2>
            {misPostulaciones.length === 0 ? (
              <p>Aún no has enviado postulaciones.</p>
            ) : (
              <div className="tabla-postulaciones-contenedor">
                <table className="tabla-postulaciones">
                  <thead>
                    <tr><th>Oferta</th><th>Empresa</th><th>Perfil</th><th>Fecha</th><th>Estado</th><th>Observación</th></tr>
                  </thead>
                  <tbody>
                    {misPostulaciones.map((postulacion) => (
                      <tr key={postulacion.id}>
                        <td>{postulacion.oferta}</td>
                        <td>{postulacion.empresa}</td>
                        <td>{postulacion.perfil}</td>
                        <td>{postulacion.fecha_postulacion ? new Date(postulacion.fecha_postulacion).toLocaleDateString('es-EC') : '—'}</td>
                        <td><span className={`estado-postulacion estado-${String(postulacion.estado || '').toLowerCase()}`}>{postulacion.estado}</span></td>
                        <td>{postulacion.motivo_decision || postulacion.observacion_empresa || '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          {cargando ? (
            <div className="panel"><p>Cargando ofertas...</p></div>
          ) : datos.length === 0 ? (
            <div className="panel"><p>Actualmente no hay ofertas publicadas.</p></div>
          ) : (
            <div className="ofertas-estudiante-grid">
              {datos.map((oferta) => (
                <article className="oferta-estudiante-card" key={oferta.id}>
                  <div className="oferta-estudiante-card-cabecera">
                    <span className="oferta-etiqueta">Oferta #{oferta.id}</span>
                    <span className={oferta.plazo_vigente ? 'estado-plazo vigente' : 'estado-plazo cerrado'}>
                      {oferta.plazo_vigente ? 'Postulaciones abiertas' : 'Plazo cerrado'}
                    </span>
                  </div>
                  <h2>{oferta.titulo}</h2>
                  <p className="oferta-empresa">{oferta.empresa_nombre_comercial || oferta.empresa}</p>
                  <p>{oferta.descripcion || 'La empresa no agregó una descripción.'}</p>
                  <div className="oferta-estudiante-datos">
                    <span><strong>Tipo:</strong> {oferta.tipo_practica || '—'}</span>
                    <span><strong>Periodo:</strong> {oferta.periodo || '—'}</span>
                    <span><strong>Cierre:</strong> {oferta.fecha_cierre || '—'}</span>
                    <span><strong>Perfiles:</strong> {(oferta.perfiles || []).length}</span>
                  </div>
                  <p className={oferta.tiene_perfil_compatible ? 'compatibilidad compatible' : 'compatibilidad no-compatible'}>
                    {oferta.tiene_perfil_compatible
                      ? 'Tiene al menos un perfil dirigido a tu carrera.'
                      : 'No tiene perfiles dirigidos a tu carrera.'}
                  </p>
                  <button type="button" className="primario" onClick={() => abrirDetalleOfertaEstudiante(oferta)}>
                    Ver detalles
                  </button>
                </article>
              ))}
            </div>
          )}

          {ofertaDetalleEstudiante && (
            <section className="panel detalle-oferta detalle-oferta-estudiante">
              <div className="detalle-oferta-cabecera">
                <div>
                  <small>OFERTA PUBLICADA #{ofertaDetalleEstudiante.id}</small>
                  <h2>{ofertaDetalleEstudiante.titulo}</h2>
                  <p>{ofertaDetalleEstudiante.empresa_nombre_comercial || ofertaDetalleEstudiante.empresa}</p>
                </div>
                <button type="button" className="secundario" onClick={() => setOfertaDetalleEstudiante(null)}>
                  Cerrar detalle
                </button>
              </div>

              <h3>Información de la empresa</h3>
              <div className="detalle-oferta-grid">
                <p><strong>Razón social:</strong> {ofertaDetalleEstudiante.empresa || '—'}</p>
                <p><strong>Nombre comercial:</strong> {ofertaDetalleEstudiante.empresa_nombre_comercial || '—'}</p>
                <p><strong>RUC:</strong> {ofertaDetalleEstudiante.empresa_ruc || '—'}</p>
                <p><strong>Dirección:</strong> {ofertaDetalleEstudiante.empresa_direccion || '—'}</p>
                <p><strong>Correo:</strong> {ofertaDetalleEstudiante.empresa_correo || '—'}</p>
                <p><strong>Teléfono:</strong> {ofertaDetalleEstudiante.empresa_telefono || '—'}</p>
              </div>

              <h3>Información de la práctica</h3>
              <div className="detalle-oferta-grid">
                <p><strong>Tipo de práctica:</strong> {ofertaDetalleEstudiante.tipo_practica || '—'}</p>
                <p><strong>Periodo:</strong> {ofertaDetalleEstudiante.periodo || '—'}</p>
                <p><strong>Fechas del periodo:</strong> {ofertaDetalleEstudiante.periodo_fecha_inicio || '—'} a {ofertaDetalleEstudiante.periodo_fecha_fin || '—'}</p>
                <p><strong>Convenio:</strong> {ofertaDetalleEstudiante.convenio || '—'}</p>
                <p><strong>Vigencia del convenio:</strong> {ofertaDetalleEstudiante.convenio_fecha_inicio || '—'} a {ofertaDetalleEstudiante.convenio_fecha_fin || '—'}</p>
                <p><strong>Fecha de cierre:</strong> {ofertaDetalleEstudiante.fecha_cierre || '—'} ({ofertaDetalleEstudiante.plazo_vigente ? 'Abierto' : 'Cerrado'})</p>
                <p><strong>Inicio de actividades:</strong> {ofertaDetalleEstudiante.fecha_inicio_actividades || '—'}</p>
                <p><strong>Fin de actividades:</strong> {ofertaDetalleEstudiante.fecha_fin_actividades || '—'}</p>
                <p><strong>Horas requeridas:</strong> {ofertaDetalleEstudiante.horas_requeridas ?? '—'}</p>
                <p><strong>Nota mínima:</strong> {ofertaDetalleEstudiante.nota_minima ?? '—'}</p>
              </div>

              <h3>Descripción de la oferta</h3>
              <p className="texto-detalle">{ofertaDetalleEstudiante.descripcion || 'La empresa no agregó una descripción.'}</p>

              <h3>Perfiles, plazas y requisitos</h3>
              <label className="campo-mensaje-postulacion">
                Mensaje para la empresa (opcional)
                <textarea
                  rows="3"
                  value={mensajePostulacion}
                  onChange={(e) => setMensajePostulacion(e.target.value)}
                  placeholder="Puedes incluir una breve presentación o información adicional."
                />
              </label>
              {(ofertaDetalleEstudiante.perfiles || []).length === 0 ? <p>No hay perfiles registrados.</p> : (
                <div className="perfiles-revision">
                  {ofertaDetalleEstudiante.perfiles.map((perfil) => (
                    <article className="perfil-revision" key={perfil.id}>
                      <h4>
                        {perfil.nombre}
                        <span>{perfil.plazas} plazas</span>
                      </h4>
                      <p>{perfil.descripcion || 'Sin descripción del perfil.'}</p>
                      <p className={perfil.carrera_compatible ? 'compatibilidad compatible' : 'compatibilidad no-compatible'}>
                        {perfil.carrera_compatible
                          ? `Compatible con tu carrera (${ofertaDetalleEstudiante.carrera_estudiante || 'carrera registrada'}).`
                          : `Este perfil no está dirigido a tu carrera (${ofertaDetalleEstudiante.carrera_estudiante || 'carrera registrada'}).`}
                      </p>
                      <strong>Carreras a las que está dirigido</strong>
                      {perfil.abierto_a_todas_las_carreras ? (
                        <p>Abierto a todas las carreras.</p>
                      ) : (
                        <ul>{(perfil.carreras || []).map((carrera) => <li key={carrera.id}>{carrera.nombre}</li>)}</ul>
                      )}
                      <strong>Requisitos</strong>
                      {perfil.requisitos?.length ? (
                        <ul>{perfil.requisitos.map((req) => (
                          <li key={req.id}>{req.descripcion} {req.obligatorio ? '(Obligatorio)' : '(Opcional)'}</li>
                        ))}</ul>
                      ) : <p>No se registraron requisitos.</p>}
                      <p><strong>Plazas disponibles:</strong> {perfil.plazas_disponibles ?? perfil.plazas}</p>
                      {perfil.ya_postulado ? (
                        <p className="estado-postulacion-aviso">Ya tienes una postulación para este perfil: {perfil.postulacion_estado}.</p>
                      ) : (
                        <button
                          type="button"
                          className="primario"
                          disabled={!ofertaDetalleEstudiante.plazo_vigente || !perfil.carrera_compatible || (perfil.plazas_disponibles ?? 0) <= 0 || postulandoPerfilId !== null}
                          onClick={() => postularse(perfil.id)}
                        >
                          {postulandoPerfilId === perfil.id ? 'Enviando...' : 'Postularme a este perfil'}
                        </button>
                      )}
                      {!ofertaDetalleEstudiante.plazo_vigente && <p className="ayuda">El plazo de postulación ha finalizado.</p>}
                    </article>
                  ))}
                </div>
              )}
            </section>
          )}
        </div>
      ) : (
        <div className="panel">
          {cargando ? (
            <p>Cargando...</p>
          ) : datos.length === 0 ? (
            <p>No hay registros.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  {COLUMNAS[vista].map((columna) => (
                    <th key={columna}>{columna}</th>
                  ))}
                  {vista !== 'auditoria' && <th>Acción</th>}
                </tr>
              </thead>
              <tbody>
                {datos.map((registro, indice) => (
                  <tr key={registro.id ?? indice}>
                    {obtenerCeldas(registro).map((valor, columna) => (
                      <td key={columna}>{valor ?? '—'}</td>
                    ))}
                    {vista !== 'auditoria' && (
                      <td>
                        {vista === 'periodos' ? (
                          registro.activo ? <span>Activo</span> : (
                            <button type="button" onClick={() => activar(registro.id)}>Activar</button>
                          )
                        ) : vista === 'facultades' || vista === 'carreras' ? (
                          registro.activa ? (
                            <button type="button" onClick={() => desactivar(registro.id)}>Desactivar</button>
                          ) : <span>Desactivada</span>
                        ) : vista === 'empresas' ? (
                          <button type="button" onClick={() => cambiarEstadoEmpresa(registro)}>
                            {registro.activa ? 'Desactivar' : 'Activar'}
                          </button>
                        ) : vista === 'convenios' ? (
                          registro.estado_efectivo === 'VENCIDO' || registro.estado === 'TERMINADO' ? (
                            <span>Sin acciones</span>
                          ) : (
                            <button type="button" onClick={() => cambiarEstadoConvenio(registro, registro.estado === 'VIGENTE' ? 'SUSPENDIDO' : 'VIGENTE')}>
                              {registro.estado === 'VIGENTE' ? 'Suspender' : 'Reanudar'}
                            </button>
                          )
                        ) : vista === 'representantes' ? (
                          <button type="button" onClick={() => cambiarEstadoRepresentante(registro)}>
                            {registro.activo ? 'Desactivar' : 'Activar'}
                          </button>
                        ) : vista === 'ofertas' ? (
                          <div className="acciones-usuario">
                            {['BORRADOR', 'RECHAZADA'].includes(registro.estado) && (
                              <button type="button" onClick={() => editarOferta(registro)}>Editar</button>
                            )}
                            {['BORRADOR', 'RECHAZADA'].includes(registro.estado) ? (
                              <button type="button" onClick={() => enviarOfertaRevision(registro)}>Enviar a revisión</button>
                            ) : <span>Sin acciones</span>}
                          </div>
                        ) : vista === 'revisionOfertas' ? (
                          <button type="button" onClick={() => abrirDetalleOferta(registro)}>Revisar oferta</button>
                        ) : vista === 'revisionPostulaciones' ? (
                          <button type="button" onClick={() => abrirDetallePostulacion(registro)}>Revisar postulación</button>
                        ) : vista === 'usuarios' ? (
                          <div className="acciones-usuario">
                            <button type="button" onClick={() => cambiarEstadoUsuario(registro)}>
                              {registro.activo ? 'Desactivar' : 'Activar'}
                            </button>
                            <button type="button" onClick={() => editarCarrerasUsuario(registro)}>Carreras</button>
                          </div>
                        ) : <span>—</span>}
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {vista === 'revisionOfertas' && ofertaDetalle && (
        <section className="panel detalle-oferta">
          <div className="detalle-oferta-cabecera">
            <div>
              <small>OFERTA #{ofertaDetalle.id}</small>
              <h2>{ofertaDetalle.titulo}</h2>
              <p>{ofertaDetalle.empresa} · Estado: {ofertaDetalle.estado}</p>
            </div>
            <button type="button" className="secundario" onClick={() => setOfertaDetalle(null)}>Cerrar detalle</button>
          </div>

          <h3>Información general</h3>
          <div className="detalle-oferta-grid">
            <p><strong>Nombre comercial:</strong> {ofertaDetalle.empresa_nombre_comercial || '—'}</p>
            <p><strong>RUC:</strong> {ofertaDetalle.empresa_ruc || '—'}</p>
            <p><strong>Correo:</strong> {ofertaDetalle.empresa_correo || '—'}</p>
            <p><strong>Teléfono:</strong> {ofertaDetalle.empresa_telefono || '—'}</p>
            <p><strong>Convenio:</strong> {ofertaDetalle.convenio || '—'} ({ofertaDetalle.convenio_estado || '—'})</p>
            <p><strong>Vigencia del convenio:</strong> {ofertaDetalle.convenio_fecha_inicio || '—'} a {ofertaDetalle.convenio_fecha_fin || '—'}</p>
            <p><strong>Tipo de práctica:</strong> {ofertaDetalle.tipo_practica || '—'}</p>
            <p><strong>Periodo:</strong> {ofertaDetalle.periodo || '—'} ({ofertaDetalle.periodo_fecha_inicio || '—'} a {ofertaDetalle.periodo_fecha_fin || '—'})</p>
            <p><strong>Fecha de cierre:</strong> {ofertaDetalle.fecha_cierre || '—'}</p>
            <p><strong>Actividades:</strong> {ofertaDetalle.fecha_inicio_actividades || '—'} a {ofertaDetalle.fecha_fin_actividades || '—'}</p>
            <p><strong>Horas requeridas:</strong> {ofertaDetalle.horas_requeridas_snapshot ?? ofertaDetalle.tipo_horas_requeridas ?? '—'}</p>
            <p><strong>Nota mínima:</strong> {ofertaDetalle.nota_minima_snapshot ?? ofertaDetalle.tipo_nota_minima ?? '—'}</p>
          </div>
          <h3>Descripción</h3>
          <p className="texto-detalle">{ofertaDetalle.descripcion || 'La empresa no agregó una descripción.'}</p>

          <h3>Perfiles, plazas y requisitos</h3>
          {(ofertaDetalle.perfiles || []).length === 0 ? <p>No hay perfiles registrados.</p> : (
            <div className="perfiles-revision">
              {ofertaDetalle.perfiles.map((perfil) => (
                <article className="perfil-revision" key={perfil.id}>
                  <h4>{perfil.nombre} <span>{perfil.plazas} plazas</span></h4>
                  <p>{perfil.descripcion || 'Sin descripción del perfil.'}</p>
                  <strong>Requisitos</strong>
                  {perfil.requisitos?.length ? (
                    <ul>{perfil.requisitos.map((req) => <li key={req.id}>{req.descripcion} {req.obligatorio ? '(Obligatorio)' : '(Opcional)'}</li>)}</ul>
                  ) : <p>No se registraron requisitos.</p>}
                </article>
              ))}
            </div>
          )}

          <div className="acciones-revision">
            <button type="button" className="primario" onClick={aprobarOferta}>Aprobar y publicar</button>
            <form onSubmit={rechazarOferta} className="form-rechazo">
              <label htmlFor="motivo-rechazo">Motivo del rechazo</label>
              <textarea id="motivo-rechazo" rows="3" maxLength="2000" required value={motivoRechazo} onChange={(e) => setMotivoRechazo(e.target.value)} placeholder="Explique qué debe corregir la empresa" />
              <button type="submit" className="secundario">Rechazar oferta</button>
            </form>
          </div>
        </section>
      )}

      {vista === 'revisionPostulaciones' && postulacionDetalle && (
        <section className="panel detalle-oferta detalle-postulacion">
          <div className="detalle-oferta-cabecera">
            <div>
              <small>POSTULACIÓN #{postulacionDetalle.id}</small>
              <h2>{postulacionDetalle.estudiante}</h2>
              <p>{postulacionDetalle.oferta} · {postulacionDetalle.empresa}</p>
            </div>
            <button type="button" className="secundario" onClick={() => setPostulacionDetalle(null)}>Cerrar detalle</button>
          </div>

          <h3>Información del estudiante</h3>
          <div className="detalle-oferta-grid">
            <p><strong>Nombre:</strong> {postulacionDetalle.estudiante || '—'}</p>
            <p><strong>Correo:</strong> {postulacionDetalle.correo_estudiante || '—'}</p>
            <p><strong>Documento:</strong> {postulacionDetalle.documento_estudiante || '—'}</p>
            <p><strong>Teléfono:</strong> {postulacionDetalle.telefono_estudiante || '—'}</p>
            <p><strong>Código:</strong> {postulacionDetalle.codigo_estudiante || '—'}</p>
            <p><strong>Carrera:</strong> {postulacionDetalle.carrera_estudiante || '—'}</p>
          </div>

          <h3>Oferta y perfil seleccionado</h3>
          <div className="detalle-oferta-grid">
            <p><strong>Oferta:</strong> {postulacionDetalle.oferta || '—'}</p>
            <p><strong>Empresa:</strong> {postulacionDetalle.empresa || '—'}</p>
            <p><strong>Perfil:</strong> {postulacionDetalle.perfil || '—'}</p>
            <p><strong>Plazas totales:</strong> {postulacionDetalle.plazas ?? '—'}</p>
            <p><strong>Plazas aceptadas:</strong> {postulacionDetalle.plazas_aceptadas ?? 0}</p>
            <p><strong>Plazas disponibles:</strong> {postulacionDetalle.plazas_disponibles ?? 0}</p>
            <p><strong>Tipo de práctica:</strong> {postulacionDetalle.tipo_practica || '—'}</p>
            <p><strong>Periodo:</strong> {postulacionDetalle.periodo || '—'}</p>
            <p><strong>Fechas:</strong> {postulacionDetalle.fecha_inicio_actividades || '—'} a {postulacionDetalle.fecha_fin_actividades || '—'}</p>
            <p><strong>Horas requeridas:</strong> {postulacionDetalle.horas_requeridas ?? '—'}</p>
            <p><strong>Nota mínima:</strong> {postulacionDetalle.nota_minima ?? '—'}</p>
            <p><strong>Convenio:</strong> {postulacionDetalle.convenio || '—'} ({postulacionDetalle.convenio_estado || '—'})</p>
          </div>
          <p className="texto-detalle">{postulacionDetalle.descripcion_oferta || 'La oferta no tiene descripción adicional.'}</p>

          <h3>Requisitos del perfil</h3>
          {postulacionDetalle.requisitos?.length ? (
            <ul>{postulacionDetalle.requisitos.map((req) => <li key={req.id}>{req.descripcion} {req.obligatorio ? '(Obligatorio)' : '(Opcional)'}</li>)}</ul>
          ) : <p>No se registraron requisitos.</p>}

          <h3>Información de la postulación</h3>
          <div className="detalle-oferta-grid">
            <p><strong>Fecha:</strong> {postulacionDetalle.fecha_postulacion ? new Date(postulacionDetalle.fecha_postulacion).toLocaleString('es-EC') : '—'}</p>
            <p><strong>Recomendación empresarial:</strong> {postulacionDetalle.recomendacion_empresa || 'SIN_EVALUAR'}</p>
            <p><strong>Observación de la empresa:</strong> {postulacionDetalle.observacion_empresa || 'Sin observaciones.'}</p>
          </div>
          <h3>Mensaje del estudiante</h3>
          <p className="texto-detalle">{postulacionDetalle.mensaje || 'El estudiante no agregó un mensaje.'}</p>

          <div className="acciones-revision">
            <button type="button" className="primario" onClick={aceptarPostulacion} disabled={(postulacionDetalle.plazas_disponibles ?? 0) <= 0}>
              Aceptar y generar práctica
            </button>
            <form onSubmit={rechazarPostulacion} className="form-rechazo">
              <label htmlFor="motivo-rechazo-postulacion">Motivo del rechazo</label>
              <textarea id="motivo-rechazo-postulacion" rows="3" maxLength="2000" required value={motivoRechazoPostulacion} onChange={(e) => setMotivoRechazoPostulacion(e.target.value)} placeholder="Explique el motivo del rechazo" />
              <button type="submit" className="secundario">Rechazar postulación</button>
            </form>
          </div>
        </section>
      )}

      {usuarioEditando && (
        <form className="formulario formulario-alcance" onSubmit={guardarCarrerasUsuario}>
          <h2>Asignar carreras: {usuarioEditando.nombres} {usuarioEditando.apellidos}</h2>
          <label className="opcion-global">
            <input
              type="checkbox"
              checked={alcanceGlobal}
              onChange={(e) => setAlcanceGlobal(e.target.checked)}
            />
            Alcance global (puede consultar todas las carreras)
          </label>
          <label>Seleccione las carreras permitidas</label>
          <select
            multiple
            disabled={alcanceGlobal}
            value={carrerasElegidas}
            onChange={(e) => setCarrerasElegidas(Array.from(e.target.selectedOptions, (o) => o.value))}
          >
            {carrerasDisponibles.filter((c) => c.activa).map((carrera) => (
              <option key={carrera.id} value={String(carrera.id)}>
                {carrera.nombre} ({carrera.codigo})
              </option>
            ))}
          </select>
          <div className="acciones-formulario">
            <button type="submit">Guardar asignación</button>
            <button type="button" className="secundario" onClick={() => setUsuarioEditando(null)}>Cancelar</button>
          </div>
        </form>
      )}

      {/* FORMULARIOS DE CREACIÓN */}
      {vista !== 'auditoria' && vista !== 'revisionOfertas' && vista !== 'revisionPostulaciones' && vista !== 'ofertasEstudiante' && (
        <form
          className="formulario"
          onSubmit={enviar}
        >
          <h2>
            {vista === 'ofertas' && ofertaEditandoId ? 'Editar oferta de prácticas' : titulosFormulario[vista]}
          </h2>

          {/* CREAR USUARIO */}
          {vista === 'usuarios' && (
            <>
              <div className="grupo-formulario">
                <h3>Datos de acceso</h3>

                {campo('correo', 'Correo electrónico', 'email')}
                {campo('password', 'Contraseña', 'password')}
              </div>

              <div className="grupo-formulario">
                <h3>Datos personales</h3>

                <div className="campos-documento">
                  {campo('nombres', 'Nombres')}
                  {campo('apellidos', 'Apellidos')}

                  <label>Tipo de documento</label>
                  <select
                    required
                    value={form.tipoDocumento || ''}
                    onChange={(e) =>
                      setForm((anterior) => ({
                        ...anterior,
                        tipoDocumento: e.target.value
                      }))
                    }
                  >
                    <option value="">Seleccione un tipo</option>
                    <option value="CEDULA">Cédula</option>
                    <option value="PASAPORTE">Pasaporte</option>
                  </select>

                  {campo('numeroDocumento', 'Número de documento')}
                  {campo('telefono', 'Teléfono')}
                </div>
              </div>

              <div className="grupo-formulario">
                <h3>Asignación de roles</h3>

                <label>Roles</label>
                <select
                  multiple
                  value={rolesElegidos}
                  onChange={(e) => {
                    const seleccionados = Array.from(
                      e.target.selectedOptions,
                      (opcion) => opcion.value
                    );

                    setRolesElegidos(seleccionados);
                  }}
                >
                  {roles.map((rol) => (
                    <option key={rol.id} value={rol.codigo}>
                      {rol.nombre}
                    </option>
                  ))}
                </select>
              </div>
            </>
          )}

          {/* CREAR FACULTAD */}
          {vista === 'facultades' && (
            <>
              {campo('codigo', 'Código')}
              {campo('nombre', 'Nombre')}
            </>
          )}

          {/* CREAR CARRERA */}
          {vista === 'carreras' && (
            <>
              <label>Facultad</label>
              <select
                required
                value={form.facultadId || ''}
                onChange={(e) => setForm((anterior) => ({ ...anterior, facultadId: e.target.value }))}
              >
                <option value="">Seleccione una facultad</option>
                {facultadesDisponibles.filter((f) => f.activa).map((facultad) => (
                  <option key={facultad.id} value={facultad.id}>{facultad.nombre}</option>
                ))}
              </select>
              {campo('codigo', 'Código')}
              {campo('nombre', 'Nombre')}
            </>
          )}

          {/* REGISTRAR CONVENIO */}
          {vista === 'convenios' && (
            <>
              <label>Empresa receptora</label>
              <select
                required
                value={form.empresaId || ''}
                onChange={(e) => setForm((anterior) => ({ ...anterior, empresaId: e.target.value }))}
              >
                <option value="">Seleccione una empresa activa</option>
                {empresasDisponibles.filter((empresa) => empresa.activa).map((empresa) => (
                  <option key={empresa.id} value={empresa.id}>{empresa.razon_social} — {empresa.ruc}</option>
                ))}
              </select>
              {campo('numero', 'Número de convenio (opcional)', 'text', false)}
              <label>Fecha de inicio</label>
              {campo('fechaInicio', 'Fecha de inicio', 'date')}
              <label>Fecha de finalización</label>
              {campo('fechaFin', 'Fecha de finalización', 'date')}
              {campo('documentoUrl', 'Enlace al documento (opcional)', 'text', false)}
            </>
          )}

          {/* ASIGNAR REPRESENTANTE A EMPRESA */}
          {vista === 'representantes' && (
            <>
              <p>
                Solo aparecen usuarios activos que ya tienen el rol global REPRESENTANTE_EMPRESA.
              </p>

              <label>Empresa</label>
              <select
                required
                value={form.empresaId || ''}
                onChange={(e) => setForm((anterior) => ({ ...anterior, empresaId: e.target.value }))}
              >
                <option value="">Seleccione una empresa activa</option>
                {empresasDisponibles.map((empresa) => (
                  <option key={empresa.id} value={empresa.id}>
                    {empresa.razon_social} — {empresa.ruc}
                  </option>
                ))}
              </select>

              <label>Usuario representante</label>
              <select
                required
                value={form.usuarioId || ''}
                onChange={(e) => setForm((anterior) => ({ ...anterior, usuarioId: e.target.value }))}
              >
                <option value="">Seleccione un usuario</option>
                {representantesDisponibles.map((usuario) => (
                  <option key={usuario.id} value={usuario.id}>
                    {usuario.nombres} {usuario.apellidos} — {usuario.correo}
                  </option>
                ))}
              </select>

              <label>Permiso dentro de la empresa</label>
              <select
                required
                value={form.rolEmpresa || 'PUBLICADOR'}
                onChange={(e) => setForm((anterior) => ({ ...anterior, rolEmpresa: e.target.value }))}
              >
                <option value="ADMIN_EMPRESA">Administrador de empresa</option>
                <option value="PUBLICADOR">Publicador</option>
              </select>

              <p>
                El primer representante activo de cada empresa debe ser Administrador de empresa.
              </p>
            </>
          )}

          {/* REGISTRAR PERFIL DE ESTUDIANTE */}
          {vista === 'estudiantes' && (
            <>
              <label>Cuenta de usuario con rol ESTUDIANTE</label>
              <select
                required
                value={form.usuarioId || ''}
                onChange={(e) => setForm((anterior) => ({ ...anterior, usuarioId: e.target.value }))}
              >
                <option value="">Seleccione un estudiante sin perfil</option>
                {estudiantesDisponibles.map((usuario) => (
                  <option key={usuario.id} value={usuario.id}>
                    {usuario.nombres} {usuario.apellidos} — {usuario.correo}
                  </option>
                ))}
              </select>

              <label>Carrera</label>
              <select
                required
                value={form.carreraId || ''}
                onChange={(e) => setForm((anterior) => ({ ...anterior, carreraId: e.target.value }))}
              >
                <option value="">Seleccione una carrera</option>
                {carrerasDisponibles.map((carrera) => (
                  <option key={carrera.id} value={carrera.id}>{carrera.nombre} ({carrera.codigo})</option>
                ))}
              </select>
              {campo('codigoEstudiante', 'Código de estudiante (opcional)', 'text', false)}
              <label>Fecha de nacimiento (opcional)</label>
              {campo('fechaNacimiento', 'Fecha de nacimiento', 'date', false)}
              {estudiantesDisponibles.length === 0 && (
                <p className="ayuda">No hay usuarios activos con rol ESTUDIANTE sin perfil académico. Primero crea un usuario y asígnale ese rol.</p>
              )}
            </>
          )}

          {/* REGISTRAR EMPRESA */}
          {vista === 'empresas' && (
            <>
              {campo('ruc', 'RUC (13 dígitos)')}
              {campo('razonSocial', 'Razón social')}
              {campo('nombreComercial', 'Nombre comercial', 'text', false)}
              {campo('direccion', 'Dirección', 'text', false)}
              {campo('telefono', 'Teléfono', 'text', false)}
              {campo('correo', 'Correo de contacto', 'email', false)}
            </>
          )}

          {/* CREAR OFERTA DE PRÁCTICAS */}
          {vista === 'ofertas' && (
            <>
              <label>Empresa</label>
              <select
                required
                value={form.empresaId || ''}
                onChange={(e) => setForm((anterior) => ({
                  ...anterior,
                  empresaId: e.target.value,
                  convenioId: '',
                }))}
              >
                <option value="">Seleccione una empresa</option>
                {empresasDisponibles.map((empresa) => (
                  <option key={empresa.id} value={empresa.id}>
                    {empresa.razon_social} — {empresa.rol_empresa}
                  </option>
                ))}
              </select>

              <label>Convenio vigente</label>
              <select
                required
                value={form.convenioId || ''}
                onChange={(e) => setForm((anterior) => ({ ...anterior, convenioId: e.target.value }))}
              >
                <option value="">Seleccione un convenio</option>
                {conveniosOfertaDisponibles
                  .filter((convenio) => String(convenio.empresa_id) === String(form.empresaId))
                  .map((convenio) => (
                    <option key={convenio.id} value={convenio.id}>
                      {convenio.numero || `Convenio #${convenio.id}`} ({convenio.fecha_inicio} a {convenio.fecha_fin})
                    </option>
                  ))}
              </select>

              {campo('titulo', 'Título de la oferta')}
              <label>Descripción</label>
              <textarea
                rows="3"
                value={form.descripcion || ''}
                onChange={(e) => setForm((anterior) => ({ ...anterior, descripcion: e.target.value }))}
                placeholder="Describe las actividades y el objetivo de la práctica"
              />

              <label>Tipo de práctica</label>
              <select
                required
                value={form.tipoPracticaId || ''}
                onChange={(e) => setForm((anterior) => ({ ...anterior, tipoPracticaId: e.target.value }))}
              >
                <option value="">Seleccione un tipo</option>
                {tiposPracticaDisponibles.map((tipo) => (
                  <option key={tipo.id} value={tipo.id}>
                    {tipo.nombre} — {tipo.horas_requeridas} horas
                  </option>
                ))}
              </select>

              <label>Periodo académico</label>
              <select
                required
                value={form.periodoId || ''}
                onChange={(e) => setForm((anterior) => ({ ...anterior, periodoId: e.target.value }))}
              >
                <option value="">Seleccione un periodo activo</option>
                {periodosOfertaDisponibles.map((periodo) => (
                  <option key={periodo.id} value={periodo.id}>
                    {periodo.codigo} — {periodo.nombre}
                  </option>
                ))}
              </select>

              <label>Fecha de cierre de postulaciones</label>
              {campo('fechaCierre', 'Fecha de cierre', 'date')}
              <label>Inicio de actividades</label>
              {campo('fechaInicioActividades', 'Inicio de actividades', 'date')}
              <label>Fin de actividades</label>
              {campo('fechaFinActividades', 'Fin de actividades', 'date')}

              <h3>Perfil solicitado</h3>
              {campo('perfilNombre', 'Nombre del perfil (ej. Desarrollador Java)')}
              <label>Descripción del perfil</label>
              <textarea
                rows="2"
                value={form.perfilDescripcion || ''}
                onChange={(e) => setForm((anterior) => ({ ...anterior, perfilDescripcion: e.target.value }))}
                placeholder="Conocimientos o funciones del perfil"
              />
              <label>Número de plazas</label>
              {campo('plazas', 'Cantidad de plazas', 'number')}
              <label>Requisitos (uno por línea)</label>
              <textarea
                rows="4"
                value={form.requisitosTexto || ''}
                onChange={(e) => setForm((anterior) => ({ ...anterior, requisitosTexto: e.target.value }))}
                placeholder={'Manejo de Java\nConocimientos de bases de datos\nTrabajo en equipo'}
              />
              {empresasDisponibles.length === 0 && (
                <p className="ayuda">Tu usuario no tiene una empresa activa asignada. Solicita al administrador que te asigne como representante.</p>
              )}
              {conveniosOfertaDisponibles.length === 0 && (
                <p className="ayuda">No hay convenios vigentes disponibles para tus empresas.</p>
              )}
              {periodosOfertaDisponibles.length === 0 && (
                <p className="ayuda">No existe un periodo académico activo para las ofertas.</p>
              )}
            </>
          )}

          {/* CREAR PERIODO */}
          {vista === 'periodos' && (
            <>
              {campo('codigo', 'Código')}
              {campo('nombre', 'Nombre')}

              <label>
                Fecha de inicio
                {campo('fechaInicio', 'Seleccionar fecha de inicio', 'date')}
              </label>

              <label>
                Fecha de finalización
                {campo('fechaFin', 'Seleccionar fecha de finalización', 'date')}
              </label>
            </>
          )}


          <button
            type="submit"
            disabled={guardando || (vista === 'estudiantes' && estudiantesDisponibles.length === 0) || (vista === 'ofertas' && (empresasDisponibles.length === 0 || conveniosOfertaDisponibles.length === 0 || tiposPracticaDisponibles.length === 0 || periodosOfertaDisponibles.length === 0))}
          >
            {guardando ? 'Guardando...' : (vista === 'ofertas' && ofertaEditandoId ? 'Actualizar oferta' : 'Guardar')}
          </button>
          {vista === 'ofertas' && ofertaEditandoId && (
            <button type="button" className="secundario" onClick={cancelarEdicionOferta}>Cancelar edición</button>
          )}

        </form>
      )}
    </>
  );
}

/* =========================================================
   CAMBIO OBLIGATORIO DE CONTRASEÑA
========================================================= */

function AsignacionDocentePanel({ api, setError }) {
  const [practicas, setPracticas] = useState([]);
  const [practicaSeleccionada, setPracticaSeleccionada] = useState(null);
  const [docentes, setDocentes] = useState([]);
  const [docenteId, setDocenteId] = useState('');
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);

  const cargarPracticas = useCallback(async () => {
    setCargando(true);
    try {
      const respuesta = await api('/coordinacion/practicas/pendientes-docente');
      setPracticas(Array.isArray(respuesta) ? respuesta : []);
    } catch (err) {
      setPracticas([]);
      setError(err.message);
    } finally {
      setCargando(false);
    }
  }, [api, setError]);

  useEffect(() => { cargarPracticas(); }, [cargarPracticas]);

  const abrirAsignacion = async (practica) => {
    setError('');
    setPracticaSeleccionada(practica);
    setDocenteId('');
    setDocentes([]);
    try {
      const respuesta = await api(`/coordinacion/practicas/${practica.id}/docentes-disponibles`);
      setDocentes(Array.isArray(respuesta) ? respuesta : []);
    } catch (err) {
      setError(err.message);
      setPracticaSeleccionada(null);
    }
  };

  const guardarAsignacion = async (e) => {
    e.preventDefault();
    if (!practicaSeleccionada || !docenteId) {
      setError('Seleccione un docente para continuar.');
      return;
    }
    setGuardando(true);
    setError('');
    try {
      await api(`/coordinacion/practicas/${practicaSeleccionada.id}/docente`, {
        method: 'PATCH',
        body: JSON.stringify({ docenteId: Number(docenteId) }),
      });
      setPracticaSeleccionada(null);
      setDocenteId('');
      await cargarPracticas();
    } catch (err) {
      setError(err.message);
    } finally {
      setGuardando(false);
    }
  };

  return (
    <>
      <section className="panel">
        <h2>Prácticas aprobadas sin docente tutor</h2>
        <p>Seleccione una práctica para asignarle un docente de la misma carrera. Solo se mostrarán docentes activos con rol DOCENTE y alcance académico compatible.</p>
        {cargando ? <p>Cargando prácticas...</p> : practicas.length === 0 ? (
          <p>No existen prácticas aprobadas pendientes de asignación en las carreras a las que tiene acceso.</p>
        ) : (
          <div className="tabla-postulaciones-contenedor">
            <table>
              <thead><tr><th>Estudiante</th><th>Correo</th><th>Carrera</th><th>Empresa</th><th>Periodo</th><th>Fecha de aprobación</th><th>Acción</th></tr></thead>
              <tbody>
                {practicas.map((practica) => (
                  <tr key={practica.id}>
                    <td>{practica.estudiante}</td><td>{practica.correo_estudiante || '—'}</td>
                    <td>{practica.carrera}</td><td>{practica.empresa}</td><td>{practica.periodo}</td>
                    <td>{practica.fecha_aprobacion ? new Date(practica.fecha_aprobacion).toLocaleDateString('es-EC') : '—'}</td>
                    <td><button type="button" onClick={() => abrirAsignacion(practica)}>Asignar docente</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
      {practicaSeleccionada && (
        <form className="panel formulario" onSubmit={guardarAsignacion}>
          <h2>Asignar docente tutor</h2>
          <p><strong>Estudiante:</strong> {practicaSeleccionada.estudiante}</p>
          <p><strong>Carrera:</strong> {practicaSeleccionada.carrera}</p>
          <p><strong>Empresa:</strong> {practicaSeleccionada.empresa}</p>
          <label htmlFor="docente-practica">Docente disponible</label>
          <select id="docente-practica" required value={docenteId} onChange={(e) => setDocenteId(e.target.value)}>
            <option value="">Seleccione un docente</option>
            {docentes.map((docente) => <option key={docente.id} value={docente.id}>{docente.nombre}{docente.codigo_docente ? ` · ${docente.codigo_docente}` : ''} ({docente.correo})</option>)}
          </select>
          {docentes.length === 0 && <p>No hay docentes activos con alcance asignado a esta carrera.</p>}
          <div className="acciones-formulario">
            <button type="submit" disabled={guardando || docentes.length === 0}>{guardando ? 'Guardando...' : 'Confirmar asignación'}</button>
            <button type="button" className="secundario" onClick={() => setPracticaSeleccionada(null)}>Cancelar</button>
          </div>
        </form>
      )}
    </>
  );
}

function CambiarPassword({ api, salir }) {
  const [actual, setActual] = useState('');
  const [nueva, setNueva] = useState('');

  const [error, setError] = useState('');
  const [cargando, setCargando] = useState(false);

  const enviar = async (e) => {
    e.preventDefault();

    setCargando(true);
    setError('');

    try {
      await api('/auth/cambiar-password', {
        method: 'POST',
        body: JSON.stringify({
          actual,
          nueva,
        }),
      });

      salir();
    } catch (err) {
      setError(err.message);
    } finally {
      setCargando(false);
    }
  };

  return (
    <main className="login-fondo">
      <form
        className="login"
        onSubmit={enviar}
      >
        <h1>Cambia tu contraseña</h1>

        <p>
          Por seguridad, debes cambiar la contraseña temporal antes de continuar.
        </p>

        <label>
          Contraseña actual

          <input
            type="password"
            autoComplete="current-password"
            required
            value={actual}
            onChange={(e) => setActual(e.target.value)}
          />
        </label>

        <label>
          Nueva contraseña (mínimo 12 caracteres)

          <input
            type="password"
            autoComplete="new-password"
            minLength={12}
            required
            value={nueva}
            onChange={(e) => setNueva(e.target.value)}
          />
        </label>

        {error && (
          <p className="error" role="alert">
            {error}
          </p>
        )}

        <button
          className="primario"
          type="submit"
          disabled={cargando}
        >
          {cargando
            ? 'Actualizando...'
            : 'Actualizar contraseña'}
        </button>
      </form>
    </main>
  );
}