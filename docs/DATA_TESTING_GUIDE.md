# Demo 数据测试说明

这份文档用于说明如何使用仓库内的“基础 `seed.sql` + 默认富数据 seeder + `bulk-demo` 超大演示档”，快速完成各角色、各功能路径的联调、回归和手工验收。

## 1. 适用目标

- 验证管理员、教师、学生三端主流程是否可用
- 验证分页、筛选、互评、异常评分治理、排行榜、结果发布等功能
- 验证在“大量课程 / 大量作业 / 大量成员 / 大量评分”条件下页面是否仍然可见、可操作
- 让其他开发者拉代码后，无需依赖某个人本地数据库快照，也能复现同样的数据规模和场景分布

## 2. 三层数据集

项目当前默认包含三层数据能力：

### 2.1 基础 `seed.sql`

用途：

- 提供稳定、可读、便于理解的小规模样例数据
- 适合单接口调试、冒烟测试、开发初期排查

特点：

- 账号固定
- 课程和作业数量少
- 样本场景有限，但更容易肉眼理解

来源：

- [schema.sql](/D:/course-platform-demo-v1/backend/src/main/resources/db/schema.sql)
- [seed.sql](/D:/course-platform-demo-v1/backend/src/main/resources/db/seed.sql)

### 2.2 默认富数据 seeder

用途：

- 提供默认启动即可得到的学生大厅联调数据
- 适合验证 `/student/courses` 学生大厅、项目广场、结果看板、教师评审和分页场景
- 让 `mvn spring-boot:run` 直接具备真实开课中的任务密度、互评热度和结果反馈氛围

默认规模：

- 15 门课程
- 48 个作业
- 每门课约 20 个成员
- 状态分布固定为：
  - `REVIEWING=20`
  - `SUBMITTING=18`
  - `CLOSED=10`
- 模式分布固定为：
  - `GROUP=26`
  - `INDIVIDUAL=22`
- 至少 6 个作业会在未来 72 小时内截止
- 提交总量不低于 380
- 评分总量不低于 2400
- 至少 12 个作业会出现 6 条以上匿名评语

特点：

- 通过代码生成，可重复执行
- 默认会清理上一轮 `bulk_*` 前缀数据后重新生成
- 不会删除基础 `seed.sql` 中的原始账号、课程和作业
- 当前默认配置下，实际启动日志已验证可生成：
  - 421 条提交
  - 2997 条评分

配置见：

- [application.yml](/D:/course-platform-demo-v1/backend/src/main/resources/application.yml)
- [BulkDemoDataProperties.java](/D:/course-platform-demo-v1/backend/src/main/java/com/demo/courseplatform/config/BulkDemoDataProperties.java)
- [BulkDemoDataSeeder.java](/D:/course-platform-demo-v1/backend/src/main/java/com/demo/courseplatform/service/BulkDemoDataSeeder.java)

### 2.3 `bulk-demo` 超大演示档

用途：

- 提供比默认档更高体量的演示数据
- 适合专门验证更重分页、更密集互评、更厚结果墙和教师侧大列表场景

当前规模：

- 18 门课程
- 60 个作业
- 每门课约 20 个成员
- 其余分布规则沿用默认富数据 seeder 的状态、模式、互评和治理逻辑，但整体规模更大

配置见：

- [application-bulk-demo.yml](/D:/course-platform-demo-v1/backend/src/main/resources/application-bulk-demo.yml)
- [BulkDemoDataProperties.java](/D:/course-platform-demo-v1/backend/src/main/java/com/demo/courseplatform/config/BulkDemoDataProperties.java)
- [BulkDemoDataSeeder.java](/D:/course-platform-demo-v1/backend/src/main/java/com/demo/courseplatform/service/BulkDemoDataSeeder.java)

## 3. 启动方式

### 3.1 默认启动：基础数据 + 默认富数据 seeder

```bash
cd backend
mvn spring-boot:run
```

默认启动后建议重点验证：

- 学生登录后默认进入 `/student/courses` 学生大厅
- 大厅的 Hero、今日任务区、互评热区、最近结果区、课程现场区都有内容
- 项目广场和结果看板不再是稀疏样例页，而会出现较明显的评论、排行榜和分页密度

