
# --- !Ups
ALTER TABLE Crew DROP COLUMN country;
ALTER TABLE City
    ADD COLUMN country VARCHAR(255);

# --- !Downs
ALTER VIEW Crews DROP COLUMN City.country;
ALTER TABLE City DROP COLUMN country;
