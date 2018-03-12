# Add Role Table and User Role n:m Relation
#
# Created by jottmann on 02.02.2018
#

# --- !Ups

CREATE TABLE Role(
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  role VARCHAR(255),
  PRIMARY KEY (id)
);

CREATE TABLE User_Role(
  user_id BIGINT(20),
  role_id BIGINT(20),
  PRIMARY KEY (user_id, role_id),
  FOREIGN KEY (user_id) REFERENCES User(id) ON UPDATE CASCADE,
  FOREIGN KEY (role_id) REFERENCES Role(id) ON UPDATE CASCADE
);

# --- !Downs

DROP TABLE Role;
DROP TABLE User_Role;