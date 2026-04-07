# 企业在线招聘与求职管理系统

一个面向毕业设计场景的前后端分离招聘平台，覆盖企业招聘、求职者简历管理、岗位投递、岗位推荐、站内通知与即时沟通等核心流程。

项目当前已经打通了从注册登录到岗位发布、简历投递、状态跟踪、消息通知、WebSocket 聊天的完整闭环，适合作为毕业设计展示、课程项目答辩材料或进一步扩展的基础工程。

## 项目亮点

- 三角色闭环：支持 `ADMIN`、`COMPANY`、`JOBSEEKER` 三类角色，具备差异化菜单、页面与接口权限控制
- 核心招聘流程完整：企业发布岗位，求职者维护简历并投递，企业查看并推进投递状态
- 推荐与检索能力已落地：支持关键字搜索、多条件筛选、匹配度排序与岗位推荐
- 实时通信能力：包含站内通知和基于 STOMP WebSocket 的一对一聊天
- 前后端分离：Vue 3 + Vite 前端，Spring Boot 3 + MyBatis Plus 后端，便于独立开发和部署

## 系统角色

| 角色 | 主要能力 |
| --- | --- |
| 管理员 | 审核企业、查看系统关键数据、进入后台管理页面 |
| 企业用户 | 注册并完善企业资料、发布岗位、管理岗位状态、查看投递记录、与候选人沟通 |
| 求职者 | 注册账号、编辑简历、搜索岗位、投递岗位、查看申请状态、接收通知与聊天 |

## 技术栈

### 前端

- Vue 3
- TypeScript
- Vite
- Vue Router
- Pinia
- Element Plus
- Axios
- STOMP WebSocket

### 后端

- Java 21
- Spring Boot 3.5
- Spring Security
- JWT
- MyBatis Plus
- MySQL 8
- WebSocket / STOMP

### 测试与构建

- Maven Wrapper
- JUnit + Spring Boot Test
- H2 内存数据库
- `vue-tsc` + Vite Build

## 核心功能

### 1. 用户与权限

- 企业用户注册、登录、资料录入、管理员审核
- 求职者注册、登录、基础档案初始化
- 管理员、企业、求职者三类角色权限隔离
- 基于 JWT 的接口鉴权与前端路由守卫

### 2. 岗位管理

- 企业发布岗位并维护岗位信息
- 支持岗位新增、编辑、删除、查询
- 支持岗位状态流转：`DRAFT`、`PUBLISHED`、`OFFLINE`、`EXPIRED`
- 管理员可进入后台查看岗位与企业数据

### 3. 简历管理

- 在线维护基本信息、教育经历、技能等内容
- 自动计算简历完整度
- 技能标签化管理与词典补全
- 已保留 `templateCode` 扩展位，便于后续增加多模板简历

### 4. 投递与招聘流程

- 投递前校验简历完整性
- 同一岗位 7 天内禁止重复投递
- 投递状态跟踪：已投递、已查看、面试中、已拒绝、已录用
- 企业端与求职者端均可查看申请记录

### 5. 搜索与推荐

- 岗位关键字检索
- 多条件筛选与排序
- 基于技能、岗位类别、城市、薪资、学历、经验的匹配度推荐

### 6. 消息与通信

- 站内通知
- 基于 WebSocket 的实时消息推送
- 企业与求职者一对一聊天
- 已存在投递关系后才能建立会话

## 页面模块

前端当前包含以下主要页面：

- `/login`：登录页
- `/dashboard`：企业/管理员工作台
- `/jobs`：岗位列表与搜索页
- `/resume`：求职者简历页
- `/applications`：投递记录页
- `/admin`：管理员后台页
- `/chat`：一对一聊天页

## 项目结构

```text
.
├─ frontend/                     # Vue 3 前端工程
│  ├─ src/
│  │  ├─ api/                    # Axios 请求封装
│  │  ├─ layouts/                # 布局组件
│  │  ├─ router/                 # 路由配置与权限守卫
│  │  ├─ stores/                 # Pinia 状态管理
│  │  └─ views/                  # 业务页面
│  └─ vite.config.ts             # 开发代理配置
├─ backend/                      # Spring Boot 后端工程
│  ├─ src/main/java/com/bishe/recruitment/
│  │  ├─ config/                 # 安全、MyBatis、WebSocket、初始化数据
│  │  ├─ controller/             # 接口层
│  │  ├─ service/                # 业务层
│  │  ├─ mapper/                 # MyBatis Plus Mapper
│  │  ├─ entity/                 # 实体类
│  │  ├─ dto/                    # 数据传输对象
│  │  └─ security/               # JWT 与鉴权逻辑
│  ├─ src/main/resources/db/     # 数据库脚本
│  └─ src/test/                  # 后端测试
└─ README.md
```

