# 前端联调修改说明（2026-03）

这份文档用于同步本次后端修复后的前端改动点，供前端工程师直接联调和修改。

本次后端已修复 3 个问题：

- `allowLate` 不再是伪配置，已真正参与提交窗口和状态计算
- 学生项目广场接口补充了明确的不可评原因 `ineligibleReason`
- 教师统计页的 `scoreDistribution` 已改为“项目成绩分布”，不再是“单条评分记录分布”

此外后端还额外补了 2 个联调兜底：

- 附件上传接口现在返回可直接访问的绝对 URL，并且后端已暴露 `/uploads/**` 静态资源
- 教师评价记录接口现在补充了 `evaluatorUsername`
- 学生首页任务卡和课程列表作业摘要也补充了 `submissionCloseAt`

## 1. 学生作业详情 / 提交页

### 1.1 后端变化

学生作业详情接口：

- `GET /api/v1/student/assignments/{assignmentId}`

返回结构里，`AssignmentDetailVo` 新增了字段：

- `submissionCloseAt`

相关字段现在的语义是：

- `deadline`：正常截止时间
- `submissionCloseAt`：最终关闭提交时间
- `allowLate=true`：允许在 `deadline` 之后继续提交，直到 `submissionCloseAt`

当前后端规则：

- 若 `allowLate=false`，则 `submissionCloseAt == deadline`
- 若 `allowLate=true`，则 `submissionCloseAt = deadline + 24h`
- 超过 `deadline` 但未超过 `submissionCloseAt` 时，仍允许提交，但提交记录会被标记为 `late=true`
- 超过 `submissionCloseAt` 后，才真正进入评分窗口

### 1.2 前端需要修改

涉及页面建议重点检查：

- `frontend/src/views/student/StudentAssignmentView.vue`
- `frontend/src/views/student/StudentSubmissionView.vue`
- `frontend/src/views/teacher/TeacherCoursesView.vue`

需要同步的点：

- 页面不要再把 `deadline` 当成“最终关闭时间”
- 作业详情页增加“提交关闭时间”展示
- 学生首页任务卡、课程列表、作业卡片也应优先显示或利用 `submissionCloseAt`
- 如果 `allowLate=true` 且当前时间已过 `deadline` 但未过 `submissionCloseAt`，文案不要再写“已进入评分阶段”
- 提交页按钮状态和顶部提示需要按 `submissionCloseAt` 理解，而不是只按 `deadline`
- 教师端创建作业页里，“允许迟交”开关现在已真正生效，不需要再当成无效 UI

### 1.3 建议文案

- 正常阶段：`截止时间：{deadline}`
- 开启迟交时：`正常截止：{deadline}`，`迟交截止：{submissionCloseAt}`
- 迟交窗口中：`当前处于迟交窗口，提交会被标记为迟交`
- 超过关闭时间后：`当前作业已截止，不能再提交或修改项目`

## 2. 学生项目广场

### 2.1 后端变化

学生项目广场接口：

- `GET /api/v1/student/assignments/{assignmentId}/projects`

返回结构里，`ProjectCardVo` 新增了字段：

- `ineligibleReason`

当前字段语义：

- `canEvaluate=true`：当前项目可评
- `canEvaluate=false`：当前项目不可评
- `ineligibleReason`：不可评的明确原因；若可评则为 `null`

目前后端可能返回的值：

- `SELF`：自己的项目或自己所在小组的项目
- `ALREADY_EVALUATED`：当前用户已经评过
- `BLACKLISTED`：当前用户被配置为需要回避该项目
- `REVIEW_CLOSED`：当前不在学生互评窗口

注意：

- 后端仍然返回“全量项目列表”
- 不可评项目不会被过滤掉，只会标记 `canEvaluate=false`

### 2.2 前端需要修改

涉及页面：

- `frontend/src/views/student/StudentProjectsView.vue`

需要同步的点：

- 页面文案不要再写“系统会自动屏蔽自己、同组成员以及黑名单项目”
- 应改成“系统会保留项目列表，但会标记当前不可评项目及原因”
- 表格里的“不可评价”状态建议细化为可解释提示
- 如果需要 tooltip / tag 文案，直接基于 `ineligibleReason` 映射，不要再在前端重复猜规则

### 2.3 建议映射

- `SELF` -> `自己的项目`
- `ALREADY_EVALUATED` -> `已完成评分`
- `BLACKLISTED` -> `已回避该项目`
- `REVIEW_CLOSED` -> `当前不在互评阶段`

### 2.4 建议文案替换

原文案建议替换为：

- `项目广场会展示全部项目，系统会自动标记当前不可评项目，你只需要处理“可评价”的项目。`

## 3. 教师统计页

### 3.1 后端变化

教师统计接口：

- `GET /api/v1/teacher/assignments/{assignmentId}/stats`

接口里的 `scoreDistribution` 字段仍然保留原名，但统计口径已经变化：

