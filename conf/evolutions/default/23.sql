# --- !Ups
CREATE VIEW Crews AS
  SELECT Crew.id as Crew_id,
    uuid_of(Crew.publicId) as Crew_publicId,
    Crew.name as Crew_name,
    Crew.country as Crew_country,
    City.id as City_id,
    City.name as City_name,
    City.crew_id as City_crew_id
  from Crew
    INNER JOIN City ON Crew.id = City.crew_id;

CREATE TABLE Profile_Organization(
				profile_id BIGINT(20),
				organization_id BIGINT(20),
				role VARCHAR(255),
				PRIMARY KEY (profile_id, organization_id),
				FOREIGN KEY (profile_id) REFERENCES Profile(id) ON UPDATE CASCADE ,
				FOREIGN KEY (organization_id) REFERENCES Organization(id) ON UPDATE CASCADE 
);

# --- !Downs

DROP VIEW Crews;
DROP TABLE Profile_Organization;
				
