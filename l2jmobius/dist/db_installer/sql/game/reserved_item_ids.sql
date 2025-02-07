DROP TABLE IF EXISTS `reserved_item_ids`;
CREATE TABLE IF NOT EXISTS `reserved_item_ids` (
  `item_id` int(10) NOT NULL,
  PRIMARY KEY (`item_id`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