### 3.2 使用 `bulk-demo` 超大演示档

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=bulk-demo
```

建议：

- 日常联调、回归和 UI 验收优先用默认启动
- 只在需要更高体量压力时，再切到 `bulk-demo`

### 3.3 可选：临时关闭富数据 seeder

如果你只想看最小 `seed.sql` 样本，可以临时关闭默认富数据 seeder：

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.arguments=--demo.bulk-seed.enabled=false
```

这个模式只建议用于非常细颗粒度的接口排查，不是当前项目默认体验。

## 4. 账号说明

### 4.1 固定演示账号

- 管理员：`admin / admin`
- 教师：`t001 / t001`、`t002 / t002`、`t003 / t003`
- 学生：`s001 / s001`、`s002 / s002`、`s003 / s003`、`s004 / s004`、`s005 / s005`

注意：

- `s001` 首次登录会被要求修改密码
- 当前 demo 环境中，`sys_user.password_hash` 为了方便查看和联调，实际直接保存明码；字段名未改，只是为了兼容旧表结构

### 4.2 富数据 seeder 生成账号

命名规律：

- 教师：`bulk_t001`、`bulk_t002` ...
- 学生：`bulk_s001`、`bulk_s002` ...

登录规则：

- 默认密码和用户名相同
- 例如 `bulk_t001 / bulk_t001`
- 例如 `bulk_s001 / bulk_s001`

推荐：

- 教师端联调用固定教师账号和 `bulk_t***` 都可以
- 学生端分页、项目广场、排行榜测试更适合使用 `bulk_s***`

## 5. 数据识别规则

为了避免测试时依赖不稳定的数据库主键，建议优先通过下面这些约定识别大数据样本：

- 批量课程编码前缀：`BULK-C`
- 批量作业标题前缀：`BULK-DEMO | `
- 批量项目名关键词：`Bulk Demo | `
- 批量用户名前缀：`bulk_`

建议不要把测试说明写死为某个数据库 ID。
优先按“课程编码 / 作业标题 / 用户名前缀 / 状态”定位样本。

## 6. 功能覆盖矩阵

默认富数据 seeder 与 `bulk-demo` 都覆盖以下场景：

- 学生大厅：`/student/courses` 五个核心区块默认非空
- 课程分页：默认 15 门课程，管理员 / 教师 / 学生三端都能翻页
- 作业模式：个人作业 + 小组作业
- 作业状态：
  - `SUBMITTING`
  - `REVIEWING`
  - 已发布结果的 `CLOSED`
- 提交情况：
  - 已提交
  - 未提交
  - 迟交
  - 小组已组队但未全部提交
  - 个人作业自动单人组
- 项目广场：
  - 可评
  - 已评
  - 不可评
  - 黑名单回避
- 排行榜：
  - 实时榜
  - 最终榜
  - 多页分页
- 异常评分治理：
  - `PENDING`
  - `IGNORED`
  - `RESTORED`
  - 已忽略后影响聚合
  - 已恢复后重新参与聚合
- 教师侧流程：
  - 教师评分
  - 黑名单
  - 异常治理
- 发布最终成绩

## 7. 推荐测试路径

下面是建议的手工测试顺序。按这个顺序走，基本能覆盖当前系统的核心链路。

### 7.1 管理员

登录账号：

- `admin / admin`

建议验证：

- 课程列表是否能分页翻到第 2 页
- 课程总数是否大于 10
- 成员管理弹窗是否能正常打开
- CSV 导入后课程页是否刷新
- 新建课程、新建作业后列表是否还能正常展示

重点页面：

- 管理员课程页 [AdminCoursesView.vue](/D:/course-platform-demo-v1/frontend/src/views/admin/AdminCoursesView.vue)

### 7.2 学生

推荐账号：

- 基础流程：`s002 / s002`
- 大量数据联调：`bulk_s001 / bulk_s001`

建议验证：

- 学生默认首页 `/student/courses` 是否已经呈现学生大厅，而不是旧课程列表
- 学生大厅五个区块是否都非空
- 今日任务区是否能同时看到临近截止、去互评、结果已发布等类型
- 学生课程现场区是否分页
- 进入不同状态作业后，页面是否能正确显示 `SUBMITTING / REVIEWING / 已发布`
- 项目广场是否出现多页
- 项目广场顶部是否显示“本作业项目数 / 可评项目数 / 已评分项目数”
- 项目广场中是否存在：
  - 可评分项目
  - 已评分项目
  - 不可评分项目
