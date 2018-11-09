# Add User Table and Update the FK Data Type for the User reference
#
# Created by jottmann on 29.11.2017
#

# --- !Ups
CREATE TABLE User (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  public_id VARCHAR(36) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT public_id_constraint UNIQUE (public_id)
);

ALTER TABLE User_Task
  MODIFY user_id VARCHAR(36);

# --- !Downs
DROP TABLE User;

ALTER TABLE User_Task
  MODIFY user_id BINARY(16);