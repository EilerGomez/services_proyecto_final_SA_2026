-- =====================================================================
-- V3 · sales-service · Copia local de los clientes de Identity
-- La escriben solo los manejadores de ClienteRegistrado y PerfilActualizado (upsert, gana el último:
-- Identity no manda version). S2 toma de aquí NIT, nombre y correo; S4 completa los datos si el NIT existe;
-- S7 busca el NIT del usuario del token.
-- nit sin UNIQUE: si un cliente cambia su NIT y el evento de otro llega antes, un UNIQUE haría fallar el
-- manejador (y el evento iría a la DLQ). La unicidad ya la garantiza Identity (perfiles.nit UNIQUE).
-- =====================================================================

CREATE TABLE vista_clientes (
  usuario_id  UUID          PRIMARY KEY,               
  nit         VARCHAR(20)   NOT NULL,
  nombre      VARCHAR(201)  NOT NULL,                   
  correo      VARCHAR(150)  NOT NULL
);

CREATE INDEX ix_vista_clientes_nit ON vista_clientes (nit);
