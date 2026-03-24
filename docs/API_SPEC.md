# 课程平台 API 接口文档

说明：

- 本文件是接口文档的 Markdown 源版本，便于后续由 AI 或开发者继续维护。
- 同目录下会额外生成一份 PDF 版本，内容与本文件保持一致，更适合人类阅读和传阅。
- 本文档只描述当前项目真实存在的 `/api/v1` 对外接口，不补不存在的接口，也不扩展未实现字段。

## 1. 通用约定

### 1.1 Base URL

- 开发环境默认：`http://localhost:8080/api/v1`

### 1.2 鉴权规则

- 鉴权方式：`Authorization: Bearer <token>`
- 仅 `POST /auth/login` 不需要登录态
- 其余 `/api/v1/**` 接口默认都需要 Bearer Token

角色访问规则：

- `/api/v1/admin/**`：仅 `ADMIN`
- `/api/v1/student/**`：仅 `STUDENT`
- `/api/v1/teacher/**`：仅 `TEACHER`
- `/api/v1/files/**`：任意已登录用户

首次登录限制：

- 如果用户 `firstLoginResetRequired=true`，则登录后只能访问：
  - `GET /auth/me`
  - `POST /auth/change-password`
  - `POST /auth/logout`
- 其他接口会返回 `4030`，提示：`首次登录请先修改密码`

### 1.3 Content-Type

- JSON 接口：`application/json`
- 文件上传接口：`multipart/form-data`

### 1.4 时间格式

- 统一时间字符串格式：`yyyy-MM-dd HH:mm:ss`
- 仅作业创建/更新请求体中的 `deadline` 使用 ISO LocalDateTime 字符串，例如：`2026-03-30T23:59:00`

### 1.5 统一成功响应

所有接口成功时都返回：

```json
{
  "code": 0,
  "message": "ok",
  "data": {},
  "timestamp": "2026-03-17 11:47:23"
}
```

字段说明：

- `code`：业务状态码，成功固定为 `0`
- `message`：成功固定为 `ok`
- `data`：实际业务数据；无返回内容时为 `null`
- `timestamp`：服务端响应时间

### 1.6 统一分页结构

所有分页接口的 `data` 都使用：

```json
{
  "list": [],
  "total": 12,
  "pageNo": 1,
  "pageSize": 10
}
```

字段说明：

- `list`：当前页数据
- `total`：总条数
- `pageNo`：当前页码，从 `1` 开始
- `pageSize`：每页条数，最小会被矫正到 `1`

### 1.7 统一失败响应

```json
{
  "code": 4000,
  "message": "项目名称不能为空",
  "data": null,
  "timestamp": "2026-03-17 11:47:23"
}
```

### 1.8 错误码

- `0`：成功
- `4000`：业务参数错误，通常对应 `IllegalArgumentException`
- `4001`：校验失败，通常对应 `@Valid` 校验不通过
- `4010`：未登录或 token 无效
- `4030`：无权限，或首次登录尚未改密
- `4090`：业务状态冲突，通常对应 `IllegalStateException`

## 2. 公共请求体与返回对象

本节用于减少重复描述。接口章节会直接引用这些结构名称。

### 2.1 公共请求体

#### 2.1.1 `LoginRequest`

- `username`：`string`，必填
- `password`：`string`，必填

#### 2.1.2 `ChangePasswordRequest`

- `newPassword`：`string`，必填

#### 2.1.3 `CreateCourseRequest`

- `code`：`string`，必填，课程编码
- `name`：`string`，必填，课程名称
- `term`：`string`，选填；为空时默认 `2026 春`
- `courseDeadline`：`string`，选填，格式示例：`2026-06-30T23:59:00`

#### 2.1.4 `CourseMemberRequest`

- `userId`：`number`，必填，用户 ID
- `courseRole`：`string`，必填，仅支持 `TEACHER` 或 `STUDENT`

额外约束：

- `courseRole` 必须和用户系统角色一致
- 不能重复加入同一门课

#### 2.1.5 `CreateAssignmentRequest`

- `title`：`string`，必填
- `mode`：`string`，必填，推荐值：`GROUP` / `INDIVIDUAL`
- `description`：`string`，选填；为空时使用默认描述
- `deadline`：`string`，选填，格式示例：`2026-03-30T23:59:00`
- `allowLate`：`boolean`，必填
- `peerWeight`：`number`，必填，`0-100`
- `teacherWeight`：`number`，必填，`0-100`
- `status`：`string`，选填，当前主流程使用 `SUBMITTING` / `REVIEWING` / `CLOSED`

额外约束：

- `peerWeight + teacherWeight` 必须等于 `100`
- `mode` 只有传入 `GROUP` 时才会按小组作业处理，其他值都会归一化为 `INDIVIDUAL`

#### 2.1.6 `UpsertSubmissionRequest`

- `projectName`：`string`，必填
- `repoUrl`：`string`，必填
- `memberUserIds`：`number[]`，选填，当前提交接口保留该字段但服务端不依赖它决定组员
- `videoUrl`：`string`，选填
- `previewUrl`：`string`，选填
- `docUrl`：`string`，选填
- `attachmentUrl`：`string`，选填
- `description`：`string`，选填

额外约束：

- 个人作业的真实成员由服务端自动创建单人组
- 小组作业的真实成员来自教师分组，不由该请求体改组

#### 2.1.7 `UpsertAssignmentGroupRequest`

- `groupName`：`string`，必填
- `memberUserIds`：`number[]`，必填，至少 1 个成员

额外约束：

- 组员必须全部来自当前课程学生名单
- 组员不能已存在于当前作业其他小组
- 若小组已有提交，组名可改，但组员不能改

#### 2.1.8 `UpdateRubricRequest`

- `items`：`RubricItemRequest[]`，必填，不能为空

`RubricItemRequest` 字段：

- `id`：`number`，选填，已有评分项时传原 ID
- `name`：`string`，必填
- `description`：`string`，选填
- `weight`：`number`，必填，`0-100`

额外约束：

- 全部评分项权重和必须等于 `100`

#### 2.1.9 `CreateEvaluationRequest` / `TeacherScoreRequest`

- `itemScores`：`ItemScoreRequest[]`，必填，不能为空
- `overallComment`：`string`，选填

`ItemScoreRequest` 字段：

- `rubricItemId`：`number`，必填
- `score`：`number`，必填，范围 `0.0-10.0`
- `comment`：`string`，选填

额外约束：

- 所有 Rubric 项都必须被覆盖，否则会报 `评分项不完整`
- 服务端会按 Rubric 权重换算总分，结果保留两位小数

### 2.2 公共返回对象

#### 2.2.1 `UserProfileVo`

- `id`：`number`
- `username`：`string`
- `displayName`：`string`
- `role`：`string`，取值可能为 `ADMIN` / `TEACHER` / `STUDENT`
- `firstLoginResetRequired`：`boolean`

#### 2.2.2 `AssignmentSummaryVo`

- `id`：`number`
- `title`：`string`
- `mode`：`string`
- `deadline`：`string`
- `submissionCloseAt`：`string | null`
- `status`：`string`
- `resultsPublished`：`boolean`
- `resultsPublishedAt`：`string | null`
- `displayStatus`：`string`

`displayStatus` 当前真实显示口径：

- `SUBMITTING` -> `提交中`
- `REVIEWING` -> `学生互评与教师评分中`
- 已生成最终成绩 -> `最终成绩已生成`
- 其他状态 -> 原样返回

#### 2.2.3 `CourseCardVo`

- `id`：`number`
- `code`：`string`
- `name`：`string`
- `term`：`string`
- `courseDeadline`：`string | null`
- `roleInCourse`：`string`，可能为 `ADMIN` / `TEACHER` / `STUDENT`
- `assignmentCount`：`number`
- `assignments`：`AssignmentSummaryVo[]`

#### 2.2.4 `CourseMemberVo`

- `userId`：`number`
- `username`：`string`
- `displayName`：`string`
- `courseRole`：`string`

