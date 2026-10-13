-- Migración Flyway V3 · reporting-service
-- Sales manda cliente_nombre de hasta 201 caracteres (nombres 100 + espacio + apellidos 100)
-- y la V1 lo dejó en VARCHAR(200). En reporting solo ingresos tiene esa columna.

ALTER TABLE ingresos ALTER COLUMN cliente_nombre TYPE VARCHAR(201);
