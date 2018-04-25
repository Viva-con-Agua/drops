# ACL Schema
#
# Created bei jottmann on 29.08.2017
#

# --- !Ups
CREATE TABLE AccessRight(
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  uri VARCHAR(512) NOT NULL,
  name VARCHAR(255),
  description TEXT,
  PRIMARY KEY (id)
);


# --- !Downs
DROP TABLE AccessRight;
