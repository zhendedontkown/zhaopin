SET NAMES utf8mb4;
USE recruitment_system;

START TRANSACTION;

SET @demo_password_hash = '$2a$10$XsHtqW3.iGx2dfpNgwHoFO4BaQDrAObDCSWvIaQpKfw28PDD4KLBC';

INSERT INTO skill_dict (skill_name, category)
SELECT 'React', 'default' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM skill_dict WHERE skill_name = 'React');

INSERT INTO skill_dict (skill_name, category)
SELECT 'Node.js', 'default' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM skill_dict WHERE skill_name = 'Node.js');

INSERT INTO skill_dict (skill_name, category)
SELECT 'Python', 'default' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM skill_dict WHERE skill_name = 'Python');

INSERT INTO skill_dict (skill_name, category)
SELECT 'SQL', 'default' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM skill_dict WHERE skill_name = 'SQL');

INSERT INTO skill_dict (skill_name, category)
SELECT 'Figma', 'default' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM skill_dict WHERE skill_name = 'Figma');

INSERT INTO skill_dict (skill_name, category)
SELECT 'Product Design', 'default' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM skill_dict WHERE skill_name = 'Product Design');

INSERT INTO skill_dict (skill_name, category)
SELECT 'Testing', 'default' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM skill_dict WHERE skill_name = 'Testing');

INSERT INTO sys_user (username, password, email, phone, display_name, status, created_at, updated_at)
SELECT 'recruiter@bluecloud.com', @demo_password_hash, 'recruiter@bluecloud.com', '13800000003', 'BlueCloud Recruiter', 'ACTIVE',
       '2026-04-01 09:00:00', '2026-04-01 09:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE email = 'recruiter@bluecloud.com');

INSERT INTO sys_user (username, password, email, phone, display_name, status, created_at, updated_at)
SELECT 'talent@smarthire.com', @demo_password_hash, 'talent@smarthire.com', '13800000004', 'SmartHire HR', 'ACTIVE',
       '2026-04-01 09:10:00', '2026-04-01 09:10:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE email = 'talent@smarthire.com');

INSERT INTO sys_user (username, password, email, phone, display_name, status, created_at, updated_at)
SELECT 'bob@example.com', @demo_password_hash, 'bob@example.com', '13700000005', 'Bob Wang', 'ACTIVE',
       '2026-04-01 09:20:00', '2026-04-01 09:20:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE email = 'bob@example.com');

INSERT INTO sys_user (username, password, email, phone, display_name, status, created_at, updated_at)
SELECT 'charlie@example.com', @demo_password_hash, 'charlie@example.com', '13700000006', 'Charlie Chen', 'ACTIVE',
       '2026-04-01 09:30:00', '2026-04-01 09:30:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE email = 'charlie@example.com');

INSERT INTO sys_user (username, password, email, phone, display_name, status, created_at, updated_at)
SELECT 'diana@example.com', @demo_password_hash, 'diana@example.com', '13700000007', 'Diana Zhou', 'ACTIVE',
       '2026-04-01 09:40:00', '2026-04-01 09:40:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE email = 'diana@example.com');

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.code = 'COMPANY'
WHERE u.email IN ('recruiter@bluecloud.com', 'talent@smarthire.com')
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.code = 'JOBSEEKER'
WHERE u.email IN ('bob@example.com', 'charlie@example.com', 'diana@example.com')
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO company_profile (
    user_id, company_name, unified_social_credit_code, contact_person, phone, email, address, description,
    audit_status, created_at, updated_at
)
SELECT u.id, 'BlueCloud Data Ltd.', '91310000MA1K654321', 'Zhou Lead', '13800000003', 'recruiter@bluecloud.com',
       'Tianhe Software Park, Guangzhou', 'Builds data platforms, analytics dashboards and cloud-native business systems.',
       'APPROVED', '2026-04-01 09:00:00', '2026-04-01 09:00:00'
FROM sys_user u
WHERE u.email = 'recruiter@bluecloud.com'
  AND NOT EXISTS (SELECT 1 FROM company_profile cp WHERE cp.user_id = u.id);

INSERT INTO company_profile (
    user_id, company_name, unified_social_credit_code, contact_person, phone, email, address, description,
    audit_status, created_at, updated_at
)
SELECT u.id, 'SmartHire Tech Ltd.', '91310000MA1K987654', 'Lin Manager', '13800000004', 'talent@smarthire.com',
       'Zhangjiang Hi-Tech Park, Shanghai', 'Focuses on hiring SaaS, process collaboration and talent matching products.',
       'APPROVED', '2026-04-01 09:10:00', '2026-04-01 09:10:00'
FROM sys_user u
WHERE u.email = 'talent@smarthire.com'
  AND NOT EXISTS (SELECT 1 FROM company_profile cp WHERE cp.user_id = u.id);