#### 2.2.5 `CourseMemberManageVo`

- `members`：`CourseMemberVo[]`
- `teacherCandidates`：`CourseMemberVo[]`
- `studentCandidates`：`CourseMemberVo[]`

#### 2.2.6 `RubricItemVo`

- `id`：`number`
- `name`：`string`
- `description`：`string | null`
- `weight`：`number`

#### 2.2.7 `MemberVo`

- `id`：`number`
- `name`：`string`
- `username`：`string`

#### 2.2.8 `SubmissionSummaryVo`

- `peerScore`：`number | null`
- `teacherScore`：`number | null`
- `realtimeFinalScore`：`number | null`
- `finalScore`：`number | null`
- `published`：`boolean`
- `publishedAt`：`string | null`
- `currentRank`：`number | null`
- `totalProjects`：`number`
- `dimensionAverages`：`MetricVo[]`
- `radar`：`MetricVo[]`
- `comments`：`CommentVo[]`

#### 2.2.9 `AssignmentGroupSubmissionVo`

- `submissionId`：`number`
- `projectName`：`string`
- `submittedAt`：`string | null`
- `late`：`boolean`

#### 2.2.10 `AssignmentGroupVo`

- `id`：`number`
- `groupName`：`string`
- `members`：`MemberVo[]`
- `submission`：`AssignmentGroupSubmissionVo | null`
- `memberLocked`：`boolean`

#### 2.2.11 `AssignmentGroupManageVo`

- `assignmentId`：`number`
- `assignmentTitle`：`string`
- `assignmentStatus`：`string`
- `displayStatus`：`string`
- `resultsPublished`：`boolean`
- `resultsPublishedAt`：`string | null`
- `groups`：`AssignmentGroupVo[]`
- `ungroupedStudents`：`MemberVo[]`

#### 2.2.12 `AssignmentDetailVo`

- `id`：`number`
- `courseId`：`number`
- `courseName`：`string`
- `title`：`string`
- `mode`：`string`
- `description`：`string`
- `deadline`：`string`
- `submissionCloseAt`：`string | null`
- `allowLate`：`boolean`
- `peerWeight`：`number`
- `teacherWeight`：`number`
- `status`：`string`
- `resultsPublished`：`boolean`
- `resultsPublishedAt`：`string | null`
- `displayStatus`：`string`
- `rubric`：`RubricItemVo[]`
- `studentMembers`：`MemberVo[]`
- `myGroup`：`AssignmentGroupVo | null`
- `ungroupedForGroupAssignment`：`boolean`
- `summary`：`SubmissionSummaryVo | null`

#### 2.2.13 `SubmissionVo`

- `id`：`number`
- `assignmentId`：`number`
- `groupId`：`number`
- `groupName`：`string`
- `projectName`：`string`
- `repoUrl`：`string`
- `members`：`MemberVo[]`
- `videoUrl`：`string | null`
- `previewUrl`：`string | null`
- `docUrl`：`string | null`
- `attachmentUrl`：`string | null`
- `description`：`string | null`
- `submittedAt`：`string`
- `late`：`boolean`

#### 2.2.14 `ProjectCardVo`

- `id`：`number`
- `projectName`：`string`
- `repoUrl`：`string`
- `memberNames`：`string[]`
- `finalScore`：`number`
- `scoreType`：`string`，取值为 `REALTIME` 或 `FINAL`
- `canEvaluate`：`boolean`
- `evaluated`：`boolean`
- `ineligibleReason`：`string | null`，当前可能值为 `SELF` / `ALREADY_EVALUATED` / `BLACKLISTED` / `REVIEW_CLOSED`
- `late`：`boolean`

#### 2.2.15 `EvaluationItemScoreVo`

- `rubricItemId`：`number`
- `rubricItemName`：`string`
- `score`：`number`
- `comment`：`string | null`

#### 2.2.16 `EvaluationRecordVo`

- `id`：`number`
- `submissionId`：`number`
- `projectName`：`string`
- `evaluatorUserId`：`number`
- `evaluatorName`：`string`
- `evaluatorUsername`：`string | null`
- `evaluatorRole`：`string`
- `totalScore`：`number`
- `comment`：`string | null`
- `abnormal`：`boolean`
- `abnormalReason`：`string | null`
- `excluded`：`boolean`
- `reviewStatus`：`string`，当前支持 `PENDING` / `IGNORED` / `RESTORED`
- `itemScores`：`EvaluationItemScoreVo[]`
- `createdAt`：`string`

#### 2.2.17 `LeaderboardItemVo`

- `rank`：`number`
- `projectName`：`string`
- `finalScore`：`number`
- `scoreType`：`string`，取值为 `REALTIME` 或 `FINAL`

#### 2.2.18 `DashboardVo`

- `assignmentTitle`：`string`
- `courseName`：`string`
- `assignmentStatus`：`string`
- `displayStatus`：`string`
- `leaderboardType`：`string`，取值为 `REALTIME` 或 `FINAL`
- `submission`：`SubmissionVo | null`
- `summary`：`SubmissionSummaryVo`
- `leaderboard`：`LeaderboardItemVo[]`

#### 2.2.19 `TeacherStatsVo`

- `courseId`：`number`
- `courseName`：`string`
- `assignmentId`：`number`
- `assignmentTitle`：`string`
- `assignmentStatus`：`string`
- `displayStatus`：`string`
- `resultsPublished`：`boolean`
- `resultsPublishedAt`：`string | null`
- `totalSubmissions`：`number`
- `lateSubmissions`：`number`
- `totalEvaluations`：`number`
- `abnormalEvaluations`：`number`
- `abnormalPendingCount`：`number`
- `abnormalHandledCount`：`number`
- `abnormalIgnoredCount`：`number`
- `abnormalRestoredCount`：`number`
- `completionRate`：`number`
- `scoreDistribution`：`MetricVo[]`
- `dimensionAverages`：`MetricVo[]`
- `leaderboardType`：`string`
- `leaderboard`：`LeaderboardItemVo[]`
- `projectScores`：`ProjectScoreVo[]`
- `reviewProgressByStudent`：`ReviewProgressVo[]`
- `abnormalHints`：`AbnormalHintVo[]`
- `abnormalImpacts`：`AbnormalImpactVo[]`
- `blacklistRules`：`BlacklistRuleVo[]`

#### 2.2.20 其他嵌套对象

`MetricVo`

- `name`：`string`
- `value`：`number`

`CommentVo`

- `authorRole`：`string`
- `content`：`string`

`ProjectScoreVo`

- `submissionId`：`number`
- `projectName`：`string`
- `peerScore`：`number | null`
- `teacherScore`：`number | null`
- `realtimeFinalScore`：`number | null`
- `finalScore`：`number | null`
- `late`：`boolean`

`ReviewProgressVo`

- `userId`：`number`
- `displayName`：`string`
- `completedCount`：`number`
- `totalCount`：`number`
- `completionRate`：`number | null`

`AbnormalHintVo`

- `evaluationId`：`number`
- `projectName`：`string`
- `evaluatorName`：`string`
- `totalScore`：`number`
- `reason`：`string`
- `excluded`：`boolean`

`AbnormalImpactVo`

- `submissionId`：`number`
- `projectName`：`string`
- `currentPeerScore`：`number | null`
- `rawPeerScore`：`number | null`
- `peerScoreDelta`：`number | null`
- `currentFinalScore`：`number | null`
- `rawFinalScore`：`number | null`
- `finalScoreDelta`：`number | null`

`BlacklistRuleVo`

- `id`：`number`
- `evaluatorUserId`：`number`
- `evaluatorName`：`string`
- `targetSubmissionId`：`number`
- `targetProjectName`：`string`
- `createdAt`：`string`

#### 2.2.21 学生大厅聚合对象

`StudentOverviewVo`

- `totalCourses`：`number`
- `totalAssignments`：`number`
- `pendingSubmissionCount`：`number`
- `reviewingAssignmentCount`：`number`
- `publishedResultCount`：`number`
- `availableReviewCount`：`number`

