# --- !Ups

DROP Table Supporter_Crew;

CREATE TABLE Supporter_Crew(
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `supporter_id` BIGINT(20) NOT NULL,
  `crew_id` BIGINT(20) NOT NULL,
  `pillar` VARCHAR(255),
  `role` VARCHAR(255),
  `updated` BIGINT(20) NOT NULL,
  `created` BIGINT(20) NOT NULL,
  `nvmDate` BIGINT(20), 
  PRIMARY KEY (id),
  FOREIGN KEY (supporter_id) REFERENCES Supporter(id),
  FOREIGN KEY (crew_id) REFERENCES Crew(id)
);

# --- !Downs

Drop Table Supporter_Crew;

CREATE TABLE Supporter_Crew(
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `supporter_id` BIGINT(20) NOT NULL,
  `crew_id` BIGINT(20) NOT NULL,
  `pillar` VARCHAR(255),
  `role` VARCHAR(255),
  `updated` BIGINT(20) NOT NULL,
  `created` BIGINT(20) NOT NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (supporter_id) REFERENCES Supporter(id),
  FOREIGN KEY (crew_id) REFERENCES Crew(id)
);