INSERT INTO jobseeker_profile (
    user_id, full_name, phone, email, desired_position_category, expected_salary_min, expected_salary_max,
    preferred_city, highest_education, years_of_experience, created_at, updated_at
)
SELECT u.id, 'Bob Wang', '13700000005', 'bob@example.com', 'frontend', 9000, 16000, 'Guangzhou', 'Bachelor', 2,
       '2026-04-01 09:20:00', '2026-04-01 09:20:00'
FROM sys_user u
WHERE u.email = 'bob@example.com'
  AND NOT EXISTS (SELECT 1 FROM jobseeker_profile jp WHERE jp.user_id = u.id);

INSERT INTO jobseeker_profile (
    user_id, full_name, phone, email, desired_position_category, expected_salary_min, expected_salary_max,
    preferred_city, highest_education, years_of_experience, created_at, updated_at
)
SELECT u.id, 'Charlie Chen', '13700000006', 'charlie@example.com', 'product', 12000, 20000, 'Shenzhen', 'Bachelor', 3,
       '2026-04-01 09:30:00', '2026-04-01 09:30:00'
FROM sys_user u
WHERE u.email = 'charlie@example.com'
  AND NOT EXISTS (SELECT 1 FROM jobseeker_profile jp WHERE jp.user_id = u.id);

INSERT INTO jobseeker_profile (
    user_id, full_name, phone, email, desired_position_category, expected_salary_min, expected_salary_max,
    preferred_city, highest_education, years_of_experience, created_at, updated_at
)
SELECT u.id, 'Diana Zhou', '13700000007', 'diana@example.com', 'data', 13000, 22000, 'Shanghai', 'Master', 4,
       '2026-04-01 09:40:00', '2026-04-01 09:40:00'
FROM sys_user u
WHERE u.email = 'diana@example.com'
  AND NOT EXISTS (SELECT 1 FROM jobseeker_profile jp WHERE jp.user_id = u.id);

INSERT INTO resume (
    user_id, template_code, full_name, gender, age, phone, email, city, summary, expected_category,
    expected_salary_min, expected_salary_max, highest_education, years_of_experience, completeness_score,
    created_at, updated_at
)
SELECT u.id, 'classic', 'Bob Wang', 'Male', 23, '13700000005', 'bob@example.com', 'Guangzhou',
       'Frontend engineer experienced in Vue3, TypeScript, Element Plus and admin console development.',
       'frontend', 9000, 16000, 'Bachelor', 2, 90, '2026-04-01 09:25:00', '2026-04-05 18:00:00'
FROM sys_user u
WHERE u.email = 'bob@example.com'
  AND NOT EXISTS (SELECT 1 FROM resume r WHERE r.user_id = u.id);

INSERT INTO resume (
    user_id, template_code, full_name, gender, age, phone, email, city, summary, expected_category,
    expected_salary_min, expected_salary_max, highest_education, years_of_experience, completeness_score,
    created_at, updated_at
)
SELECT u.id, 'classic', 'Charlie Chen', 'Male', 26, '13700000006', 'charlie@example.com', 'Shenzhen',
       'Product manager with hiring platform and workflow tooling experience, strong in requirement breakdown and planning.',
       'product', 12000, 20000, 'Bachelor', 3, 91, '2026-04-01 09:35:00', '2026-04-05 19:00:00'
FROM sys_user u
WHERE u.email = 'charlie@example.com'
  AND NOT EXISTS (SELECT 1 FROM resume r WHERE r.user_id = u.id);

INSERT INTO resume (
    user_id, template_code, full_name, gender, age, phone, email, city, summary, expected_category,
    expected_salary_min, expected_salary_max, highest_education, years_of_experience, completeness_score,
    created_at, updated_at
)
SELECT u.id, 'classic', 'Diana Zhou', 'Female', 27, '13700000007', 'diana@example.com', 'Shanghai',
       'Data analyst experienced in SQL, Python, dashboarding and funnel analysis for recruiting and growth.',
       'data', 13000, 22000, 'Master', 4, 94, '2026-04-01 09:45:00', '2026-04-05 20:00:00'
FROM sys_user u
WHERE u.email = 'diana@example.com'
  AND NOT EXISTS (SELECT 1 FROM resume r WHERE r.user_id = u.id);

INSERT INTO resume_education (resume_id, school_name, major, degree, start_date, end_date, description, sort_order)
SELECT r.id, 'Jinan University', 'Digital Media Technology', 'Bachelor', '2019-09-01', '2023-06-30',
       'Focused on frontend engineering, interaction design and web application development.', 0
FROM resume r
JOIN sys_user u ON u.id = r.user_id
WHERE u.email = 'bob@example.com'
  AND NOT EXISTS (SELECT 1 FROM resume_education re WHERE re.resume_id = r.id AND re.school_name = 'Jinan University');

