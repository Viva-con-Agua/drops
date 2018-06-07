# --- !Ups
CREATE TABLE Profile_Organization(
				profile_id BIGINT(20),
				organization_id BIGINT(20),
				role VARCHAR(255),
				PRIMARY KEY (profile_id, organization_id),
				FOREIGN KEY (profile_id) REFERENCES Profile(id) ON UPDATE CASCADE ,
				FOREIGN KEY (organization_id) REFERENCES Organization(id) ON UPDATE CASCADE 
);

# --- !Downs
DROP TABLE Profile_Organization;
				
