-- Create the table if it doesn't exist
CREATE TABLE IF NOT EXISTS `fiskpay_withdrawals` (
  `transaction_hash` varchar(66) NOT NULL,
  `server_id` int(11) NOT NULL,
  `character_name` varchar(35) NOT NULL,
  `wallet_address` varchar(42) NOT NULL,
  `amount` int(10) UNSIGNED NOT NULL,
  `served` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`transaction_hash`),
  KEY `served` (`served`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
