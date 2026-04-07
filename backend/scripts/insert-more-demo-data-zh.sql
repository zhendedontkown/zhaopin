SET NAMES utf8mb4;
USE recruitment_system;

START TRANSACTION;

SET @demo_password_hash = '$2a$10$XsHtqW3.iGx2dfpNgwHoFO4BaQDrAObDCSWvIaQpKfw28PDD4KLBC';

UPDATE sys_user SET display_name = '蓝云招聘' WHERE email = 'recruiter@bluecloud.com';
UPDATE sys_user SET display_name = '智聘科技HR' WHERE email = 'talent@smarthire.com';
UPDATE sys_user SET display_name = '王同学' WHERE email = 'bob@example.com';
UPDATE sys_user SET display_name = '陈同学' WHERE email = 'charlie@example.com';
UPDATE sys_user SET display_name = '周同学' WHERE email = 'diana@example.com';

UPDATE company_profile
SET company_name = '蓝云数据有限公司',
    contact_person = '周主管',
    address = '广州天河区软件园',
    description = '专注于数据平台、分析看板和企业数字化系统建设。'
WHERE email = 'recruiter@bluecloud.com';

UPDATE company_profile
SET company_name = '智聘科技有限公司',
    contact_person = '林经理',
    address = '上海浦东新区张江高科',
    description = '聚焦招聘协同平台、流程工具和智能匹配产品。'
WHERE email = 'talent@smarthire.com';

UPDATE jobseeker_profile
SET full_name = '王同学',
    desired_position_category = '前端开发',
    preferred_city = '广州',
    highest_education = '本科'
WHERE email = 'bob@example.com';

UPDATE jobseeker_profile
SET full_name = '陈同学',
    desired_position_category = '产品经理',
    preferred_city = '深圳',
    highest_education = '本科'
WHERE email = 'charlie@example.com';

UPDATE jobseeker_profile
SET full_name = '周同学',
    desired_position_category = '数据分析',
    preferred_city = '上海',
    highest_education = '硕士'
WHERE email = 'diana@example.com';

UPDATE resume
SET full_name = '王同学',
    gender = '男',
    city = '广州',
    summary = '熟悉 Vue3、TypeScript、Element Plus 和中后台页面开发，参与过招聘工作台和数据看板建设。',
    expected_category = '前端开发',
    highest_education = '本科'
WHERE email = 'bob@example.com';

UPDATE resume
SET full_name = '陈同学',
    gender = '男',
    city = '深圳',
    summary = '具备招聘平台和流程产品经验，擅长需求拆解、原型设计和跨团队推进。',
    expected_category = '产品经理',
    highest_education = '本科'
WHERE email = 'charlie@example.com';

UPDATE resume
SET full_name = '周同学',
    gender = '女',
    city = '上海',
    summary = '熟悉 SQL、Python、数据看板和漏斗分析，能独立完成指标体系搭建与分析输出。',
    expected_category = '数据分析',
    highest_education = '硕士'
WHERE email = 'diana@example.com';

UPDATE resume_education
SET school_name = '暨南大学',
    major = '数字媒体技术',
    degree = '本科',
    description = '主修前端开发、交互设计和 Web 应用开发。'
WHERE school_name = 'Jinan University';

UPDATE resume_education
SET school_name = '深圳大学',
    major = '信息管理与信息系统',
    degree = '本科',
    description = '主修产品设计、信息架构、用户研究与数据分析。'
WHERE school_name = 'Shenzhen University';

UPDATE resume_education
SET school_name = '华东师范大学',
    major = '统计学',
    degree = '硕士',
    description = '研究方向为商业分析、统计建模和数据可视化。'
WHERE school_name = 'East China Normal University';

UPDATE resume_experience
SET company_name = '广州某互联网公司',
    job_title = '前端开发工程师',
    description = '负责中后台系统、数据看板和移动端页面开发，推进组件复用和性能优化。'
WHERE company_name = 'Guangzhou Internet Co.';

UPDATE resume_experience
SET company_name = '深圳某软件公司',
    job_title = '产品经理',
    description = '负责招聘协同产品的版本规划、流程设计和跨团队推进。'
WHERE company_name = 'Shenzhen SaaS Co.';

UPDATE resume_experience
SET company_name = '上海某数据公司',
    job_title = '数据分析师',
    description = '负责招聘漏斗分析、指标体系搭建和报表自动化。'
WHERE company_name = 'Shanghai Data Co.';

UPDATE resume_project
SET project_name = '招聘工作台重构项目',
    role_name = '前端主开发',
    description = '负责岗位工作台、双栏简历编辑器和消息中心的设计与实现。'
WHERE project_name = 'Recruiting Workbench Rewrite';

UPDATE resume_project
SET project_name = '智能招聘协同平台',
    role_name = '产品负责人',
    description = '主导候选人流程、状态流转、通知和分析模块设计。'
WHERE project_name = 'Smart Hiring Platform';

UPDATE resume_project
SET project_name = '招聘漏斗分析项目',
    role_name = '数据分析负责人',
    description = '搭建从曝光到录用的漏斗指标体系，并输出渠道优化建议。'
WHERE project_name = 'Recruiting Funnel Analytics';

