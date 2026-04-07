



SET NAMES utf8mb4;
USE recruitment_system;

-- 所有演示账号的登录密码都是 123456
-- BCrypt 密文: $2a$10$SJelv4L6pIWsUdlzGKwj..6X.ZEwMcgZxUchcwMnKBcWfUAxXNx4m

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM notification;
DELETE FROM chat_message;
DELETE FROM conversation;
DELETE FROM application_status_log;
DELETE FROM job_application;
DELETE FROM resume_skill;
DELETE FROM resume_project;
DELETE FROM resume_experience;
DELETE FROM resume_education;
DELETE FROM resume;
DELETE FROM job_post;
DELETE FROM skill_dict;
DELETE FROM company_profile;
DELETE FROM jobseeker_profile;
DELETE FROM sys_user_role;
DELETE FROM sys_role;
DELETE FROM sys_user;

ALTER TABLE sys_user AUTO_INCREMENT = 1;
ALTER TABLE sys_role AUTO_INCREMENT = 1;
ALTER TABLE sys_user_role AUTO_INCREMENT = 1;
ALTER TABLE company_profile AUTO_INCREMENT = 1;
ALTER TABLE jobseeker_profile AUTO_INCREMENT = 1;
ALTER TABLE job_post AUTO_INCREMENT = 1;
ALTER TABLE resume AUTO_INCREMENT = 1;
ALTER TABLE resume_education AUTO_INCREMENT = 1;
ALTER TABLE resume_experience AUTO_INCREMENT = 1;
ALTER TABLE resume_project AUTO_INCREMENT = 1;
ALTER TABLE skill_dict AUTO_INCREMENT = 1;
ALTER TABLE resume_skill AUTO_INCREMENT = 1;
ALTER TABLE job_application AUTO_INCREMENT = 1;
ALTER TABLE application_status_log AUTO_INCREMENT = 1;
ALTER TABLE conversation AUTO_INCREMENT = 1;
ALTER TABLE chat_message AUTO_INCREMENT = 1;
ALTER TABLE notification AUTO_INCREMENT = 1;

INSERT INTO sys_role (id, code, name) VALUES
(1, 'ADMIN', '管理员'),
(2, 'COMPANY', '企业用户'),
(3, 'JOBSEEKER', '求职者');

INSERT INTO sys_user (id, username, password, email, phone, display_name, status, created_at, updated_at) VALUES
(1, 'admin@recruitment.local', '$2a$10$SJelv4L6pIWsUdlzGKwj..6X.ZEwMcgZxUchcwMnKBcWfUAxXNx4m', 'admin@recruitment.local', '13900000000', '系统管理员', 'ACTIVE', '2026-03-20 09:00:00', '2026-03-20 09:00:00'),
(2, 'hr@futuretech.com', '$2a$10$SJelv4L6pIWsUdlzGKwj..6X.ZEwMcgZxUchcwMnKBcWfUAxXNx4m', 'hr@futuretech.com', '13800000001', '未来科技HR', 'ACTIVE', '2026-03-20 09:10:00', '2026-03-20 09:10:00'),
(3, 'recruiter@bluecloud.com', '$2a$10$SJelv4L6pIWsUdlzGKwj..6X.ZEwMcgZxUchcwMnKBcWfUAxXNx4m', 'recruiter@bluecloud.com', '13800000003', '蓝云招聘', 'ACTIVE', '2026-03-20 09:20:00', '2026-03-20 09:20:00'),
(4, 'alice@example.com', '$2a$10$SJelv4L6pIWsUdlzGKwj..6X.ZEwMcgZxUchcwMnKBcWfUAxXNx4m', 'alice@example.com', '13700000002', '李同学', 'ACTIVE', '2026-03-20 09:30:00', '2026-03-20 09:30:00'),
(5, 'bob@example.com', '$2a$10$SJelv4L6pIWsUdlzGKwj..6X.ZEwMcgZxUchcwMnKBcWfUAxXNx4m', 'bob@example.com', '13700000005', '王同学', 'ACTIVE', '2026-03-20 09:40:00', '2026-03-20 09:40:00');

INSERT INTO sys_user_role (id, user_id, role_id) VALUES
(1, 1, 1),
(2, 2, 2),
(3, 3, 2),
(4, 4, 3),
(5, 5, 3);

INSERT INTO company_profile (id, user_id, company_name, unified_social_credit_code, contact_person, phone, email, address, description, audit_status, created_at, updated_at) VALUES
(1, 2, '未来科技有限公司', '91310000MA1K123456', '张经理', '13800000001', 'hr@futuretech.com', '深圳南山区科技园', '专注于企业招聘平台、人力协同系统和面向高校招聘的数字化产品建设。', 'APPROVED', '2026-03-20 09:10:00', '2026-03-20 09:10:00'),
(2, 3, '蓝云数据有限公司', '91310000MA1K654321', '周主管', '13800000003', 'recruiter@bluecloud.com', '广州天河区软件园', '提供数据平台、云原生应用和智能分析系统研发服务，持续招聘后端与前端工程师。', 'APPROVED', '2026-03-20 09:20:00', '2026-03-20 09:20:00');

