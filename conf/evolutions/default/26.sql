# Instead of ALTER VIEW, DROP and CREATE are used, since otherwise the order of resulting columns is not the same as
# expected by the Scala code.

# --- !Ups

DROP VIEW Users;

CREATE VIEW Users AS
  SELECT u.id AS User_Id,
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
         s.crew_id as Supporter_crewId,
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
         o.profile_id AS OAuth1Token_profileId
  FROM User AS u
    INNER JOIN Profile AS p ON p.user_id = u.id
    INNER JOIN Supporter AS s ON p.id = s.profile_id
    INNER JOIN LoginInfo AS l ON l.profile_id = p.id
    LEFT JOIN PasswordInfo as pI ON pI.profile_id = pI.id
    LEFT JOIN OAuth1Info AS o on o.profile_id = p.id;

# --- !Downs

DROP VIEW Users;

CREATE VIEW Users AS
  SELECT u.id AS User_Id,
         uuid_of(u.public_id) AS User_publicId,
         u.roles AS User_roles,
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
         s.crew_id as Supporter_crewId,
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
         o.profile_id AS OAuth1Token_profileId
  FROM User AS u
    INNER JOIN Profile AS p ON p.user_id = u.id
    INNER JOIN Supporter AS s ON p.id = s.profile_id
    INNER JOIN LoginInfo AS l ON l.profile_id = p.id
    LEFT JOIN PasswordInfo as pI ON pI.profile_id = pI.id
    LEFT JOIN OAuth1Info AS o on o.profile_id = p.id;