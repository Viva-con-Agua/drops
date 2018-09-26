# ACL Schema
#
# Created bei Johann Sell on 21.09.2018
#

# --- !Ups
ALTER TABLE `User`
  ADD `updated` BIGINT(20);
ALTER TABLE `User`
  ADD `created` BIGINT(20);

# --- !Downs
ALTER TABLE `User`
  DROP COLUMN `updated`;
ALTER TABLE `User`
  DROP COLUMN `created`;
