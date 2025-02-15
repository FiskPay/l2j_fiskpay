-- Create the table if it doesn't exist
CREATE TABLE IF NOT EXISTS `fiskpay_temporary` (
  `server_id` int(11) NOT NULL,
  `character_id` int(10) NOT NULL,
  `refund` int(10) UNSIGNED NOT NULL,
  `amount` int(10) UNSIGNED NOT NULL,
  PRIMARY KEY (`server_id`,`character_id`,`refund`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;