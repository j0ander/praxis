-- V2__catalogos.sql  (producción: solo roles)
INSERT INTO rol (codigo, nombre, descripcion) VALUES
('ESTUDIANTE',        'Estudiante',                'Postula, registra horas e informes'),
('DOCENTE',           'Docente / Tutor académico', 'Evalúa, aprueba informes, registra visitas'),
('TUTOR_EMPRESARIAL', 'Tutor empresarial',         'Valida horas, evalúa desempeño'),
('COORDINADOR',       'Coordinador de prácticas',  'Gestiona convenios, requisitos y certificados'),
('ADMINISTRADOR',     'Administrador',             'Gestiona usuarios, roles y catálogos'),
('AUTORIDAD',         'Autoridad académica',       'Solo lectura: reportes y estadísticas');
