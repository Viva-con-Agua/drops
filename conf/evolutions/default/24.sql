# --- !Ups
CREATE TABLE Bankaccount(
	id BIGINT(20) NOT NULL AUTO_INCREMENT,
	bankName VARCHAR(255),
	number VARCHAR(255),
	blz VARCHAR(255),
	iban VARCHAR(255),
	bic VARCHAR(255),
	organization_id BIGINT(20),
	PRIMARY KEY (id),
	FOREIGN KEY (organization_id) REFERENCES Organization(id) ON UPDATE CASCADE
);

# --- !Downs
DROP TABLE Bankaccount;
