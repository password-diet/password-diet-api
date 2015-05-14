-- name: get-password-reset-keys-for-userid
-- get the password reset key(s) for a given userid
SELECT id
       , reset_key
       , already_used
       , user_id
       , valid_until
FROM   password_reset_key
WHERE  user_id = :userid;

-- name: get-reset-row-by-reset-key
-- get the row containing the specified reset_key
SELECT id
       , reset_key
       , already_used
       , user_id
       , valid_until
FROM   password_reset_key
WHERE  reset_key = :reset_key;

-- name: insert-password-reset-key-with-default-valid-until<!
-- inserts a row in the password_reset_key table using the default valid until timestamp
INSERT INTO password_reset_key (reset_key , user_id)
VALUES (:reset_key, :user_id);

-- name: insert-password-reset-key-with-provide-valid-until-date<!
-- inserts a row in the password_reset_key table using the provided valid until timestamp
INSERT INTO password_reset_key (reset_key , user_id, valid_until)
VALUES (:reset_key, :user_id, :valid_until);

-- name: invalidate-reset-key<!
-- sets the `already_used` value of a specified reset_key ture
UPDATE password_reset_key
SET    already_used = TRUE
WHERE  reset_key = :reset_key
