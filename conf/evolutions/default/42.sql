# --- !Ups

ALTER TABLE User ADD termsOfService BOOLEAN;
ALTER TABLE User ADD rulesAccepted BOOLEAN;

# --- !Downs

ALTER TABLE User DROP termsOfService BOOLEAN;
ALTER TABLE User DROP rulesAccepted BOOLEAN;

