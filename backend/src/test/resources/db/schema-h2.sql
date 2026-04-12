CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(128) NOT NULL UNIQUE,
    phone VARCHAR(32) NOT NULL UNIQUE,
    display_name VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
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
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
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
    description CLOB,
    audit_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
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
    preferred_skill_tags_json CLOB,
    preferred_benefit_tags_json CLOB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
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
    description CLOB NOT NULL,
    benefit_tags_json CLOB,
    skill_tags_json CLOB,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMP,
    expire_at TIMESTAMP,
    deleted_flag TINYINT NOT NULL DEFAULT 0,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS job_favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_id BIGINT NOT NULL,
    jobseeker_user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_job_favorite UNIQUE (jobseeker_user_id, job_id)
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
    summary CLOB,
    module_config_json CLOB,
    expected_category VARCHAR(64),
    expected_salary_min INT,
    expected_salary_max INT,
    highest_education VARCHAR(64),
    years_of_experience INT,
    completeness_score INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS resume_education (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    resume_id BIGINT NOT NULL,
    school_name VARCHAR(128) NOT NULL,
    major VARCHAR(64),
    degree VARCHAR(64),
    start_date DATE,
    end_date DATE,
    current_flag TINYINT NOT NULL DEFAULT 0,
    description CLOB,
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
    description CLOB,
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
    description CLOB,
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
    description CLOB,
    sort_order INT NOT NULL DEFAULT 0
);

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
    snapshot_json CLOB NOT NULL,
    completeness_score INT NOT NULL DEFAULT 0,
    complete_flag TINYINT NOT NULL DEFAULT 0,
    missing_items_json CLOB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_saved_resume_user_name UNIQUE (user_id, name)
);

CREATE TABLE IF NOT EXISTS job_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_id BIGINT NOT NULL,
    company_user_id BIGINT NOT NULL,
    jobseeker_user_id BIGINT NOT NULL,
    resume_id BIGINT NOT NULL,
    saved_resume_id BIGINT,
    saved_resume_name VARCHAR(128),
    resume_snapshot_json CLOB,
    status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED',
    status_remark VARCHAR(255),
    applied_at TIMESTAMP NOT NULL,
    viewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS application_status_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    operator_user_id BIGINT NOT NULL,
    operator_role VARCHAR(32) NOT NULL,
    remark VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    company_user_id BIGINT NOT NULL,
    jobseeker_user_id BIGINT NOT NULL,
    last_message_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_conversation_pair UNIQUE (company_user_id, jobseeker_user_id)
);

CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    sender_user_id BIGINT NOT NULL,
    receiver_user_id BIGINT NOT NULL,
    content VARCHAR(1000) NOT NULL,
    read_flag TINYINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL
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
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS admin_action_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    reason VARCHAR(255),
    operator_user_id BIGINT NOT NULL,
    metadata_json CLOB,
    created_at TIMESTAMP NOT NULL
);
