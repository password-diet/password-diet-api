-- name: create-password-reset-key-table-if-not-exists!
-- create the password_reset_key table if it does not exist
CREATE TABLE IF NOT EXISTS password_reset_key (
  id              SERIAL    PRIMARY KEY NOT NULL
  , reset_key     TEXT                  NOT NULL UNIQUE
  , already_used  BOOLEAN               NOT NULL DEFAULT FALSE
  , user_id       INTEGER   REFERENCES registered_user (id) ON DELETE CASCADE
  , valid_until   TIMESTAMP WITH TIME ZONE DEFAULT NOW() + INTERVAL '24 hours'
);

-- name: drop-password-reset-key-table!
-- drop the passowrd-reset-key table
DROP TABLE password_reset_key;
