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

  const [vista, setVista] = useState('usuarios');
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
    setError('');
  };

  /* Cerrar sesión */
  const salir = () => {
    sessionStorage.removeItem('praxisSesion');

    setSesion(null);
    setError('');
  };

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
          }).map(([id, nombre]) => (
            <button
              key={id}
              className={vista === id ? 'seleccionado' : ''}
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
            <small>ADMINISTRACIÓN</small>

            <h1>
              {vista.charAt(0).toUpperCase() + vista.slice(1)}
            </h1>
          </div>

          <span>
            {sesion.nombres} {sesion.apellidos}
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

function Panel({ vista, api, setError }) {
  const [datos, setDatos] = useState([]);
  const [roles, setRoles] = useState([]);

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
    setError('');

    cargar();

    if (vista === 'usuarios') {
      api('/admin/roles')
        .then((respuesta) => {
          setRoles(
            Array.isArray(respuesta) ? respuesta : []
          );
        })
        .catch((err) => {
          setRoles([]);
          setError(err.message);
        });
    } else {
      setRoles([]);
    }
  }, [vista, cargar, api, setError]);

  /* -------------------------------------------------------
     CAMPOS DINÁMICOS DE LOS FORMULARIOS
  ------------------------------------------------------- */

  const campo = (nombre, placeholder, tipo = 'text') => {
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
        required
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

      setForm({});
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
     ACTIVAR PERIODOS
  ------------------------------------------------------- */

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
  };

  /* -------------------------------------------------------
     INTERFAZ DEL PANEL
  ------------------------------------------------------- */

  return (
    <>
      {/* TABLA DE REGISTROS */}
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
                  <th key={columna}>
                    {columna}
                  </th>
                ))}

                {vista !== 'auditoria' && (
                  <th>Acción</th>
                )}
              </tr>
            </thead>

            <tbody>
              {datos.map((registro, indice) => (
                <tr key={registro.id ?? indice}>

                  {obtenerCeldas(registro).map((valor, columna) => (
                    <td key={columna}>
                      {valor ?? '—'}
                    </td>
                  ))}

                  {vista !== 'auditoria' && (
                    <td>
                      {vista === 'periodos' ? (
                        registro.activo ? (
                          <span>Activo</span>
                        ) : (
                          <button
                            type="button"
                            onClick={() => activar(registro.id)}
                          >
                            Activar
                          </button>
                        )
                      ) : vista === 'facultades' || vista === 'carreras' ? (
                        registro.activa ? (
                          <button
                            type="button"
                            onClick={() => desactivar(registro.id)}
                          >
                            Desactivar
                          </button>
                        ) : (
                          <span>Desactivada</span>
                        )
                      ) : (
                        <span>—</span>
                      )}
                    </td>
                  )}

                </tr>
              ))}
            </tbody>
          </table>
        )}

      </div>

      {/* FORMULARIOS DE CREACIÓN */}
      {vista !== 'auditoria' && (
        <form
          className="formulario"
          onSubmit={enviar}
        >
          <h2>
            {titulosFormulario[vista]}
          </h2>

          {/* CREAR USUARIO */}
          {vista === 'usuarios' && (
            <>
              {campo('correo', 'Correo electrónico', 'email')}
              {campo('password', 'Contraseña', 'password')}
              {campo('nombres', 'Nombres')}
              {campo('apellidos', 'Apellidos')}
              {campo('numeroDocumento', 'Número de documento')}
              {campo('tipoDocumento', 'Tipo de documento')}
              {campo('telefono', 'Teléfono')}

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
                  <option
                    key={rol.id}
                    value={rol.codigo}
                  >
                    {rol.nombre}
                  </option>
                ))}
              </select>
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
              {campo('facultadId', 'ID de facultad', 'number')}
              {campo('codigo', 'Código')}
              {campo('nombre', 'Nombre')}
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
            disabled={guardando}
          >
            {guardando ? 'Guardando...' : 'Guardar'}
          </button>

        </form>
      )}
    </>
  );
}

/* =========================================================
   CAMBIO OBLIGATORIO DE CONTRASEÑA
========================================================= */

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