-- Create the table if it doesn't exist
CREATE TABLE IF NOT EXISTS `accounts` (
  `login` VARCHAR(45) NOT NULL DEFAULT '',
  PRIMARY KEY (`login`)
);

-- Add missing columns if they do not exist
ALTER TABLE `accounts`
  ADD COLUMN IF NOT EXISTS `password` VARCHAR(60) NOT NULL DEFAULT '',
  ADD COLUMN IF NOT EXISTS `last_active` BIGINT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS `access_level` INT(3) NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS `last_server` INT(4) NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS `wallet_address` VARCHAR(42) NOT NULL DEFAULT 'not linked';

-- Ensure index exists on `wallet_address`
CREATE INDEX IF NOT EXISTS `wallet_address_index` ON `accounts` (`wallet_address`);
