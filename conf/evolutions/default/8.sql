# Add Profile Table
#
# Created by jottmann on 13.12.2017
#

# --- !Ups
CREATE TABLE Profile(
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  confirmed BOOLEAN NOT NULL,
  email VARCHAR(256) NOT NULL,
  user_id BIGINT(20) NOT NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (user_id) REFERENCES User(id) ON UPDATE CASCADE
);

# --- !Downs
DROP TABLE Profile;