- 提交互评后，当前页是否刷新
- 个人结果看板评论墙是否明显增密，多个作业应能看到 6 条以上匿名评语
- 个人结果看板中排行榜是否分页
- 排行榜是否能翻到第 2 页且保持当前作业上下文
- 已发布作业中排行榜是否显示最终榜

重点页面：

- 学生大厅 [StudentCoursesView.vue](/D:/course-platform-demo-v1/frontend/src/views/student/StudentCoursesView.vue)
- 项目广场 [StudentProjectsView.vue](/D:/course-platform-demo-v1/frontend/src/views/student/StudentProjectsView.vue)
- 结果看板 [StudentDashboardView.vue](/D:/course-platform-demo-v1/frontend/src/views/student/StudentDashboardView.vue)

### 7.3 教师

推荐账号：

- 基础流程：`t001 / t001`
- 大量数据联调：`bulk_t001 / bulk_t001`

建议验证：

- 教师课程页是否分页
- 小组作业是否能进入小组管理页
- 教师评审页的提交列表是否分页
- 教师评审页的评分记录是否分页
- 筛选条件变化后，评分记录是否会回到第一页
- 异常评分是否能执行“忽略 / 恢复”
- 黑名单规则添加后，学生端可评项目是否受影响
- 发布最终成绩后，学生端看板是否切到最终结果

重点页面：

- 教师课程页 [TeacherCoursesView.vue](/D:/course-platform-demo-v1/frontend/src/views/teacher/TeacherCoursesView.vue)
- 教师评审页 [TeacherReviewView.vue](/D:/course-platform-demo-v1/frontend/src/views/teacher/TeacherReviewView.vue)

## 8. 适合专项回归的场景

### 8.1 分页回归

建议检查：

- 管理员课程页
- 学生课程页
- 教师课程页
- 学生项目广场
- 学生排行榜
- 教师提交列表
- 教师评分记录列表

重点断言：

- 总数大于每页条数时出现分页器
- 切页后数据变化
- 当前页码同步更新
- 操作后刷新时尽量留在当前页

### 8.2 异常评分治理回归

建议检查：

- 教师评审页筛选 `仅异常`
- 不同 `reviewStatus` 是否能筛出数据
- 执行“忽略”后记录状态和计分状态是否变化
- 执行“恢复”后记录是否重新参与计分

### 8.3 结果发布回归

建议检查：

- 找一个教师端可发布的作业执行发布
- 学生端结果看板是否显示已发布
- 排行榜是否切换为最终榜
- 评分相关按钮是否进入只读状态

### 8.4 黑名单回归

建议检查：

- 教师端对某提交添加黑名单
- 目标学生在学生项目广场中不再看到该项目为可评
- 其他未受影响学生仍可正常互评

## 9. 数据重置与幂等说明

默认富数据 seeder / `bulk-demo` 共有行为：

- 每次启动前清理上一轮 `bulk_*` 数据
- 再重新生成一轮同规模数据
- 基础 `seed.sql` 数据保留

这意味着：

- 不会因为反复启动而把批量数据插到翻倍
- 适合本地反复做联调和回归
- 如果你在 `bulk_*` 数据上手工改了内容，下次重新启动后会被覆盖

## 10. 推荐联调习惯

- 开发接口时先用基础 `seed.sql` 数据看清问题
- 做默认功能联调、UI 验收和学生大厅体验时，优先直接用默认启动
- 需要更高体量时，再切到 `bulk-demo`
- 定位数据时优先通过前缀识别，不依赖主键 ID
- 做分页和筛选测试时，尽量使用 `bulk_t***` / `bulk_s***` 账号

## 11. 常见问题

### 11.1 为什么推到 GitHub 后别人没有我的本地数据库数据

因为 GitHub 保存的是代码和配置，不会保存你本地 MySQL 中的行数据。
这个项目的复用方式是：把造数逻辑提交到仓库，让别人启动后自动生成相同规模的数据。

### 11.2 为什么我重启后 `bulk_*` 数据会重建

如果启用了 `demo.bulk-seed.reset-before-seed=true`，启动时会先删掉旧的 `bulk_*` 数据再重建。
这是为了保证幂等和可重复联调。

### 11.3 为什么测试时建议按前缀找数据

因为数据库自增 ID、插入顺序和不同环境下的本地状态可能不同，而前缀约定更稳定，跨人协作时不容易失效。
