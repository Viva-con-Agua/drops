# --- !Ups
CREATE TABLE Organization(
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  publicId BINARY(16),
  name VARCHAR(255),
  address VARCHAR(255),
  telefon VARCHAR(255),
  fax VARCHAR(255),
  email VARCHAR(255),
  executive VARCHAR(255),
  abbreviation VARCHAR(255),
  impressum VARCHAR(255),
  PRIMARY KEY (id)
);

# --- !Downs
DROP TABLE Organization;
