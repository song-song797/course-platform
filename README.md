# 课程项目提交、开放互评与 Rubric 加权评分平台 Demo

这是一个面向课程项目制教学场景的前后端分离单体 demo，当前版本已经使用真实的 `Spring Boot + MyBatis + MySQL + Redis` 主链路，而不是内存态假数据。

当前默认体验已经不是“轻量课程列表示例”，而是：

- 学生默认首页继续使用 `/student/courses`，但页面定位已升级为“学生大厅”
- 默认启动即加载“基础 `seed.sql` + 默认富数据 seeder”两层数据
- `bulk-demo` profile 仍保留，但定位已经变成“更大规模的超大演示档”，不再是默认大数据入口

已覆盖的核心能力：

- 管理员导入用户、创建课程、创建作业、维护课程成员
- 学生进入学生大厅、查看课程与作业、提交个人/小组项目、参与互评、查看结果看板与排行榜
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

除此之外，默认启动还会继续执行可重复生成的富数据 seeder，因此 `mvn spring-boot:run` 启动后就会得到适合大厅、项目广场和结果看板联调的默认大体量数据。

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
- `demo.bulk-seed.enabled`：是否启用默认富数据 seeder，默认 `true`
- `demo.bulk-seed.reset-before-seed`：启动前是否清理上一轮 `bulk_*` 数据，默认 `true`
- `demo.bulk-seed.random-seed`：默认富数据随机种子，默认 `20260317`
- `demo.bulk-seed.course-count`：默认课程总量，默认 `15`
- `demo.bulk-seed.members-per-course`：每门课成员量级，默认 `20`
- `demo.bulk-seed.assignment-count`：默认作业总量，默认 `48`
- `demo.bulk-seed.reviewing-assignment-count`：默认互评中作业数，默认 `20`
- `demo.bulk-seed.closed-assignment-count`：默认已发布结果作业数，默认 `10`
- `demo.bulk-seed.group-student-review-count`：小组作业每个项目的学生互评目标数，默认 `10`
- `demo.bulk-seed.individual-student-review-count`：个人作业每个项目的学生互评目标数，默认 `9`

### 前端

- `VITE_API_BASE_URL`：前端请求后端的基础地址，默认 `http://localhost:8080/api/v1`

## 演示账号

- 管理员：`admin / admin`
- 教师：`t001 / t001`、`t002 / t002`、`t003 / t003`
- 学生：`s001 / s001`、`s002 / s002`、`s003 / s003`、`s004 / s004`、`s005 / s005`

说明：

- `s001` 首次登录后会被要求修改密码
- 当前 demo 环境为便于查看和联调，数据库中的 `sys_user.password_hash` 实际按明码存储；列名保留仅为兼容现有代码和表结构
- `seed.sql` 保留基础演示账号、基础课程 ID 和基础样例作业
- 默认启动还会追加生成一批 `bulk_*` 账号与富数据样本
- 如果需要比默认档更大的联调体量，可使用下文的 `bulk-demo` profile

## 启动方式

### 后端

```bash
cd backend
mvn spring-boot:run
```

默认启动行为：

- 保留 `seed.sql` 的固定账号、基础课程和基础样例作业
- 再执行默认富数据 seeder
- 学生登录后直接看到 `/student/courses` 学生大厅，而不是轻量课程列表

默认档当前配置与校验目标：

- 课程数：`15`
- 作业数：`48`
- 状态分布：`REVIEWING=20`、`SUBMITTING=18`、`CLOSED=10`
- 模式分布：`GROUP=26`、`INDIVIDUAL=22`
- 每课成员约：`20`
- 提交总量不低于：`380`
- 评分总量不低于：`2400`
- 至少 `6` 个提交中作业会落在未来 `72` 小时内截止

如果你需要仓库内更高体量的超大演示数据，可以使用 `bulk-demo` profile：

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=bulk-demo
```

`bulk-demo` profile 会在基础 `schema.sql` 和 `seed.sql` 之后，按更高配置追加生成一批可重复复现的数据，当前默认特征如下：

- 18 门课程
- 60 个作业
- 每门课约 20 个成员
- 覆盖学生大厅、项目广场、结果看板、教师评审、异常评分、黑名单治理等高密度联调场景

相关配置见：

- [application.yml](D:/course-platform-demo-v1/backend/src/main/resources/application.yml)
- [application-bulk-demo.yml](D:/course-platform-demo-v1/backend/src/main/resources/application-bulk-demo.yml)
- [BulkDemoDataProperties.java](D:/course-platform-demo-v1/backend/src/main/java/com/demo/courseplatform/config/BulkDemoDataProperties.java)
- [BulkDemoDataSeeder.java](D:/course-platform-demo-v1/backend/src/main/java/com/demo/courseplatform/service/BulkDemoDataSeeder.java)

如需调整规模，可修改以下配置项：

- `demo.bulk-seed.enabled`
- `demo.bulk-seed.reset-before-seed`
- `demo.bulk-seed.random-seed`
- `demo.bulk-seed.course-count`
- `demo.bulk-seed.members-per-course`
- `demo.bulk-seed.assignment-count`
- `demo.bulk-seed.reviewing-assignment-count`
- `demo.bulk-seed.closed-assignment-count`
- `demo.bulk-seed.group-student-review-count`
- `demo.bulk-seed.individual-student-review-count`

功能测试和数据使用建议见：

- [Demo 数据测试说明](/D:/course-platform-demo-v1/docs/DATA_TESTING_GUIDE.md)

### 前端

```bash
cd frontend
npm install
npm run dev
```

默认访问地址：

- 前端：[http://localhost:5173](http://localhost:5173)
- 后端：[http://localhost:8080](http://localhost:8080)

## 当前实现范围

- 登录、首次改密、Redis token 会话
- 管理员课程配置、课程成员管理、CSV 导入用户
- 学生大厅 `/student/courses` 聚合概览、任务队列、互评热区、最近放榜与课程现场
- 多课程、多作业入口
- 个人作业 / 小组作业提交
- 开放互评、黑名单回避、教师评分
- 学生结果看板、排行榜、教师统计页
- 统一成绩计算与异常评分提示

## 说明

- 附件上传当前使用本地目录存储，后续可替换为对象存储
- 当前版本仍以最小闭环为目标，没有引入 Spring Security、消息队列或其他重型组件
- 本仓库中的 controller URL 与大部分前端页面结构保持稳定，便于继续增量开发
- 学生大厅新增聚合接口为 `/api/v1/student/home`，原有 `/student/courses`、`/student/assignments/{id}` 等接口保持兼容