INSERT INTO jobseeker_profile (id, user_id, full_name, phone, email, desired_position_category, expected_salary_min, expected_salary_max, preferred_city, highest_education, years_of_experience, created_at, updated_at) VALUES
(1, 4, '李同学', '13700000002', 'alice@example.com', '后端开发', 10000, 18000, '深圳', '本科', 3, '2026-03-20 09:30:00', '2026-03-20 09:30:00'),
(2, 5, '王同学', '13700000005', 'bob@example.com', '前端开发', 9000, 16000, '广州', '本科', 2, '2026-03-20 09:40:00', '2026-03-20 09:40:00');

INSERT INTO skill_dict (id, skill_name, category) VALUES
(1, 'Java', '默认'),
(2, 'Spring Boot', '默认'),
(3, 'MySQL', '默认'),
(4, 'MyBatis Plus', '默认'),
(5, 'Vue3', '默认'),
(6, 'TypeScript', '默认'),
(7, 'Element Plus', '默认'),
(8, 'Redis', '默认'),
(9, 'Linux', '默认'),
(10, 'Docker', '默认'),
(11, 'HTML', '默认'),
(12, 'CSS', '默认'),
(13, 'JavaScript', '默认'),
(14, 'Git', '默认'),
(15, 'RESTful API', '默认');

INSERT INTO job_post (id, company_user_id, job_code, title, category, location, salary_min, salary_max, experience_requirement, education_requirement, headcount, description, status, published_at, expire_at, created_at, updated_at) VALUES
(1, 2, 'JOB202603200001', 'Java后端开发工程师', '后端开发', '深圳', 12000, 20000, '3年', '本科', 3,
'负责企业招聘系统后台服务开发，参与 Spring Boot、MyBatis Plus、MySQL、Vue3 相关业务联调与优化，覆盖岗位管理、简历投递、推荐匹配、消息通知等核心模块建设，并持续参与接口设计、性能优化与上线支持工作。', 'PUBLISHED', '2026-03-21 10:00:00', '2026-04-30 23:59:59', '2026-03-20 10:00:00', '2026-03-21 10:00:00'),
(2, 3, 'JOB202603200002', 'Vue3前端开发工程师', '前端开发', '广州', 10000, 17000, '2年', '本科', 2,
'负责招聘工作台、简历创作器、消息中心和数据看板的前端实现，使用 Vue3、TypeScript、Element Plus 构建响应式界面，参与页面性能优化、交互设计落地和前后端联调。', 'PUBLISHED', '2026-03-21 11:00:00', '2026-04-30 23:59:59', '2026-03-20 10:10:00', '2026-03-21 11:00:00'),
(3, 2, 'JOB202603200003', '招聘产品实习生', '产品实习', '深圳', 4000, 6000, '在校生', '本科', 1,
'协助整理招聘系统产品需求、跟进页面交互方案、参与原型评审和测试验收，支持岗位管理、简历投递和消息通知模块的产品文档输出。', 'DRAFT', NULL, '2026-05-30 23:59:59', '2026-03-20 10:20:00', '2026-03-20 10:20:00');

INSERT INTO resume (id, user_id, template_code, full_name, gender, age, phone, email, city, summary, expected_category, expected_salary_min, expected_salary_max, highest_education, years_of_experience, completeness_score, created_at, updated_at) VALUES
(1, 4, 'classic', '李同学', '女', 24, '13700000002', 'alice@example.com', '深圳',
'熟悉 Java、Spring Boot、MySQL 和 Vue3 的全栈开发，具备企业管理系统与招聘平台项目经验，擅长接口设计、数据建模和前后端联调。',
'后端开发', 10000, 18000, '本科', 3, 92, '2026-03-20 09:35:00', '2026-03-22 20:00:00'),
(2, 5, 'classic', '王同学', '男', 23, '13700000005', 'bob@example.com', '广州',
'熟悉 Vue3、TypeScript、Element Plus 和前端工程化，参与过后台管理系统、信息展示平台和数据可视化项目开发。',
'前端开发', 9000, 16000, '本科', 2, 88, '2026-03-20 09:45:00', '2026-03-22 18:00:00');

INSERT INTO resume_education (id, resume_id, school_name, major, degree, start_date, end_date, description, sort_order) VALUES
(1, 1, '华南理工大学', '软件工程', '本科', '2018-09-01', '2022-06-30', '主修 Java 开发、数据库原理、软件工程与 Web 应用开发。', 0),
(2, 2, '暨南大学', '数字媒体技术', '本科', '2019-09-01', '2023-06-30', '主修前端开发、交互设计、图形图像处理与可视化课程。', 0);

