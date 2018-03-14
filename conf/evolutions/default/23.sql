# --- !Ups
CREATE VIEW Crews AS
  SELECT Crew.id,
    uuid_of(Crew.publicId),
    Crew.name,
    Crew.country,
    City.id as City_id,
    City.name as City_name,
    City.crew_id
  from Crew
    INNER JOIN City ON Crew.id = City.crew_id;

# --- !Downs

DROP VIEW Crews;