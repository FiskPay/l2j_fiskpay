-- Create the table if it doesn't exist
CREATE TABLE IF NOT EXISTS `gameservers` (
  `server_id` INT(3) NOT NULL DEFAULT 0,
  PRIMARY KEY (`server_id`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- Add missing columns if they do not exist
ALTER TABLE `gameservers`
  ADD COLUMN IF NOT EXISTS `hexid` VARCHAR(50) NOT NULL DEFAULT '',
  ADD COLUMN IF NOT EXISTS `host` VARCHAR(50) NOT NULL DEFAULT '',
  ADD COLUMN IF NOT EXISTS `balance` BIGINT UNSIGNED NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS `reward_id` INT(11) UNSIGNED NOT NULL DEFAULT 4037;

-- Insert default values if not already present
INSERT INTO `gameservers` (`server_id`, `hexid`, `host`) 
VALUES (2, '-2ad66b3f483c22be097019f55c8abdf0', '') 
ON DUPLICATE KEY UPDATE `hexid` = VALUES(`hexid`), `host` = VALUES(`host`);