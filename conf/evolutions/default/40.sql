# --- !Ups

ALTER TABLE Supporter_Crew ADD active VARCHAR(255);
ALTER TABLE Supporter_Crew ADD nvm_date BIGINT(20);

# --- !Downs

ALTER TABLE Supporter_Crew DROP active VARCHAR(255);
ALTER TABLE Supporter_Crew DROP nvm_date BIGINT(20);