- 旧口径：按 `evaluation.totalScore` 统计，即“单条评分记录分布”
- 新口径：按项目成绩统计，即“项目分布”

当前后端规则：

- 若结果未发布：按项目 `realtimeFinalScore` 统计
- 若结果已发布：按项目 `finalScore` 统计

因此这张图现在和排行榜、项目成绩表是同一口径。

### 3.2 前端需要修改

涉及页面：

- `frontend/src/views/teacher/TeacherStatsView.vue`

需要同步的点：

- “成绩区间”标题现在是成立的，可以继续保留
- 如果页面上有任何说明文案暗示这是“评分记录分布”或“评价分布”，需要删掉
- 如果后续还想展示“单条评价分布”，需要单独新开接口字段，不能再复用 `scoreDistribution`

### 3.3 建议文案

- `成绩区间`
- `按项目当前成绩统计，未发布时展示实时综合分，已发布时展示最终分`

## 4. 前端联调验收点

建议前端完成修改后，至少手动验证下面这些点。

### 4.1 学生提交链路

- 对 `allowLate=true` 的作业，超过 `deadline` 后页面仍显示可提交
- 页面能展示 `submissionCloseAt`
- 迟交窗口中的提交成功后，提交记录应显示 `late=true`
- 超过 `submissionCloseAt` 后，页面不再允许继续提交

### 4.2 学生项目广场

- 页面仍能看到全部项目
- 自己的项目显示不可评，原因是 `SELF`
- 已经评过的项目显示不可评，原因是 `ALREADY_EVALUATED`
- 被回避的项目显示不可评，原因是 `BLACKLISTED`
- 互评结束后项目显示不可评，原因是 `REVIEW_CLOSED`

### 4.3 教师统计页

- “成绩区间”与项目数量口径一致
- 不再出现“项目只有 3 个，但区间统计远大于 3”的情况
- 榜单、项目成绩表、区间分布三者语义一致

## 4.4 逐文件问题清单

下面这部分不是“建议看一下”，而是当前前端代码里已经存在的具体问题清单。前端工程师可以直接按文件逐条处理。

### `frontend/src/views/student/StudentCoursesView.vue`

当前问题：

- Hero 主任务卡仍然直接展示 `primaryTask.deadline`
- 课程列表里的作业摘要仍然直接展示 `assignment.deadline`
- 待办卡片文案仍默认把 `deadline` 理解为最终关闭提交时间

需要修改：

- 对 `taskQueue[*]` 和 `course.assignments[*]` 同步使用 `submissionCloseAt`
- 当 `submissionCloseAt !== deadline` 时，页面要明确区分“正常截止”和“最终关闭提交”
- “待提交”任务的说明文案不要只强调截止时间，应兼容迟交窗口

### `frontend/src/views/student/StudentAssignmentView.vue`

当前问题：

- 顶部状态区仍只显示 `assignment.deadline`
- “互评阶段关注项目广场”这段说明还写着“系统会根据规则过滤不可评项目”，和后端真实行为不一致

需要修改：

- 增加 `submissionCloseAt` 展示
- 开启迟交时，页面说明要显示迟交关闭时间
- 把“过滤不可评项目”改成“保留项目列表并标记不可评原因”

### `frontend/src/views/student/StudentSubmissionView.vue`

当前问题：

- 侧边栏仍把 `assignment.deadline` 当成唯一截止时间
- 上传附件成功后虽然会写入 `attachmentUrl`，但页面没有任何“查看附件/打开附件”的显式入口
- 当前按钮只按“是否未分组”禁用，没有把迟交窗口文案体现在页面上

需要修改：

- 侧边栏改成展示 `deadline` + `submissionCloseAt`
- 迟交窗口内增加明确提示，例如“当前提交会被标记为迟交”
- 上传成功后如有需要，可增加“打开附件”或“复制附件链接”入口

### `frontend/src/views/student/StudentProjectsView.vue`

当前问题：

- Hero 文案仍写“自动屏蔽自己、同组成员以及黑名单项目”
- 表格里只显示“不可评价”，没有利用 `ineligibleReason`

需要修改：

- 把“自动屏蔽”改成“展示全量项目，并标记当前不可评原因”
- 在状态列或 tooltip 中按 `ineligibleReason` 展示具体原因
- 不要再在前端用业务规则重复推断不可评原因

### `frontend/src/views/teacher/TeacherReviewView.vue`

当前问题：

- 评分人筛选下拉的 fallback 逻辑把 `evaluatorRole` 当成了 `username`

需要修改：

- 使用后端新增的 `evaluatorUsername`
- 当 `evaluatorUsername` 为空时，只显示 `evaluatorName`
- 禁止继续使用 `evaluatorRole` 拼账号展示文案

### `frontend/src/views/teacher/TeacherCoursesView.vue`

当前问题：

