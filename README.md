# 课程项目提交、开放互评与 Rubric 加权评分平台 Demo

这是一个面向课程项目制教学场景的前后端分离单体 demo，当前版本已经使用真实的 `Spring Boot + MyBatis + MySQL + Redis` 主链路，而不是内存态假数据。

已覆盖的核心能力：

- 管理员导入用户、创建课程、创建作业、维护课程成员
- 学生查看课程与作业、提交个人/小组项目、参与互评、查看结果看板与排行榜
- 教师维护 Rubric、查看提交、评分、处理黑名单、查看统计
- 统一成绩引擎：Rubric 权重、学生互评分去最高最低、教师评分聚合、实时分与最终分

## 项目结构

```text
project-root/
├─ frontend/   # Vue 3 + Vite + Vue Router + Pinia + Axios + Element Plus + ECharts
├─ backend/    # Spring Boot + MyBatis + MySQL + Redis
└─ docs/       # 项目说明与接口文档
```

## 技术与运行约定

- API 前缀统一为 `/api/v1`
- 统一返回格式：`{ code, message, data, timestamp }`
- 列表分页格式：`{ list, total, pageNo, pageSize }`
- 时间格式：`yyyy-MM-dd HH:mm:ss`
- 课程与成员关系以 `course_member` 为唯一事实来源，不引入班级维度

## 环境要求

后端运行依赖：

- MySQL：必需
- Redis：必需
- JDK 17
- Maven 3.9+

前端运行依赖：

- Node.js 20+

数据库会在应用启动时通过以下脚本自动初始化基础 demo 数据：

- [schema.sql](D:/course-platform-demo-v1/backend/src/main/resources/db/schema.sql)
- [seed.sql](D:/course-platform-demo-v1/backend/src/main/resources/db/seed.sql)

如果需要更接近真实联调场景的大体量数据，本仓库还提供可重复执行的 `bulk-demo` 造数 profile。它提交到仓库的是“造数逻辑和配置”，不是你本地数据库 dump，因此其他开发者拉代码后也能复用同一批数据。

## 环境变量

### 后端

- `DB_HOST`：MySQL 主机，默认 `localhost`
- `DB_PORT`：MySQL 端口，默认 `3306`
- `DB_NAME`：数据库名，默认 `course_platform_demo`
- `DB_USERNAME`：数据库用户名，默认 `root`
- `DB_PASSWORD`：数据库密码，默认 `1234`
- `REDIS_HOST`：Redis 主机，默认 `localhost`
- `REDIS_PORT`：Redis 端口，默认 `6379`
- `REDIS_PASSWORD`：Redis 密码，默认空
- `SERVER_PORT`：后端端口，默认 `8080`
- `TOKEN_TTL_HOURS`：登录 token 过期小时数，默认 `12`
- `UPLOAD_DIR`：本地上传目录，默认 `uploads`

### 前端

- `VITE_API_BASE_URL`：前端请求后端的基础地址，默认 `http://localhost:8080/api/v1`
- `demo.frontend-origins`：后端允许跨域访问的前端来源，默认 `http://localhost:5173,http://127.0.0.1:5173`

## 演示账号

- 管理员：`admin / admin`
- 教师：`t001 / t001`、`t002 / t002`、`t003 / t003`
- 学生：`s001 / s001`、`s002 / s002`、`s003 / s003`、`s004 / s004`、`s005 / s005`

说明：

- `s001` 首次登录后会被要求修改密码
- `seed.sql` 保留基础演示账号与小规模样例数据
- 更大规模的数据请使用下文的 `bulk-demo` profile 生成

## 启动方式

### 后端

```bash
cd backend
mvn spring-boot:run
```

如果你要生成仓库内可复用的大体量 Demo 数据：

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=bulk-demo
```

`bulk-demo` profile 会在基础 `schema.sql` 和 `seed.sql` 之后，按配置追加生成一批可重复复现的数据，默认特征如下：

- 12 门课程
- 30 个作业
- 每门课约 20 个成员
- 覆盖个人作业、小组作业、未提交、迟交、互评中、已发布、异常评分、黑名单治理等场景

相关配置见：

- [application-bulk-demo.yml](D:/course-platform-demo-v1/backend/src/main/resources/application-bulk-demo.yml)

如需调整规模，可修改以下配置项：

- `demo.bulk-seed.enabled`
- `demo.bulk-seed.reset-before-seed`
- `demo.bulk-seed.random-seed`
- `demo.bulk-seed.course-count`
- `demo.bulk-seed.members-per-course`
- `demo.bulk-seed.assignment-count`

功能测试和数据使用建议见：

- [Demo 数据测试说明](/D:/course-platform-demo-v1/docs/DATA_TESTING_GUIDE.md)

### 前端

```bash
cd frontend
npm install
npm run dev
```

### 一键本地启动

如果你本机已经装好了 `Homebrew + MySQL + Redis + Node.js + Maven + JDK`，可以直接在项目根目录执行：

```bash
chmod +x scripts/start-dev.sh
./scripts/start-dev.sh
```

停止本地开发进程：

```bash
chmod +x scripts/stop-dev.sh
./scripts/stop-dev.sh
```

如果你连 Homebrew MySQL 也想一起停掉：

```bash
./scripts/stop-dev.sh --with-mysql
```

脚本会自动：

- 确保 MySQL 已启动
- 启动本地 Redis
- 检查并补齐前端依赖
- 启动后端 `Spring Boot`
- 启动前端 `Vite`

日志输出位置：

- `.codex-runtime/runlogs/backend.out.log`
- `.codex-runtime/runlogs/backend.err.log`
- `.codex-runtime/runlogs/frontend.out.log`
- `.codex-runtime/runlogs/frontend.err.log`
- `.codex-runtime/runlogs/redis.log`

说明：

- 脚本默认按本机 Homebrew MySQL 的无密码 `root` 用户启动后端，因此会覆盖 `DB_PASSWORD=''`
- 如果 `127.0.0.1:8080` 或 `127.0.0.1:5173` 已有进程监听，脚本会直接复用，不会主动杀进程
- `stop-dev.sh` 默认不会停 MySQL，只会停前端、后端和 Redis；传 `--with-mysql` 才会一并停止 MySQL 服务

默认访问地址：

- 前端：[http://localhost:5173](http://localhost:5173)
- 后端：[http://localhost:8080](http://localhost:8080)

## 当前实现范围

- 登录、首次改密、Redis token 会话
- 管理员课程配置、课程成员管理、CSV 导入用户
- 多课程、多作业入口
- 个人作业 / 小组作业提交
- 开放互评、黑名单回避、教师评分
- 学生结果看板、排行榜、教师统计页
- 统一成绩计算与异常评分提示

## 说明

- 附件上传当前使用本地目录存储，后续可替换为对象存储
- 当前版本仍以最小闭环为目标，没有引入 Spring Security、消息队列或其他重型组件
- 本仓库中的 controller URL 与大部分前端页面结构保持稳定，便于继续增量开发
