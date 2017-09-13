# Task Schema

# --- !Ups
CREATE TABLE Task (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL,
  description TEXT,
  deadline DATETIME,
  count_supporter INT,
  PRIMARY KEY (id)
);

# --- !Downs

DROP TABLE Task;