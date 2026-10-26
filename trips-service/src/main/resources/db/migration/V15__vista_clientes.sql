-- =====================================================================
-- V15 · trips-service · Copia local de los clientes con cuenta (de Identity)
-- La llenan los manejadores de ClienteRegistrado y PerfilActualizado.
--
-- Para qué: cuando la ventanilla pide un alquiler a nombre de alguien, el NIT es lo único que se
-- teclea. Si ese NIT es de una cuenta del sistema, el alquiler debe quedar ligado a ella
-- (cliente_usuario_id) para que le aparezca al cliente en "Mis alquileres" y pueda pagarlo con su
-- billetera. Sin esta copia, Trips no tendría cómo saber de quién es el NIT.
--
-- Identity no manda version en estos eventos: gana el último que llega (igual que en Sales).
-- =====================================================================

CREATE TABLE vista_clientes (
  usuario_id  UUID          PRIMARY KEY,
  nit         VARCHAR(20)   NOT NULL,
  nombre      VARCHAR(201)  NOT NULL,
  correo      VARCHAR(150)  NOT NULL
);

-- Búsqueda por NIT sin distinguir mayúsculas (la K del NIT puede venir escrita de las dos formas)
CREATE INDEX ix_vista_clientes_nit ON vista_clientes (upper(nit));
