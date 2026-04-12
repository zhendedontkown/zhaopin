create database recruitment_system;

use recruitment_system;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(128) NOT NULL UNIQUE,
    phone VARCHAR(32) NOT NULL UNIQUE,
    display_name VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    UNIQUE KEY uk_user_role (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS company_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    company_name VARCHAR(128) NOT NULL,
    unified_social_credit_code VARCHAR(32) NOT NULL UNIQUE,
    contact_person VARCHAR(64) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    email VARCHAR(128) NOT NULL,
    address VARCHAR(255),
    description TEXT,
    audit_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS jobseeker_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    full_name VARCHAR(64) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    email VARCHAR(128) NOT NULL,
    desired_position_category VARCHAR(64),
    expected_salary_min INT,
    expected_salary_max INT,
    preferred_city VARCHAR(64),
    highest_education VARCHAR(64),
    years_of_experience INT,
    preferred_skill_tags_json TEXT,
    preferred_benefit_tags_json TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS job_post (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    company_user_id BIGINT NOT NULL,
    job_code VARCHAR(32) NOT NULL UNIQUE,
    title VARCHAR(128) NOT NULL,
    category VARCHAR(64) NOT NULL,
    location VARCHAR(64) NOT NULL,
    salary_min INT NOT NULL,
    salary_max INT NOT NULL,
    experience_requirement VARCHAR(64) NOT NULL,
    education_requirement VARCHAR(64) NOT NULL,
    headcount INT NOT NULL,
    description TEXT NOT NULL,
    benefit_tags_json TEXT,
    skill_tags_json TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    published_at DATETIME,
    expire_at DATETIME,
    deleted_flag TINYINT NOT NULL DEFAULT 0,
    deleted_at DATETIME,
    deleted_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

SET @jobseeker_profile_preferred_skill_tags_json_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'jobseeker_profile'
      AND COLUMN_NAME = 'preferred_skill_tags_json'
);
SET @jobseeker_profile_preferred_skill_tags_json_sql = IF(
    @jobseeker_profile_preferred_skill_tags_json_exists = 0,
    'ALTER TABLE jobseeker_profile ADD COLUMN preferred_skill_tags_json TEXT AFTER years_of_experience',
    'SELECT 1'
);
PREPARE jobseeker_profile_preferred_skill_tags_json_stmt FROM @jobseeker_profile_preferred_skill_tags_json_sql;
EXECUTE jobseeker_profile_preferred_skill_tags_json_stmt;
DEALLOCATE PREPARE jobseeker_profile_preferred_skill_tags_json_stmt;

SET @jobseeker_profile_preferred_benefit_tags_json_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'jobseeker_profile'
      AND COLUMN_NAME = 'preferred_benefit_tags_json'
);
SET @jobseeker_profile_preferred_benefit_tags_json_sql = IF(
    @jobseeker_profile_preferred_benefit_tags_json_exists = 0,
    'ALTER TABLE jobseeker_profile ADD COLUMN preferred_benefit_tags_json TEXT AFTER preferred_skill_tags_json',
    'SELECT 1'
);
PREPARE jobseeker_profile_preferred_benefit_tags_json_stmt FROM @jobseeker_profile_preferred_benefit_tags_json_sql;
EXECUTE jobseeker_profile_preferred_benefit_tags_json_stmt;
DEALLOCATE PREPARE jobseeker_profile_preferred_benefit_tags_json_stmt;

SET @job_post_benefit_tags_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'job_post'
      AND COLUMN_NAME = 'benefit_tags_json'
);
SET @job_post_benefit_tags_sql = IF(
    @job_post_benefit_tags_exists = 0,
    'ALTER TABLE job_post ADD COLUMN benefit_tags_json TEXT AFTER description',
    'SELECT 1'
);
PREPARE job_post_benefit_tags_stmt FROM @job_post_benefit_tags_sql;
EXECUTE job_post_benefit_tags_stmt;
DEALLOCATE PREPARE job_post_benefit_tags_stmt;

SET @job_post_skill_tags_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'job_post'
      AND COLUMN_NAME = 'skill_tags_json'
);
SET @job_post_skill_tags_sql = IF(
    @job_post_skill_tags_exists = 0,
    'ALTER TABLE job_post ADD COLUMN skill_tags_json TEXT AFTER benefit_tags_json',
    'SELECT 1'
);
PREPARE job_post_skill_tags_stmt FROM @job_post_skill_tags_sql;
EXECUTE job_post_skill_tags_stmt;
DEALLOCATE PREPARE job_post_skill_tags_stmt;

