-- Create the table if it doesn't exist
CREATE TABLE IF NOT EXISTS `fiskpay_withdrawals` (
  `transaction_hash` VARCHAR(66) NOT NULL,
  `server_id` INT(11) NOT NULL,
  `character_name` VARCHAR(45) NOT NULL,
  `wallet_address` VARCHAR(42) NOT NULL,
  `amount` BIGINT UNSIGNED NOT NULL,
  `withdrawal_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`transaction_hash`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;