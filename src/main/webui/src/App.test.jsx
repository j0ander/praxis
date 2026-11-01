import { render, screen } from '@testing-library/react'
import App from './App.jsx'

test('muestra el estado devuelto por el servidor', async () => {
  render(<App obtener={() => Promise.resolve({ baseDatos: 'OK', versionEsquema: 2 })} />)
  expect(await screen.findByTestId('estado')).toBeTruthy()
})

test('muestra alerta si el servidor falla', async () => {
  render(<App obtener={() => Promise.reject(new Error('x'))} />)
  expect(await screen.findByRole('alert')).toBeTruthy()
})
