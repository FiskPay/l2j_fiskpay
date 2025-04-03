-- Create the table if it doesn't exist
CREATE TABLE IF NOT EXISTS `gameservers` (
  `server_id` INT(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`server_id`)
);

-- Add missing columns if they do not exist
ALTER TABLE `gameservers`
  ADD COLUMN IF NOT EXISTS `hexid` VARCHAR(50) NOT NULL DEFAULT '',
  ADD COLUMN IF NOT EXISTS `host` VARCHAR(50) NOT NULL DEFAULT '',
  ADD COLUMN IF NOT EXISTS `balance` INT UNSIGNED NOT NULL DEFAULT 0;