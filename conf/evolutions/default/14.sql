# Add Avatar Table
#
# Created by jottmann on 02.02.2018
#

# --- !Ups

CREATE TABLE Avatar(
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  typeOf VARCHAR(255),
  url VARCHAR(255),
  file BLOB,
  profile_id BIGINT(20) NOT NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (profile_id) REFERENCES Profile(id) ON UPDATE CASCADE
);

# --- !Downs

DROP TABLE Avatar;