SET @job_post_deleted_flag_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'job_post'
      AND COLUMN_NAME = 'deleted_flag'
);
SET @job_post_deleted_flag_sql = IF(
    @job_post_deleted_flag_exists = 0,
    'ALTER TABLE job_post ADD COLUMN deleted_flag TINYINT NOT NULL DEFAULT 0 AFTER expire_at',
    'SELECT 1'
);
PREPARE job_post_deleted_flag_stmt FROM @job_post_deleted_flag_sql;
EXECUTE job_post_deleted_flag_stmt;
DEALLOCATE PREPARE job_post_deleted_flag_stmt;

SET @job_post_deleted_at_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'job_post'
      AND COLUMN_NAME = 'deleted_at'
);
SET @job_post_deleted_at_sql = IF(
    @job_post_deleted_at_exists = 0,
    'ALTER TABLE job_post ADD COLUMN deleted_at DATETIME AFTER deleted_flag',
    'SELECT 1'
);
PREPARE job_post_deleted_at_stmt FROM @job_post_deleted_at_sql;
EXECUTE job_post_deleted_at_stmt;
DEALLOCATE PREPARE job_post_deleted_at_stmt;

SET @job_post_deleted_by_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'job_post'
      AND COLUMN_NAME = 'deleted_by'
);
SET @job_post_deleted_by_sql = IF(
    @job_post_deleted_by_exists = 0,
    'ALTER TABLE job_post ADD COLUMN deleted_by BIGINT AFTER deleted_at',
    'SELECT 1'
);
PREPARE job_post_deleted_by_stmt FROM @job_post_deleted_by_sql;
EXECUTE job_post_deleted_by_stmt;
DEALLOCATE PREPARE job_post_deleted_by_stmt;

UPDATE job_post
SET benefit_tags_json = JSON_ARRAY('五险一金', '带薪年假', '周末双休', '团队氛围好')
WHERE benefit_tags_json IS NULL
   OR TRIM(benefit_tags_json) = '';

UPDATE job_post
SET skill_tags_json = JSON_ARRAY()
WHERE skill_tags_json IS NULL
   OR TRIM(skill_tags_json) = '';

CREATE TABLE IF NOT EXISTS job_favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_id BIGINT NOT NULL,
    jobseeker_user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_job_favorite (jobseeker_user_id, job_id)
);

CREATE TABLE IF NOT EXISTS resume (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    template_code VARCHAR(32) NOT NULL,
    full_name VARCHAR(64),
    gender VARCHAR(16),
    age INT,
    birth_date DATE,
    display_age TINYINT DEFAULT 1,
    phone VARCHAR(32),
    email VARCHAR(128),
    city VARCHAR(64),
    summary TEXT,
    module_config_json TEXT,
    expected_category VARCHAR(64),
    expected_salary_min INT,
    expected_salary_max INT,
    highest_education VARCHAR(64),
    years_of_experience INT,
    completeness_score INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

SET @resume_module_config_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'resume'
      AND COLUMN_NAME = 'module_config_json'
);
SET @resume_module_config_sql = IF(
    @resume_module_config_exists = 0,
    'ALTER TABLE resume ADD COLUMN module_config_json TEXT AFTER summary',
    'SELECT 1'
);
PREPARE resume_module_config_stmt FROM @resume_module_config_sql;
EXECUTE resume_module_config_stmt;
DEALLOCATE PREPARE resume_module_config_stmt;

SET @resume_birth_date_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'resume'
      AND COLUMN_NAME = 'birth_date'
);
SET @resume_birth_date_sql = IF(
    @resume_birth_date_exists = 0,
    'ALTER TABLE resume ADD COLUMN birth_date DATE AFTER age',
    'SELECT 1'
);
PREPARE resume_birth_date_stmt FROM @resume_birth_date_sql;
EXECUTE resume_birth_date_stmt;
DEALLOCATE PREPARE resume_birth_date_stmt;

SET @resume_display_age_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'resume'
      AND COLUMN_NAME = 'display_age'
);
SET @resume_display_age_sql = IF(
    @resume_display_age_exists = 0,
    'ALTER TABLE resume ADD COLUMN display_age TINYINT DEFAULT 1 AFTER birth_date',
    'SELECT 1'
);
PREPARE resume_display_age_stmt FROM @resume_display_age_sql;
EXECUTE resume_display_age_stmt;
DEALLOCATE PREPARE resume_display_age_stmt;

