# ACL Schema
#
# Created bei jottmann on 21.09.2017
#

# --- !Ups
ALTER TABLE AccessRight
  ADD service VARCHAR(255);

# --- !Downs
ALTER TABLE AccessRight
  DROP COLUMN service;
