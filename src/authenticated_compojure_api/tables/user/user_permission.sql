-- name: create-user-permission-table-if-not-exists!
-- create the user_permission table if it does not exist
CREATE TABLE IF NOT EXISTS user_permission (
    id           SERIAL  PRIMARY KEY
    , user_id    INTEGER REFERENCES registered_user (id)    ON DELETE CASCADE
    , permission TEXT    REFERENCES permission (permission) ON DELETE CASCADE);

-- name: drop-user-permission-table!
-- drop the user_permission table
DROP TABLE user_permission CASCADE;