INSERT INTO resume_education (resume_id, school_name, major, degree, start_date, end_date, description, sort_order)
SELECT r.id, 'Shenzhen University', 'Information Systems', 'Bachelor', '2016-09-01', '2020-06-30',
       'Studied product design, information architecture, user research and analytics.', 0
FROM resume r
JOIN sys_user u ON u.id = r.user_id
WHERE u.email = 'charlie@example.com'
  AND NOT EXISTS (SELECT 1 FROM resume_education re WHERE re.resume_id = r.id AND re.school_name = 'Shenzhen University');

INSERT INTO resume_education (resume_id, school_name, major, degree, start_date, end_date, description, sort_order)
SELECT r.id, 'East China Normal University', 'Statistics', 'Master', '2019-09-01', '2022-06-30',
       'Focused on business analytics, statistical modeling and data visualization.', 0
FROM resume r
JOIN sys_user u ON u.id = r.user_id
WHERE u.email = 'diana@example.com'
  AND NOT EXISTS (SELECT 1 FROM resume_education re WHERE re.resume_id = r.id AND re.school_name = 'East China Normal University');

INSERT INTO resume_experience (resume_id, company_name, job_title, start_date, end_date, description, sort_order)
SELECT r.id, 'Guangzhou Internet Co.', 'Frontend Engineer', '2023-07-01', '2025-03-01',
       'Built admin consoles, dashboards and mobile web pages, and improved component reuse and performance.', 0
FROM resume r
JOIN sys_user u ON u.id = r.user_id
WHERE u.email = 'bob@example.com'
  AND NOT EXISTS (SELECT 1 FROM resume_experience re WHERE re.resume_id = r.id AND re.company_name = 'Guangzhou Internet Co.');

INSERT INTO resume_experience (resume_id, company_name, job_title, start_date, end_date, description, sort_order)
SELECT r.id, 'Shenzhen SaaS Co.', 'Product Manager', '2022-07-01', '2025-03-01',
       'Owned roadmap planning, workflow design and cross-team delivery for a recruiting collaboration product.', 0
FROM resume r
JOIN sys_user u ON u.id = r.user_id
WHERE u.email = 'charlie@example.com'
  AND NOT EXISTS (SELECT 1 FROM resume_experience re WHERE re.resume_id = r.id AND re.company_name = 'Shenzhen SaaS Co.');

INSERT INTO resume_experience (resume_id, company_name, job_title, start_date, end_date, description, sort_order)
SELECT r.id, 'Shanghai Data Co.', 'Data Analyst', '2022-07-01', '2025-03-01',
       'Built KPI systems, recruiting funnel analysis and automated reporting workflows.', 0
FROM resume r
JOIN sys_user u ON u.id = r.user_id
WHERE u.email = 'diana@example.com'
  AND NOT EXISTS (SELECT 1 FROM resume_experience re WHERE re.resume_id = r.id AND re.company_name = 'Shanghai Data Co.');

INSERT INTO resume_project (resume_id, project_name, role_name, start_date, end_date, description, sort_order)
SELECT r.id, 'Recruiting Workbench Rewrite', 'Lead Frontend', '2024-08-01', '2025-02-28',
       'Delivered job workflow screens, a split-view resume editor and a mobile-friendly message center.', 0
FROM resume r
JOIN sys_user u ON u.id = r.user_id
WHERE u.email = 'bob@example.com'
  AND NOT EXISTS (SELECT 1 FROM resume_project rp WHERE rp.resume_id = r.id AND rp.project_name = 'Recruiting Workbench Rewrite');

INSERT INTO resume_project (resume_id, project_name, role_name, start_date, end_date, description, sort_order)
SELECT r.id, 'Smart Hiring Platform', 'Product Owner', '2024-04-01', '2025-01-31',
       'Designed candidate workflow, status transitions, notifications and analytics modules.', 0
FROM resume r
JOIN sys_user u ON u.id = r.user_id
WHERE u.email = 'charlie@example.com'
  AND NOT EXISTS (SELECT 1 FROM resume_project rp WHERE rp.resume_id = r.id AND rp.project_name = 'Smart Hiring Platform');

INSERT INTO resume_project (resume_id, project_name, role_name, start_date, end_date, description, sort_order)
SELECT r.id, 'Recruiting Funnel Analytics', 'Lead Analyst', '2024-05-01', '2025-02-15',
       'Built funnel metrics from exposure to offer and recommended improvements for channels and job postings.', 0
FROM resume r
JOIN sys_user u ON u.id = r.user_id
WHERE u.email = 'diana@example.com'
  AND NOT EXISTS (SELECT 1 FROM resume_project rp WHERE rp.resume_id = r.id AND rp.project_name = 'Recruiting Funnel Analytics');

