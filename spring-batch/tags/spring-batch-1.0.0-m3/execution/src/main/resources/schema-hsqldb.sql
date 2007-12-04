-- Autogenerated: do not edit this file
DROP TABLE BATCH_STEP_EXECUTION IF EXISTS;
DROP TABLE BATCH_JOB_EXECUTION IF EXISTS;
DROP TABLE BATCH_STEP IF EXISTS;
DROP TABLE BATCH_JOB IF EXISTS;

DROP TABLE BATCH_STEP_EXECUTION_SEQ IF EXISTS;
DROP TABLE BATCH_STEP_SEQ IF EXISTS;
DROP TABLE BATCH_JOB_EXECUTION_SEQ IF EXISTS;
DROP TABLE BATCH_JOB_SEQ IF EXISTS;

-- Autogenerated: do not edit this file
CREATE TABLE BATCH_JOB  (
	ID BIGINT IDENTITY PRIMARY KEY ,  
	VERSION BIGINT,  
	JOB_NAME VARCHAR(100) NOT NULL , 
	JOB_KEY VARCHAR(250) , 
	SCHEDULE_DATE DATE ,
	STATUS VARCHAR(10) );

CREATE TABLE BATCH_JOB_EXECUTION  (
	ID BIGINT IDENTITY PRIMARY KEY ,
	VERSION BIGINT,  
	JOB_ID BIGINT NOT NULL,
	START_TIME TIMESTAMP NOT NULL , 
	END_TIME TIMESTAMP ,
	STATUS VARCHAR(10),
	CONTINUABLE CHAR(1),
	EXIT_CODE VARCHAR(20),
	EXIT_MESSAGE VARCHAR(250));

CREATE TABLE BATCH_STEP  (
	ID BIGINT IDENTITY PRIMARY KEY ,
	VERSION BIGINT,  
	JOB_ID BIGINT NOT NULL,
	STEP_NAME VARCHAR(100) NOT NULL,
	STATUS VARCHAR(10),
	RESTART_DATA VARCHAR(200));

CREATE TABLE BATCH_STEP_EXECUTION  (
	ID BIGINT IDENTITY PRIMARY KEY ,
	VERSION BIGINT NOT NULL,  
	STEP_ID BIGINT NOT NULL,
	JOB_EXECUTION_ID BIGINT NOT NULL,
	START_TIME TIMESTAMP NOT NULL , 
	END_TIME TIMESTAMP ,  
	STATUS VARCHAR(10),
	COMMIT_COUNT BIGINT , 
	TASK_COUNT BIGINT , 
	TASK_STATISTICS VARCHAR(250),
	CONTINUABLE CHAR(1),
	EXIT_CODE VARCHAR(20),
	EXIT_MESSAGE VARCHAR(250));

CREATE TABLE BATCH_STEP_EXECUTION_SEQ (
	ID BIGINT IDENTITY
);
CREATE TABLE BATCH_STEP_SEQ (
	ID BIGINT IDENTITY
);
CREATE TABLE BATCH_JOB_EXECUTION_SEQ (
	ID BIGINT IDENTITY
);
CREATE TABLE BATCH_JOB_SEQ (
	ID BIGINT IDENTITY
);
