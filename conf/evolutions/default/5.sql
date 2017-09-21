# Relation User - Task
#
# Created bei jottmann on 19.09.2017
#

# --- !Ups
CREATE TABLE User_Task (
  user_id BINARY(16) NOT NULL,
  task_id BIGINT(20) NOT NULL,

  PRIMARY KEY  (user_id, task_id),
  FOREIGN KEY (task_id) REFERENCES Task(id) ON UPDATE CASCADE
);

# --- !Downs
DROP TABLE User_Task;