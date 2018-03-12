# --- !Ups
ALTER TABLE User_Task
    MODIFY user_id BIGINT(20);

ALTER TABLE User
    MODIFY public_id BINARY(16);

# --- !Downs
ALTER TABLE User_Task
    MODIFY user_id VARCHAR(36);
ALTER TABLE User
    MODIFY public_id VARCHAR(36);