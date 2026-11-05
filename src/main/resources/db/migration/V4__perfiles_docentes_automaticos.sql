-- Completa perfiles docentes que quedaron sin crear cuando el administrador
-- asignó el rol DOCENTE antes de que existiera la sincronización automática.
INSERT INTO docente (usuario_id, created_at, updated_at)
SELECT DISTINCT ur.usuario_id, NOW(), NOW()
FROM usuario_rol ur
JOIN rol r ON r.id = ur.rol_id AND r.codigo = 'DOCENTE'
JOIN usuario u ON u.id = ur.usuario_id
WHERE NOT EXISTS (
    SELECT 1 FROM docente d WHERE d.usuario_id = ur.usuario_id
)
ON CONFLICT (usuario_id) DO NOTHING;