UPDATE job_post
SET title = 'Vue3前端开发工程师',
    category = '前端开发',
    location = '广州',
    experience_requirement = '2年',
    education_requirement = '本科',
    description = '负责招聘工作台、简历编辑器和数据看板的前端开发，使用 Vue3、TypeScript 和 Element Plus 完成页面建设。'
WHERE job_code = 'JOB202604060002';

UPDATE job_post
SET title = '数据分析师',
    category = '数据分析',
    location = '上海',
    experience_requirement = '3年',
    education_requirement = '本科',
    description = '负责招聘漏斗分析、渠道报表建设和指标体系搭建，熟悉 SQL、Python 和数据可视化。'
WHERE job_code = 'JOB202604060003';

UPDATE job_post
SET title = '产品经理',
    category = '产品经理',
    location = '深圳',
    experience_requirement = '3年',
    education_requirement = '本科',
    description = '负责招聘平台核心流程设计、需求拆解、原型输出与版本规划。'
WHERE job_code = 'JOB202604060004';

UPDATE job_post
SET title = '测试开发工程师',
    category = '测试开发',
    location = '上海',
    experience_requirement = '2年',
    education_requirement = '本科',
    description = '负责接口自动化、回归测试、质量平台建设和测试数据维护。'
WHERE job_code = 'JOB202604060005';

UPDATE job_post
SET title = 'Java后端开发实习生',
    category = '后端开发',
    location = '深圳',
    experience_requirement = '在校生',
    education_requirement = '本科',
    description = '参与招聘系统后端接口联调、基础模块维护和测试环境问题排查。'
WHERE job_code = 'JOB202604060006';

UPDATE job_application
SET status_remark = '简历已查看，待安排技术沟通'
WHERE status_remark = 'Resume reviewed and technical screen pending';

UPDATE job_application
SET status_remark = '已投递，等待企业处理'
WHERE status_remark = 'Waiting for company review';

UPDATE job_application
SET status_remark = '已通过终面，待发放录用通知'
WHERE status_remark = 'Passed final round, offer pending';

UPDATE job_application
SET status_remark = '已投递实习岗位'
WHERE status_remark = 'Intern position applied';

UPDATE application_status_log SET remark = '求职者提交简历' WHERE remark = 'Candidate submitted resume';
UPDATE application_status_log SET remark = '企业已查看简历' WHERE remark = 'Company reviewed the resume';
UPDATE application_status_log SET remark = '进入终面阶段' WHERE remark = 'Moved into final round';
UPDATE application_status_log SET remark = '候选人已通过终面' WHERE remark = 'Candidate passed the final round';
UPDATE application_status_log SET remark = '投递实习岗位' WHERE remark = 'Applied to intern role';

UPDATE chat_message SET content = '您好，我们看了您的前端项目经历，想进一步了解组件化和性能优化经验。'
WHERE content = 'We reviewed your frontend work and would like to discuss performance optimization experience.';

UPDATE chat_message SET content = '可以的，我最近主要负责双栏简历编辑器和数据看板页面，也做过首屏性能优化。'
WHERE content = 'I recently led the resume editor and dashboard pages and improved first-screen loading time.';

UPDATE chat_message SET content = '我们这周会安排一次线上沟通，稍后把时间发给您。'
WHERE content = 'We will arrange a short online meeting this week and send the time shortly.';

UPDATE chat_message SET content = '您好，我们对您的招聘协同产品经验比较感兴趣，方便补充一个代表项目吗？'
WHERE content = 'We are interested in your workflow product experience. Could you share a representative project?';

UPDATE chat_message SET content = '我补充过智能招聘协同平台项目，里面包含流程、通知和分析模块。'
WHERE content = 'I added a smart hiring platform project that covers workflow, notifications and analytics.';

UPDATE chat_message SET content = '恭喜通过终面，我们会在今天内同步录用细节。'
WHERE content = 'Congratulations on passing the final round. We will share offer details today.';

UPDATE notification
SET title = '投递状态已更新',
    content = '您投递的前端开发岗位已进入已查看阶段。'
WHERE title = 'Application status updated' AND content = 'Your application for Frontend Engineer has been reviewed.';

UPDATE notification
SET title = '收到新的聊天消息',
    content = '蓝云招聘向您发送了一条新消息。'
WHERE title = 'New message received' AND content = 'BlueCloud Recruiter sent you a new message.';

UPDATE notification
SET title = '收到新的聊天消息',
    content = '智聘科技HR向您发送了一条新消息。'
WHERE title = 'New message received' AND content = 'SmartHire HR sent you a new message.';

UPDATE notification
SET title = '投递状态已更新',
    content = '您投递的数据分析师岗位已通过终面。'
WHERE title = 'Application status updated' AND content = 'Your application for Data Analyst passed the final round.';

UPDATE notification
SET title = '收到新的简历投递',
    content = '岗位 Vue3前端开发工程师 收到了一份新的简历。'
WHERE title = 'New application received' AND content = 'The Frontend Engineer role received a new application.';

UPDATE notification
SET title = '收到新的简历投递',
    content = '岗位 产品经理 收到了一份新的简历。'
WHERE title = 'New application received' AND content = 'The Product Manager role received a new application.';

