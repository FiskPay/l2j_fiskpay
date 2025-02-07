DROP TABLE IF EXISTS `gameservers`;
CREATE TABLE IF NOT EXISTS `gameservers` (
  `server_id` int(11) NOT NULL DEFAULT '0',
  `hexid` varchar(50) NOT NULL DEFAULT '',
  `host` varchar(50) NOT NULL DEFAULT '',
  `balance` int(10) UNSIGNED NOT NULL DEFAULT '0',
  PRIMARY KEY (`server_id`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

INSERT INTO `gameservers` VALUES ('2', '-2ad66b3f483c22be097019f55c8abdf0', '');
