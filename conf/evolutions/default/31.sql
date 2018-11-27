# --- !Ups

ALTER TABLE Supporter DROP FOREIGN KEY Supporter_ibfk_2;
ALTER TABLE Supporter DROP COLUMN `crew_id`;

CREATE TABLE Supporter_Crew(
  `supporter_id` BIGINT(20) NOT NULL,
  `crew_id` BIGINT(20) NOT NULL,
  `pillar` VARCHAR(255),
  `role` VARCHAR(255),
  `updated` BIGINT(20) NOT NULL,
  `created` BIGINT(20) NOT NULL,
  PRIMARY KEY (supporter_id, crew_id),
  FOREIGN KEY (supporter_id) REFERENCES Supporter(id),
  FOREIGN KEY (crew_id) REFERENCES Crew(id)
);

DROP VIEW Users;

CREATE VIEW Users AS
  SELECT DISTINCT u.id AS User_Id,
         uuid_of(u.public_id) AS User_publicId,
         u.roles AS User_roles,
         u.updated AS User_updated,
         u.created AS User_created,
         p.id AS Profile_id,
         p.confirmed AS Profile_confirmed,
         p.email AS Profile_email,
         p.user_id as Profile_userId,
         s.id AS Supporter_id,
         s.first_name AS Supporter_firstName,
         s.last_name AS Supporter_lastName,
         s.full_name AS Supporter_fullName,
         s.mobile_phone AS Supporter_mobilePhone,
         s.place_of_residence AS Supporter_placeOfResidence,
         s.birthday AS Supporter_birthday,
         s.sex AS Supporter_sex,
         s.profile_id AS Supporter_profileId,
         l.id AS LoginInfo_id,
         l.provider_id AS LoginInfo_providerId,
         l.provider_key AS LoginInfo_providerKey,
         l.profile_id AS LoginInfo_profileId,
         pI.id AS ProfileInfo_id,
         pI.hasher AS ProfileInfo_hasher,
         pI.password AS ProfileInfo_password,
         pI.profile_id AS ProfileInfo_profileId,
         o.id AS OAuth1Token_id,
         o.token AS OAuth1Token_token,
         o.secret AS OAuth1Token_secret,
         o.profile_id AS OAuth1Token_profileId,
         sc.supporter_id AS SupporterCrew_supporterId,
         sc.crew_id AS SupporterCrew_crewId,
         sc.role AS SupporterCrew_role,
         sc.pillar AS SupporterCrew_pillar,
         sc.created AS SupporterCrew_created,
         sc.updated AS SupporterCrew_updated,
         uuid_of(c.publicId) AS SupporterCrew_publicId,
         c.name AS SupporterCrew_name,
         c.country AS SupporterCrew_country
  FROM User AS u
    INNER JOIN Profile AS p ON p.user_id = u.id
    INNER JOIN Supporter AS s ON p.id = s.profile_id
    INNER JOIN LoginInfo AS l ON l.profile_id = p.id
    LEFT JOIN PasswordInfo as pI ON pI.profile_id = p.id
    LEFT JOIN OAuth1Info AS o on o.profile_id = p.id
    LEFT JOIN Supporter_Crew AS sc ON s.id = sc.supporter_id
    LEFT JOIN Crew AS c ON sc.crew_id = c.id;
    
ALTER TABLE Crew DROP COLUMN country;
ALTER TABLE City
    ADD COLUMN country VARCHAR(255);

# --- !Downs

DROP TABLE Supporter_Crew;

ALTER TABLE Supporter
    ADD crew_id BIGINT(20);
ALTER TABLE Supporter
    ADD FOREIGN KEY (crew_id) REFERENCES Crew(id) ON UPDATE CASCADE;

DROP VIEW Users;

ALTER VIEW Crews DROP COLUMN City.country;
ALTER TABLE City DROP COLUMN country;