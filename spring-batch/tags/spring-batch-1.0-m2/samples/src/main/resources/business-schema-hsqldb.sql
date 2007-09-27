DROP TABLE TRADE IF EXISTS;
DROP TABLE TRADE_SEQ IF EXISTS;
DROP TABLE CUSTOMER IF EXISTS;
DROP TABLE CUSTOMER_SEQ IF EXISTS;
DROP TABLE PLAYERS IF EXISTS;
DROP TABLE GAMES IF EXISTS;
DROP TABLE PLAYER_SUMMARY IF EXISTS;

CREATE TABLE TRADE  (
		  ID BIGINT PRIMARY KEY,  
		  VERSION BIGINT,
 		  ISIN VARCHAR(45) NOT NULL, 
		  QUANTITY BIGINT,
		  PRICE FLOAT, 
		  CUSTOMER VARCHAR(45)
);


CREATE TABLE TRADE_SEQ (
		ID BIGINT IDENTITY
);

CREATE TABLE CUSTOMER (
	ID INTEGER PRIMARY KEY,
    VERSION BIGINT,
	NAME VARCHAR(45),
	CREDIT FLOAT
);

CREATE TABLE CUSTOMER_SEQ (
		ID BIGINT IDENTITY
);

INSERT INTO customer (id, version, name, credit) VALUES (1, 0, 'customer1', 100000);
INSERT INTO customer (id, version, name, credit) VALUES (2, 0, 'customer2', 100000);
INSERT INTO customer (id, version, name, credit) VALUES (3, 0, 'customer3', 100000);
INSERT INTO customer (id, version, name, credit) VALUES (4, 0, 'customer4', 100000);

CREATE TABLE PLAYERS (
	PLAYER_ID char(8) primary key,
	LAST_NAME varchar(35) not null,
	FIRST_NAME varchar(25) not null,
	POS varchar(10),
	YEAR_OF_BIRTH BIGINT not null,
	YEAR_DRAFTED BIGINT not null);

CREATE TABLE GAMES (
   PLAYER_ID char(8) not null,
   YEAR      integer not null,
   TEAM      char(3) not null,
   WEEK      integer not null,
   OPPONENT  char(3),
   COMPLETES integer,
   ATTEMPTS  integer,
   PASSING_YARDS integer,
   PASSING_TD    integer,
   INTERCEPTIONS integer,
   RUSHES integer,
   RUSH_YARDS integer,
   RECEPTIONS integer,
   RECEPTIONS_YARDS integer,
   TOTAL_TD integer
);

CREATE TABLE PLAYER_SUMMARY  (
		  ID CHAR(8) NOT NULL , 
		  YEAR INTEGER NOT NULL,
		  COMPLETES INTEGER NOT NULL , 
		  ATTEMPTS INTEGER NOT NULL , 
		  PASSING_YARDS INTEGER NOT NULL , 
		  PASSING_TD INTEGER NOT NULL , 
		  INTERCEPTIONS INTEGER NOT NULL , 
		  RUSHES INTEGER NOT NULL , 
		  RUSH_YARDS INTEGER NOT NULL , 
		  RECEPTIONS INTEGER NOT NULL , 
		  RECEPTIONS_YARDS INTEGER NOT NULL , 
		  TOTAL_TD INTEGER NOT NULL );   