# Relation Task - AccessRight
#
# Created bei jottmann on 29.08.2017
#

# --- !Ups
CREATE TABLE Task_AccessRight (
  task_id BIGINT(20) NOT NULL,
  access_right_id BIGINT(20) NOT NULL,
  PRIMARY KEY  (task_id, access_right_id),
  FOREIGN KEY (task_id) REFERENCES Task(id) ON UPDATE CASCADE ,
  FOREIGN KEY (access_right_id) REFERENCES AccessRight(id) ON UPDATE CASCADE
);

# --- !Downs
DROP TABLE Task_AccessRight;