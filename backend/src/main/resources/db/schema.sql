CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(64) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    role VARCHAR(32) NOT NULL,
    first_login_reset_required TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS course (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    term VARCHAR(32) NOT NULL,
    course_deadline DATETIME NULL
);

SET @course_deadline_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'course'
      AND column_name = 'course_deadline'
);
SET @course_deadline_sql = IF(
    @course_deadline_exists = 0,
    'ALTER TABLE course ADD COLUMN course_deadline DATETIME NULL',
    'SELECT 1'
);
PREPARE stmt FROM @course_deadline_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS course_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    course_role VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_course_member (course_id, user_id)
);

CREATE TABLE IF NOT EXISTS assignment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    mode VARCHAR(32) NOT NULL,
    description TEXT,
    deadline DATETIME NOT NULL,
    allow_late TINYINT NOT NULL DEFAULT 1,
    peer_weight INT NOT NULL,
    teacher_weight INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    results_published TINYINT NOT NULL DEFAULT 0,
    results_published_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

SET @results_published_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'assignment'
      AND column_name = 'results_published'
);
SET @results_published_sql = IF(
    @results_published_exists = 0,
    'ALTER TABLE assignment ADD COLUMN results_published TINYINT NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt FROM @results_published_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @results_published_at_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'assignment'
      AND column_name = 'results_published_at'
);
SET @results_published_at_sql = IF(
    @results_published_at_exists = 0,
    'ALTER TABLE assignment ADD COLUMN results_published_at DATETIME NULL',
    'SELECT 1'
);
PREPARE stmt FROM @results_published_at_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS assignment_group (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    assignment_id BIGINT NOT NULL,
    group_name VARCHAR(128) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS assignment_group_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    assignment_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_assignment_user (assignment_id, user_id),
    UNIQUE KEY uk_group_member (group_id, user_id)
);

CREATE TABLE IF NOT EXISTS submission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    assignment_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    project_name VARCHAR(128) NOT NULL,
    repo_url VARCHAR(255) NOT NULL,
    video_url VARCHAR(255),
    preview_url VARCHAR(255),
    doc_url VARCHAR(255),
    attachment_url VARCHAR(255),
    description TEXT,
    submitted_by BIGINT NOT NULL,
    submitted_at DATETIME NOT NULL,
    is_late TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_submission_group (group_id)
);

CREATE TABLE IF NOT EXISTS rubric (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    assignment_id BIGINT NOT NULL UNIQUE,
    version_no INT NOT NULL DEFAULT 1,
    is_active TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS rubric_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rubric_id BIGINT NOT NULL,
    item_name VARCHAR(128) NOT NULL,
    description VARCHAR(255),
    weight INT NOT NULL
);

CREATE TABLE IF NOT EXISTS evaluation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    assignment_id BIGINT NOT NULL,
    submission_id BIGINT NOT NULL,
    evaluator_user_id BIGINT NOT NULL,
    evaluator_role VARCHAR(32) NOT NULL,
    total_score DECIMAL(6,2) NOT NULL,
    comment TEXT,
    is_abnormal TINYINT NOT NULL DEFAULT 0,
    abnormal_reason VARCHAR(255),
    is_excluded TINYINT NOT NULL DEFAULT 0,
    review_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_evaluation_once (submission_id, evaluator_user_id, evaluator_role)
);

SET @evaluation_review_status_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'evaluation'
      AND column_name = 'review_status'
);
SET @evaluation_review_status_sql = IF(
    @evaluation_review_status_exists = 0,
    "ALTER TABLE evaluation ADD COLUMN review_status VARCHAR(32) NOT NULL DEFAULT 'PENDING'",
    'SELECT 1'
);
PREPARE stmt FROM @evaluation_review_status_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS evaluation_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    evaluation_id BIGINT NOT NULL,
    rubric_item_id BIGINT NOT NULL,
    score DECIMAL(4,2) NOT NULL,
    comment TEXT
);

CREATE TABLE IF NOT EXISTS evaluation_blacklist (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    assignment_id BIGINT NOT NULL,
    evaluator_user_id BIGINT NOT NULL,
    target_submission_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_evaluation_blacklist (assignment_id, evaluator_user_id, target_submission_id)
);