INSERT INTO skill_dict (skill_name, category)
SELECT '交互设计', '默认' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM skill_dict WHERE skill_name = '交互设计');

INSERT INTO skill_dict (skill_name, category)
SELECT '数据运营', '默认' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM skill_dict WHERE skill_name = '数据运营');

INSERT INTO skill_dict (skill_name, category)
SELECT '自动化运维', '默认' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM skill_dict WHERE skill_name = '自动化运维');

INSERT INTO skill_dict (skill_name, category)
SELECT '数据可视化', '默认' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM skill_dict WHERE skill_name = '数据可视化');

INSERT INTO sys_user (username, password, email, phone, display_name, status, created_at, updated_at)
SELECT 'campus@huaxing.com', @demo_password_hash, 'campus@huaxing.com', '13800000008', '华星互联HR', 'ACTIVE',
       '2026-04-02 09:00:00', '2026-04-02 09:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE email = 'campus@huaxing.com');

INSERT INTO sys_user (username, password, email, phone, display_name, status, created_at, updated_at)
SELECT 'hire@yunsheng.com', @demo_password_hash, 'hire@yunsheng.com', '13800000009', '云盛科技HR', 'ACTIVE',
       '2026-04-02 09:10:00', '2026-04-02 09:10:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE email = 'hire@yunsheng.com');

INSERT INTO sys_user (username, password, email, phone, display_name, status, created_at, updated_at)
SELECT 'sun@example.com', @demo_password_hash, 'sun@example.com', '13700000008', '孙同学', 'ACTIVE',
       '2026-04-02 09:20:00', '2026-04-02 09:20:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE email = 'sun@example.com');

INSERT INTO sys_user (username, password, email, phone, display_name, status, created_at, updated_at)
SELECT 'lin@example.com', @demo_password_hash, 'lin@example.com', '13700000009', '林同学', 'ACTIVE',
       '2026-04-02 09:30:00', '2026-04-02 09:30:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE email = 'lin@example.com');

INSERT INTO sys_user (username, password, email, phone, display_name, status, created_at, updated_at)
SELECT 'gao@example.com', @demo_password_hash, 'gao@example.com', '13700000010', '高同学', 'ACTIVE',
       '2026-04-02 09:40:00', '2026-04-02 09:40:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE email = 'gao@example.com');

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.code = 'COMPANY'
WHERE u.email IN ('campus@huaxing.com', 'hire@yunsheng.com')
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.code = 'JOBSEEKER'
WHERE u.email IN ('sun@example.com', 'lin@example.com', 'gao@example.com')
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO company_profile (
    user_id, company_name, unified_social_credit_code, contact_person, phone, email, address, description,
    audit_status, created_at, updated_at
)
SELECT u.id, '华星互联科技有限公司', '91330100MA1H888001', '赵经理', '13800000008', 'campus@huaxing.com',
       '杭州余杭区梦想小镇', '专注于校园招聘系统、企业门户和协同办公产品建设。', 'APPROVED',
       '2026-04-02 09:00:00', '2026-04-02 09:00:00'
FROM sys_user u
WHERE u.email = 'campus@huaxing.com'
  AND NOT EXISTS (SELECT 1 FROM company_profile cp WHERE cp.user_id = u.id);

INSERT INTO company_profile (
    user_id, company_name, unified_social_credit_code, contact_person, phone, email, address, description,
    audit_status, created_at, updated_at
)
SELECT u.id, '云盛数字科技有限公司', '91510100MA6C999002', '何主管', '13800000009', 'hire@yunsheng.com',
       '成都高新区天府软件园', '提供企业数据服务、内容平台和增长运营解决方案。', 'APPROVED',
       '2026-04-02 09:10:00', '2026-04-02 09:10:00'
FROM sys_user u
WHERE u.email = 'hire@yunsheng.com'
  AND NOT EXISTS (SELECT 1 FROM company_profile cp WHERE cp.user_id = u.id);

INSERT INTO jobseeker_profile (
    user_id, full_name, phone, email, desired_position_category, expected_salary_min, expected_salary_max,
    preferred_city, highest_education, years_of_experience, created_at, updated_at
)
SELECT u.id, '孙同学', '13700000008', 'sun@example.com', '运维开发', 11000, 18000, '杭州', '本科', 3,
       '2026-04-02 09:20:00', '2026-04-02 09:20:00'
FROM sys_user u
WHERE u.email = 'sun@example.com'
  AND NOT EXISTS (SELECT 1 FROM jobseeker_profile jp WHERE jp.user_id = u.id);

INSERT INTO jobseeker_profile (
    user_id, full_name, phone, email, desired_position_category, expected_salary_min, expected_salary_max,
    preferred_city, highest_education, years_of_experience, created_at, updated_at
)
SELECT u.id, '林同学', '13700000009', 'lin@example.com', '界面设计', 9000, 15000, '成都', '本科', 2,
       '2026-04-02 09:30:00', '2026-04-02 09:30:00'
FROM sys_user u
WHERE u.email = 'lin@example.com'
  AND NOT EXISTS (SELECT 1 FROM jobseeker_profile jp WHERE jp.user_id = u.id);