INSERT INTO resume_skill (resume_id, skill_id, skill_name)
SELECT r.id, s.id, s.skill_name
FROM resume r
JOIN sys_user u ON u.id = r.user_id
JOIN skill_dict s ON s.skill_name IN ('Vue3', 'TypeScript', 'Element Plus', 'React')
WHERE u.email = 'bob@example.com'
  AND NOT EXISTS (SELECT 1 FROM resume_skill rs WHERE rs.resume_id = r.id AND rs.skill_name = s.skill_name);

INSERT INTO resume_skill (resume_id, skill_id, skill_name)
SELECT r.id, s.id, s.skill_name
FROM resume r
JOIN sys_user u ON u.id = r.user_id
JOIN skill_dict s ON s.skill_name IN ('Product Design', 'Figma', 'SQL')
WHERE u.email = 'charlie@example.com'
  AND NOT EXISTS (SELECT 1 FROM resume_skill rs WHERE rs.resume_id = r.id AND rs.skill_name = s.skill_name);

INSERT INTO resume_skill (resume_id, skill_id, skill_name)
SELECT r.id, s.id, s.skill_name
FROM resume r
JOIN sys_user u ON u.id = r.user_id
JOIN skill_dict s ON s.skill_name IN ('Python', 'SQL', 'MySQL')
WHERE u.email = 'diana@example.com'
  AND NOT EXISTS (SELECT 1 FROM resume_skill rs WHERE rs.resume_id = r.id AND rs.skill_name = s.skill_name);

INSERT INTO job_post (
    company_user_id, job_code, title, category, location, salary_min, salary_max,
    experience_requirement, education_requirement, headcount, description, status,
    published_at, expire_at, created_at, updated_at
)
SELECT u.id, 'JOB202604060002', 'Frontend Engineer', 'frontend', 'Guangzhou', 10000, 17000,
       '2 years', 'Bachelor', 2, 'Build the recruiting workbench, resume editor and dashboard pages with Vue3 and TypeScript.',
       'PUBLISHED', '2026-04-02 09:00:00', '2026-05-31 23:59:59', '2026-04-02 09:00:00', '2026-04-02 09:00:00'
FROM sys_user u
WHERE u.email = 'recruiter@bluecloud.com'
  AND NOT EXISTS (SELECT 1 FROM job_post jp WHERE jp.job_code = 'JOB202604060002');

INSERT INTO job_post (
    company_user_id, job_code, title, category, location, salary_min, salary_max,
    experience_requirement, education_requirement, headcount, description, status,
    published_at, expire_at, created_at, updated_at
)
SELECT u.id, 'JOB202604060003', 'Data Analyst', 'data', 'Shanghai', 15000, 25000,
       '3 years', 'Bachelor', 1, 'Own recruiting funnel analysis, channel reporting and KPI dashboard design with SQL and Python.',
       'PUBLISHED', '2026-04-02 09:30:00', '2026-05-31 23:59:59', '2026-04-02 09:30:00', '2026-04-02 09:30:00'
FROM sys_user u
WHERE u.email = 'recruiter@bluecloud.com'
  AND NOT EXISTS (SELECT 1 FROM job_post jp WHERE jp.job_code = 'JOB202604060003');

INSERT INTO job_post (
    company_user_id, job_code, title, category, location, salary_min, salary_max,
    experience_requirement, education_requirement, headcount, description, status,
    published_at, expire_at, created_at, updated_at
)
SELECT u.id, 'JOB202604060004', 'Product Manager', 'product', 'Shenzhen', 14000, 22000,
       '3 years', 'Bachelor', 1, 'Drive job workflow design, candidate experience and release planning across teams.',
       'PUBLISHED', '2026-04-02 10:00:00', '2026-05-31 23:59:59', '2026-04-02 10:00:00', '2026-04-02 10:00:00'
FROM sys_user u
WHERE u.email = 'talent@smarthire.com'
  AND NOT EXISTS (SELECT 1 FROM job_post jp WHERE jp.job_code = 'JOB202604060004');

INSERT INTO job_post (
    company_user_id, job_code, title, category, location, salary_min, salary_max,
    experience_requirement, education_requirement, headcount, description, status,
    published_at, expire_at, created_at, updated_at
)
SELECT u.id, 'JOB202604060005', 'QA Automation Engineer', 'testing', 'Shanghai', 12000, 19000,
       '2 years', 'Bachelor', 2, 'Build API automation, regression suites and testing utilities for the recruiting platform.',
       'PUBLISHED', '2026-04-02 10:30:00', '2026-05-31 23:59:59', '2026-04-02 10:30:00', '2026-04-02 10:30:00'
