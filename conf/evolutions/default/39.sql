# --- !Ups

ALTER TABLE User ADD COLUMN `termsOfService` BOOL DEFAULT true;
ALTER TABLE User ADD COLUMN `rulesAccepted` BOOL DEFAULT false;

# --- !Downs

ALTER TABLE User DROP `termsOfService` BOOLEAN;
ALTER TABLE User DROP `rulesAccepted` BOOLEAN;