INSERT INTO jobseeker_profile (
    user_id, full_name, phone, email, desired_position_category, expected_salary_min, expected_salary_max,
    preferred_city, highest_education, years_of_experience, created_at, updated_at
)
SELECT u.id, '高同学', '13700000010', 'gao@example.com', '数据运营', 10000, 17000, '杭州', '本科', 2,
       '2026-04-02 09:40:00', '2026-04-02 09:40:00'
FROM sys_user u
WHERE u.email = 'gao@example.com'
  AND NOT EXISTS (SELECT 1 FROM jobseeker_profile jp WHERE jp.user_id = u.id);

INSERT INTO resume (
    user_id, template_code, full_name, gender, age, phone, email, city, summary, expected_category,
    expected_salary_min, expected_salary_max, highest_education, years_of_experience, completeness_score,
    created_at, updated_at
)
SELECT u.id, 'classic', '孙同学', '男', 25, '13700000008', 'sun@example.com', '杭州',
       '熟悉 Linux、Docker、自动化脚本和部署流程，参与过企业业务系统的日常运维与发布保障。',
       '运维开发', 11000, 18000, '本科', 3, 89, '2026-04-02 09:25:00', '2026-04-05 18:30:00'
FROM sys_user u
WHERE u.email = 'sun@example.com'
  AND NOT EXISTS (SELECT 1 FROM resume r WHERE r.user_id = u.id);

INSERT INTO resume (
    user_id, template_code, full_name, gender, age, phone, email, city, summary, expected_category,
    expected_salary_min, expected_salary_max, highest_education, years_of_experience, completeness_score,
    created_at, updated_at
)
SELECT u.id, 'classic', '林同学', '女', 24, '13700000009', 'lin@example.com', '成都',
       '熟悉 界面设计、交互设计 和 Figma，做过招聘页面、活动页面和企业后台的视觉改版。',
       '界面设计', 9000, 15000, '本科', 2, 90, '2026-04-02 09:35:00', '2026-04-05 19:10:00'
FROM sys_user u
WHERE u.email = 'lin@example.com'
  AND NOT EXISTS (SELECT 1 FROM resume r WHERE r.user_id = u.id);

INSERT INTO resume (
    user_id, template_code, full_name, gender, age, phone, email, city, summary, expected_category,
    expected_salary_min, expected_salary_max, highest_education, years_of_experience, completeness_score,
    created_at, updated_at
)
SELECT u.id, 'classic', '高同学', '男', 24, '13700000010', 'gao@example.com', '杭州',
       '擅长 数据运营、报表整理、活动复盘 和 数据可视化，参与过内容增长和用户转化分析。',
       '数据运营', 10000, 17000, '本科', 2, 88, '2026-04-02 09:45:00', '2026-04-05 19:30:00'
FROM sys_user u
WHERE u.email = 'gao@example.com'
  AND NOT EXISTS (SELECT 1 FROM resume r WHERE r.user_id = u.id);

INSERT INTO resume_education (resume_id, school_name, major, degree, start_date, end_date, description, sort_order)
SELECT r.id, '杭州电子科技大学', '软件工程', '本科', '2017-09-01', '2021-06-30',
       '主修操作系统、计算机网络、数据库和自动化运维相关课程。', 0
FROM resume r
JOIN sys_user u ON u.id = r.user_id
WHERE u.email = 'sun@example.com'
  AND NOT EXISTS (SELECT 1 FROM resume_education re WHERE re.resume_id = r.id AND re.school_name = '杭州电子科技大学');

INSERT INTO resume_education (resume_id, school_name, major, degree, start_date, end_date, description, sort_order)
SELECT r.id, '四川美术学院', '视觉传达设计', '本科', '2018-09-01', '2022-06-30',
       '主修视觉设计、交互设计、品牌表达和界面布局。', 0
FROM resume r
JOIN sys_user u ON u.id = r.user_id
WHERE u.email = 'lin@example.com'
  AND NOT EXISTS (SELECT 1 FROM resume_education re WHERE re.resume_id = r.id AND re.school_name = '四川美术学院');

INSERT INTO resume_education (resume_id, school_name, major, degree, start_date, end_date, description, sort_order)
SELECT r.id, '浙江工商大学', '电子商务', '本科', '2018-09-01', '2022-06-30',
       '主修用户运营、数据分析、活动策划和商业增长课程。', 0
FROM resume r
JOIN sys_user u ON u.id = r.user_id
WHERE u.email = 'gao@example.com'
  AND NOT EXISTS (SELECT 1 FROM resume_education re WHERE re.resume_id = r.id AND re.school_name = '浙江工商大学');

INSERT INTO resume_experience (resume_id, company_name, job_title, start_date, end_date, description, sort_order)
SELECT r.id, '杭州某科技公司', '运维开发工程师', '2022-07-01', '2025-03-01',
       '负责业务系统部署、自动化巡检、日志排查和发布流程保障。', 0
FROM resume r
JOIN sys_user u ON u.id = r.user_id
WHERE u.email = 'sun@example.com'
  AND NOT EXISTS (SELECT 1 FROM resume_experience re WHERE re.resume_id = r.id AND re.company_name = '杭州某科技公司');

