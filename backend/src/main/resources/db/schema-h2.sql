CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(64) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    role VARCHAR(32) NOT NULL,
    first_login_reset_required BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS course (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    term VARCHAR(32) NOT NULL
);

CREATE TABLE IF NOT EXISTS course_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    course_role VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_course_member UNIQUE (course_id, user_id)
);

CREATE TABLE IF NOT EXISTS assignment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    mode VARCHAR(32) NOT NULL,
    description CLOB,
    deadline TIMESTAMP NOT NULL,
    allow_late BOOLEAN NOT NULL DEFAULT TRUE,
    peer_weight INT NOT NULL,
    teacher_weight INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    results_published BOOLEAN NOT NULL DEFAULT FALSE,
    results_published_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS assignment_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_id BIGINT NOT NULL,
    group_name VARCHAR(128) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS assignment_group_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_assignment_user UNIQUE (assignment_id, user_id),
    CONSTRAINT uk_group_member UNIQUE (group_id, user_id)
);

CREATE TABLE IF NOT EXISTS submission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    project_name VARCHAR(128) NOT NULL,
    repo_url VARCHAR(255) NOT NULL,
    video_url VARCHAR(255),
    preview_url VARCHAR(255),
    doc_url VARCHAR(255),
    attachment_url VARCHAR(255),
    description CLOB,
    submitted_by BIGINT NOT NULL,
    submitted_at TIMESTAMP NOT NULL,
    is_late BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_submission_group UNIQUE (group_id)
);

CREATE TABLE IF NOT EXISTS rubric (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_id BIGINT NOT NULL UNIQUE,
    version_no INT NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS rubric_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rubric_id BIGINT NOT NULL,
    item_name VARCHAR(128) NOT NULL,
    description VARCHAR(255),
    weight INT NOT NULL
);

CREATE TABLE IF NOT EXISTS evaluation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_id BIGINT NOT NULL,
    submission_id BIGINT NOT NULL,
    evaluator_user_id BIGINT NOT NULL,
    evaluator_role VARCHAR(32) NOT NULL,
    total_score DECIMAL(6, 2) NOT NULL,
    comment CLOB,
    is_abnormal BOOLEAN NOT NULL DEFAULT FALSE,
    abnormal_reason VARCHAR(255),
    is_excluded BOOLEAN NOT NULL DEFAULT FALSE,
    review_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_evaluation_once UNIQUE (submission_id, evaluator_user_id, evaluator_role)
);

CREATE TABLE IF NOT EXISTS evaluation_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    evaluation_id BIGINT NOT NULL,
    rubric_item_id BIGINT NOT NULL,
    score DECIMAL(4, 2) NOT NULL,
    comment CLOB
);

CREATE TABLE IF NOT EXISTS evaluation_blacklist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_id BIGINT NOT NULL,
    evaluator_user_id BIGINT NOT NULL,
    target_submission_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_evaluation_blacklist UNIQUE (assignment_id, evaluator_user_id, target_submission_id)
);