`StudentTaskCardVo`

- `assignmentId`：`number`
- `courseId`：`number`
- `courseName`：`string`
- `assignmentTitle`：`string`
- `mode`：`string`
- `displayStatus`：`string`
- `deadline`：`string | null`
- `submissionCloseAt`：`string | null`
- `taskType`：`string`，当前真实取值为 `GO_SUBMIT` / `GO_REVIEW` / `RESULT_AVAILABLE`
- `actionLabel`：`string`

`StudentReviewHighlightVo`

- `assignmentId`：`number`
- `courseName`：`string`
- `assignmentTitle`：`string`
- `totalProjects`：`number`
- `reviewableProjects`：`number`
- `leaderboardType`：`string`，当前为 `REALTIME` / `FINAL`
- `displayStatus`：`string`

`StudentResultHighlightVo`

- `assignmentId`：`number`
- `courseName`：`string`
- `assignmentTitle`：`string`
- `publishedAt`：`string | null`
- `currentRank`：`number | null`
- `finalScore`：`number | null`

`StudentActivityBannerVo`

- `title`：`string`
- `description`：`string`
- `tone`：`string`，当前为 `warning` / `primary` / `success`

`StudentHomeVo`

- `overview`：`StudentOverviewVo`
- `taskQueue`：`StudentTaskCardVo[]`，最多 12 条
- `reviewHighlights`：`StudentReviewHighlightVo[]`，最多 6 条
- `resultHighlights`：`StudentResultHighlightVo[]`，最多 6 条
- `activityBanners`：`StudentActivityBannerVo[]`，最多 3 条

## 3. 认证接口

### 3.1 登录

**接口名称**：登录  
**请求路径**：`/auth/login`  
**请求方式**：`POST`  
**是否鉴权**：否  
**请求头**：`Content-Type: application/json`

**请求参数**

- Path 参数：无
- Query 参数：无
- Body：`LoginRequest`

**请求示例**

```json
{
  "username": "s002",
  "password": "s002"
}
```

**成功响应 `data` 结构**

- `token`：`string`
- `user`：`UserProfileVo`

**成功响应示例**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "token": "6e8bb1f7-5fc5-4ac8-87a4-9f4b4d9a2e51",
    "user": {
      "id": 4,
      "username": "s002",
      "displayName": "李同学",
      "role": "STUDENT",
      "firstLoginResetRequired": false
    }
  },
  "timestamp": "2026-03-17 11:47:23"
}
```

**典型失败**

```json
{
  "code": 4000,
  "message": "密码错误",
  "data": null,
  "timestamp": "2026-03-17 11:47:23"
}
```

**业务说明**

- token 实际由 Redis 保存并带过期时间
- 账号不存在或密码错误都返回 `4000`

### 3.2 获取当前登录用户

**接口名称**：获取当前登录用户  
**请求路径**：`/auth/me`  
**请求方式**：`GET`  
**是否鉴权**：是

**请求参数**

- Path 参数：无
- Query 参数：无
- Body：无

**成功响应 `data` 结构**

- `UserProfileVo`

**成功响应示例**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 3,
    "username": "s001",
    "displayName": "张同学",
    "role": "STUDENT",
    "firstLoginResetRequired": true
  },
  "timestamp": "2026-03-17 11:47:23"
}
```

### 3.3 修改密码

**接口名称**：修改密码  
**请求路径**：`/auth/change-password`  
**请求方式**：`POST`  
**是否鉴权**：是  
**请求头**：`Content-Type: application/json`

**请求参数**

- Path 参数：无
- Query 参数：无
- Body：`ChangePasswordRequest`

**请求示例**

```json
{
  "newPassword": "new-demo-password"
}
```

**成功响应 `data` 结构**

- `null`

**成功响应示例**

```json
{
  "code": 0,
  "message": "ok",
  "data": null,
  "timestamp": "2026-03-17 11:47:23"
}
```

**典型失败**

```json
{
  "code": 4001,
  "message": "newPassword must not be blank",
  "data": null,
  "timestamp": "2026-03-17 11:47:23"
}
```

**业务说明**

- 修改成功后会把 `firstLoginResetRequired` 置为 `false`

### 3.4 退出登录

**接口名称**：退出登录  
**请求路径**：`/auth/logout`  
**请求方式**：`POST`  
**是否鉴权**：是

**请求参数**

- Path 参数：无
- Query 参数：无
- Body：无

**成功响应 `data` 结构**

- `null`

**成功响应示例**

```json
{
  "code": 0,
  "message": "ok",
  "data": null,
  "timestamp": "2026-03-17 11:47:23"
}
```

## 4. 文件接口

### 4.1 上传文件

**接口名称**：上传文件  
**请求路径**：`/files/upload`  
**请求方式**：`POST`  
**是否鉴权**：是  
**请求头**：`Content-Type: multipart/form-data`

**请求参数**

- FormData 字段：
  - `file`：`file`，必填

**成功响应 `data` 结构**

- `url`：`string`，上传后可访问的相对路径
- `originalName`：`string`，原始文件名

**成功响应示例**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "url": "/uploads/5fd13c13-6323-4f1a-b321-0c0614bcdef1-demo.pdf",
    "originalName": "demo.pdf"
  },
  "timestamp": "2026-03-17 11:47:23"
}
```

**典型失败**

```json
{
  "code": 4000,
  "message": "请选择文件",
  "data": null,
  "timestamp": "2026-03-17 11:47:23"
}
```

## 5. 管理员接口

### 5.1 导入用户 CSV

**接口名称**：导入用户 CSV  
**请求路径**：`/admin/users/import`  
**请求方式**：`POST`  
**是否鉴权**：是  
**适用角色**：`ADMIN`  
**请求头**：`Content-Type: multipart/form-data`

**请求参数**

- FormData 字段：
  - `file`：`file`，必填，CSV 文件

CSV 建议格式：

```csv
username,displayName,role
s1001,测试学生,STUDENT
t1001,测试教师,TEACHER
```

**成功响应 `data` 结构**

- `count`：`number`，本次成功导入数量
- `items`：`object[]`
  - `username`：`string`
  - `displayName`：`string`
  - `role`：`string`

**成功响应示例**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "count": 2,
    "items": [
      {
        "username": "s1001",
        "displayName": "测试学生",
        "role": "STUDENT"
      },
      {
        "username": "t1001",
        "displayName": "测试教师",
        "role": "TEACHER"
      }
    ]
  },
  "timestamp": "2026-03-17 11:47:23"
}
```

**业务说明**

- 重复用户名会被自动跳过，不报错
- 导入用户默认密码等于用户名
- 导入用户默认 `firstLoginResetRequired=true`

### 5.2 获取课程分页列表

**接口名称**：获取课程分页列表  
**请求路径**：`/admin/courses`  
**请求方式**：`GET`  
**是否鉴权**：是  
**适用角色**：`ADMIN`

**请求参数**

- Query 参数：
  - `pageNo`：`number`，选填，默认 `1`
  - `pageSize`：`number`，选填，默认 `10`

**成功响应 `data` 结构**

- `PageResult<CourseCardVo>`

**成功响应示例**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "list": [
      {
        "id": 101,
        "code": "SE2026",
        "name": "软件工程课程设计",
        "term": "2026 春",
        "roleInCourse": "ADMIN",
        "assignmentCount": 2,
        "assignments": [
          {
            "id": 1001,
            "title": "课程项目 Demo",
            "mode": "GROUP",
            "deadline": "2026-03-30 23:59:00",
            "status": "REVIEWING",
            "resultsPublished": false,
            "resultsPublishedAt": null,
            "displayStatus": "互评中"
          }
        ]
      }
    ],
    "total": 12,
    "pageNo": 1,
    "pageSize": 10
  },
  "timestamp": "2026-03-17 11:47:23"
}
```

### 5.3 创建课程

**接口名称**：创建课程  
**请求路径**：`/admin/courses`  
**请求方式**：`POST`  
**是否鉴权**：是  
**适用角色**：`ADMIN`  
**请求头**：`Content-Type: application/json`

**请求参数**

- Body：`CreateCourseRequest`

**请求示例**

```json
{
  "code": "SE2027",
  "name": "软件工程课程设计 2027",
  "term": "2027 春",
  "courseDeadline": "2027-06-30T23:59:00"
}
```

**成功响应 `data` 结构**

- `CourseCardVo`

**成功响应示例**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 201,
    "code": "SE2027",
    "name": "软件工程课程设计 2027",
    "term": "2027 春",
    "courseDeadline": "2027-06-30 23:59:00",
    "roleInCourse": "ADMIN",
    "assignmentCount": 0,
    "assignments": []
  },
  "timestamp": "2026-03-17 11:47:23"
}
```

