# ACL Schema
#
# Created bei jottmann on 29.08.2017
#

# --- !Ups
ALTER TABLE AccessRight
  ADD method VARCHAR(255);

# --- !Downs
ALTER TABLE AccessRight
  DROP COLUMN method;
