# --- !Ups
CREATE TABLE OauthToken(
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  token VARCHAR(255),
  refresh_token VARCHAR(255),
  scope VARCHAR(255),
  life_seconds BIGINT(20),
  created_at DATETIME,
  user_id  BINARY(16),
  client_id  VARCHAR(255),
  PRIMARY KEY (id),
  FOREIGN KEY (user_id) REFERENCES User(public_id) ON UPDATE CASCADE,
  FOREIGN KEY (client_id) REFERENCES OauthClient(id) ON UPDATE CASCADE
);

# --- !Downs
DROP TABLE OauthToken;