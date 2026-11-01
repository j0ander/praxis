-- R__seed_dev.sql  (solo %dev / %test). Repetible e idempotente.
INSERT INTO tipo_practica (nombre, horas_requeridas, nota_minima) VALUES
('Prácticas pre-profesionales I',  240, 14.00),
('Prácticas pre-profesionales II', 240, 14.00),
('Pasantías',                      120, 14.00)
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO requisito (nombre, descripcion, obligatorio, carrera_id) VALUES
('Haber aprobado el 60% de la malla curricular', NULL, TRUE, NULL),
('Estar matriculado en el periodo vigente',       NULL, TRUE, NULL),
('Aprobar el curso de inducción de prácticas',    NULL, TRUE, NULL)
ON CONFLICT ON CONSTRAINT uq_requisito_nombre_carrera DO NOTHING;
