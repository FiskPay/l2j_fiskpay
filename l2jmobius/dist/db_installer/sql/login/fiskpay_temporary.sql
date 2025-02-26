-- Create the table if it doesn't exist
CREATE TABLE IF NOT EXISTS `fiskpay_temporary` (
  `server_id` INT(11) UNSIGNED NOT NULL,
  `character_name` VARCHAR(45) NOT NULL,
  `refund` INT(10) UNSIGNED NOT NULL,
  `amount` INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (`server_id`,`character_id`,`refund`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;