INSERT INTO resume_experience (id, resume_id, company_name, job_title, start_date, end_date, description, sort_order) VALUES
(1, 1, '某企业软件公司', '后端开发工程师', '2023-07-01', '2025-03-01', '参与招聘管理平台、权限系统和消息中心开发，负责接口设计、数据库建模和后端优化。', 0),
(2, 2, '某互联网工作室', '前端开发工程师', '2024-01-01', '2025-03-01', '负责管理后台、数据看板和移动端页面开发，参与组件封装和交互体验优化。', 0);

INSERT INTO resume_project (id, resume_id, project_name, role_name, start_date, end_date, description, sort_order) VALUES
(1, 1, '企业在线招聘系统', '后端负责人', '2024-06-01', '2025-01-31', '负责用户权限、岗位管理、投递流程、聊天通知等模块设计与实现。', 0),
(2, 2, '招聘工作台重构项目', '前端主开发', '2024-08-01', '2025-02-28', '负责岗位工作台、双栏简历创作器、消息中心和移动端适配。', 0);

INSERT INTO resume_skill (id, resume_id, skill_id, skill_name) VALUES
(1, 1, 1, 'Java'),
(2, 1, 2, 'Spring Boot'),
(3, 1, 3, 'MySQL'),
(4, 1, 4, 'MyBatis Plus'),
(5, 1, 5, 'Vue3'),
(6, 2, 5, 'Vue3'),
(7, 2, 6, 'TypeScript'),
(8, 2, 7, 'Element Plus'),
(9, 2, 11, 'HTML'),
(10, 2, 12, 'CSS');

INSERT INTO job_application (id, job_id, company_user_id, jobseeker_user_id, resume_id, status, status_remark, applied_at, viewed_at, created_at, updated_at) VALUES
(1, 1, 2, 4, 1, 'INTERVIEWING', '已进入面试安排阶段', '2026-03-22 09:30:00', '2026-03-22 10:00:00', '2026-03-22 09:30:00', '2026-03-22 15:00:00'),
(2, 2, 3, 5, 2, 'VIEWED', '简历已查看', '2026-03-22 11:00:00', '2026-03-22 12:00:00', '2026-03-22 11:00:00', '2026-03-22 12:00:00');

INSERT INTO application_status_log (id, application_id, from_status, to_status, operator_user_id, operator_role, remark, created_at) VALUES
(1, 1, NULL, 'SUBMITTED', 4, 'JOBSEEKER', '求职者提交简历', '2026-03-22 09:30:00'),
(2, 1, 'SUBMITTED', 'VIEWED', 2, 'COMPANY', '企业已查看简历', '2026-03-22 10:00:00'),
(3, 1, 'VIEWED', 'INTERVIEWING', 2, 'COMPANY', '通知候选人准备面试', '2026-03-22 15:00:00'),
(4, 2, NULL, 'SUBMITTED', 5, 'JOBSEEKER', '求职者提交简历', '2026-03-22 11:00:00'),
(5, 2, 'SUBMITTED', 'VIEWED', 3, 'COMPANY', '企业已查看简历', '2026-03-22 12:00:00');

INSERT INTO conversation (id, company_user_id, jobseeker_user_id, last_message_at, created_at) VALUES
(1, 2, 4, '2026-03-22 16:10:00', '2026-03-22 15:20:00'),
(2, 3, 5, '2026-03-22 13:30:00', '2026-03-22 12:20:00');

INSERT INTO chat_message (id, conversation_id, sender_user_id, receiver_user_id, content, read_flag, created_at) VALUES
(1, 1, 2, 4, '您好，我们已经查看了您的简历，方便进一步沟通项目经验吗？', 1, '2026-03-22 15:20:00'),
(2, 1, 4, 2, '可以的，我最近主要负责权限模块和招聘流程模块开发。', 1, '2026-03-22 15:28:00'),
(3, 1, 2, 4, '很好，我们计划本周安排一轮线上面试。', 0, '2026-03-22 16:10:00'),
(4, 2, 3, 5, '您好，我们对您的前端项目比较感兴趣，方便发一些作品链接吗？', 0, '2026-03-22 13:30:00');

INSERT INTO notification (id, user_id, type, title, content, read_flag, created_at) VALUES
(1, 4, 'APPLICATION_STATUS', '投递状态已更新', '您的简历投递状态已更新为 INTERVIEWING。', 0, '2026-03-22 15:00:00'),
(2, 4, 'NEW_MESSAGE', '收到新的聊天消息', '未来科技HR 向您发送了一条新消息。', 0, '2026-03-22 16:10:00'),
(3, 5, 'APPLICATION_STATUS', '投递状态已更新', '您的简历投递状态已更新为 VIEWED。', 1, '2026-03-22 12:00:00'),
(4, 2, 'NEW_APPLICATION', '收到新的简历投递', '岗位 Java后端开发工程师 收到了一份新的简历投递。', 1, '2026-03-22 09:30:00'),
(5, 3, 'NEW_APPLICATION', '收到新的简历投递', '岗位 Vue3前端开发工程师 收到了一份新的简历投递。', 1, '2026-03-22 11:00:00');

SET FOREIGN_KEY_CHECKS = 1;