- 教师创建作业表单里虽然已经有 `allowLate` 开关，但页面还没有把它解释成“截止后仍有宽限期”
- 课程卡片里的作业摘要仍然只显示 `assignment.deadline`

需要修改：

- 在创建作业处补充说明：开启“允许迟交”后，系统会在正常截止后保留 24 小时提交窗口
- 课程卡片作业摘要如需展示截止信息，应考虑 `submissionCloseAt`

### `frontend/src/views/teacher/TeacherStatsView.vue`

当前问题：

- 当前图表标题已经能成立，但如果后续再补说明文字，不能再把 `scoreDistribution` 描述成“评分记录分布”

需要修改：

- 统一按“项目成绩分布”理解和展示

### `frontend/src/views/student/StudentProjectsView.vue` 与 `frontend/src/views/student/StudentAssignmentView.vue`

共同问题：

- 两个页面都还有旧语义残留，一个写“屏蔽”，一个写“过滤”

需要修改：

- 两处统一成同一个口径：`全量项目列表 + canEvaluate + ineligibleReason`

### `frontend/src/views/student/StudentCoursesView.vue`、`frontend/src/views/student/StudentAssignmentView.vue`、`frontend/src/views/student/StudentSubmissionView.vue`

共同问题：

- 三个页面都还存在“只展示 `deadline`”的旧实现

需要修改：

- 三个页面统一接入 `submissionCloseAt`
- 所有“截止”相关文案都要区分：
  - 正常截止时间
  - 最终关闭提交时间
  - 迟交窗口中

### `frontend/src/views/student/StudentSubmissionView.vue`

补充注意：

- 上传接口现在返回：
  - `url`
  - `path`
- 页面如果后续要做附件预览、打开、复制链接，不要再假设只有一个相对路径字符串

## 5. 附件上传链路

### 5.1 后端变化

附件上传接口：

- `POST /api/v1/files/upload`

当前返回结构：

- `url`：可直接访问的绝对 URL
- `path`：后端静态资源相对路径
- `originalName`

例如：

```json
{
  "url": "http://127.0.0.1:8080/uploads/xxx-demo.pdf",
  "path": "/uploads/xxx-demo.pdf",
  "originalName": "demo.pdf"
}
```

后端已补齐 `/uploads/**` 静态资源访问能力，因此这个 `url` 应该可以直接访问。

### 5.2 前端需要修改

涉及页面：

- `frontend/src/views/student/StudentSubmissionView.vue`

需要同步的点：

- 上传成功后，优先保存和展示 `result.url`
- 如果页面后续要做“查看附件”或“下载附件”，优先使用 `attachmentUrl`
- 不要假设上传接口返回的一定是相对路径
- 如果页面上还存在手动拼接后端 host 的逻辑，不需要再继续拼接上传返回值

## 6. 教师评分治理页：评分人账号显示

### 6.1 后端变化

教师评价记录接口：

- `GET /api/v1/teacher/assignments/{assignmentId}/evaluations`

返回结构里，`EvaluationRecordVo` 新增了字段：

- `evaluatorUsername`

当前字段语义：

- `evaluatorName`：显示姓名
- `evaluatorUsername`：登录账号
- `evaluatorRole`：角色，取值如 `STUDENT` / `TEACHER`

### 6.2 前端需要修改

涉及页面：

- `frontend/src/views/teacher/TeacherReviewView.vue`

当前页面里，筛选下拉的 fallback 逻辑把 `evaluatorRole` 当成 `username` 展示，这是不对的。

建议改成：

- 优先使用 `evaluatorUsername`
- 若为空，再退化为仅显示 `evaluatorName`
- 不要再把 `evaluatorRole` 当作账号文案

建议显示格式：

- `${evaluatorName} (${evaluatorUsername})`

当 `evaluatorUsername` 为空时：

- `${evaluatorName}`

## 7. 接口字段变更摘要

### `GET /api/v1/student/assignments/{assignmentId}`

新增：

- `submissionCloseAt: string | null`

### `GET /api/v1/student/assignments/{assignmentId}/projects`

新增：

- `ineligibleReason: 'SELF' | 'ALREADY_EVALUATED' | 'BLACKLISTED' | 'REVIEW_CLOSED' | null`

### `GET /api/v1/teacher/assignments/{assignmentId}/stats`

无字段新增，但以下字段语义已变更：

- `scoreDistribution`

新语义：

- 项目成绩分布

### `POST /api/v1/files/upload`

返回结构补充：

- `url: string`
- `path: string`

### `GET /api/v1/teacher/assignments/{assignmentId}/evaluations`

新增：

- `evaluatorUsername: string | null`

### `GET /api/v1/student/home`

`taskQueue[*]` 新增：

- `submissionCloseAt: string | null`

### `GET /api/v1/student/courses`

`list[*].assignments[*]` 新增：

- `submissionCloseAt: string | null`