FROM sys_user u
WHERE u.email = 'talent@smarthire.com'
  AND NOT EXISTS (SELECT 1 FROM job_post jp WHERE jp.job_code = 'JOB202604060005');

INSERT INTO job_post (
    company_user_id, job_code, title, category, location, salary_min, salary_max,
    experience_requirement, education_requirement, headcount, description, status,
    published_at, expire_at, created_at, updated_at
)
SELECT u.id, 'JOB202604060006', 'Java Backend Intern', 'backend', 'Shenzhen', 5000, 7000,
       'student', 'Bachelor', 2, 'Support backend APIs, test environment issues and integration tasks for the hiring system.',
       'PUBLISHED', '2026-04-02 11:00:00', '2026-05-31 23:59:59', '2026-04-02 11:00:00', '2026-04-02 11:00:00'
FROM sys_user u
WHERE u.email = 'hr@futuretech.com'
  AND NOT EXISTS (SELECT 1 FROM job_post jp WHERE jp.job_code = 'JOB202604060006');

INSERT INTO job_application (
    job_id, company_user_id, jobseeker_user_id, resume_id, status, status_remark,
    applied_at, viewed_at, created_at, updated_at
)
SELECT jp.id, cu.id, ju.id, r.id, 'VIEWED', 'Resume reviewed and technical screen pending',
       '2026-04-03 10:00:00', '2026-04-03 11:00:00', '2026-04-03 10:00:00', '2026-04-03 11:00:00'
FROM job_post jp
JOIN sys_user cu ON cu.id = jp.company_user_id
JOIN sys_user ju ON ju.email = 'bob@example.com'
JOIN resume r ON r.user_id = ju.id
WHERE jp.job_code = 'JOB202604060002'
  AND NOT EXISTS (SELECT 1 FROM job_application ja WHERE ja.job_id = jp.id AND ja.jobseeker_user_id = ju.id);

INSERT INTO job_application (
    job_id, company_user_id, jobseeker_user_id, resume_id, status, status_remark,
    applied_at, viewed_at, created_at, updated_at
)
SELECT jp.id, cu.id, ju.id, r.id, 'SUBMITTED', 'Waiting for company review',
       '2026-04-03 11:00:00', NULL, '2026-04-03 11:00:00', '2026-04-03 11:00:00'
FROM job_post jp
JOIN sys_user cu ON cu.id = jp.company_user_id
JOIN sys_user ju ON ju.email = 'charlie@example.com'
JOIN resume r ON r.user_id = ju.id
WHERE jp.job_code = 'JOB202604060004'
  AND NOT EXISTS (SELECT 1 FROM job_application ja WHERE ja.job_id = jp.id AND ja.jobseeker_user_id = ju.id);

INSERT INTO job_application (
    job_id, company_user_id, jobseeker_user_id, resume_id, status, status_remark,
    applied_at, viewed_at, created_at, updated_at
)
SELECT jp.id, cu.id, ju.id, r.id, 'ACCEPTED', 'Passed final round, offer pending',
       '2026-04-03 12:00:00', '2026-04-03 13:00:00', '2026-04-03 12:00:00', '2026-04-05 15:00:00'
FROM job_post jp
JOIN sys_user cu ON cu.id = jp.company_user_id
JOIN sys_user ju ON ju.email = 'diana@example.com'
JOIN resume r ON r.user_id = ju.id
WHERE jp.job_code = 'JOB202604060003'
  AND NOT EXISTS (SELECT 1 FROM job_application ja WHERE ja.job_id = jp.id AND ja.jobseeker_user_id = ju.id);

INSERT INTO job_application (
    job_id, company_user_id, jobseeker_user_id, resume_id, status, status_remark,
    applied_at, viewed_at, created_at, updated_at
)
SELECT jp.id, cu.id, ju.id, r.id, 'SUBMITTED', 'Intern position applied',
       '2026-04-04 09:30:00', NULL, '2026-04-04 09:30:00', '2026-04-04 09:30:00'
FROM job_post jp
JOIN sys_user cu ON cu.id = jp.company_user_id
JOIN sys_user ju ON ju.email = 'alice@example.com'
JOIN resume r ON r.user_id = ju.id
WHERE jp.job_code = 'JOB202604060006'
  AND NOT EXISTS (SELECT 1 FROM job_application ja WHERE ja.job_id = jp.id AND ja.jobseeker_user_id = ju.id);

INSERT INTO application_status_log (application_id, from_status, to_status, operator_user_id, operator_role, remark, created_at)
SELECT ja.id, NULL, 'SUBMITTED', ju.id, 'JOBSEEKER', 'Candidate submitted resume', '2026-04-03 10:00:00'
FROM job_application ja
JOIN sys_user ju ON ju.id = ja.jobseeker_user_id
JOIN job_post jp ON jp.id = ja.job_id
WHERE jp.job_code = 'JOB202604060002'
  AND ju.email = 'bob@example.com'
  AND NOT EXISTS (SELECT 1 FROM application_status_log l WHERE l.application_id = ja.id AND l.to_status = 'SUBMITTED' AND l.created_at = '2026-04-03 10:00:00');

