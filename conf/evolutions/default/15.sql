# User Token Schema

# --- !Ups
CREATE TABLE UserToken(
  id BINARY(16) NOT NULL,
  user_id BINARY(16),
  email VARCHAR(255),
  expiration_time BIGINT(20),
  is_sign_up BOOLEAN,
  PRIMARY KEY (id)
);


# --- !Downs
DROP TABLE UserToken;