CREATE TABLE IF NOT EXISTS resume_education (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    resume_id BIGINT NOT NULL,
    school_name VARCHAR(128) NOT NULL,
    major VARCHAR(64),
    degree VARCHAR(64),
    start_date DATE,
    end_date DATE,
    current_flag TINYINT NOT NULL DEFAULT 0,
    description TEXT,
    sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS resume_experience (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    resume_id BIGINT NOT NULL,
    company_name VARCHAR(128) NOT NULL,
    job_title VARCHAR(64) NOT NULL,
    start_date DATE,
    end_date DATE,
    current_flag TINYINT NOT NULL DEFAULT 0,
    description TEXT,
    sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS resume_project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    resume_id BIGINT NOT NULL,
    project_name VARCHAR(128) NOT NULL,
    role_name VARCHAR(64),
    start_date DATE,
    end_date DATE,
    current_flag TINYINT NOT NULL DEFAULT 0,
    description TEXT,
    sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS resume_extra_section_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    resume_id BIGINT NOT NULL,
    section_code VARCHAR(32) NOT NULL,
    title VARCHAR(128),
    subtitle VARCHAR(128),
    start_date DATE,
    end_date DATE,
    current_flag TINYINT NOT NULL DEFAULT 0,
    description TEXT,
    sort_order INT NOT NULL DEFAULT 0
);

SET @resume_education_current_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'resume_education'
      AND COLUMN_NAME = 'current_flag'
);
SET @resume_education_current_sql = IF(
    @resume_education_current_exists = 0,
    'ALTER TABLE resume_education ADD COLUMN current_flag TINYINT NOT NULL DEFAULT 0 AFTER end_date',
    'SELECT 1'
);
PREPARE resume_education_current_stmt FROM @resume_education_current_sql;
EXECUTE resume_education_current_stmt;
DEALLOCATE PREPARE resume_education_current_stmt;

SET @resume_experience_current_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'resume_experience'
      AND COLUMN_NAME = 'current_flag'
);
SET @resume_experience_current_sql = IF(
    @resume_experience_current_exists = 0,
    'ALTER TABLE resume_experience ADD COLUMN current_flag TINYINT NOT NULL DEFAULT 0 AFTER end_date',
    'SELECT 1'
);
PREPARE resume_experience_current_stmt FROM @resume_experience_current_sql;
EXECUTE resume_experience_current_stmt;
DEALLOCATE PREPARE resume_experience_current_stmt;

SET @resume_project_current_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'resume_project'
      AND COLUMN_NAME = 'current_flag'
);
SET @resume_project_current_sql = IF(
    @resume_project_current_exists = 0,
    'ALTER TABLE resume_project ADD COLUMN current_flag TINYINT NOT NULL DEFAULT 0 AFTER end_date',
    'SELECT 1'
);
PREPARE resume_project_current_stmt FROM @resume_project_current_sql;
EXECUTE resume_project_current_stmt;
DEALLOCATE PREPARE resume_project_current_stmt;

SET @resume_extra_section_current_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'resume_extra_section_item'
      AND COLUMN_NAME = 'current_flag'
);
SET @resume_extra_section_current_sql = IF(
    @resume_extra_section_current_exists = 0,
    'ALTER TABLE resume_extra_section_item ADD COLUMN current_flag TINYINT NOT NULL DEFAULT 0 AFTER end_date',
    'SELECT 1'
);
PREPARE resume_extra_section_current_stmt FROM @resume_extra_section_current_sql;
EXECUTE resume_extra_section_current_stmt;
DEALLOCATE PREPARE resume_extra_section_current_stmt;

CREATE TABLE IF NOT EXISTS skill_dict (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    skill_name VARCHAR(64) NOT NULL UNIQUE,
    category VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS resume_skill (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    resume_id BIGINT NOT NULL,
    skill_id BIGINT,
    skill_name VARCHAR(64) NOT NULL
);

CREATE TABLE IF NOT EXISTS saved_resume (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    template_code VARCHAR(32) NOT NULL,
    snapshot_json LONGTEXT NOT NULL,
    completeness_score INT NOT NULL DEFAULT 0,
    complete_flag TINYINT NOT NULL DEFAULT 0,
    missing_items_json LONGTEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_saved_resume_user_name (user_id, name)
);

CREATE TABLE IF NOT EXISTS job_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_id BIGINT NOT NULL,
    company_user_id BIGINT NOT NULL,
    jobseeker_user_id BIGINT NOT NULL,
    resume_id BIGINT NOT NULL,
    saved_resume_id BIGINT,
    saved_resume_name VARCHAR(128),
    resume_snapshot_json LONGTEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED',
    status_remark VARCHAR(255),
    applied_at DATETIME NOT NULL,
    viewed_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS application_status_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    operator_user_id BIGINT NOT NULL,
    operator_role VARCHAR(32) NOT NULL,
    remark VARCHAR(255),
    created_at DATETIME NOT NULL
);

SET @job_application_saved_resume_id_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'job_application'
      AND COLUMN_NAME = 'saved_resume_id'
);
SET @job_application_saved_resume_id_sql = IF(
    @job_application_saved_resume_id_exists = 0,
    'ALTER TABLE job_application ADD COLUMN saved_resume_id BIGINT AFTER resume_id',
    'SELECT 1'
);
PREPARE job_application_saved_resume_id_stmt FROM @job_application_saved_resume_id_sql;
EXECUTE job_application_saved_resume_id_stmt;
DEALLOCATE PREPARE job_application_saved_resume_id_stmt;

