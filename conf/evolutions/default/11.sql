# Add Password Info
#
# Created by jottmann on 17.01.2018
#

# --- !Ups

CREATE TABLE PasswordInfo(
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  hasher VARCHAR(255),
  password VARCHAR(255),
  profile_id BIGINT(20) NOT NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (profile_id) REFERENCES Profile(id) ON UPDATE CASCADE
);

# --- !Downs

DROP TABLE PasswordInfo;