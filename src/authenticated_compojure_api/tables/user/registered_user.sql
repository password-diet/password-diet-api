-- name: create-registered-user-table-if-not-exists!
-- create the registered_user table if it does not exist
CREATE TABLE IF NOT EXISTS registered_user (
   id              SERIAL PRIMARY KEY NOT NULL
   , email         CITEXT             NOT NULL UNIQUE
   , username      CITEXT             NOT NULL UNIQUE
   , password      TEXT               NOT NULL
   , refresh_token TEXT
);

-- name: drop-registered-user-table!
-- drop the registered_user table
DROP TABLE registered_user;
