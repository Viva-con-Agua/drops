# Add OAuth1 Info
#
# Created by jottmann on 02.02.2018
#

# --- !Ups

CREATE TABLE OAuth1Info(
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  token VARCHAR(255),
  secret VARCHAR(255),
  profile_id BIGINT(20) NOT NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (profile_id) REFERENCES Profile(id) ON UPDATE CASCADE
);

# --- !Downs

DROP TABLE OAuth1Info;