INSERT INTO resume_experience (resume_id, company_name, job_title, start_date, end_date, description, sort_order)
SELECT r.id, '成都某设计工作室', '界面设计师', '2023-01-01', '2025-03-01',
       '负责招聘页面、活动专题页和后台界面的视觉设计与交互优化。', 0
FROM resume r
JOIN sys_user u ON u.id = r.user_id
WHERE u.email = 'lin@example.com'
  AND NOT EXISTS (SELECT 1 FROM resume_experience re WHERE re.resume_id = r.id AND re.company_name = '成都某设计工作室');

INSERT INTO resume_experience (resume_id, company_name, job_title, start_date, end_date, description, sort_order)
SELECT r.id, '杭州某内容平台', '数据运营专员', '2023-02-01', '2025-03-01',
       '负责活动数据复盘、渠道分析、报表整理和用户转化跟踪。', 0
FROM resume r
JOIN sys_user u ON u.id = r.user_id
WHERE u.email = 'gao@example.com'
  AND NOT EXISTS (SELECT 1 FROM resume_experience re WHERE re.resume_id = r.id AND re.company_name = '杭州某内容平台');

INSERT INTO resume_project (resume_id, project_name, role_name, start_date, end_date, description, sort_order)
SELECT r.id, '发布流程自动化项目', '运维负责人', '2024-03-01', '2024-12-31',
       '负责部署脚本整理、巡检任务配置和异常告警流程优化。', 0
FROM resume r
JOIN sys_user u ON u.id = r.user_id
WHERE u.email = 'sun@example.com'
  AND NOT EXISTS (SELECT 1 FROM resume_project rp WHERE rp.resume_id = r.id AND rp.project_name = '发布流程自动化项目');

INSERT INTO resume_project (resume_id, project_name, role_name, start_date, end_date, description, sort_order)
SELECT r.id, '招聘门户视觉改版项目', '界面设计负责人', '2024-05-01', '2025-01-31',
       '负责首页、岗位详情页和投递流程页的视觉升级和交互规范整理。', 0
FROM resume r
JOIN sys_user u ON u.id = r.user_id
WHERE u.email = 'lin@example.com'
  AND NOT EXISTS (SELECT 1 FROM resume_project rp WHERE rp.resume_id = r.id AND rp.project_name = '招聘门户视觉改版项目');

INSERT INTO resume_project (resume_id, project_name, role_name, start_date, end_date, description, sort_order)
SELECT r.id, '内容增长数据分析项目', '数据运营负责人', '2024-04-01', '2025-02-28',
       '搭建活动转化报表并输出渠道投放与内容选题优化建议。', 0
FROM resume r
JOIN sys_user u ON u.id = r.user_id
WHERE u.email = 'gao@example.com'
  AND NOT EXISTS (SELECT 1 FROM resume_project rp WHERE rp.resume_id = r.id AND rp.project_name = '内容增长数据分析项目');

INSERT INTO resume_skill (resume_id, skill_id, skill_name)
SELECT r.id, s.id, s.skill_name
FROM resume r
JOIN sys_user u ON u.id = r.user_id
JOIN skill_dict s ON s.skill_name IN ('Linux', 'Docker', '自动化运维')
WHERE u.email = 'sun@example.com'
  AND NOT EXISTS (SELECT 1 FROM resume_skill rs WHERE rs.resume_id = r.id AND rs.skill_name = s.skill_name);

INSERT INTO resume_skill (resume_id, skill_id, skill_name)
SELECT r.id, s.id, s.skill_name
FROM resume r
JOIN sys_user u ON u.id = r.user_id
JOIN skill_dict s ON s.skill_name IN ('Figma', '交互设计', 'CSS')
WHERE u.email = 'lin@example.com'
  AND NOT EXISTS (SELECT 1 FROM resume_skill rs WHERE rs.resume_id = r.id AND rs.skill_name = s.skill_name);

INSERT INTO resume_skill (resume_id, skill_id, skill_name)
SELECT r.id, s.id, s.skill_name
FROM resume r
JOIN sys_user u ON u.id = r.user_id
JOIN skill_dict s ON s.skill_name IN ('数据运营', 'SQL', '数据可视化')
WHERE u.email = 'gao@example.com'
  AND NOT EXISTS (SELECT 1 FROM resume_skill rs WHERE rs.resume_id = r.id AND rs.skill_name = s.skill_name);

INSERT INTO job_post (
    company_user_id, job_code, title, category, location, salary_min, salary_max,
    experience_requirement, education_requirement, headcount, description, status,
    published_at, expire_at, created_at, updated_at
)
SELECT u.id, 'JOB202604060007', '运维开发工程师', '运维开发', '杭州', 13000, 20000,
       '3年', '本科', 2, '负责服务器部署、发布流程自动化、日志排查和线上环境稳定性保障。', 'PUBLISHED',
       '2026-04-03 09:00:00', '2026-06-30 23:59:59', '2026-04-03 09:00:00', '2026-04-03 09:00:00'
FROM sys_user u
WHERE u.email = 'campus@huaxing.com'
  AND NOT EXISTS (SELECT 1 FROM job_post jp WHERE jp.job_code = 'JOB202604060007');

