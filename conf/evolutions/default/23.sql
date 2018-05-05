# --- !Ups
CREATE TABLE Profile_Organization (
				profileId = BIGINT(20) NOT NULL,
				organizationId = BIGINT(20) NOT NULL,
				PRIMARY KEY (profileId, organizationId),
				FOREIGN KEY (profileId) REFERENCES Profile(id) ON UPDATE CASCADE ,
				FOREIGN KEY (organizationId) REFERENCES Organization(id) ON UPDATE CASCADE 
);

# --- !Downs
DROP TABLE Profile_Organization;
				