### 5.4 获取课程成员管理视图

**接口名称**：获取课程成员管理视图  
**请求路径**：`/admin/courses/{courseId}/members`  
**请求方式**：`GET`  
**是否鉴权**：是  
**适用角色**：`ADMIN`

**请求参数**

- Path 参数：
  - `courseId`：`number`，必填，课程 ID

**成功响应 `data` 结构**

- `CourseMemberManageVo`

**成功响应示例**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "members": [
      {
        "userId": 2,
        "username": "t001",
        "displayName": "王老师",
        "courseRole": "TEACHER"
      },
      {
        "userId": 3,
        "username": "s001",
        "displayName": "张同学",
        "courseRole": "STUDENT"
      }
    ],
    "teacherCandidates": [
      {
        "userId": 8,
        "username": "t003",
        "displayName": "周老师",
        "courseRole": "TEACHER"
      }
    ],
    "studentCandidates": [
      {
        "userId": 14,
        "username": "s010",
        "displayName": "何同学",
        "courseRole": "STUDENT"
      }
    ]
  },
  "timestamp": "2026-03-17 11:47:23"
}
```

### 5.5 添加课程成员

**接口名称**：添加课程成员  
**请求路径**：`/admin/courses/{courseId}/members`  
**请求方式**：`POST`  
**是否鉴权**：是  
**适用角色**：`ADMIN`  
**请求头**：`Content-Type: application/json`

**请求参数**

- Path 参数：
  - `courseId`：`number`，必填
- Body：`CourseMemberRequest`

**请求示例**

```json
{
  "userId": 14,
  "courseRole": "STUDENT"
}
```

**成功响应 `data` 结构**

- `CourseMemberVo`

**成功响应示例**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "userId": 14,
    "username": "s010",
    "displayName": "何同学",
    "courseRole": "STUDENT"
  },
  "timestamp": "2026-03-17 11:47:23"
}
```

**典型失败**

```json
{
  "code": 4090,
  "message": "该成员已在当前课程中",
  "data": null,
  "timestamp": "2026-03-17 11:47:23"
}
```

### 5.6 移除课程成员

**接口名称**：移除课程成员  
**请求路径**：`/admin/courses/{courseId}/members/{userId}`  
**请求方式**：`DELETE`  
**是否鉴权**：是  
**适用角色**：`ADMIN`

**请求参数**

- Path 参数：
  - `courseId`：`number`，必填
  - `userId`：`number`，必填

**成功响应 `data` 结构**

- `removed`：`boolean`

**成功响应示例**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "removed": true
  },
  "timestamp": "2026-03-17 11:47:23"
}
```

## 6. 学生接口

### 6.1 获取学生大厅聚合数据

**接口名称**：获取学生大厅聚合数据  
**请求路径**：`/student/home`  
**请求方式**：`GET`  
**是否鉴权**：是  
**适用角色**：`STUDENT`

**请求参数**

- Path 参数：无
- Query 参数：无

**成功响应 `data` 结构**

- `StudentHomeVo`

**成功响应示例**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "overview": {
      "totalCourses": 15,
      "totalAssignments": 48,
      "pendingSubmissionCount": 9,
      "reviewingAssignmentCount": 20,
      "publishedResultCount": 10,
      "availableReviewCount": 136
    },
    "taskQueue": [
      {
        "assignmentId": 1016,
        "courseId": 101,
        "courseName": "软件工程课程设计",
        "assignmentTitle": "BULK-DEMO | 团队协同中台 / 软件工程",
        "mode": "GROUP",
        "displayStatus": "提交中",
        "deadline": "2026-03-18 23:59:00",
        "taskType": "DUE_SOON",
        "actionLabel": "去提交"
      }
    ],
    "reviewHighlights": [
      {
        "assignmentId": 1028,
        "courseName": "课程平台增强实验 8",
        "assignmentTitle": "BULK-DEMO | 任务编排与追踪中心 / 课程平台增强实验 8",
        "totalProjects": 16,
        "reviewableProjects": 9,
        "leaderboardType": "REALTIME",
        "displayStatus": "互评中"
      }
    ],
    "resultHighlights": [
      {
        "assignmentId": 1005,
        "courseName": "数据可视化专题",
        "assignmentTitle": "可视化数据故事",
        "publishedAt": "2026-03-16 18:00:00",
        "currentRank": 2,
        "finalScore": 88.4
      }
    ],
    "activityBanners": [
      {
        "title": "今日优先完成",
        "description": "还有 9 个待提交任务，建议优先处理临近截止的作业。",
        "tone": "warning"
      }
    ]
  },
  "timestamp": "2026-03-17 11:47:23"
}
```

**业务说明**

- 该接口是学生默认大厅首屏的聚合接口，不替代现有课程分页接口
- `activityBanners` 只基于真实课程、作业、提交和评分数据聚合生成，不包含伪活动流
- 当学生尚无任何课程时，会返回各模块空集合和全零概览

### 6.2 获取我的课程分页列表

**接口名称**：获取我的课程分页列表  
**请求路径**：`/student/courses`  
**请求方式**：`GET`  
**是否鉴权**：是  
**适用角色**：`STUDENT`

**请求参数**

- Query 参数：
  - `pageNo`：`number`，选填，默认 `1`
  - `pageSize`：`number`，选填，默认 `10`

**成功响应 `data` 结构**

- `PageResult<CourseCardVo>`

### 6.3 获取作业详情

**接口名称**：获取作业详情  
**请求路径**：`/student/assignments/{assignmentId}`  
**请求方式**：`GET`  
**是否鉴权**：是  
**适用角色**：`STUDENT`

**请求参数**

- Path 参数：
  - `assignmentId`：`number`，必填

**成功响应 `data` 结构**

- `AssignmentDetailVo`

