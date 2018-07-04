# --- !Ups
CREATE TABLE Pool1User(
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  email VARCHAR(255),
  confirmed BOOLEAN,
  PRIMARY KEY (id)
);

# --- !Downs
DROP Table Pool1User;
