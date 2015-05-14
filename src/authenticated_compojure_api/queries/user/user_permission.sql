-- name: insert-permission-for-user<!
-- inserts the specified permission for the designated user
INSERT INTO user_permission (
    user_id
    , permission)
VALUES (
    :userid
    , :permission);

-- name: delete-user-permission!
-- delete a single user permission matching provided id and permission name
DELETE
FROM  user_permission
WHERE user_id = :userid
  AND permission = :permission;

-- name: get-permissions-for-userid
-- get all the permissions for a given userid
SELECT STRING_AGG(permission, ',') AS permissions
FROM   user_permission
WHERE  user_id = :userid