**成功响应示例**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 1001,
    "courseId": 101,
    "courseName": "软件工程课程设计",
    "title": "课程项目 Demo",
    "mode": "GROUP",
    "description": "支持项目提交、开放互评与 Rubric 评分的课程项目",
    "deadline": "2026-03-30 23:59:00",
    "allowLate": true,
    "peerWeight": 40,
    "teacherWeight": 60,
    "status": "REVIEWING",
    "resultsPublished": false,
    "resultsPublishedAt": null,
    "displayStatus": "互评中",
    "rubric": [
      {
        "id": 2001,
        "name": "完成度",
        "description": "功能是否完整",
        "weight": 30
      }
    ],
    "studentMembers": [
      {
        "id": 3,
        "name": "张同学",
        "username": "s001"
      }
    ],
    "myGroup": {
      "id": 3001,
      "groupName": "Campus Pair",
      "members": [
        {
          "id": 3,
          "name": "张同学",
          "username": "s001"
        },
        {
          "id": 4,
          "name": "李同学",
          "username": "s002"
        }
      ],
      "submission": {
        "submissionId": 5001,
        "projectName": "Campus Pair",
        "submittedAt": "2026-03-15 18:00:00",
        "late": false
      },
      "memberLocked": true
    },
    "ungroupedForGroupAssignment": false,
    "summary": {
      "peerScore": 89.5,
      "teacherScore": 87.5,
      "realtimeFinalScore": 88.3,
      "finalScore": null,
      "published": false,
      "publishedAt": null,
      "currentRank": 2,
      "totalProjects": 3,
      "dimensionAverages": [],
      "radar": [],
      "comments": []
    }
  },
  "timestamp": "2026-03-17 11:47:23"
}
```

**业务说明**

- 当前学生必须属于该作业所在课程，否则返回 `4030`
- 若学生尚未提交，`summary` 可能为 `null`
- 若是小组作业但还未被分组，`myGroup=null` 且 `ungroupedForGroupAssignment=true`

### 6.4 获取我的提交

**接口名称**：获取我的提交  
**请求路径**：`/student/assignments/{assignmentId}/my-submission`  
**请求方式**：`GET`  
**是否鉴权**：是  
**适用角色**：`STUDENT`

**请求参数**

- Path 参数：
  - `assignmentId`：`number`，必填

**成功响应 `data` 结构**

- `SubmissionVo`
- 若当前学生还没有提交，则 `data` 为 `null`

**成功响应示例**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 5001,
    "assignmentId": 1001,
    "groupId": 3001,
    "groupName": "Campus Pair",
    "projectName": "Campus Pair",
    "repoUrl": "https://github.com/demo/campus-pair",
    "members": [
      {
        "id": 3,
        "name": "张同学",
        "username": "s001"
      },
      {
        "id": 4,
        "name": "李同学",
        "username": "s002"
      }
    ],
    "videoUrl": "https://www.bilibili.com/video/BV1xx411c7mD",
    "previewUrl": "https://example.com/campus-pair",
    "docUrl": "https://example.com/docs/campus-pair",
    "attachmentUrl": "https://example.com/files/campus-pair.pdf",
    "description": "校园互助结对项目",
    "submittedAt": "2026-03-15 18:00:00",
    "late": false
  },
  "timestamp": "2026-03-17 11:47:23"
}
```

### 6.5 提交项目

**接口名称**：提交项目  
**请求路径**：`/student/assignments/{assignmentId}/submit`  
**请求方式**：`POST`  
**是否鉴权**：是  
**适用角色**：`STUDENT`  
**请求头**：`Content-Type: application/json`

**请求参数**

- Path 参数：
  - `assignmentId`：`number`，必填
- Body：`UpsertSubmissionRequest`

**请求示例**

```json
{
  "projectName": "Campus Pair",
  "repoUrl": "https://github.com/demo/campus-pair",
  "memberUserIds": [3, 4],
  "videoUrl": "https://www.bilibili.com/video/BV1xx411c7mD",
  "previewUrl": "https://example.com/campus-pair",
  "docUrl": "https://example.com/docs/campus-pair",
  "attachmentUrl": "https://example.com/files/campus-pair.pdf",
  "description": "校园互助结对项目"
}
```

**成功响应 `data` 结构**

- `SubmissionVo`

**典型失败**

```json
{
  "code": 4090,
  "message": "你还未被教师分配到小组，暂时不能提交项目",
  "data": null,
  "timestamp": "2026-03-17 11:47:23"
}
```

**业务说明**

- 仅 `status=SUBMITTING` 时允许提交
- 超过截止时间且 `allowLate=false` 时不允许补交
- 小组作业必须先由教师完成分组
- 个人作业会自动创建个人组

### 6.6 更新我的提交

**接口名称**：更新我的提交  
**请求路径**：`/student/submissions/{submissionId}`  
**请求方式**：`PUT`  
**是否鉴权**：是  
**适用角色**：`STUDENT`  
**请求头**：`Content-Type: application/json`

**请求参数**

- Path 参数：
  - `submissionId`：`number`，必填
- Body：`UpsertSubmissionRequest`

**业务说明**

- 只有提交成员本人才能修改
- 其他约束与“提交项目”一致

### 6.7 获取项目广场分页列表

**接口名称**：获取项目广场分页列表  
**请求路径**：`/student/assignments/{assignmentId}/projects`  
**请求方式**：`GET`  
**是否鉴权**：是  
**适用角色**：`STUDENT`

**请求参数**

- Path 参数：
  - `assignmentId`：`number`，必填
- Query 参数：
  - `pageNo`：`number`，选填，默认 `1`
  - `pageSize`：`number`，选填，默认 `10`

**成功响应 `data` 结构**

- `PageResult<ProjectCardVo>`

**成功响应示例**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "list": [
      {
        "id": 5002,
        "projectName": "Sprint Board",
        "repoUrl": "https://github.com/demo/sprint-board",
        "memberNames": ["陈同学"],
        "finalScore": 89.6,
        "scoreType": "REALTIME",
        "canEvaluate": true,
        "evaluated": false,
        "late": false
      }
    ],
    "total": 18,
    "pageNo": 1,
    "pageSize": 10
  },
  "timestamp": "2026-03-17 11:47:23"
}
```

**业务说明**

- `canEvaluate=true` 的条件是：
  - 当前作业处于 `REVIEWING`
  - 不是自己的项目
  - 还没有评过
  - 不在黑名单中

### 6.8 提交学生互评

**接口名称**：提交学生互评  
**请求路径**：`/student/projects/{submissionId}/evaluations`  
**请求方式**：`POST`  
**是否鉴权**：是  
**适用角色**：`STUDENT`  
**请求头**：`Content-Type: application/json`

**请求参数**

- Path 参数：
  - `submissionId`：`number`，必填
- Body：`CreateEvaluationRequest`

**请求示例**

```json
{
  "itemScores": [
    {
      "rubricItemId": 2001,
      "score": 8.5,
      "comment": "功能比较完整"
    },
    {
      "rubricItemId": 2002,
      "score": 8.0,
      "comment": "代码结构清晰"
    }
  ],
  "overallComment": "演示流畅，建议继续打磨边界处理"
}
```

**成功响应 `data` 结构**

- `saved`：`boolean`
- `evaluationId`：`number`
- `totalScore`：`number`

**成功响应示例**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "saved": true,
    "evaluationId": 9001,
    "totalScore": 84.5
  },
  "timestamp": "2026-03-17 11:47:23"
}
```

**典型失败**

```json
{
  "code": 4090,
  "message": "同一用户对同一项目只能评分一次",
  "data": null,
  "timestamp": "2026-03-17 11:47:23"
}
```

**业务说明**

- 仅 `REVIEWING` 阶段允许学生互评
- 不能评价自己或自己所在小组
- 黑名单命中的项目不可评价

### 6.9 获取我的结果看板

**接口名称**：获取我的结果看板  
**请求路径**：`/student/assignments/{assignmentId}/my-dashboard`  
**请求方式**：`GET`  
**是否鉴权**：是  
**适用角色**：`STUDENT`

**请求参数**

- Path 参数：
  - `assignmentId`：`number`，必填

**成功响应 `data` 结构**

- `DashboardVo`

