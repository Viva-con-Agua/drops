# --- !Ups

CREATE TABLE Address (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  public_id BINARY(16) NOT NULL,
  supporter_id BIGINT(10) NOT NULL,
  street TEXT NOT NULL,
  additional TEXT DEFAULT NULL,
  zip VARCHAR(10) NOT NULL,
  city TEXT NOT NULL ,
  country TEXT NOT NULL,
  PRIMARY KEY (id),
  FOREIGN KEY supporter_id REFERENCES Supporter(id) ON UPDATE CASCADE;
);

# --- !Downs

DROP TABLE Address;