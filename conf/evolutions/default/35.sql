# --- !Ups

ALTER TABLE Upload MODIFY data MEDIUMBLOB NOT NULL;

# --- !Downs

ALTER TABLE Upload MODIFY data BLOB NOT NULL;