INSERT INTO application_status_log (application_id, from_status, to_status, operator_user_id, operator_role, remark, created_at)
SELECT ja.id, 'SUBMITTED', 'VIEWED', cu.id, 'COMPANY', 'Company reviewed the resume', '2026-04-03 11:00:00'
FROM job_application ja
JOIN sys_user cu ON cu.id = ja.company_user_id
JOIN job_post jp ON jp.id = ja.job_id
WHERE jp.job_code = 'JOB202604060002'
  AND cu.email = 'recruiter@bluecloud.com'
  AND NOT EXISTS (SELECT 1 FROM application_status_log l WHERE l.application_id = ja.id AND l.to_status = 'VIEWED' AND l.created_at = '2026-04-03 11:00:00');

INSERT INTO application_status_log (application_id, from_status, to_status, operator_user_id, operator_role, remark, created_at)
SELECT ja.id, NULL, 'SUBMITTED', ju.id, 'JOBSEEKER', 'Candidate submitted resume', '2026-04-03 11:00:00'
FROM job_application ja
JOIN sys_user ju ON ju.id = ja.jobseeker_user_id
JOIN job_post jp ON jp.id = ja.job_id
WHERE jp.job_code = 'JOB202604060004'
  AND ju.email = 'charlie@example.com'
  AND NOT EXISTS (SELECT 1 FROM application_status_log l WHERE l.application_id = ja.id AND l.to_status = 'SUBMITTED' AND l.created_at = '2026-04-03 11:00:00');

INSERT INTO application_status_log (application_id, from_status, to_status, operator_user_id, operator_role, remark, created_at)
SELECT ja.id, NULL, 'SUBMITTED', ju.id, 'JOBSEEKER', 'Candidate submitted resume', '2026-04-03 12:00:00'
FROM job_application ja
JOIN sys_user ju ON ju.id = ja.jobseeker_user_id
JOIN job_post jp ON jp.id = ja.job_id
WHERE jp.job_code = 'JOB202604060003'
  AND ju.email = 'diana@example.com'
  AND NOT EXISTS (SELECT 1 FROM application_status_log l WHERE l.application_id = ja.id AND l.to_status = 'SUBMITTED' AND l.created_at = '2026-04-03 12:00:00');

INSERT INTO application_status_log (application_id, from_status, to_status, operator_user_id, operator_role, remark, created_at)
SELECT ja.id, 'SUBMITTED', 'VIEWED', cu.id, 'COMPANY', 'Company reviewed the resume', '2026-04-03 13:00:00'
FROM job_application ja
JOIN sys_user cu ON cu.id = ja.company_user_id
JOIN job_post jp ON jp.id = ja.job_id
WHERE jp.job_code = 'JOB202604060003'
  AND cu.email = 'recruiter@bluecloud.com'
  AND NOT EXISTS (SELECT 1 FROM application_status_log l WHERE l.application_id = ja.id AND l.to_status = 'VIEWED' AND l.created_at = '2026-04-03 13:00:00');

INSERT INTO application_status_log (application_id, from_status, to_status, operator_user_id, operator_role, remark, created_at)
SELECT ja.id, 'VIEWED', 'INTERVIEWING', cu.id, 'COMPANY', 'Moved into final round', '2026-04-04 10:00:00'
FROM job_application ja
JOIN sys_user cu ON cu.id = ja.company_user_id
JOIN job_post jp ON jp.id = ja.job_id
WHERE jp.job_code = 'JOB202604060003'
  AND cu.email = 'recruiter@bluecloud.com'
  AND NOT EXISTS (SELECT 1 FROM application_status_log l WHERE l.application_id = ja.id AND l.to_status = 'INTERVIEWING' AND l.created_at = '2026-04-04 10:00:00');

INSERT INTO application_status_log (application_id, from_status, to_status, operator_user_id, operator_role, remark, created_at)
SELECT ja.id, 'INTERVIEWING', 'ACCEPTED', cu.id, 'COMPANY', 'Candidate passed the final round', '2026-04-05 15:00:00'
FROM job_application ja
JOIN sys_user cu ON cu.id = ja.company_user_id
JOIN job_post jp ON jp.id = ja.job_id
WHERE jp.job_code = 'JOB202604060003'
  AND cu.email = 'recruiter@bluecloud.com'
  AND NOT EXISTS (SELECT 1 FROM application_status_log l WHERE l.application_id = ja.id AND l.to_status = 'ACCEPTED' AND l.created_at = '2026-04-05 15:00:00');

