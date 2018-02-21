# --- !Ups
CREATE TABLE OauthClient(
  id VARCHAR(255) NOT NULL,
  secret VARCHAR(255),
  redirectUri VARCHAR(255),
  grantTypes VARCHAR (255),
  PRIMARY KEY (id)
);


# --- !Downs
DROP TABLE OauthClient;