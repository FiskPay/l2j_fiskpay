-- Create the table if it doesn't exist
CREATE TABLE IF NOT EXISTS `gameservers` (
  `server_id` INT(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`server_id`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- Check and add columns if they do not exist
SET @table_name = 'gameservers';

-- Add `server_id` column if not exists
SET @col_name = 'server_id';
SET @sql = (SELECT IF(
  EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = @table_name AND COLUMN_NAME = @col_name),
  'SELECT 1',
  'ALTER TABLE `gameservers` ADD COLUMN `server_id` INT(11) NOT NULL DEFAULT 0'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add `hexid` column if not exists
SET @col_name = 'hexid';
SET @sql = (SELECT IF(
  EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = @table_name AND COLUMN_NAME = @col_name),
  'SELECT 1',
  'ALTER TABLE `gameservers` ADD COLUMN `hexid` VARCHAR(50) NOT NULL DEFAULT ""'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add `host` column if not exists
SET @col_name = 'host';
SET @sql = (SELECT IF(
  EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = @table_name AND COLUMN_NAME = @col_name),
  'SELECT 1',
  'ALTER TABLE `gameservers` ADD COLUMN `host` VARCHAR(50) NOT NULL DEFAULT ""'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add `balance` column if not exists
SET @col_name = 'balance';
SET @sql = (SELECT IF(
  EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = @table_name AND COLUMN_NAME = @col_name),
  'SELECT 1',
  'ALTER TABLE `gameservers` ADD COLUMN `balance` INT(10) UNSIGNED NOT NULL DEFAULT 0'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ensure the primary key on `server_id` exists (this should already exist but a check can be added for safety)
SET @index_name = 'PRIMARY';
SET @sql = (SELECT IF(
  EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE WHERE TABLE_NAME = @table_name AND CONSTRAINT_NAME = @index_name),
  'SELECT 1',
  'ALTER TABLE `gameservers` ADD PRIMARY KEY (`server_id`)'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO `gameservers` VALUES ('2', '-2ad66b3f483c22be097019f55c8abdf0', '');