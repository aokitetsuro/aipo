-- 20130930
ALTER TABLE TURBINE_USER ADD MIGRATE_VERSION INTEGER NOT NULL DEFAULT 0;
UPDATE TURBINE_USER SET MIGRATE_VERSION = 0 ;
ALTER TABLE TURBINE_USER ALTER COLUMN TUTORIAL_FORBID TYPE VARCHAR (64);
-- 20130930