**成功响应示例**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "assignmentTitle": "课程项目 Demo",
    "courseName": "软件工程课程设计",
    "assignmentStatus": "REVIEWING",
    "displayStatus": "互评中",
    "leaderboardType": "REALTIME",
    "submission": {
      "id": 5001,
      "assignmentId": 1001,
      "groupId": 3001,
      "groupName": "Campus Pair",
      "projectName": "Campus Pair",
      "repoUrl": "https://github.com/demo/campus-pair",
      "members": [
        {
          "id": 3,
          "name": "张同学",
          "username": "s001"
        }
      ],
      "videoUrl": "https://www.bilibili.com/video/BV1xx411c7mD",
      "previewUrl": "https://example.com/campus-pair",
      "docUrl": "https://example.com/docs/campus-pair",
      "attachmentUrl": "https://example.com/files/campus-pair.pdf",
      "description": "校园互助结对项目",
      "submittedAt": "2026-03-15 18:00:00",
      "late": false
    },
    "summary": {
      "peerScore": 89.5,
      "teacherScore": 87.5,
      "realtimeFinalScore": 88.3,
      "finalScore": null,
      "published": false,
      "publishedAt": null,
      "currentRank": 2,
      "totalProjects": 3,
      "dimensionAverages": [],
      "radar": [],
      "comments": []
    },
    "leaderboard": [
      {
        "rank": 1,
        "projectName": "Sprint Board",
        "finalScore": 90.7,
        "scoreType": "REALTIME"
      }
    ]
  },
  "timestamp": "2026-03-17 11:47:23"
}
```

**业务说明**

- 该接口里的 `leaderboard` 是完整榜单，不分页
- 若学生尚未提交，`submission` 可能为 `null`

### 6.10 获取排行榜分页列表

**接口名称**：获取排行榜分页列表  
**请求路径**：`/student/assignments/{assignmentId}/leaderboard`  
**请求方式**：`GET`  
**是否鉴权**：是  
**适用角色**：`STUDENT`

**请求参数**

- Path 参数：
  - `assignmentId`：`number`，必填
- Query 参数：
  - `pageNo`：`number`，选填，默认 `1`
  - `pageSize`：`number`，选填，默认 `10`

**成功响应 `data` 结构**

- `PageResult<LeaderboardItemVo>`

`list` 内字段：

- `rank`：`number`
- `projectName`：`string`
- `finalScore`：`number | null`
- `scoreType`：`string`，当前可能为 `REALTIME` 或 `FINAL`

**请求示例**

```http
GET /api/v1/student/assignments/1001/leaderboard?pageNo=1&pageSize=2
Authorization: Bearer <student-token>
```

**成功响应示例**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "list": [
      {
        "rank": 1,
        "projectName": "Sprint Board",
        "finalScore": 90.7,
        "scoreType": "REALTIME"
      },
      {
        "rank": 2,
        "projectName": "Campus Pair",
        "finalScore": 86.4,
        "scoreType": "REALTIME"
      }
    ],
    "total": 3,
    "pageNo": 1,
    "pageSize": 2
  },
  "timestamp": "2026-03-17 11:47:23"
}
```

**业务说明**

- 排行榜是独立分页接口，适合大数据量场景
- `scoreType` 与当前成绩快照一致；最终成绩自动生成前通常显示 `REALTIME`

## 7. 教师接口

### 7.1 获取我的课程分页列表

**接口名称**：获取我的课程分页列表  
**请求路径**：`/teacher/courses`  
**请求方式**：`GET`  
**是否鉴权**：是  
**适用角色**：`TEACHER`

**请求参数**

- Query 参数：
  - `pageNo`：`number`，选填，默认 `1`
  - `pageSize`：`number`，选填，默认 `10`

**成功响应 `data` 结构**

- `PageResult<CourseCardVo>`

**请求示例**

```http
GET /api/v1/teacher/courses?pageNo=1&pageSize=10
Authorization: Bearer <teacher-token>
```

**成功响应示例**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "list": [
      {
        "id": 101,
        "code": "SE2026",
        "name": "软件工程课程设计",
        "term": "2026 春",
        "roleInCourse": "TEACHER",
        "assignmentCount": 2,
        "assignments": [
          {
            "id": 1001,
            "title": "课程项目 Demo",
            "mode": "GROUP",
            "deadline": "2026-03-30 23:59:00",
            "status": "REVIEWING",
            "resultsPublished": false,
            "resultsPublishedAt": null,
            "displayStatus": "互评中"
          }
        ]
      }
    ],
    "total": 2,
    "pageNo": 1,
    "pageSize": 10
  },
  "timestamp": "2026-03-17 11:47:23"
}
```

### 7.2 获取教师视角作业详情

**接口名称**：获取教师视角作业详情  
**请求路径**：`/teacher/assignments/{assignmentId}`  
**请求方式**：`GET`  
**是否鉴权**：是  
**适用角色**：`TEACHER`

**请求参数**

- Path 参数：
  - `assignmentId`：`number`，必填

**成功响应 `data` 结构**

- `AssignmentDetailVo`

**业务说明**

- 教师视角返回相同结构，但 `summary` 通常为 `null`
- 教师视角下 `myGroup` 固定为 `null`

**请求示例**

```http
GET /api/v1/teacher/assignments/1001
Authorization: Bearer <teacher-token>
```

**成功响应示例**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 1001,
    "courseId": 101,
    "courseName": "软件工程课程设计",
    "title": "课程项目 Demo",
    "mode": "GROUP",
    "description": "支持项目提交、开放互评与 Rubric 评分的课程项目",
    "deadline": "2026-03-30 23:59:00",
    "allowLate": true,
    "peerWeight": 40,
    "teacherWeight": 60,
    "status": "REVIEWING",
    "resultsPublished": false,
    "resultsPublishedAt": null,
    "displayStatus": "互评中",
    "rubric": [
      {
        "id": 2001,
        "name": "完成度",
        "description": "功能是否完整",
        "weight": 30
      }
    ],
    "studentMembers": [
      {
        "id": 3,
        "name": "张同学",
        "username": "s001"
      }
    ],
    "myGroup": null,
    "ungroupedForGroupAssignment": false,
    "summary": null
  },
  "timestamp": "2026-03-17 11:47:23"
}
```

### 7.3 获取小组管理视图

**接口名称**：获取小组管理视图  
**请求路径**：`/teacher/assignments/{assignmentId}/groups`  
**请求方式**：`GET`  
**是否鉴权**：是  
**适用角色**：`TEACHER`

**请求参数**

- Path 参数：
  - `assignmentId`：`number`，必填

**成功响应 `data` 结构**

- `AssignmentGroupManageVo`

**业务说明**

- 仅小组作业支持该接口
- 个人作业会返回 `4090`：`个人作业不支持教师小组管理`

**请求示例**

```http
GET /api/v1/teacher/assignments/1001/groups
Authorization: Bearer <teacher-token>
```

**成功响应示例**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "assignmentId": 1001,
    "assignmentTitle": "课程项目 Demo",
    "assignmentStatus": "REVIEWING",
    "displayStatus": "互评中",
    "resultsPublished": false,
    "resultsPublishedAt": null,
    "groups": [
      {
        "id": 3001,
        "groupName": "Campus Pair",
        "members": [
          {
            "id": 3,
            "name": "张同学",
            "username": "s001"
          },
          {
            "id": 4,
            "name": "李同学",
            "username": "s002"
          }
        ],
        "submission": {
          "submissionId": 5001,
          "projectName": "Campus Pair",
          "submittedAt": "2026-03-15 18:00:00",
          "late": false
        },
        "memberLocked": true
      }
    ],
    "ungroupedStudents": [
      {
        "id": 10,
        "name": "郑同学",
        "username": "s006"
      }
    ]
  },
  "timestamp": "2026-03-17 11:47:23"
}
```

### 7.4 创建小组

**接口名称**：创建小组  
**请求路径**：`/teacher/assignments/{assignmentId}/groups`  
**请求方式**：`POST`  
**是否鉴权**：是  
**适用角色**：`TEACHER`  
**请求头**：`Content-Type: application/json`

**请求参数**

- Path 参数：
  - `assignmentId`：`number`，必填
- Body：`UpsertAssignmentGroupRequest`

**请求示例**

```json
{
  "groupName": "Campus Pair",
  "memberUserIds": [3, 4]
}
```

**成功响应 `data` 结构**

- `AssignmentGroupVo`

**业务说明**

- 最终成绩已发布后不可再创建小组
- 如果成员已被其他小组占用，会返回 `4090`

**成功响应示例**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 3012,
    "groupName": "Campus Pair",
    "members": [
      {
        "id": 3,
        "name": "张同学",
        "username": "s001"
      },
      {
        "id": 4,
        "name": "李同学",
        "username": "s002"
      }
    ],
    "submission": null,
    "memberLocked": false
  },
  "timestamp": "2026-03-17 11:47:23"
}
```

### 7.5 更新小组

**接口名称**：更新小组  
**请求路径**：`/teacher/assignments/{assignmentId}/groups/{groupId}`  
**请求方式**：`PUT`  
**是否鉴权**：是  
**适用角色**：`TEACHER`  
**请求头**：`Content-Type: application/json`

**请求参数**