INSERT INTO application_status_log (application_id, from_status, to_status, operator_user_id, operator_role, remark, created_at)
SELECT ja.id, NULL, 'SUBMITTED', ju.id, 'JOBSEEKER', 'Applied to intern role', '2026-04-04 09:30:00'
FROM job_application ja
JOIN sys_user ju ON ju.id = ja.jobseeker_user_id
JOIN job_post jp ON jp.id = ja.job_id
WHERE jp.job_code = 'JOB202604060006'
  AND ju.email = 'alice@example.com'
  AND NOT EXISTS (SELECT 1 FROM application_status_log l WHERE l.application_id = ja.id AND l.to_status = 'SUBMITTED' AND l.created_at = '2026-04-04 09:30:00');

INSERT INTO conversation (company_user_id, jobseeker_user_id, last_message_at, created_at)
SELECT cu.id, ju.id, '2026-04-03 11:40:00', '2026-04-03 11:05:00'
FROM sys_user cu
JOIN sys_user ju ON ju.email = 'bob@example.com'
WHERE cu.email = 'recruiter@bluecloud.com'
  AND NOT EXISTS (SELECT 1 FROM conversation c WHERE c.company_user_id = cu.id AND c.jobseeker_user_id = ju.id);

INSERT INTO conversation (company_user_id, jobseeker_user_id, last_message_at, created_at)
SELECT cu.id, ju.id, '2026-04-03 14:20:00', '2026-04-03 14:00:00'
FROM sys_user cu
JOIN sys_user ju ON ju.email = 'charlie@example.com'
WHERE cu.email = 'talent@smarthire.com'
  AND NOT EXISTS (SELECT 1 FROM conversation c WHERE c.company_user_id = cu.id AND c.jobseeker_user_id = ju.id);

INSERT INTO conversation (company_user_id, jobseeker_user_id, last_message_at, created_at)
SELECT cu.id, ju.id, '2026-04-05 15:20:00', '2026-04-04 10:10:00'
FROM sys_user cu
JOIN sys_user ju ON ju.email = 'diana@example.com'
WHERE cu.email = 'recruiter@bluecloud.com'
  AND NOT EXISTS (SELECT 1 FROM conversation c WHERE c.company_user_id = cu.id AND c.jobseeker_user_id = ju.id);

INSERT INTO chat_message (conversation_id, sender_user_id, receiver_user_id, content, read_flag, created_at)
SELECT c.id, cu.id, ju.id, 'We reviewed your frontend work and would like to discuss performance optimization experience.', 1, '2026-04-03 11:05:00'
FROM conversation c
JOIN sys_user cu ON cu.id = c.company_user_id
JOIN sys_user ju ON ju.id = c.jobseeker_user_id
WHERE cu.email = 'recruiter@bluecloud.com'
  AND ju.email = 'bob@example.com'
  AND NOT EXISTS (SELECT 1 FROM chat_message m WHERE m.conversation_id = c.id AND m.created_at = '2026-04-03 11:05:00');

INSERT INTO chat_message (conversation_id, sender_user_id, receiver_user_id, content, read_flag, created_at)
SELECT c.id, ju.id, cu.id, 'I recently led the resume editor and dashboard pages and improved first-screen loading time.', 1, '2026-04-03 11:18:00'
FROM conversation c
JOIN sys_user cu ON cu.id = c.company_user_id
JOIN sys_user ju ON ju.id = c.jobseeker_user_id
WHERE cu.email = 'recruiter@bluecloud.com'
  AND ju.email = 'bob@example.com'
  AND NOT EXISTS (SELECT 1 FROM chat_message m WHERE m.conversation_id = c.id AND m.created_at = '2026-04-03 11:18:00');

INSERT INTO chat_message (conversation_id, sender_user_id, receiver_user_id, content, read_flag, created_at)
SELECT c.id, cu.id, ju.id, 'We will arrange a short online meeting this week and send the time shortly.', 0, '2026-04-03 11:40:00'
FROM conversation c
JOIN sys_user cu ON cu.id = c.company_user_id
JOIN sys_user ju ON ju.id = c.jobseeker_user_id
WHERE cu.email = 'recruiter@bluecloud.com'
  AND ju.email = 'bob@example.com'
  AND NOT EXISTS (SELECT 1 FROM chat_message m WHERE m.conversation_id = c.id AND m.created_at = '2026-04-03 11:40:00');

INSERT INTO chat_message (conversation_id, sender_user_id, receiver_user_id, content, read_flag, created_at)
SELECT c.id, cu.id, ju.id, 'We are interested in your workflow product experience. Could you share a representative project?', 1, '2026-04-03 14:00:00'
FROM conversation c
JOIN sys_user cu ON cu.id = c.company_user_id
JOIN sys_user ju ON ju.id = c.jobseeker_user_id
WHERE cu.email = 'talent@smarthire.com'
  AND ju.email = 'charlie@example.com'
  AND NOT EXISTS (SELECT 1 FROM chat_message m WHERE m.conversation_id = c.id AND m.created_at = '2026-04-03 14:00:00');