INSERT INTO job_post (
    company_user_id, job_code, title, category, location, salary_min, salary_max,
    experience_requirement, education_requirement, headcount, description, status,
    published_at, expire_at, created_at, updated_at
)
SELECT u.id, 'JOB202604060008', '界面设计师', '界面设计', '成都', 9000, 15000,
       '2年', '本科', 1, '负责招聘门户、活动专题页和企业后台的界面设计、交互稿输出和视觉规范整理。', 'PUBLISHED',
       '2026-04-03 09:30:00', '2026-06-30 23:59:59', '2026-04-03 09:30:00', '2026-04-03 09:30:00'
FROM sys_user u
WHERE u.email = 'hire@yunsheng.com'
  AND NOT EXISTS (SELECT 1 FROM job_post jp WHERE jp.job_code = 'JOB202604060008');

INSERT INTO job_post (
    company_user_id, job_code, title, category, location, salary_min, salary_max,
    experience_requirement, education_requirement, headcount, description, status,
    published_at, expire_at, created_at, updated_at
)
SELECT u.id, 'JOB202604060009', '数据运营专员', '数据运营', '杭州', 10000, 16000,
       '2年', '本科', 2, '负责活动数据复盘、渠道效果统计、报表输出和用户增长数据跟踪。', 'PUBLISHED',
       '2026-04-03 10:00:00', '2026-06-30 23:59:59', '2026-04-03 10:00:00', '2026-04-03 10:00:00'
FROM sys_user u
WHERE u.email = 'campus@huaxing.com'
  AND NOT EXISTS (SELECT 1 FROM job_post jp WHERE jp.job_code = 'JOB202604060009');

INSERT INTO job_application (
    job_id, company_user_id, jobseeker_user_id, resume_id, status, status_remark,
    applied_at, viewed_at, created_at, updated_at
)
SELECT jp.id, cu.id, ju.id, r.id, 'INTERVIEWING', '已进入面试安排阶段',
       '2026-04-04 10:00:00', '2026-04-04 11:00:00', '2026-04-04 10:00:00', '2026-04-05 09:00:00'
FROM job_post jp
JOIN sys_user cu ON cu.id = jp.company_user_id
JOIN sys_user ju ON ju.email = 'sun@example.com'
JOIN resume r ON r.user_id = ju.id
WHERE jp.job_code = 'JOB202604060007'
  AND NOT EXISTS (SELECT 1 FROM job_application ja WHERE ja.job_id = jp.id AND ja.jobseeker_user_id = ju.id);

INSERT INTO job_application (
    job_id, company_user_id, jobseeker_user_id, resume_id, status, status_remark,
    applied_at, viewed_at, created_at, updated_at
)
SELECT jp.id, cu.id, ju.id, r.id, 'VIEWED', '简历已查看',
       '2026-04-04 10:20:00', '2026-04-04 11:20:00', '2026-04-04 10:20:00', '2026-04-04 11:20:00'
FROM job_post jp
JOIN sys_user cu ON cu.id = jp.company_user_id
JOIN sys_user ju ON ju.email = 'lin@example.com'
JOIN resume r ON r.user_id = ju.id
WHERE jp.job_code = 'JOB202604060008'
  AND NOT EXISTS (SELECT 1 FROM job_application ja WHERE ja.job_id = jp.id AND ja.jobseeker_user_id = ju.id);

INSERT INTO job_application (
    job_id, company_user_id, jobseeker_user_id, resume_id, status, status_remark,
    applied_at, viewed_at, created_at, updated_at
)
SELECT jp.id, cu.id, ju.id, r.id, 'SUBMITTED', '已投递，待企业处理',
       '2026-04-04 10:40:00', NULL, '2026-04-04 10:40:00', '2026-04-04 10:40:00'
FROM job_post jp
JOIN sys_user cu ON cu.id = jp.company_user_id
JOIN sys_user ju ON ju.email = 'gao@example.com'
JOIN resume r ON r.user_id = ju.id
WHERE jp.job_code = 'JOB202604060009'
  AND NOT EXISTS (SELECT 1 FROM job_application ja WHERE ja.job_id = jp.id AND ja.jobseeker_user_id = ju.id);

INSERT INTO application_status_log (application_id, from_status, to_status, operator_user_id, operator_role, remark, created_at)
SELECT ja.id, NULL, 'SUBMITTED', ju.id, 'JOBSEEKER', '求职者提交简历', '2026-04-04 10:00:00'
FROM job_application ja
JOIN sys_user ju ON ju.id = ja.jobseeker_user_id
JOIN job_post jp ON jp.id = ja.job_id
WHERE jp.job_code = 'JOB202604060007'
  AND ju.email = 'sun@example.com'
  AND NOT EXISTS (SELECT 1 FROM application_status_log l WHERE l.application_id = ja.id AND l.created_at = '2026-04-04 10:00:00');

INSERT INTO application_status_log (application_id, from_status, to_status, operator_user_id, operator_role, remark, created_at)
SELECT ja.id, 'SUBMITTED', 'VIEWED', cu.id, 'COMPANY', '企业已查看简历', '2026-04-04 11:00:00'
FROM job_application ja
JOIN sys_user cu ON cu.id = ja.company_user_id
JOIN job_post jp ON jp.id = ja.job_id
WHERE jp.job_code = 'JOB202604060007'
  AND cu.email = 'campus@huaxing.com'
  AND NOT EXISTS (SELECT 1 FROM application_status_log l WHERE l.application_id = ja.id AND l.created_at = '2026-04-04 11:00:00');

