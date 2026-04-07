# 企业在线招聘与求职管理系统

前后端分离毕业设计项目，覆盖企业注册与审核、岗位管理、简历管理、岗位投递、规则推荐、站内通知与一对一聊天。

## 技术栈

- 前端: Vue 3、TypeScript、Vite、Pinia、Vue Router、Element Plus、STOMP WebSocket
- 后端: Spring Boot 3.5、Spring Security、JWT、MyBatis Plus、MySQL 8、WebSocket
- 测试: JUnit + H2 内存数据库

## 目录结构

- `frontend`: 前端工程
- `backend`: 后端工程

## 启动方式

### 1. 启动数据库

创建数据库:

```sql
CREATE DATABASE recruitment_system DEFAULT CHARACTER SET utf8mb4;
```

默认后端连接信息:

- 数据库地址: `localhost:3306`
- 数据库名: `recruitment_system`
- 用户名: `root`
- 密码: `123456`

如需修改，可通过环境变量覆盖:

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`

### 2. 启动后端

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

后端默认地址: [http://localhost:8080](http://localhost:8080)

### 3. 启动前端

```powershell
cd frontend
npm install
npm run dev
```

前端默认地址: [http://localhost:5173](http://localhost:5173)

## 演示账号

- 管理员: `admin@recruitment.local / Admin123!`
- 企业: `hr@futuretech.com / Company123!`
- 求职者: `alice@example.com / Job123456!`

系统首次启动时会自动初始化角色、技能字典以及上述演示数据。

## 已实现模块

- 用户管理: 企业/求职者注册登录、JWT 鉴权、三角色权限控制
- 岗位管理: 草稿、发布、下线、过期，企业岗位 CRUD，管理员监管
- 简历管理: 多模板、教育经历、工作经历、项目经历、技能标签自动补全
- 投递流程: 简历完整性校验、7 天防重复投递、状态跟踪
- 信息检索: 关键词检索、多条件筛选、规则匹配推荐
- 消息通信: 站内通知、WebSocket 聊天、会话初始化

## 验证结果

- 后端编译: `.\mvnw.cmd -q -DskipTests compile`
- 后端测试: `.\mvnw.cmd -q test`
- 前端构建: `npm run build`
"# zhaopin" 
