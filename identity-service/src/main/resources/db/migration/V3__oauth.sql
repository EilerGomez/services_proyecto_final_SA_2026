-- V3: inicio de sesión con Google (OAuth2 Authorization Code en el backend).
-- Los usuarios OAuth no tienen contraseña: contrasena_hash pasa a nullable.
-- proveedor/proveedor_id identifican la cuenta externa (GOOGLE/<sub>); LOCAL = registro clásico.
-- El perfil de un usuario OAuth se completa después: nit/dpi/telefono/direccion pasan a nullable
-- (el registro clásico sigue exigiéndolos por validación en RegistroRequest).

ALTER TABLE usuarios ALTER COLUMN contrasena_hash DROP NOT NULL;

ALTER TABLE usuarios ADD COLUMN proveedor VARCHAR(20)  NOT NULL DEFAULT 'LOCAL';
ALTER TABLE usuarios ADD COLUMN proveedor_id VARCHAR(255);

-- Un mismo correo no puede quedar vinculado a dos cuentas externas distintas por accidente:
-- la unicidad (proveedor, proveedor_id) lo impide (los NULL no colisionan en PostgreSQL).
CREATE UNIQUE INDEX ux_usuarios_proveedor_externo ON usuarios(proveedor, proveedor_id);

ALTER TABLE perfiles ALTER COLUMN nit DROP NOT NULL;
ALTER TABLE perfiles ALTER COLUMN dpi DROP NOT NULL;
ALTER TABLE perfiles ALTER COLUMN telefono DROP NOT NULL;
ALTER TABLE perfiles ALTER COLUMN direccion DROP NOT NULL;
