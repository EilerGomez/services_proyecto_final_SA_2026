-- =====================================================================
-- V4__cartera_a_wallet.sql · La cartera digital se movió a wallet-service (bd_cartera).
-- Identity ya no guarda saldos ni movimientos. Los triggers de estas tablas se borran con ellas.
-- (V1 no se modifica: Flyway valida su checksum en las bases que ya la aplicaron.)
-- =====================================================================
DROP TABLE IF EXISTS movimientos_cartera;
DROP TABLE IF EXISTS carteras;
