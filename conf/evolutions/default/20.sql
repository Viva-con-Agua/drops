# --- !Ups
DROP TABLE User_Role;
DROP TABLE Role;


ALTER TABLE User
    ADD roles VARCHAR(255);

# --- !Downs
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

ALTER TABLE User DROP COLUMN roles;