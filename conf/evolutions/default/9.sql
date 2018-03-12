# Add Login Info and Supporter Table
#
# Created by jottmann on 13.12.2017
#

# --- !Ups

CREATE TABLE Supporter(
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  first_name VARCHAR(255),
  last_name VARCHAR(255),
  full_name VARCHAR(255),
  mobile_phone VARCHAR(255),
  place_of_residence VARCHAR(255),
  birthday BIGINT(20),
  sex VARCHAR(255),
  profile_id BIGINT(20) NOT NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (profile_id) REFERENCES Profile(id) ON UPDATE CASCADE
);

CREATE TABLE LoginInfo(
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  provider_id VARCHAR(255),
  provider_key VARCHAR(255),
  profile_id BIGINT(20) NOT NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (profile_id) REFERENCES Profile(id) ON UPDATE CASCADE
);

# --- !Downs

DROP TABLE Supporter;
DROP TABLE LoginInfo;