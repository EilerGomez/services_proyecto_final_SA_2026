-- =====================================================================
-- V3__descripcion_categorias.sql · maintenance-service
-- Descripción de las categorías de gasto que crea la V1 (se ven en Taller → Categorías y ayudan a
-- elegir la correcta al registrar un servicio). Solo se llena si sigue vacía: no pisa lo que un
-- administrador ya haya escrito.
-- =====================================================================

UPDATE categorias_gasto AS c
SET descripcion = d.descripcion
FROM (VALUES
  ('Llantas',          'Compra, cambio, reparación, rotación y balanceo de llantas y aros'),
  ('Motor',            'Reparación y ajuste del motor: inyección, distribución, culata, refrigeración y escape'),
  ('Frenos',           'Pastillas, zapatas, discos, tambores, líquido y revisión del sistema de frenos'),
  ('Suspensión',       'Amortiguadores, resortes, bolsas de aire, bujes, terminales y alineación'),
  ('Cambio de aceite', 'Aceite de motor, filtros de aceite, aire y combustible, y lubricación general'),
  ('Eléctrico',        'Batería, alternador, arranque, luces, cableado y tablero'),
  ('Carrocería',       'Pintura, golpes, vidrios, espejos, puertas y asientos'),
  ('Otros',            'Gastos de taller que no entran en las demás categorías')
) AS d(nombre, descripcion)
WHERE c.nombre = d.nombre
  AND c.descripcion IS NULL;
