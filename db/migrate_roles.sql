-- Migration: move comma-separated roles column (app_user.roles) to normalized role table + join table
-- Usage: run on your database after taking a backup.
-- Example: mysql -h HOST -u root -p sampledb < migrate_roles.sql

START TRANSACTION;

-- 1) Create role table if not exists
CREATE TABLE IF NOT EXISTS `role` (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  UNIQUE KEY uq_role_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2) Create join table if not exists
CREATE TABLE IF NOT EXISTS `app_user_roles` (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id),
  KEY idx_role_id (role_id),
  CONSTRAINT fk_aur_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_aur_role FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3) Populate role table from distinct roles found in app_user.roles
-- Use a recursive CTE to split comma-separated values
WITH RECURSIVE split_roles AS (
  -- first element
  SELECT
    id AS user_id,
    TRIM(REPLACE(SUBSTRING_INDEX(roles, ',', 1), 'ROLE_', '')) AS role,
    CASE
      WHEN roles LIKE '%,%' THEN TRIM(SUBSTR(roles, INSTR(roles, ',') + 1))
      ELSE NULL
    END AS rest
  FROM app_user
  WHERE roles IS NOT NULL AND roles <> ''

  UNION ALL

  -- subsequent elements
  SELECT
    user_id,
    TRIM(REPLACE(SUBSTRING_INDEX(rest, ',', 1), 'ROLE_', '')) AS role,
    CASE
      WHEN rest LIKE '%,%' THEN TRIM(SUBSTR(rest, INSTR(rest, ',') + 1))
      ELSE NULL
    END AS rest
  FROM split_roles
  WHERE rest IS NOT NULL
)
INSERT IGNORE INTO role (name)
SELECT DISTINCT UPPER(NULLIF(role, '')) AS role_name
FROM split_roles
WHERE role IS NOT NULL AND TRIM(role) <> '';

-- 4) Populate join table app_user_roles
WITH RECURSIVE split_roles AS (
  SELECT
    id AS user_id,
    TRIM(REPLACE(SUBSTRING_INDEX(roles, ',', 1), 'ROLE_', '')) AS role,
    CASE
      WHEN roles LIKE '%,%' THEN TRIM(SUBSTR(roles, INSTR(roles, ',') + 1))
      ELSE NULL
    END AS rest
  FROM app_user
  WHERE roles IS NOT NULL AND roles <> ''

  UNION ALL

  SELECT
    user_id,
    TRIM(REPLACE(SUBSTRING_INDEX(rest, ',', 1), 'ROLE_', '')) AS role,
    CASE
      WHEN rest LIKE '%,%' THEN TRIM(SUBSTR(rest, INSTR(rest, ',') + 1))
      ELSE NULL
    END AS rest
  FROM split_roles
  WHERE rest IS NOT NULL
)
INSERT IGNORE INTO app_user_roles (user_id, role_id)
SELECT DISTINCT
  sr.user_id,
  r.id
FROM split_roles sr
JOIN role r ON r.name = UPPER(sr.role)
WHERE sr.role IS NOT NULL AND TRIM(sr.role) <> '';

COMMIT;

-- 5) Optional: verify results (run manually)
-- SELECT * FROM role;
-- SELECT * FROM app_user_roles LIMIT 100;

-- 6) Optional: after verifying correctness, you may DROP the old roles column.
-- WARNING: Do not run this until you verified mappings!
-- ALTER TABLE app_user DROP COLUMN roles;