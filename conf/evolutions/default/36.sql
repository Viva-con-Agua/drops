# --- !Ups

ALTER TABLE Upload ADD COLUMN `email` VARCHAR(256) NOT NULL;

# --- !Downs

ALTER TABLE Upload DROP COLUMN `email`;