SET @job_application_saved_resume_name_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'job_application'
      AND COLUMN_NAME = 'saved_resume_name'
);
SET @job_application_saved_resume_name_sql = IF(
    @job_application_saved_resume_name_exists = 0,
    'ALTER TABLE job_application ADD COLUMN saved_resume_name VARCHAR(128) AFTER saved_resume_id',
    'SELECT 1'
);
PREPARE job_application_saved_resume_name_stmt FROM @job_application_saved_resume_name_sql;
EXECUTE job_application_saved_resume_name_stmt;
DEALLOCATE PREPARE job_application_saved_resume_name_stmt;

SET @job_application_status_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'job_application'
      AND COLUMN_NAME = 'status'
);
SET @job_application_status_sql = IF(
    @job_application_status_exists = 0,
    'ALTER TABLE job_application ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT ''SUBMITTED'' AFTER resume_id',
    'SELECT 1'
);
PREPARE job_application_status_stmt FROM @job_application_status_sql;
EXECUTE job_application_status_stmt;
DEALLOCATE PREPARE job_application_status_stmt;

SET @job_application_resume_snapshot_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'job_application'
      AND COLUMN_NAME = 'resume_snapshot_json'
);
SET @job_application_resume_snapshot_sql = IF(
    @job_application_resume_snapshot_exists = 0,
    'ALTER TABLE job_application ADD COLUMN resume_snapshot_json LONGTEXT AFTER resume_id',
    'SELECT 1'
);
PREPARE job_application_resume_snapshot_stmt FROM @job_application_resume_snapshot_sql;
EXECUTE job_application_resume_snapshot_stmt;
DEALLOCATE PREPARE job_application_resume_snapshot_stmt;

UPDATE job_application
SET status = 'SUBMITTED'
WHERE status IS NULL
   OR TRIM(status) = '';

UPDATE job_application
SET status = 'OFFERED'
WHERE status = 'ACCEPTED';

ALTER TABLE job_application
MODIFY COLUMN status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED';

UPDATE application_status_log
SET from_status = 'OFFERED'
WHERE from_status = 'ACCEPTED';

UPDATE application_status_log
SET to_status = 'OFFERED'
WHERE to_status = 'ACCEPTED';

CREATE TABLE IF NOT EXISTS conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    company_user_id BIGINT NOT NULL,
    jobseeker_user_id BIGINT NOT NULL,
    last_message_at DATETIME,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_conversation_pair (company_user_id, jobseeker_user_id)
);

CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    sender_user_id BIGINT NOT NULL,
    receiver_user_id BIGINT NOT NULL,
    content VARCHAR(1000) NOT NULL,
    read_flag TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    title VARCHAR(128) NOT NULL,
    content VARCHAR(500) NOT NULL,
    read_flag TINYINT NOT NULL DEFAULT 0,
    related_user_id BIGINT,
    related_conversation_id BIGINT,
    related_application_id BIGINT,
    created_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS admin_action_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    reason VARCHAR(255),
    operator_user_id BIGINT NOT NULL,
    metadata_json TEXT,
    created_at DATETIME NOT NULL
);

SET @notification_related_user_id_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notification'
      AND COLUMN_NAME = 'related_user_id'
);
SET @notification_related_user_id_sql = IF(
    @notification_related_user_id_exists = 0,
    'ALTER TABLE notification ADD COLUMN related_user_id BIGINT',
    'SELECT 1'
);
PREPARE notification_related_user_id_stmt FROM @notification_related_user_id_sql;
EXECUTE notification_related_user_id_stmt;
DEALLOCATE PREPARE notification_related_user_id_stmt;

SET @notification_related_conversation_id_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notification'
      AND COLUMN_NAME = 'related_conversation_id'
);
SET @notification_related_conversation_id_sql = IF(
    @notification_related_conversation_id_exists = 0,
    'ALTER TABLE notification ADD COLUMN related_conversation_id BIGINT',
    'SELECT 1'
);
PREPARE notification_related_conversation_id_stmt FROM @notification_related_conversation_id_sql;
EXECUTE notification_related_conversation_id_stmt;
DEALLOCATE PREPARE notification_related_conversation_id_stmt;

SET @notification_related_application_id_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notification'
      AND COLUMN_NAME = 'related_application_id'
);
SET @notification_related_application_id_sql = IF(
    @notification_related_application_id_exists = 0,
    'ALTER TABLE notification ADD COLUMN related_application_id BIGINT',
    'SELECT 1'
);
PREPARE notification_related_application_id_stmt FROM @notification_related_application_id_sql;
EXECUTE notification_related_application_id_stmt;
DEALLOCATE PREPARE notification_related_application_id_stmt;
