DROP TABLE IF EXISTS `fiskpay_withdrawals`;
CREATE TABLE IF NOT EXISTS `fiskpay_withdrawals` (
  `server_id` int(11) NOT NULL,
  `transaction_hash` varchar(66) NOT NULL,
  `character_name` varchar(35) NOT NULL,
  `wallet_address` varchar(42) NOT NULL,
  `amount` int(10) UNSIGNED NOT NULL,
  PRIMARY KEY (`transaction_hash`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
