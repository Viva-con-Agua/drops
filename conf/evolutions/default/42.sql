# --- !Ups

ALTER TABLE User ADD termsOfService BOOLEAN;
ALTER TABLE Profile ADD actionRule BOOLEAN;

# --- !Downs

ALTER TABLE User DROP termsOfService BOOLEAN;
ALTER TABLE Profile DROP actionRule BOOLEAN;

