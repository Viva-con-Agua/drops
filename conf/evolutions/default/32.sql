# --- !Ups

DROP VIEW Crews;

CREATE VIEW Crews AS
  SELECT DISTINCT Crew.id as Crew_id,
    uuid_of(Crew.publicId) as Crew_publicId,
    Crew.name as Crew_name,
    City.id as City_id,
    City.name as City_name,
    City.country as City_country,
    City.crew_id as City_crew_id
  from Crew
    INNER JOIN City ON Crew.id = City.crew_id;

# --- !Downs

DROP VIEW Users;
DROP VIEW Crews;
CREATE VIEW Crews AS
  SELECT DISTINCT Crew.id as Crew_id,
    uuid_of(Crew.publicId) as Crew_publicId,
    Crew.name as Crew_name,
    Crew.country as Crew_country,
    City.id as City_id,
    City.name as City_name,
    City.crew_id as City_crew_id
  from Crew
    INNER JOIN City ON Crew.id = City.crew_id;