- Path 参数：
  - `assignmentId`：`number`，必填
  - `groupId`：`number`，必填
- Body：`UpsertAssignmentGroupRequest`

**请求示例**

```json
{
  "groupName": "Campus Pair Pro",
  "memberUserIds": [3, 4]
}
```

**成功响应 `data` 结构**

- `AssignmentGroupVo`

**成功响应示例**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 3001,
    "groupName": "Campus Pair Pro",
    "members": [
      {
        "id": 3,
        "name": "张同学",
        "username": "s001"
      },
      {
        "id": 4,
        "name": "李同学",
        "username": "s002"
      }
    ],
    "submission": {
      "submissionId": 5001,
      "projectName": "Campus Pair",
      "submittedAt": "2026-03-15 18:00:00",
      "late": false
    },
    "memberLocked": true
  },
  "timestamp": "2026-03-17 11:47:23"
}
```

**业务说明**

- 若小组已产生提交或评分，则只能改 `groupName`，不能改成员
- 当传入成员集合与现有锁定小组成员不一致时，返回 `4090`

### 7.6 删除小组

**接口名称**：删除小组  
**请求路径**：`/teacher/assignments/{assignmentId}/groups/{groupId}`  
**请求方式**：`DELETE`  
**是否鉴权**：是  
**适用角色**：`TEACHER`

**请求参数**

- Path 参数：
  - `assignmentId`：`number`，必填
  - `groupId`：`number`，必填

**请求示例**

```http
DELETE /api/v1/teacher/assignments/1001/groups/3012
Authorization: Bearer <teacher-token>
```

**成功响应 `data` 结构**

```json
{
  "removed": true
}
```

**失败响应示例**

```json
{
  "code": 4090,
  "message": "该小组已有提交或评分，不能删除",
  "data": null,
  "timestamp": "2026-03-17 11:47:23"
}
```

### 7.7 更新 Rubric

**接口名称**：更新 Rubric  
**请求路径**：`/teacher/assignments/{assignmentId}/rubric`  
**请求方式**：`PUT`  
**是否鉴权**：是  
**适用角色**：`TEACHER`  
**请求头**：`Content-Type: application/json`

**请求参数**

- Path 参数：
  - `assignmentId`：`number`，必填
- Body：`UpdateRubricRequest`

**请求示例**

```json
{
  "items": [
    {
      "id": 2001,
      "name": "完成度",
      "description": "功能是否完整",
      "weight": 30
    },
    {
      "id": 2002,
      "name": "工程质量",
      "description": "代码结构与协作规范",
      "weight": 30
    },
    {
      "id": 2003,
      "name": "创新性",
      "description": "设计亮点与创意",
      "weight": 20
    },
    {
      "id": 2004,
      "name": "展示表达",
      "description": "说明文档和演示效果",
      "weight": 20
    }
  ]
}
```

**成功响应 `data` 结构**

- `PageResult<RubricItemVo>`
- 当前实现固定返回 `pageNo=1`、`pageSize=20`

**成功响应示例**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "list": [
      {
        "id": 2001,
        "name": "完成度",
        "description": "功能是否完整",
        "weight": 30
      },
      {
        "id": 2002,
        "name": "工程质量",
        "description": "代码结构与协作规范",
        "weight": 30
      }
    ],
    "total": 4,
    "pageNo": 1,
    "pageSize": 20
  },
  "timestamp": "2026-03-17 11:47:23"
}
```

**业务说明**

- 最终成绩发布后不可修改 Rubric
- 权重和不为 `100` 会返回 `4000`

### 7.8 获取提交分页列表

**接口名称**：获取提交分页列表  
**请求路径**：`/teacher/assignments/{assignmentId}/submissions`  
**请求方式**：`GET`  
**是否鉴权**：是  
**适用角色**：`TEACHER`

**请求参数**

- Path 参数：
  - `assignmentId`：`number`，必填
- Query 参数：
  - `pageNo`：`number`，选填，默认 `1`
  - `pageSize`：`number`，选填，默认 `10`

**成功响应 `data` 结构**

- `PageResult<SubmissionVo>`

**请求示例**

```http
GET /api/v1/teacher/assignments/1001/submissions?pageNo=1&pageSize=2
Authorization: Bearer <teacher-token>
```

**成功响应示例**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "list": [
      {
        "id": 5001,
        "assignmentId": 1001,
        "groupId": 3001,
        "groupName": "Campus Pair",
        "projectName": "Campus Pair",
        "repoUrl": "https://github.com/demo/campus-pair",
        "members": [
          {
            "id": 3,
            "name": "张同学",
            "username": "s001"
          }
        ],
        "videoUrl": "https://www.bilibili.com/video/BV1xx411c7mD",
        "previewUrl": "https://example.com/campus-pair",
        "docUrl": "https://example.com/docs/campus-pair",
        "attachmentUrl": "https://example.com/files/campus-pair.pdf",
        "description": "校园互助结对项目",
        "submittedAt": "2026-03-15 18:00:00",
        "late": false
      }
    ],
    "total": 3,
    "pageNo": 1,
    "pageSize": 2
  },
  "timestamp": "2026-03-17 11:47:23"
}
```

### 7.9 提交教师评分

**接口名称**：提交教师评分  
**请求路径**：`/teacher/submissions/{submissionId}/scores`  
**请求方式**：`POST`  
**是否鉴权**：是  
**适用角色**：`TEACHER`  
**请求头**：`Content-Type: application/json`

**请求参数**

- Path 参数：
  - `submissionId`：`number`，必填
- Body：`TeacherScoreRequest`

**请求示例**

```json
{
  "itemScores": [
    {
      "rubricItemId": 2001,
      "score": 9.2,
      "comment": "功能覆盖完整"
    },
    {
      "rubricItemId": 2002,
      "score": 9.0,
      "comment": "工程结构清晰"
    },
    {
      "rubricItemId": 2003,
      "score": 8.8,
      "comment": "有一定亮点"
    },
    {
      "rubricItemId": 2004,
      "score": 9.1,
      "comment": "表达顺畅"
    }
  ],
  "overallComment": "整体完成度较高。"
}
```

**成功响应 `data` 结构**

```json
{
  "saved": true,
  "evaluationId": 8001,
  "totalScore": 90.25
}
```

**业务说明**

- 教师重复提交评分时，会覆盖自己之前的教师评分记录
- `SUBMITTING` 状态下不能打教师分
- 最终成绩发布后不能再改教师评分

### 7.10 获取评分记录分页列表

**接口名称**：获取评分记录分页列表  
**请求路径**：`/teacher/assignments/{assignmentId}/evaluations`  
**请求方式**：`GET`  
**是否鉴权**：是  
**适用角色**：`TEACHER`

**请求参数**

- Path 参数：
  - `assignmentId`：`number`，必填
- Query 参数：
  - `submissionId`：`number`，选填
  - `evaluatorUserId`：`number`，选填
  - `reviewStatus`：`string`，选填，仅支持 `PENDING` / `IGNORED` / `RESTORED`
  - `abnormalOnly`：`boolean`，选填，默认 `false`
  - `pageNo`：`number`，选填，默认 `1`
  - `pageSize`：`number`，选填，默认 `10`

**成功响应 `data` 结构**

- `PageResult<EvaluationRecordVo>`

`list` 内关键字段：

- `evaluatorRole`：`TEACHER` 或 `STUDENT`
- `abnormal`：系统检测是否异常
- `excluded`：是否已排除出聚合计算
- `reviewStatus`：`PENDING` / `IGNORED` / `RESTORED`

**请求示例**

```http
GET /api/v1/teacher/assignments/1001/evaluations?abnormalOnly=true&pageNo=1&pageSize=5
Authorization: Bearer <teacher-token>
```

**成功响应示例**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "list": [
      {
        "id": 8007,
        "submissionId": 5002,
        "projectName": "Sprint Board",
        "evaluatorUserId": 6,
        "evaluatorName": "赵同学",
        "evaluatorRole": "STUDENT",
        "totalScore": 74.0,
        "comment": "展示略显仓促。",
        "abnormal": true,
        "abnormalReason": "与其他学生评分均值偏差 16.5 分",
        "excluded": false,
        "reviewStatus": "PENDING",
        "itemScores": [],
        "createdAt": "2026-03-16 13:05:00"
      }
    ],
    "total": 2,
    "pageNo": 1,
    "pageSize": 5
  },
  "timestamp": "2026-03-17 11:47:23"
}
```

