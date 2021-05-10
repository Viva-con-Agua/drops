# --- !Ups
ALTER TABLE Address
    MODIFY public_id BINARY(16);

# --- !Downs
ALTER TABLE Address
    MODIFY public_id VARCHAR(36);