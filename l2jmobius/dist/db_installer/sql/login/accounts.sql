-- Create the table if it doesn't exist
CREATE TABLE IF NOT EXISTS `accounts` (
  `login` VARCHAR(45) NOT NULL DEFAULT '',
  PRIMARY KEY (`login`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- Check and add columns if they do not exist
SET @table_name = 'accounts';

-- Add `password` column if not exists
SET @col_name = 'password';
SET @sql = (SELECT IF(
  EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = @table_name AND COLUMN_NAME = @col_name),
  'SELECT 1',
  'ALTER TABLE `accounts` ADD COLUMN `password` VARCHAR(45)'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add `email` column if not exists
SET @col_name = 'email';
SET @sql = (SELECT IF(
  EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = @table_name AND COLUMN_NAME = @col_name),
  'SELECT 1',
  'ALTER TABLE `accounts` ADD COLUMN `email` VARCHAR(255) DEFAULT NULL'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add `created_time` column if not exists
SET @col_name = 'created_time';
SET @sql = (SELECT IF(
  EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = @table_name AND COLUMN_NAME = @col_name),
  'SELECT 1',
  'ALTER TABLE `accounts` ADD COLUMN `created_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add `lastactive` column if not exists
SET @col_name = 'lastactive';
SET @sql = (SELECT IF(
  EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = @table_name AND COLUMN_NAME = @col_name),
  'SELECT 1',
  'ALTER TABLE `accounts` ADD COLUMN `lastactive` BIGINT(13) UNSIGNED NOT NULL DEFAULT 0'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add `accessLevel` column if not exists
SET @col_name = 'accessLevel';
SET @sql = (SELECT IF(
  EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = @table_name AND COLUMN_NAME = @col_name),
  'SELECT 1',
  'ALTER TABLE `accounts` ADD COLUMN `accessLevel` TINYINT NOT NULL DEFAULT 0'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add `lastIP` column if not exists
SET @col_name = 'lastIP';
SET @sql = (SELECT IF(
  EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = @table_name AND COLUMN_NAME = @col_name),
  'SELECT 1',
  'ALTER TABLE `accounts` ADD COLUMN `lastIP` CHAR(15) NULL DEFAULT NULL'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add `lastServer` column if not exists
SET @col_name = 'lastServer';
SET @sql = (SELECT IF(
  EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = @table_name AND COLUMN_NAME = @col_name),
  'SELECT 1',
  'ALTER TABLE `accounts` ADD COLUMN `lastServer` TINYINT DEFAULT 1'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add `pcIp`, `hop1`, `hop2`, `hop3`, `hop4` columns if not exists
SET @col_name = 'pcIp';
SET @sql = (SELECT IF(
  EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = @table_name AND COLUMN_NAME = @col_name),
  'SELECT 1',
  'ALTER TABLE `accounts` ADD COLUMN `pcIp` CHAR(15) DEFAULT NULL'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_name = 'hop1';
SET @sql = (SELECT IF(
  EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = @table_name AND COLUMN_NAME = @col_name),
  'SELECT 1',
  'ALTER TABLE `accounts` ADD COLUMN `hop1` CHAR(15) DEFAULT NULL'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_name = 'hop2';
SET @sql = (SELECT IF(
  EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = @table_name AND COLUMN_NAME = @col_name),
  'SELECT 1',
  'ALTER TABLE `accounts` ADD COLUMN `hop2` CHAR(15) DEFAULT NULL'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_name = 'hop3';
SET @sql = (SELECT IF(
  EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = @table_name AND COLUMN_NAME = @col_name),
  'SELECT 1',
  'ALTER TABLE `accounts` ADD COLUMN `hop3` CHAR(15) DEFAULT NULL'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_name = 'hop4';
SET @sql = (SELECT IF(
  EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = @table_name AND COLUMN_NAME = @col_name),
  'SELECT 1',
  'ALTER TABLE `accounts` ADD COLUMN `hop4` CHAR(15) DEFAULT NULL'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add `wallet_address` column if not exists
SET @col_name = 'wallet_address';
SET @sql = (SELECT IF(
  EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = @table_name AND COLUMN_NAME = @col_name),
  'SELECT 1',
  'ALTER TABLE `accounts` ADD COLUMN `wallet_address` VARCHAR(42) NOT NULL DEFAULT "not linked"'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ensure the index on `wallet_address` exists
SET @index_name = 'wallet_address';
SET @sql = (SELECT IF(
  EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_NAME = @table_name AND INDEX_NAME = @index_name),
  'SELECT 1',
  'ALTER TABLE `accounts` ADD KEY `wallet_address` (`wallet_address`)'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;