## 运行环境

- Node.js 18+
- npm 9+
- Java 21
- MySQL 8.x

## 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/zhendedontkown/zhaopin.git
cd zhaopin
```

### 2. 创建数据库

执行以下 SQL 创建数据库：

```sql
CREATE DATABASE recruitment_system DEFAULT CHARACTER SET utf8mb4;
```

说明：

- 后端启动时会自动执行 `classpath:db/schema.sql`
- 当前配置为 `spring.sql.init.mode=always`
- 如果脚本重复执行，项目已开启 `continue-on-error: true`

### 3. 启动后端

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

后端默认地址：

- HTTP API: [http://localhost:8080](http://localhost:8080)
- WebSocket: `ws://localhost:8080/ws/chat`、`ws://localhost:8080/ws/notifications`

### 4. 启动前端

```powershell
cd frontend
npm install
npm run dev
```

前端默认地址：

- Web: [http://localhost:5173](http://localhost:5173)

开发代理说明：

- `/api` 会被代理到 `http://localhost:8080`
- `/ws` 会被代理到 `ws://localhost:8080`

## 环境变量

后端支持以下环境变量覆盖默认配置：

| 变量名 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_HOST` | `localhost` | 数据库主机 |
| `DB_PORT` | `3306` | 数据库端口 |
| `DB_NAME` | `recruitment_system` | 数据库名 |
| `DB_USERNAME` | `root` | 数据库用户名 |
| `DB_PASSWORD` | `123456` | 数据库密码 |
| `JWT_SECRET` | `RecruitmentSystemJwtSecretKeyForGraduationProject2026` | JWT 密钥 |
| `JWT_EXPIRATION_MS` | `86400000` | Token 过期时间，单位毫秒 |

## 演示账号

系统首次启动时会自动初始化角色、技能字典和演示数据。

当前代码中的默认演示账号如下：

| 角色 | 账号 | 密码 |
| --- | --- | --- |
| 管理员 | `admin@recruitment.local` | `123456` |
| 企业 | `hr@futuretech.com` | `123456` |
| 求职者 | `alice@example.com` | `123456` |

说明：

- 当前 `DataSeeder` 会把库中现有用户密码同步为统一默认密码 `123456`
- 如果你准备做正式展示，建议把这个初始化逻辑调整为仅初始化缺失账号

## 接口与通信说明

- 前端请求统一走 `/api`
- 后端返回格式统一封装为 `ApiResponse<T>`
- 鉴权方式为 `Authorization: Bearer <token>`
- 聊天与通知通过 STOMP over WebSocket 实现

## 验证命令

### 后端编译

```powershell
cd backend
.\mvnw.cmd -q -DskipTests compile
```

### 后端测试

```powershell
cd backend
.\mvnw.cmd -q test
```

### 前端构建

```powershell
cd frontend
npm install
npm run build
```

## 当前完成度与已知范围

相较于开题报告，当前主流程已经比较完整，但仍有一些可以继续扩展的点：

- 简历模板目前实际仅有一个 `classic`
- 教育经历、工作经历尚未做成更强交互的时间轴组件
- 岗位批量导入导出尚未实现
- 岗位描述编辑目前是普通文本输入，不是富文本编辑器

如果作为毕业设计展示，这些点可以作为“后续优化方向”继续展开。

## 后续可扩展方向

- 增加简历模板中心与 PDF 导出
- 接入对象存储，实现附件简历上传
- 增加岗位批量导入导出
- 引入 Redis 提升会话与通知性能
- 优化岗位推荐算法，加入更多画像特征
- 增加 Docker 部署与 CI/CD 配置

## License

当前仓库未单独声明开源许可证。如需公开分发，建议补充 `LICENSE` 文件。