INSERT INTO chat_message (conversation_id, sender_user_id, receiver_user_id, content, read_flag, created_at)
SELECT c.id, ju.id, cu.id, 'I added a smart hiring platform project that covers workflow, notifications and analytics.', 1, '2026-04-03 14:20:00'
FROM conversation c
JOIN sys_user cu ON cu.id = c.company_user_id
JOIN sys_user ju ON ju.id = c.jobseeker_user_id
WHERE cu.email = 'talent@smarthire.com'
  AND ju.email = 'charlie@example.com'
  AND NOT EXISTS (SELECT 1 FROM chat_message m WHERE m.conversation_id = c.id AND m.created_at = '2026-04-03 14:20:00');

INSERT INTO chat_message (conversation_id, sender_user_id, receiver_user_id, content, read_flag, created_at)
SELECT c.id, cu.id, ju.id, 'Congratulations on passing the final round. We will share offer details today.', 0, '2026-04-05 15:20:00'
FROM conversation c
JOIN sys_user cu ON cu.id = c.company_user_id
JOIN sys_user ju ON ju.id = c.jobseeker_user_id
WHERE cu.email = 'recruiter@bluecloud.com'
  AND ju.email = 'diana@example.com'
  AND NOT EXISTS (SELECT 1 FROM chat_message m WHERE m.conversation_id = c.id AND m.created_at = '2026-04-05 15:20:00');

INSERT INTO notification (user_id, type, title, content, read_flag, created_at)
SELECT ju.id, 'APPLICATION_STATUS', 'Application status updated', 'Your application for Frontend Engineer has been reviewed.', 0, '2026-04-03 11:00:00'
FROM sys_user ju
WHERE ju.email = 'bob@example.com'
  AND NOT EXISTS (SELECT 1 FROM notification n WHERE n.user_id = ju.id AND n.title = 'Application status updated' AND n.created_at = '2026-04-03 11:00:00');

INSERT INTO notification (user_id, type, title, content, read_flag, created_at)
SELECT ju.id, 'NEW_MESSAGE', 'New message received', 'BlueCloud Recruiter sent you a new message.', 0, '2026-04-03 11:40:00'
FROM sys_user ju
WHERE ju.email = 'bob@example.com'
  AND NOT EXISTS (SELECT 1 FROM notification n WHERE n.user_id = ju.id AND n.title = 'New message received' AND n.created_at = '2026-04-03 11:40:00');

INSERT INTO notification (user_id, type, title, content, read_flag, created_at)
SELECT ju.id, 'NEW_MESSAGE', 'New message received', 'SmartHire HR sent you a new message.', 0, '2026-04-03 14:20:00'
FROM sys_user ju
WHERE ju.email = 'charlie@example.com'
  AND NOT EXISTS (SELECT 1 FROM notification n WHERE n.user_id = ju.id AND n.title = 'New message received' AND n.created_at = '2026-04-03 14:20:00');

INSERT INTO notification (user_id, type, title, content, read_flag, created_at)
SELECT ju.id, 'APPLICATION_STATUS', 'Application status updated', 'Your application for Data Analyst passed the final round.', 0, '2026-04-05 15:00:00'
FROM sys_user ju
WHERE ju.email = 'diana@example.com'
  AND NOT EXISTS (SELECT 1 FROM notification n WHERE n.user_id = ju.id AND n.title = 'Application status updated' AND n.created_at = '2026-04-05 15:00:00');

INSERT INTO notification (user_id, type, title, content, read_flag, created_at)
SELECT cu.id, 'NEW_APPLICATION', 'New application received', 'The Frontend Engineer role received a new application.', 0, '2026-04-03 10:00:00'
FROM sys_user cu
WHERE cu.email = 'recruiter@bluecloud.com'
  AND NOT EXISTS (SELECT 1 FROM notification n WHERE n.user_id = cu.id AND n.title = 'New application received' AND n.created_at = '2026-04-03 10:00:00');

INSERT INTO notification (user_id, type, title, content, read_flag, created_at)
SELECT cu.id, 'NEW_APPLICATION', 'New application received', 'The Product Manager role received a new application.', 0, '2026-04-03 11:00:00'
FROM sys_user cu
WHERE cu.email = 'talent@smarthire.com'
  AND NOT EXISTS (SELECT 1 FROM notification n WHERE n.user_id = cu.id AND n.title = 'New application received' AND n.created_at = '2026-04-03 11:00:00');

COMMIT;
