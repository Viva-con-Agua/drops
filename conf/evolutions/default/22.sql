# --- !Ups
CREATE FUNCTION uuid_of(uuid BINARY(16))
  RETURNS VARCHAR(36)
  RETURN LOWER(CONCAT(
                   SUBSTR(HEX(uuid), 1, 8), '-',
                   SUBSTR(HEX(uuid), 9, 4), '-',
                   SUBSTR(HEX(uuid), 13, 4), '-',
                   SUBSTR(HEX(uuid), 17, 4), '-',
                   SUBSTR(HEX(uuid), 21)
               ));

CREATE TABLE Organization(
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  publicId BINARY(16),
  name VARCHAR(255),
  address VARCHAR(255),
  telefon VARCHAR(255),
  fax VARCHAR(255),
  email VARCHAR(255),
	typ VARCHAR(255),
  executive VARCHAR(255),
  abbreviation VARCHAR(255),
  impressum BOOLEAN,
  PRIMARY KEY (id),
	UNIQUE KEY (name)
);
# --- !Downs

DROP FUNCTION uuid_of;
DROP TABLE Organization;