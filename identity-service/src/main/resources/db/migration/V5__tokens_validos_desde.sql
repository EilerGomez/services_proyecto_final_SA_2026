-- Revocación inmediata de sesiones: los JWT emitidos (iat) ANTES de esta fecha ya no se aceptan.
-- Se llena al retirar personal o piloto y al desactivar o bloquear la cuenta. Vacío = sin revocar.
ALTER TABLE usuarios ADD COLUMN tokens_validos_desde TIMESTAMPTZ;

-- El gateway carga al arrancar las revocaciones recientes (la última hora).
CREATE INDEX idx_usuarios_tokens_validos_desde ON usuarios (tokens_validos_desde)
    WHERE tokens_validos_desde IS NOT NULL;