INSERT INTO application_status_log (application_id, from_status, to_status, operator_user_id, operator_role, remark, created_at)
SELECT ja.id, 'VIEWED', 'INTERVIEWING', cu.id, 'COMPANY', '安排初试沟通', '2026-04-05 09:00:00'
FROM job_application ja
JOIN sys_user cu ON cu.id = ja.company_user_id
JOIN job_post jp ON jp.id = ja.job_id
WHERE jp.job_code = 'JOB202604060007'
  AND cu.email = 'campus@huaxing.com'
  AND NOT EXISTS (SELECT 1 FROM application_status_log l WHERE l.application_id = ja.id AND l.created_at = '2026-04-05 09:00:00');

INSERT INTO application_status_log (application_id, from_status, to_status, operator_user_id, operator_role, remark, created_at)
SELECT ja.id, NULL, 'SUBMITTED', ju.id, 'JOBSEEKER', '求职者提交简历', '2026-04-04 10:20:00'
FROM job_application ja
JOIN sys_user ju ON ju.id = ja.jobseeker_user_id
JOIN job_post jp ON jp.id = ja.job_id
WHERE jp.job_code = 'JOB202604060008'
  AND ju.email = 'lin@example.com'
  AND NOT EXISTS (SELECT 1 FROM application_status_log l WHERE l.application_id = ja.id AND l.created_at = '2026-04-04 10:20:00');

INSERT INTO application_status_log (application_id, from_status, to_status, operator_user_id, operator_role, remark, created_at)
SELECT ja.id, 'SUBMITTED', 'VIEWED', cu.id, 'COMPANY', '企业已查看简历', '2026-04-04 11:20:00'
FROM job_application ja
JOIN sys_user cu ON cu.id = ja.company_user_id
JOIN job_post jp ON jp.id = ja.job_id
WHERE jp.job_code = 'JOB202604060008'
  AND cu.email = 'hire@yunsheng.com'
  AND NOT EXISTS (SELECT 1 FROM application_status_log l WHERE l.application_id = ja.id AND l.created_at = '2026-04-04 11:20:00');

INSERT INTO application_status_log (application_id, from_status, to_status, operator_user_id, operator_role, remark, created_at)
SELECT ja.id, NULL, 'SUBMITTED', ju.id, 'JOBSEEKER', '求职者提交简历', '2026-04-04 10:40:00'
FROM job_application ja
JOIN sys_user ju ON ju.id = ja.jobseeker_user_id
JOIN job_post jp ON jp.id = ja.job_id
WHERE jp.job_code = 'JOB202604060009'
  AND ju.email = 'gao@example.com'
  AND NOT EXISTS (SELECT 1 FROM application_status_log l WHERE l.application_id = ja.id AND l.created_at = '2026-04-04 10:40:00');

INSERT INTO conversation (company_user_id, jobseeker_user_id, last_message_at, created_at)
SELECT cu.id, ju.id, '2026-04-05 09:10:00', '2026-04-04 11:05:00'
FROM sys_user cu
JOIN sys_user ju ON ju.email = 'sun@example.com'
WHERE cu.email = 'campus@huaxing.com'
  AND NOT EXISTS (SELECT 1 FROM conversation c WHERE c.company_user_id = cu.id AND c.jobseeker_user_id = ju.id);

INSERT INTO conversation (company_user_id, jobseeker_user_id, last_message_at, created_at)
SELECT cu.id, ju.id, '2026-04-04 11:35:00', '2026-04-04 11:20:00'
FROM sys_user cu
JOIN sys_user ju ON ju.email = 'lin@example.com'
WHERE cu.email = 'hire@yunsheng.com'
  AND NOT EXISTS (SELECT 1 FROM conversation c WHERE c.company_user_id = cu.id AND c.jobseeker_user_id = ju.id);

INSERT INTO chat_message (conversation_id, sender_user_id, receiver_user_id, content, read_flag, created_at)
SELECT c.id, cu.id, ju.id, '您好，我们已经查看了您的简历，想约您进行一次线上初试。', 1, '2026-04-04 11:05:00'
FROM conversation c
JOIN sys_user cu ON cu.id = c.company_user_id
JOIN sys_user ju ON ju.id = c.jobseeker_user_id
WHERE cu.email = 'campus@huaxing.com'
  AND ju.email = 'sun@example.com'
  AND NOT EXISTS (SELECT 1 FROM chat_message m WHERE m.conversation_id = c.id AND m.created_at = '2026-04-04 11:05:00');

INSERT INTO chat_message (conversation_id, sender_user_id, receiver_user_id, content, read_flag, created_at)
SELECT c.id, ju.id, cu.id, '可以的，我本周三和周四下午都方便沟通。', 1, '2026-04-04 11:18:00'
FROM conversation c
JOIN sys_user cu ON cu.id = c.company_user_id
JOIN sys_user ju ON ju.id = c.jobseeker_user_id
WHERE cu.email = 'campus@huaxing.com'
  AND ju.email = 'sun@example.com'
  AND NOT EXISTS (SELECT 1 FROM chat_message m WHERE m.conversation_id = c.id AND m.created_at = '2026-04-04 11:18:00');

