-- Autogenerated: do not edit this file
DROP TABLE IF EXISTS BATCH_STAGING_SEQ ;
DROP TABLE IF EXISTS TRADE_SEQ ;
DROP TABLE IF EXISTS CUSTOMER_SEQ ;
DROP TABLE IF EXISTS BATCH_STAGING ;
DROP TABLE IF EXISTS TRADE ;
DROP TABLE IF EXISTS CUSTOMER ;
DROP TABLE IF EXISTS PLAYERS ;
DROP TABLE IF EXISTS GAMES ;
DROP TABLE IF EXISTS PLAYER_SUMMARY ;

-- Autogenerated: do not edit this file

CREATE TABLE CUSTOMER_SEQ (ID BIGINT NOT NULL) type=MYISAM;
INSERT INTO CUSTOMER_SEQ values(0);
CREATE TABLE BATCH_STAGING_SEQ (ID BIGINT NOT NULL) type=MYISAM;
INSERT INTO BATCH_STAGING_SEQ values(0);
CREATE TABLE TRADE_SEQ (ID BIGINT NOT NULL) type=MYISAM;
INSERT INTO TRADE_SEQ values(0);

CREATE TABLE BATCH_STAGING  (
	ID BIGINT unsigned PRIMARY KEY ,  
	JOB_ID BIGINT NOT NULL,
	VALUE ${BLOB} NOT NULL,
	PROCESSED CHAR(1) NOT NULL
);

CREATE TABLE TRADE  (
	ID BIGINT unsigned PRIMARY KEY ,  
	VERSION BIGINT,
	ISIN VARCHAR(45) NOT NULL, 
	QUANTITY BIGINT,
	PRICE FLOAT, 
	CUSTOMER VARCHAR(45)
);
 
CREATE TABLE CUSTOMER (
	ID BIGINT unsigned PRIMARY KEY ,  
	VERSION BIGINT,
	NAME VARCHAR(45),
	CREDIT FLOAT
);
 
INSERT INTO customer (id, version, name, credit) VALUES (1, 0, 'customer1', 100000);
INSERT INTO customer (id, version, name, credit) VALUES (2, 0, 'customer2', 100000);
INSERT INTO customer (id, version, name, credit) VALUES (3, 0, 'customer3', 100000);
INSERT INTO customer (id, version, name, credit) VALUES (4, 0, 'customer4', 100000);

CREATE TABLE PLAYERS (
	PLAYER_ID char(8) NOT NULL PRIMARY KEY,  
	LAST_NAME varchar(35) not null,
	FIRST_NAME varchar(25) not null,
	POS varchar(10),
	YEAR_OF_BIRTH BIGINT not null,
	YEAR_DRAFTED BIGINT not null
);

CREATE TABLE GAMES (
   PLAYER_ID char(8) not null,
   YEAR_NO      BIGINT not null,
   TEAM      char(3) not null,
   WEEK      BIGINT not null,
   OPPONENT  char(3),
   COMPLETES BIGINT,
   ATTEMPTS  BIGINT,
   PASSING_YARDS BIGINT,
   PASSING_TD    BIGINT,
   INTERCEPTIONS BIGINT,
   RUSHES BIGINT,
   RUSH_YARDS BIGINT,
   RECEPTIONS BIGINT,
   RECEPTIONS_YARDS BIGINT,
   TOTAL_TD BIGINT
);

CREATE TABLE PLAYER_SUMMARY  (
		  ID CHAR(8) NOT NULL, 
		  YEAR_NO BIGINT NOT NULL,
		  COMPLETES BIGINT NOT NULL , 
		  ATTEMPTS BIGINT NOT NULL , 
		  PASSING_YARDS BIGINT NOT NULL , 
		  PASSING_TD BIGINT NOT NULL , 
		  INTERCEPTIONS BIGINT NOT NULL , 
		  RUSHES BIGINT NOT NULL , 
		  RUSH_YARDS BIGINT NOT NULL , 
		  RECEPTIONS BIGINT NOT NULL , 
		  RECEPTIONS_YARDS BIGINT NOT NULL , 
		  TOTAL_TD BIGINT NOT NULL
);