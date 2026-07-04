CREATE TABLE IF NOT EXISTS employees (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    employee_no VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS auth_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    employee_id BIGINT REFERENCES employees(id),
    created_at TIMESTAMP NOT NULL
);

ALTER TABLE auth_users
    ADD COLUMN IF NOT EXISTS employee_id BIGINT;

UPDATE auth_users au
SET employee_id = NULL
WHERE employee_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM employees e
      WHERE e.id = au.employee_id
  );

ALTER TABLE auth_users
    DROP CONSTRAINT IF EXISTS auth_users_employee_id_fkey;

ALTER TABLE auth_users
    ADD CONSTRAINT auth_users_employee_id_fkey
    FOREIGN KEY (employee_id) REFERENCES employees(id);

CREATE TABLE IF NOT EXISTS auth_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES auth_users(id) ON DELETE CASCADE,
    token VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS usage_records (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    app_name VARCHAR(100) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS detail_access_requests (
    id BIGSERIAL PRIMARY KEY,
    requester_employee_id BIGINT NOT NULL REFERENCES employees(id),
    target_employee_id BIGINT NOT NULL REFERENCES employees(id),
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP NULL,
    processed_by_employee_id BIGINT NULL REFERENCES employees(id)
);

ALTER TABLE usage_records
    ADD COLUMN IF NOT EXISTS employee_id BIGINT;

ALTER TABLE usage_records
    DROP CONSTRAINT IF EXISTS usage_records_auth_user_id_fkey;

ALTER TABLE usage_records
    DROP COLUMN IF EXISTS auth_user_id;

DELETE FROM usage_records
WHERE employee_id IS NULL;

ALTER TABLE usage_records
    ALTER COLUMN employee_id SET NOT NULL;

ALTER TABLE usage_records
    DROP CONSTRAINT IF EXISTS usage_records_employee_id_fkey;

ALTER TABLE usage_records
    ADD CONSTRAINT usage_records_employee_id_fkey
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE;