INSERT INTO chat_message (conversation_id, sender_user_id, receiver_user_id, content, read_flag, created_at)
SELECT c.id, cu.id, ju.id, '好的，我们稍后确认具体时间并发送会议链接。', 0, '2026-04-05 09:10:00'
FROM conversation c
JOIN sys_user cu ON cu.id = c.company_user_id
JOIN sys_user ju ON ju.id = c.jobseeker_user_id
WHERE cu.email = 'campus@huaxing.com'
  AND ju.email = 'sun@example.com'
  AND NOT EXISTS (SELECT 1 FROM chat_message m WHERE m.conversation_id = c.id AND m.created_at = '2026-04-05 09:10:00');

INSERT INTO chat_message (conversation_id, sender_user_id, receiver_user_id, content, read_flag, created_at)
SELECT c.id, cu.id, ju.id, '您好，您的作品风格和我们的岗位比较匹配，方便补充一个完整项目链接吗？', 1, '2026-04-04 11:20:00'
FROM conversation c
JOIN sys_user cu ON cu.id = c.company_user_id
JOIN sys_user ju ON ju.id = c.jobseeker_user_id
WHERE cu.email = 'hire@yunsheng.com'
  AND ju.email = 'lin@example.com'
  AND NOT EXISTS (SELECT 1 FROM chat_message m WHERE m.conversation_id = c.id AND m.created_at = '2026-04-04 11:20:00');

INSERT INTO chat_message (conversation_id, sender_user_id, receiver_user_id, content, read_flag, created_at)
SELECT c.id, ju.id, cu.id, '可以的，我稍后整理成一个作品集文档发给您。', 0, '2026-04-04 11:35:00'
FROM conversation c
JOIN sys_user cu ON cu.id = c.company_user_id
JOIN sys_user ju ON ju.id = c.jobseeker_user_id
WHERE cu.email = 'hire@yunsheng.com'
  AND ju.email = 'lin@example.com'
  AND NOT EXISTS (SELECT 1 FROM chat_message m WHERE m.conversation_id = c.id AND m.created_at = '2026-04-04 11:35:00');

INSERT INTO notification (user_id, type, title, content, read_flag, created_at)
SELECT ju.id, 'APPLICATION_STATUS', '投递状态已更新', '您投递的运维开发工程师岗位已进入面试安排阶段。', 0, '2026-04-05 09:00:00'
FROM sys_user ju
WHERE ju.email = 'sun@example.com'
  AND NOT EXISTS (SELECT 1 FROM notification n WHERE n.user_id = ju.id AND n.created_at = '2026-04-05 09:00:00');

INSERT INTO notification (user_id, type, title, content, read_flag, created_at)
SELECT ju.id, 'APPLICATION_STATUS', '投递状态已更新', '您投递的界面设计师岗位已被企业查看。', 0, '2026-04-04 11:20:00'
FROM sys_user ju
WHERE ju.email = 'lin@example.com'
  AND NOT EXISTS (SELECT 1 FROM notification n WHERE n.user_id = ju.id AND n.created_at = '2026-04-04 11:20:00');

INSERT INTO notification (user_id, type, title, content, read_flag, created_at)
SELECT ju.id, 'NEW_MESSAGE', '收到新的聊天消息', '华星互联HR向您发送了一条新消息。', 0, '2026-04-05 09:10:00'
FROM sys_user ju
WHERE ju.email = 'sun@example.com'
  AND NOT EXISTS (SELECT 1 FROM notification n WHERE n.user_id = ju.id AND n.created_at = '2026-04-05 09:10:00');

INSERT INTO notification (user_id, type, title, content, read_flag, created_at)
SELECT cu.id, 'NEW_APPLICATION', '收到新的简历投递', '岗位 运维开发工程师 收到了一份新的简历。', 0, '2026-04-04 10:00:00'
FROM sys_user cu
WHERE cu.email = 'campus@huaxing.com'
  AND NOT EXISTS (SELECT 1 FROM notification n WHERE n.user_id = cu.id AND n.created_at = '2026-04-04 10:00:00');

INSERT INTO notification (user_id, type, title, content, read_flag, created_at)
SELECT cu.id, 'NEW_APPLICATION', '收到新的简历投递', '岗位 界面设计师 收到了一份新的简历。', 0, '2026-04-04 10:20:00'
FROM sys_user cu
WHERE cu.email = 'hire@yunsheng.com'
  AND NOT EXISTS (SELECT 1 FROM notification n WHERE n.user_id = cu.id AND n.created_at = '2026-04-04 10:20:00');

INSERT INTO notification (user_id, type, title, content, read_flag, created_at)
SELECT cu.id, 'NEW_APPLICATION', '收到新的简历投递', '岗位 数据运营专员 收到了一份新的简历。', 0, '2026-04-04 10:40:00'
FROM sys_user cu
WHERE cu.email = 'campus@huaxing.com'
  AND NOT EXISTS (SELECT 1 FROM notification n WHERE n.user_id = cu.id AND n.created_at = '2026-04-04 10:40:00');

COMMIT;