**失败响应示例**

```json
{
  "code": 4000,
  "message": "reviewStatus 只支持 PENDING / IGNORED / RESTORED",
  "data": null,
  "timestamp": "2026-03-17 11:47:23"
}
```

### 7.11 审核评分有效性

**接口名称**：审核评分有效性  
**请求路径**：`/teacher/evaluations/{evaluationId}/review`  
**请求方式**：`PATCH`  
**是否鉴权**：是  
**适用角色**：`TEACHER`

**请求参数**

- Path 参数：
  - `evaluationId`：`number`，必填
- Query 参数：
  - `excluded`：`boolean`，选填，默认 `true`

**请求示例**

```http
PATCH /api/v1/teacher/evaluations/8007/review?excluded=true
Authorization: Bearer <teacher-token>
```

**成功响应 `data` 结构**

```json
{
  "reviewed": true,
  "excluded": true,
  "reviewStatus": "IGNORED"
}
```

**业务说明**

- `excluded=true` 表示忽略该评分；`excluded=false` 表示恢复该评分
- 学生评分被审核后，系统会重新计算该项目的异常状态与统计影响
- 最终成绩发布后不能再调整评分有效性

### 7.12 添加黑名单规则

**接口名称**：添加黑名单规则  
**请求路径**：`/teacher/assignments/{assignmentId}/blacklist`  
**请求方式**：`POST`  
**是否鉴权**：是  
**适用角色**：`TEACHER`

**请求参数**

- Path 参数：
  - `assignmentId`：`number`，必填
- Query 参数：
  - `evaluatorUserId`：`number`，必填，学生用户 ID
  - `targetSubmissionId`：`number`，必填，目标项目提交 ID

**请求示例**

```http
POST /api/v1/teacher/assignments/1001/blacklist?evaluatorUserId=10&targetSubmissionId=5001
Authorization: Bearer <teacher-token>
```

**成功响应 `data` 结构**

```json
{
  "saved": true
}
```

**业务说明**

- 同一条黑名单重复添加时，接口仍返回成功，便于幂等调用
- `evaluatorUserId` 必须是当前课程学生
- `targetSubmissionId` 必须属于当前作业

### 7.13 删除黑名单规则

**接口名称**：删除黑名单规则  
**请求路径**：`/teacher/assignments/{assignmentId}/blacklist`  
**请求方式**：`DELETE`  
**是否鉴权**：是  
**适用角色**：`TEACHER`

**请求参数**

- Path 参数：
  - `assignmentId`：`number`，必填
- Query 参数：
  - `evaluatorUserId`：`number`，必填
  - `targetSubmissionId`：`number`，必填

**请求示例**

```http
DELETE /api/v1/teacher/assignments/1001/blacklist?evaluatorUserId=10&targetSubmissionId=5001
Authorization: Bearer <teacher-token>
```

**成功响应 `data` 结构**

```json
{
  "removed": true
}
```

### 7.14 获取教师统计页数据

**接口名称**：获取教师统计页数据  
**请求路径**：`/teacher/assignments/{assignmentId}/stats`  
**请求方式**：`GET`  
**是否鉴权**：是  
**适用角色**：`TEACHER`

**请求参数**

- Path 参数：
  - `assignmentId`：`number`，必填

**成功响应 `data` 结构**

- `TeacherStatsVo`
- 关键字段详见 [第 2.2 节](#22-公共返回对象) 中的 `TeacherStatsVo`

**请求示例**

```http
GET /api/v1/teacher/assignments/1001/stats
Authorization: Bearer <teacher-token>
```

**成功响应示例**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "courseId": 101,
    "courseName": "软件工程课程设计",
    "assignmentId": 1001,
    "assignmentTitle": "课程项目 Demo",
    "assignmentStatus": "REVIEWING",
    "displayStatus": "互评中",
    "resultsPublished": false,
    "resultsPublishedAt": null,
    "totalSubmissions": 3,
    "lateSubmissions": 0,
    "totalEvaluations": 16,
    "abnormalEvaluations": 2,
    "abnormalPendingCount": 2,
    "abnormalHandledCount": 0,
    "abnormalIgnoredCount": 0,
    "abnormalRestoredCount": 0,
    "completionRate": 66.67,
    "scoreDistribution": [
      {
        "name": "90-100",
        "value": 6.0
      }
    ],
    "dimensionAverages": [
      {
        "name": "完成度",
        "value": 8.82
      }
    ],
    "leaderboardType": "REALTIME",
    "leaderboard": [
      {
        "rank": 1,
        "projectName": "Sprint Board",
        "finalScore": 90.7,
        "scoreType": "REALTIME"
      }
    ],
    "projectScores": [
      {
        "submissionId": 5002,
        "projectName": "Sprint Board",
        "peerScore": 89.0,
        "teacherScore": 91.0,
        "realtimeFinalScore": 90.2,
        "finalScore": 90.2,
        "late": false
      }
    ],
    "reviewProgressByStudent": [
      {
        "userId": 3,
        "displayName": "张同学",
        "completedCount": 2,
        "totalCount": 2,
        "completionRate": 100.0
      }
    ],
    "abnormalHints": [
      {
        "evaluationId": 8007,
        "projectName": "Sprint Board",
        "evaluatorName": "赵同学",
        "totalScore": 74.0,
        "reason": "与其他学生评分均值偏差 16.5 分",
        "excluded": false
      }
    ],
    "abnormalImpacts": [],
    "blacklistRules": [
      {
        "id": 7001,
        "evaluatorUserId": 10,
        "evaluatorName": "郑同学",
        "targetSubmissionId": 5001,
        "targetProjectName": "Campus Pair",
        "createdAt": "2026-03-16 15:10:00"
      }
    ]
  },
  "timestamp": "2026-03-17 11:47:23"
}
```

**业务说明**

- `leaderboard` 在统计页只作为摘要视图使用，默认只展示前几名
- `abnormalImpacts` 用于说明异常评分被排除后对项目得分的影响
- `resultsPublished=true` 表示最终成绩已经由系统自动生成，`leaderboardType` 会同步切换为 `FINAL`

## 8. 典型鉴权失败示例

### 8.1 未携带 Token

```json
{
  "code": 4010,
  "message": "未登录或 token 无效",
  "data": null,
  "timestamp": "2026-03-17 11:47:23"
}
```

### 8.2 首次登录未改密访问业务接口

```json
{
  "code": 4030,
  "message": "首次登录请先修改密码",
  "data": null,
  "timestamp": "2026-03-17 11:47:23"
}
```

## 9. 示例账号

- 管理员：`admin / admin`
- 教师：`t001 / t001`
- 教师：`t002 / t002`
- 学生：`s001 / s001`
- 学生：`s002 / s002`

说明：

- `s001` 在基础数据中 `firstLoginResetRequired=true`，首次登录后需要先调用 `POST /auth/change-password`
- 其余基础演示账号默认可直接进入业务流
- 若启用了 `bulk-demo`，还会额外生成一批 `bulk_*` 账号，建议主要用于分页和大数据量联调

## 10. 维护说明

- `docs/API_SPEC.md` 是主维护源文件，适合继续由 AI 或开发者增量更新
- `docs/API_SPEC.pdf` 是面向阅读、评审和传阅的导出版本
- 当 `MD` 更新后，可在项目根目录执行：

```bash
python scripts/export_api_spec_pdf.py
```

- 若本地缺少导出依赖，可先执行：

```bash
pip install reportlab
```

- 该脚本会重新读取 `docs/API_SPEC.md` 并覆盖生成 `docs/API_SPEC.pdf`
