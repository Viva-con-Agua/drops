# --- !Ups
CREATE TABLE OauthCode(
    code VARCHAR(255),
    user_id BINARY(16),
    client_id VARCHAR(255),
    created BIGINT(20),
    PRIMARY KEY (user_id, client_id),
    FOREIGN KEY (user_id) REFERENCES User(public_id) ON UPDATE CASCADE,
    FOREIGN KEY (client_id) REFERENCES OauthClient(id) ON UPDATE CASCADE
);
# --- !Downs
DROP TABLE OauthCode;