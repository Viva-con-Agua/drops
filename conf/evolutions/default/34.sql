# --- !Ups

CREATE TABLE Upload (
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    public_id BINARY(16) NOT NULL,
    parent_id BINARY(16),
    name VARCHAR(255) NOT NULL,
    contentType VARCHAR(50) NOT NULL,
    width INT NOT NULL,
    height INT NOT NULL,
    data BLOB NOT NULL,
    PRIMARY KEY (id)
);

# --- !Downs

DROP TABLE Upload;