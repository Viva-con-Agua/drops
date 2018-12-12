# --- !Ups

ALTER TABLE Upload ADD COLUMN `selected` BOOL DEFAULT false;

# --- !Downs

ALTER TABLE Upload DROP COLUMN `selected`;