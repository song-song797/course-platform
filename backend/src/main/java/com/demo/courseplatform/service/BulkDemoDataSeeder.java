package com.demo.courseplatform.service;

import com.demo.courseplatform.config.BulkDemoDataProperties;
import com.demo.courseplatform.domain.entity.PersistenceModels.AssignmentEntity;
import com.demo.courseplatform.domain.entity.PersistenceModels.CourseEntity;
import com.demo.courseplatform.domain.entity.PersistenceModels.CourseMemberEntity;
import com.demo.courseplatform.domain.entity.PersistenceModels.RubricEntity;
import com.demo.courseplatform.domain.entity.PersistenceModels.RubricItemEntity;
import com.demo.courseplatform.domain.entity.PersistenceModels.SubmissionEntity;
import com.demo.courseplatform.domain.entity.PersistenceModels.UserEntity;
import com.demo.courseplatform.mapper.AssignmentMapper;
import com.demo.courseplatform.mapper.CourseMapper;
import com.demo.courseplatform.mapper.EvaluationMapper;
import com.demo.courseplatform.mapper.SubmissionMapper;
import com.demo.courseplatform.mapper.UserMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "demo.bulk-seed", name = "enabled", havingValue = "true")
public class BulkDemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BulkDemoDataSeeder.class);
    private static final List<Long> BASE_COURSE_IDS = List.of(101L, 102L, 103L);
    private static final String BULK_USERNAME_PREFIX = "bulk_";
    private static final String BULK_COURSE_CODE_PREFIX = "BULK-C";
    private static final String BULK_ASSIGNMENT_TITLE_PREFIX = "BULK-DEMO | ";
    private static final String BULK_PROJECT_NAME_PREFIX = "Bulk Demo | ";
    private static final int TEACHERS_PER_COURSE = 2;
    private static final int FIRST_SCREEN_COURSE_TARGET = 10;
    private static final int FIRST_SCREEN_MIN_ASSIGNMENTS = 3;
    private static final int REMAINING_MIN_ASSIGNMENTS = 2;
    private static final int TARGET_GROUP_ASSIGNMENTS = 26;
    private static final int TARGET_INDIVIDUAL_ASSIGNMENTS = 22;
    private static final int MIN_DUE_SOON_SUBMITTING_ASSIGNMENTS = 6;

    private static final String[] COURSE_NAME_TEMPLATES = {
        "智能协作系统设计", "数字产品交付工作坊", "数据工程课程项目", "服务设计与前端实现",
        "交互原型与产品验证", "企业应用开发实战", "课程平台增强实验", "应用架构与工程实践",
        "开放数据应用设计", "协同办公系统专题", "可视化业务平台项目", "课堂评价系统创新"
    };

    private static final String[] TERM_TEMPLATES = {"2026 春", "2026 秋", "2027 春"};

    private static final String[] TEACHER_NAME_PREFIXES = {
        "陈老师", "林老师", "许老师", "高老师", "郭老师", "马老师", "彭老师", "唐老师", "钟老师", "沈老师", "董老师", "韩老师"
    };

    private static final String[] STUDENT_NAME_PREFIXES = {
        "林同学", "周同学", "徐同学", "许同学", "朱同学", "冯同学", "梁同学", "谢同学", "韩同学", "曹同学",
        "袁同学", "邓同学", "余同学", "叶同学", "苏同学", "魏同学", "吕同学", "丁同学", "田同学", "杜同学"
    };

    private static final String[] GROUP_ASSIGNMENT_NAMES = {
        "团队协同中台", "课程运营工作台", "知识共创引擎", "多角色协作门户", "校园服务编排平台",
        "任务编排与追踪中心", "项目治理仪表盘", "跨团队需求协同站", "课堂资源编排器", "反馈治理实验台"
    };

    private static final String[] INDIVIDUAL_ASSIGNMENT_NAMES = {
        "个人作品档案页", "课程成果展示页", "交互研究作品集", "项目复盘故事板", "学习轨迹可视化页",
        "设计系统展示页", "个人增长仪表页", "专题答辩资料站", "实践成果说明页", "学习成果导航页"
    };

    private static final String[] PROJECT_NAME_TEMPLATES = {
        "星图协作台", "北斗课堂站", "风帆任务流", "澄镜项目面板", "禾木资源仓",
        "海岳反馈站", "光谱答辩台", "栖云学习舱", "跃迁作品盒", "辰光协作轴"
    };

    private static final String[] COMMENT_TEMPLATES = {
        "结构稳定，交付节奏清晰。", "功能覆盖比较全面，展示表达自然。", "边界细节处理到位，协作痕迹明显。",
        "整体完成度不错，仍有打磨空间。", "逻辑清楚，工程组织比较规整。", "展示节奏顺畅，复盘内容比较完整。",
        "问题定义比较聚焦，交互逻辑也更容易理解。", "实现路径扎实，演示里的重点信息呈现得很完整。",
        "协作痕迹明显，成员分工和串联方式都比较自然。", "页面层次清楚，项目价值和亮点传达得比较到位。"
    };

    private static final String[] COMMENT_TRAILERS = {
        "如果继续补齐边界场景，整体完成度会更稳。", "后续可以再强化异常路径和细节打磨。",
        "如果把说明文档再压缩一下，表达会更有说服力。", "交付质量已经在线，适合继续做小步优化。",
        "如果把关键取舍写进复盘，项目成熟度会再往上走。", "继续完善数据与反馈闭环，会更像真实上线版本。"
    };

    private static final String[] HIGH_SCORE_OPENERS = {
        "项目整体完成得很成熟。", "这一版的交付完成度很高。", "从演示节奏到实现质量都比较稳定。"
    };

    private static final String[] MID_SCORE_OPENERS = {
        "主体框架已经搭起来了。", "整体方向是对的，但还留有一些可继续打磨的地方。", "当前版本已经具备清晰的主线。"
    };

    private static final String[] LOW_SCORE_OPENERS = {
        "目前能看出核心思路，但落地还不够完整。", "项目已经有雏形，不过关键细节还比较薄。", "方向是明确的，但当前完成度还需要继续拉齐。"
    };

    private static final String[] TEACHER_COMMENT_PREFIXES = {
        "教师视角看，这个项目的主线比较清楚。", "从课程目标完成情况看，这一版已经形成稳定交付。", "作为课程作业，这个版本已经覆盖了主要评审面。"
    };

    private static final String[] ITEM_COMMENT_TEMPLATES = {
        "完成情况稳定", "结构设计清楚", "细节表现较好", "展示表达自然", "整体思路完整", "仍可继续优化"
    };

    private static final List<RubricTemplate> GROUP_RUBRIC_A = List.of(
        new RubricTemplate("业务完成度", "是否覆盖核心业务场景", 30),
        new RubricTemplate("协作质量", "小组分工与协同效率", 20),
        new RubricTemplate("工程质量", "结构、规范与稳定性", 30),
        new RubricTemplate("展示表达", "答辩材料与呈现效果", 20)
    );
    private static final List<RubricTemplate> GROUP_RUBRIC_B = List.of(
        new RubricTemplate("问题定义", "场景聚焦是否准确", 25),
        new RubricTemplate("方案设计", "产品与流程设计质量", 25),
        new RubricTemplate("工程实现", "前后端落地与稳定性", 30),
        new RubricTemplate("复盘表达", "文档与复盘深度", 20)
    );
    private static final List<RubricTemplate> GROUP_RUBRIC_C = List.of(
        new RubricTemplate("价值完整度", "项目价值链条是否闭环", 30),
        new RubricTemplate("协作治理", "成员协作与职责边界", 20),
        new RubricTemplate("质量保障", "可维护性与边界处理", 25),
        new RubricTemplate("呈现效果", "演示和材料表现力", 25)
    );

    private static final List<RubricTemplate> INDIVIDUAL_RUBRIC_A = List.of(
        new RubricTemplate("视觉完成度", "页面与视觉细节完成情况", 30),
        new RubricTemplate("交互设计", "交互流程与可用性", 25),
        new RubricTemplate("代码结构", "工程组织与可维护性", 25),
        new RubricTemplate("表达说明", "说明文档与项目表达", 20)
    );
    private static final List<RubricTemplate> INDIVIDUAL_RUBRIC_B = List.of(
        new RubricTemplate("内容完整度", "信息结构是否完整", 30),
        new RubricTemplate("体验流畅度", "浏览与操作体验", 25),
        new RubricTemplate("实现质量", "组件结构与边界处理", 25),
        new RubricTemplate("讲述能力", "项目价值说明是否清楚", 20)
    );
    private static final List<RubricTemplate> INDIVIDUAL_RUBRIC_C = List.of(
        new RubricTemplate("表达清晰度", "页面信息是否易于理解", 25),
        new RubricTemplate("设计细节", "排版、层次与视觉控制", 25),
        new RubricTemplate("工程交付", "代码质量与部署完整性", 30),
        new RubricTemplate("复盘质量", "总结与反思深度", 20)
    );

    private final BulkDemoDataProperties properties;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordService passwordService;
    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final AssignmentMapper assignmentMapper;
    private final SubmissionMapper submissionMapper;
    private final EvaluationMapper evaluationMapper;

    public BulkDemoDataSeeder(BulkDemoDataProperties properties,
                              JdbcTemplate jdbcTemplate,
                              PasswordService passwordService,
                              UserMapper userMapper,
                              CourseMapper courseMapper,
                              AssignmentMapper assignmentMapper,
                              SubmissionMapper submissionMapper,
                              EvaluationMapper evaluationMapper) {
        this.properties = properties;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordService = passwordService;
        this.userMapper = userMapper;
        this.courseMapper = courseMapper;
        this.assignmentMapper = assignmentMapper;
        this.submissionMapper = submissionMapper;
        this.evaluationMapper = evaluationMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        validateConfiguration();
        if (properties.isResetBeforeSeed()) {
            cleanupBulkData();
        }

        Random random = new Random(properties.getRandomSeed());
        List<UserEntity> teacherPool = ensureTeacherPool();
        List<UserEntity> studentPool = ensureStudentPool();
        Map<Long, UserEntity> usersById = combineUsers(teacherPool, studentPool);
        List<CourseSeed> courseSeeds = prepareCourses(teacherPool, studentPool);
        int currentAssignmentCount = courseSeeds.stream()
            .mapToInt(seed -> assignmentMapper.findByCourseId(seed.course.id).size())
            .sum();
        List<GeneratedAssignment> assignments = createAssignments(courseSeeds, currentAssignmentCount, random);
        seedSubmissions(assignments, usersById);
        seedBlacklists(assignments);
        seedEvaluations(assignments);
        verifySeedTargets(courseSeeds);

        long totalAssignments = assignments.size() + currentAssignmentCount;
        long totalSubmissions = assignments.stream().mapToLong(item -> item.submissions.size()).sum() + countExistingSubmissions();
        long totalEvaluations = assignments.stream().mapToLong(item -> item.generatedEvaluationCount).sum() + countExistingEvaluations();
        log.info("Bulk demo data prepared: totalCourses={}, totalAssignments={}, totalSubmissions={}, totalEvaluations={}",
            courseSeeds.size(), totalAssignments, totalSubmissions, totalEvaluations);
    }

    private void validateConfiguration() {
        if (properties.getCourseCount() < BASE_COURSE_IDS.size()) {
            throw new IllegalStateException("bulk demo 配置的课程数不能小于基础课程数");
        }
        if (properties.getAssignmentCount() < 5) {
            throw new IllegalStateException("bulk demo 配置的作业数不能小于基础演示作业数");
        }
        if (properties.getReviewingAssignmentCount() < 1 || properties.getClosedAssignmentCount() < 1) {
            throw new IllegalStateException("reviewingAssignmentCount 和 closedAssignmentCount 必须大于 0");
        }
        if (properties.getReviewingAssignmentCount() + properties.getClosedAssignmentCount() >= properties.getAssignmentCount()) {
            throw new IllegalStateException("reviewingAssignmentCount + closedAssignmentCount 必须小于 assignmentCount");
        }
        if (properties.getAssignmentCount() < minimumAssignmentsForCoverage()) {
            throw new IllegalStateException("assignmentCount 过小，无法覆盖学生大厅的课程现场区");
        }
        if (targetStudentsPerCourse() < 14) {
            throw new IllegalStateException("membersPerCourse 至少需要支持 14 名学生，才能覆盖小组互评场景");
        }
        if (properties.getGroupStudentReviewCount() < 6 || properties.getIndividualStudentReviewCount() < 5) {
            throw new IllegalStateException("学生互评目标数过小，无法营造默认大厅氛围");
        }
    }

    private int minimumAssignmentsForCoverage() {
        int prioritizedCourseCount = Math.min(properties.getCourseCount(), FIRST_SCREEN_COURSE_TARGET);
        int remainingCourseCount = Math.max(properties.getCourseCount() - prioritizedCourseCount, 0);
        return prioritizedCourseCount * FIRST_SCREEN_MIN_ASSIGNMENTS + remainingCourseCount * REMAINING_MIN_ASSIGNMENTS;
    }

    private void verifySeedTargets(List<CourseSeed> courseSeeds) {
        List<AssignmentEntity> assignments = loadAssignments(courseSeeds);
        if (courseSeeds.size() != properties.getCourseCount()) {
            throw new IllegalStateException("课程总量校验失败，预期 " + properties.getCourseCount() + "，实际 " + courseSeeds.size());
        }
        if (assignments.size() != properties.getAssignmentCount()) {
            throw new IllegalStateException("作业总量校验失败，预期 " + properties.getAssignmentCount() + "，实际 " + assignments.size());
        }

        Map<String, Long> statusCounts = assignments.stream()
            .collect(Collectors.groupingBy(item -> item.status, LinkedHashMap::new, Collectors.counting()));
        assertCount(statusCounts, "REVIEWING", properties.getReviewingAssignmentCount(), "互评中作业数");
        assertCount(statusCounts, "CLOSED", properties.getClosedAssignmentCount(), "已发布结果作业数");
        assertCount(statusCounts, "SUBMITTING",
            properties.getAssignmentCount() - properties.getReviewingAssignmentCount() - properties.getClosedAssignmentCount(),
            "提交中作业数");

        Map<String, Long> modeCounts = assignments.stream()
            .collect(Collectors.groupingBy(item -> item.mode, LinkedHashMap::new, Collectors.counting()));
        int expectedGroupCount = expectedGroupAssignmentCount();
        assertCount(modeCounts, "GROUP", expectedGroupCount, "小组作业数");
        assertCount(modeCounts, "INDIVIDUAL", properties.getAssignmentCount() - expectedGroupCount, "个人作业数");

        for (int index = 0; index < Math.min(FIRST_SCREEN_COURSE_TARGET, courseSeeds.size()); index++) {
            CourseSeed courseSeed = courseSeeds.get(index);
            long count = assignments.stream().filter(item -> Objects.equals(item.courseId, courseSeed.course.id)).count();
            if (count < FIRST_SCREEN_MIN_ASSIGNMENTS) {
                throw new IllegalStateException("学生大厅首屏课程作业数不足：" + courseSeed.course.name + " 仅有 " + count + " 个作业");
            }
        }

        long dueSoonSubmittingAssignments = assignments.stream()
            .filter(item -> "SUBMITTING".equals(item.status))
            .filter(item -> isDueSoonDeadline(item.deadline))
            .count();
        if (dueSoonSubmittingAssignments < MIN_DUE_SOON_SUBMITTING_ASSIGNMENTS) {
            throw new IllegalStateException("临近截止作业数不足，预期至少 " + MIN_DUE_SOON_SUBMITTING_ASSIGNMENTS
                + "，实际 " + dueSoonSubmittingAssignments);
        }

        long totalSubmissions = countTotalSubmissions(assignments);
        if (totalSubmissions < 380) {
            throw new IllegalStateException("提交总量不足，预期至少 380，实际 " + totalSubmissions);
        }

        long totalEvaluations = countTotalEvaluations(assignments);
        if (totalEvaluations < 2400) {
            throw new IllegalStateException("评分总量不足，预期至少 2400，实际 " + totalEvaluations);
        }

        long denseCommentAssignments = countAssignmentsWithDenseComments(assignments);
        if (denseCommentAssignments < 12) {
            throw new IllegalStateException("高密度匿名评语作业数不足，预期至少 12，实际 " + denseCommentAssignments);
        }
    }

    private List<AssignmentEntity> loadAssignments(List<CourseSeed> courseSeeds) {
        List<AssignmentEntity> assignments = new ArrayList<>();
        for (CourseSeed courseSeed : courseSeeds) {
            assignments.addAll(assignmentMapper.findByCourseId(courseSeed.course.id));
        }
        return assignments;
    }

    private void assertCount(Map<String, Long> counts, String key, int expected, String label) {
        long actual = counts.getOrDefault(key, 0L);
        if (actual != expected) {
            throw new IllegalStateException(label + "校验失败，预期 " + expected + "，实际 " + actual);
        }
    }

    private int expectedGroupAssignmentCount() {
        return (int) Math.round(properties.getAssignmentCount() * (TARGET_GROUP_ASSIGNMENTS / 48D));
    }

    private boolean isDueSoonDeadline(LocalDateTime deadline) {
        if (deadline == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return !deadline.isBefore(now) && !deadline.isAfter(now.plusHours(72));
    }

    private long countTotalSubmissions(List<AssignmentEntity> assignments) {
        return assignments.stream()
            .mapToLong(item -> submissionMapper.findSubmissionsByAssignmentId(item.id).size())
            .sum();
    }

    private long countTotalEvaluations(List<AssignmentEntity> assignments) {
        return assignments.stream()
            .mapToLong(item -> evaluationMapper.findByAssignmentId(item.id, null, null, null, false).size())
            .sum();
    }

    private long countAssignmentsWithDenseComments(List<AssignmentEntity> assignments) {
        return assignments.stream()
            .filter(this::hasDenseStudentComments)
            .count();
    }

    private boolean hasDenseStudentComments(AssignmentEntity assignment) {
        Map<Long, Long> studentCommentCounts = evaluationMapper.findByAssignmentId(assignment.id, null, null, null, false).stream()
            .filter(item -> "STUDENT".equals(item.evaluatorRole))
            .filter(item -> item.comment != null && !item.comment.isBlank())
            .collect(Collectors.groupingBy(item -> item.submissionId, LinkedHashMap::new, Collectors.counting()));
        return studentCommentCounts.values().stream().anyMatch(count -> count >= 6);
    }

    private void cleanupBulkData() {
        jdbcTemplate.update("""
            DELETE ei
            FROM evaluation_item ei
            INNER JOIN evaluation e ON e.id = ei.evaluation_id
            INNER JOIN assignment a ON a.id = e.assignment_id
            WHERE a.title LIKE ?
            """, likePrefix(BULK_ASSIGNMENT_TITLE_PREFIX));
        jdbcTemplate.update("""
            DELETE eb
            FROM evaluation_blacklist eb
            INNER JOIN assignment a ON a.id = eb.assignment_id
            WHERE a.title LIKE ?
            """, likePrefix(BULK_ASSIGNMENT_TITLE_PREFIX));
        jdbcTemplate.update("""
            DELETE e
            FROM evaluation e
            INNER JOIN assignment a ON a.id = e.assignment_id
            WHERE a.title LIKE ?
            """, likePrefix(BULK_ASSIGNMENT_TITLE_PREFIX));
        jdbcTemplate.update("""
            DELETE s
            FROM submission s
            INNER JOIN assignment a ON a.id = s.assignment_id
            WHERE a.title LIKE ?
            """, likePrefix(BULK_ASSIGNMENT_TITLE_PREFIX));
        jdbcTemplate.update("""
            DELETE agm
            FROM assignment_group_member agm
            INNER JOIN assignment a ON a.id = agm.assignment_id
            WHERE a.title LIKE ?
            """, likePrefix(BULK_ASSIGNMENT_TITLE_PREFIX));
        jdbcTemplate.update("""
            DELETE ag
            FROM assignment_group ag
            INNER JOIN assignment a ON a.id = ag.assignment_id
            WHERE a.title LIKE ?
            """, likePrefix(BULK_ASSIGNMENT_TITLE_PREFIX));
        jdbcTemplate.update("""
            DELETE ri
            FROM rubric_item ri
            INNER JOIN rubric r ON r.id = ri.rubric_id
            INNER JOIN assignment a ON a.id = r.assignment_id
            WHERE a.title LIKE ?
            """, likePrefix(BULK_ASSIGNMENT_TITLE_PREFIX));
        jdbcTemplate.update("""
            DELETE r
            FROM rubric r
            INNER JOIN assignment a ON a.id = r.assignment_id
            WHERE a.title LIKE ?
            """, likePrefix(BULK_ASSIGNMENT_TITLE_PREFIX));
        jdbcTemplate.update("""
            DELETE FROM assignment
            WHERE title LIKE ?
            """, likePrefix(BULK_ASSIGNMENT_TITLE_PREFIX));
        jdbcTemplate.update("""
            DELETE cm
            FROM course_member cm
            INNER JOIN sys_user u ON u.id = cm.user_id
            WHERE u.username LIKE ?
            """, likePrefix(BULK_USERNAME_PREFIX));
        jdbcTemplate.update("""
            DELETE cm
            FROM course_member cm
            INNER JOIN course c ON c.id = cm.course_id
            WHERE c.code LIKE ?
            """, likePrefix(BULK_COURSE_CODE_PREFIX));
        jdbcTemplate.update("""
            DELETE FROM course
            WHERE code LIKE ?
            """, likePrefix(BULK_COURSE_CODE_PREFIX));
        jdbcTemplate.update("""
            DELETE FROM sys_user
            WHERE username LIKE ?
            """, likePrefix(BULK_USERNAME_PREFIX));
    }

    private String likePrefix(String prefix) {
        return prefix + "%";
    }

    private List<UserEntity> ensureTeacherPool() {
        List<UserEntity> existingTeachers = userMapper.findByRole("TEACHER");
        int targetTeacherPoolSize = Math.max(properties.getCourseCount(), existingTeachers.size());
        int missingTeachers = Math.max(0, targetTeacherPoolSize - existingTeachers.size());
        for (int index = 1; index <= missingTeachers; index++) {
            UserEntity teacher = new UserEntity();
            teacher.username = BULK_USERNAME_PREFIX + "t" + String.format(Locale.ROOT, "%03d", index);
            teacher.passwordHash = passwordService.encode(teacher.username);
            teacher.displayName = TEACHER_NAME_PREFIXES[(index - 1) % TEACHER_NAME_PREFIXES.length]
                + String.format(Locale.ROOT, "%02d", index);
            teacher.role = "TEACHER";
            teacher.firstLoginResetRequired = false;
            userMapper.insert(teacher);
            existingTeachers.add(teacher);
        }
        return existingTeachers.stream().sorted(Comparator.comparing(item -> item.id)).toList();
    }

    private List<UserEntity> ensureStudentPool() {
        List<UserEntity> existingStudents = userMapper.findByRole("STUDENT");
        int targetStudentPoolSize = Math.max(properties.getCourseCount() * targetStudentsPerCourse() / 2, existingStudents.size());
        int missingStudents = Math.max(0, targetStudentPoolSize - existingStudents.size());
        for (int index = 1; index <= missingStudents; index++) {
            UserEntity student = new UserEntity();
            student.username = BULK_USERNAME_PREFIX + "s" + String.format(Locale.ROOT, "%03d", index);
            student.passwordHash = passwordService.encode(student.username);
            student.displayName = STUDENT_NAME_PREFIXES[(index - 1) % STUDENT_NAME_PREFIXES.length]
                + String.format(Locale.ROOT, "%02d", index);
            student.role = "STUDENT";
            student.firstLoginResetRequired = false;
            userMapper.insert(student);
            existingStudents.add(student);
        }
        return existingStudents.stream().sorted(Comparator.comparing(item -> item.id)).toList();
    }

    private Map<Long, UserEntity> combineUsers(List<UserEntity> teachers, List<UserEntity> students) {
        Map<Long, UserEntity> usersById = new LinkedHashMap<>();
        for (UserEntity teacher : teachers) {
            usersById.put(teacher.id, teacher);
        }
        for (UserEntity student : students) {
            usersById.put(student.id, student);
        }
        return usersById;
    }

    private List<CourseSeed> prepareCourses(List<UserEntity> teachers, List<UserEntity> students) {
        List<CourseEntity> baseCourses = BASE_COURSE_IDS.stream()
            .map(courseMapper::findById)
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(item -> item.id))
            .toList();
        if (baseCourses.size() != BASE_COURSE_IDS.size()) {
            throw new IllegalStateException("基础课程不存在，无法初始化 bulk demo 数据");
        }

        List<CourseEntity> allCourses = new ArrayList<>(baseCourses);
        int extraCourseCount = properties.getCourseCount() - baseCourses.size();
        for (int index = 0; index < extraCourseCount; index++) {
            CourseEntity course = new CourseEntity();
            int courseNo = baseCourses.size() + index + 1;
            course.code = BULK_COURSE_CODE_PREFIX + String.format(Locale.ROOT, "%02d", courseNo);
            course.name = COURSE_NAME_TEMPLATES[index % COURSE_NAME_TEMPLATES.length] + " " + courseNo;
            course.term = TERM_TEMPLATES[index % TERM_TEMPLATES.length];
            courseMapper.insert(course);
            allCourses.add(course);
        }

        List<Long> teacherIds = teachers.stream().map(item -> item.id).toList();
        List<Long> studentIds = students.stream().map(item -> item.id).toList();
        List<CourseSeed> courseSeeds = new ArrayList<>();
        for (int courseIndex = 0; courseIndex < allCourses.size(); courseIndex++) {
            CourseEntity course = allCourses.get(courseIndex);
            List<CourseMemberEntity> existingMembers = courseMapper.findMembersByCourseId(course.id);
            LinkedHashSet<Long> teacherSet = existingMembers.stream()
                .filter(item -> "TEACHER".equals(item.courseRole))
                .map(item -> item.userId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
            LinkedHashSet<Long> studentSet = existingMembers.stream()
                .filter(item -> "STUDENT".equals(item.courseRole))
                .map(item -> item.userId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

            fillSet(teacherSet, teacherIds, courseIndex * 2, TEACHERS_PER_COURSE);
            fillSet(studentSet, studentIds, courseIndex * 9, targetStudentsPerCourse());

            Set<Long> existingTeacherIds = existingMembers.stream()
                .filter(item -> "TEACHER".equals(item.courseRole))
                .map(item -> item.userId)
                .collect(Collectors.toSet());
            Set<Long> existingStudentIds = existingMembers.stream()
                .filter(item -> "STUDENT".equals(item.courseRole))
                .map(item -> item.userId)
                .collect(Collectors.toSet());
            List<CourseMemberEntity> toInsert = new ArrayList<>();
            for (Long teacherId : teacherSet) {
                if (!existingTeacherIds.contains(teacherId)) {
                    toInsert.add(buildCourseMember(course.id, teacherId, "TEACHER"));
                }
            }
            for (Long studentId : studentSet) {
                if (!existingStudentIds.contains(studentId)) {
                    toInsert.add(buildCourseMember(course.id, studentId, "STUDENT"));
                }
            }
            if (!toInsert.isEmpty()) {
                courseMapper.insertMembers(toInsert);
            }
            courseSeeds.add(new CourseSeed(courseIndex, course, List.copyOf(teacherSet), List.copyOf(studentSet)));
        }
        return courseSeeds;
    }

    private int targetStudentsPerCourse() {
        return Math.max(1, properties.getMembersPerCourse() - TEACHERS_PER_COURSE);
    }

    private void fillSet(LinkedHashSet<Long> target, List<Long> pool, int start, int size) {
        if (pool.isEmpty()) {
            return;
        }
        int cursor = Math.floorMod(start, pool.size());
        while (target.size() < size) {
            target.add(pool.get(cursor));
            cursor = (cursor + 1) % pool.size();
        }
    }

    private CourseMemberEntity buildCourseMember(Long courseId, Long userId, String role) {
        CourseMemberEntity entity = new CourseMemberEntity();
        entity.courseId = courseId;
        entity.userId = userId;
        entity.courseRole = role;
        return entity;
    }

    private List<GeneratedAssignment> createAssignments(List<CourseSeed> courseSeeds, int currentAssignmentCount, Random random) {
        int newAssignmentsNeeded = properties.getAssignmentCount() - currentAssignmentCount;
        if (newAssignmentsNeeded <= 0) {
            return List.of();
        }

        Map<Long, Integer> currentCountsByCourse = courseSeeds.stream()
            .collect(Collectors.toMap(seed -> seed.course.id,
                seed -> assignmentMapper.findByCourseId(seed.course.id).size(),
                (left, right) -> left,
                LinkedHashMap::new));
        Map<Long, Integer> extraAssignmentsByCourse = buildExtraAssignmentsByCourse(courseSeeds, currentCountsByCourse, newAssignmentsNeeded);
        List<String> statusPool = buildStatusPool(courseSeeds, newAssignmentsNeeded, random);
        List<String> modePool = buildModePool(courseSeeds, newAssignmentsNeeded, random);

        List<GeneratedAssignment> assignments = new ArrayList<>();
        int newAssignmentOrder = 0;
        Map<String, Integer> statusOrders = new LinkedHashMap<>();
        for (CourseSeed seed : courseSeeds) {
            int extraCount = extraAssignmentsByCourse.getOrDefault(seed.course.id, 0);
            for (int localIndex = 0; localIndex < extraCount; localIndex++) {
                String mode = modePool.remove(0);
                String status = statusPool.remove(0);
                int statusOrder = statusOrders.getOrDefault(status, 0);
                AssignmentEntity assignment = new AssignmentEntity();
                assignment.courseId = seed.course.id;
                assignment.mode = mode;
                assignment.status = status;
                assignment.title = nextAssignmentTitle(mode, newAssignmentOrder, seed.course.name);
                assignment.description = buildAssignmentDescription(seed.course.name, mode, status);
                assignment.allowLate = resolveAllowLate(status, newAssignmentOrder);
                assignment.deadline = buildDeadline(status, statusOrder);
                int[] weightProfile = weightProfileFor(mode, newAssignmentOrder);
                assignment.peerWeight = weightProfile[0];
                assignment.teacherWeight = weightProfile[1];
                assignment.resultsPublished = "CLOSED".equals(status);
                assignment.resultsPublishedAt = assignment.resultsPublished
                    ? assignment.deadline.plusDays(2).withHour(18).withMinute(0).withSecond(0).withNano(0)
                    : null;
                assignmentMapper.insert(assignment);
                List<RubricItemEntity> rubricItems = persistRubric(assignment, newAssignmentOrder);
                assignments.add(new GeneratedAssignment(
                    newAssignmentOrder,
                    seed,
                    assignment,
                    rubricItems,
                    studentReviewTargetFor(mode),
                    teacherReviewTargetFor(mode, seed.teacherIds.size())
                ));
                statusOrders.put(status, statusOrder + 1);
                newAssignmentOrder++;
            }
        }
        return assignments;
    }

    private int studentReviewTargetFor(String mode) {
        return "GROUP".equals(mode)
            ? properties.getGroupStudentReviewCount()
            : properties.getIndividualStudentReviewCount();
    }

    private int teacherReviewTargetFor(String mode, int teacherPoolSize) {
        return "GROUP".equals(mode) ? Math.min(2, teacherPoolSize) : Math.min(1, teacherPoolSize);
    }

    private Map<Long, Integer> buildExtraAssignmentsByCourse(List<CourseSeed> courseSeeds,
                                                             Map<Long, Integer> currentCountsByCourse,
                                                             int newAssignmentsNeeded) {
        Map<Long, Integer> extras = courseSeeds.stream()
            .collect(Collectors.toMap(seed -> seed.course.id, seed -> 0, (left, right) -> left, LinkedHashMap::new));
        int remaining = newAssignmentsNeeded;

        for (int index = 0; index < courseSeeds.size(); index++) {
            CourseSeed seed = courseSeeds.get(index);
            int currentCount = currentCountsByCourse.getOrDefault(seed.course.id, 0);
            int minimumTarget = index < FIRST_SCREEN_COURSE_TARGET
                ? FIRST_SCREEN_MIN_ASSIGNMENTS
                : REMAINING_MIN_ASSIGNMENTS;
            int required = Math.max(0, minimumTarget - currentCount);
            extras.put(seed.course.id, required);
            remaining -= required;
        }

        List<CourseSeed> prioritizedCourses = new ArrayList<>(courseSeeds.stream()
            .limit(Math.min(FIRST_SCREEN_COURSE_TARGET, courseSeeds.size()))
            .toList());
        prioritizedCourses.addAll(courseSeeds.stream().skip(Math.min(FIRST_SCREEN_COURSE_TARGET, courseSeeds.size())).toList());

        for (CourseSeed seed : prioritizedCourses) {
            if (remaining <= 0) {
                break;
            }
            int next = Math.min(indexOfCourse(courseSeeds, seed.course.id) < FIRST_SCREEN_COURSE_TARGET ? 2 : 1, remaining);
            extras.put(seed.course.id, extras.get(seed.course.id) + next);
            remaining -= next;
        }

        for (int index = 0; remaining > 0; index = (index + 1) % prioritizedCourses.size()) {
            CourseSeed seed = prioritizedCourses.get(index);
            extras.put(seed.course.id, extras.get(seed.course.id) + 1);
            remaining--;
        }
        return extras;
    }

    private int indexOfCourse(List<CourseSeed> courseSeeds, Long courseId) {
        for (int index = 0; index < courseSeeds.size(); index++) {
            if (Objects.equals(courseSeeds.get(index).course.id, courseId)) {
                return index;
            }
        }
        return Integer.MAX_VALUE;
    }

    private List<String> buildStatusPool(List<CourseSeed> courseSeeds, int newAssignmentsNeeded, Random random) {
        Map<String, Integer> desired = desiredStatusCounts(properties.getAssignmentCount());
        Map<String, Integer> existing = new LinkedHashMap<>();
        existing.put("SUBMITTING", 0);
        existing.put("REVIEWING", 0);
        existing.put("CLOSED", 0);
        for (CourseSeed seed : courseSeeds) {
            for (AssignmentEntity assignment : assignmentMapper.findByCourseId(seed.course.id)) {
                existing.computeIfPresent(assignment.status, (key, value) -> value + 1);
            }
        }
        Map<String, Integer> remaining = new LinkedHashMap<>();
        desired.forEach((status, count) -> remaining.put(status, Math.max(0, count - existing.getOrDefault(status, 0))));
        rebalanceCounts(remaining, newAssignmentsNeeded, List.of("SUBMITTING", "REVIEWING", "CLOSED"));
        return shufflePool(remaining, random);
    }

    private List<String> buildModePool(List<CourseSeed> courseSeeds, int newAssignmentsNeeded, Random random) {
        int desiredGroupCount = (int) Math.round(properties.getAssignmentCount() * (TARGET_GROUP_ASSIGNMENTS / 48D));
        int desiredIndividualCount = Math.max(properties.getAssignmentCount() - desiredGroupCount, 0);
        int existingGroupCount = 0;
        int existingIndividualCount = 0;
        for (CourseSeed seed : courseSeeds) {
            for (AssignmentEntity assignment : assignmentMapper.findByCourseId(seed.course.id)) {
                if ("GROUP".equals(assignment.mode)) {
                    existingGroupCount++;
                } else {
                    existingIndividualCount++;
                }
            }
        }
        Map<String, Integer> remaining = new LinkedHashMap<>();
        remaining.put("GROUP", Math.max(0, desiredGroupCount - existingGroupCount));
        remaining.put("INDIVIDUAL", Math.max(0, desiredIndividualCount - existingIndividualCount));
        rebalanceCounts(remaining, newAssignmentsNeeded, List.of("GROUP", "INDIVIDUAL"));
        return shufflePool(remaining, random);
    }

    private Map<String, Integer> desiredStatusCounts(int totalAssignments) {
        int closed = Math.min(properties.getClosedAssignmentCount(), totalAssignments);
        int reviewing = Math.min(properties.getReviewingAssignmentCount(), Math.max(totalAssignments - closed, 0));
        int submitting = Math.max(totalAssignments - closed - reviewing, 0);
        Map<String, Integer> desired = new LinkedHashMap<>();
        desired.put("SUBMITTING", submitting);
        desired.put("REVIEWING", reviewing);
        desired.put("CLOSED", closed);
        return desired;
    }

    private void rebalanceCounts(Map<String, Integer> counts, int targetTotal, List<String> preferredOrder) {
        int current = counts.values().stream().mapToInt(Integer::intValue).sum();
        int growIndex = 0;
        int shrinkIndex = preferredOrder.size() - 1;
        while (current < targetTotal) {
            String key = preferredOrder.get(growIndex % preferredOrder.size());
            counts.compute(key, (ignored, value) -> value == null ? 1 : value + 1);
            current++;
            growIndex++;
        }
        while (current > targetTotal) {
            String key = preferredOrder.get(Math.floorMod(shrinkIndex, preferredOrder.size()));
            Integer value = counts.getOrDefault(key, 0);
            if (value > 0) {
                counts.put(key, value - 1);
                current--;
            }
            shrinkIndex--;
        }
    }

    private List<String> shufflePool(Map<String, Integer> counts, Random random) {
        List<String> pool = new ArrayList<>();
        counts.forEach((value, count) -> {
            for (int index = 0; index < count; index++) {
                pool.add(value);
            }
        });
        Collections.shuffle(pool, random);
        return pool;
    }

    private String nextAssignmentTitle(String mode, int order, String courseName) {
        String template = "GROUP".equals(mode)
            ? GROUP_ASSIGNMENT_NAMES[order % GROUP_ASSIGNMENT_NAMES.length]
            : INDIVIDUAL_ASSIGNMENT_NAMES[order % INDIVIDUAL_ASSIGNMENT_NAMES.length];
        return BULK_ASSIGNMENT_TITLE_PREFIX + template + " / " + simplifyCourseName(courseName);
    }

    private String simplifyCourseName(String courseName) {
        return courseName.replace("课程设计", "").replace("专题", "").replace("实战", "").trim();
    }

    private String buildAssignmentDescription(String courseName, String mode, String status) {
        String modeLabel = "GROUP".equals(mode) ? "多成员协作" : "个人作品展示";
        String statusLabel = switch (status) {
            case "SUBMITTING" -> "提交期";
            case "REVIEWING" -> "互评期";
            case "CLOSED" -> "已发布最终成绩";
            default -> status;
        };
        return "面向 " + courseName + " 的 " + modeLabel + " 任务，当前用于覆盖 " + statusLabel + "、排行榜、统计和治理联调场景。";
    }

    private boolean resolveAllowLate(String status, int order) {
        if ("CLOSED".equals(status)) {
            return order % 2 == 0;
        }
        return order % 3 != 0;
    }

    private LocalDateTime buildDeadline(String status, int order) {
        LocalDateTime anchor = referenceTime();
        return switch (status) {
            case "SUBMITTING" -> {
                if (order < MIN_DUE_SOON_SUBMITTING_ASSIGNMENTS) {
                    yield anchor.plusHours(18L + order * 9L).withMinute(0).withSecond(0).withNano(0);
                }
                long daysOffset = 4L + (order - MIN_DUE_SOON_SUBMITTING_ASSIGNMENTS) % 11;
                yield anchor.plusDays(daysOffset).withHour(23).withMinute(59).withSecond(0).withNano(0);
            }
            case "REVIEWING" -> anchor.minusDays(2L + order % 5).withHour(23).withMinute(59).withSecond(0).withNano(0);
            case "CLOSED" -> anchor.minusDays(6L + order % 4).withHour(23).withMinute(59).withSecond(0).withNano(0);
            default -> anchor.plusDays(5L).withHour(23).withMinute(59).withSecond(0).withNano(0);
        };
    }

    private LocalDateTime referenceTime() {
        return LocalDateTime.now()
            .withMinute(0)
            .withSecond(0)
            .withNano(0);
    }

    private int[] weightProfileFor(String mode, int order) {
        List<int[]> groupProfiles = List.of(new int[] {40, 60}, new int[] {50, 50}, new int[] {30, 70});
        List<int[]> individualProfiles = List.of(new int[] {30, 70}, new int[] {40, 60}, new int[] {20, 80});
        return ("GROUP".equals(mode) ? groupProfiles : individualProfiles).get(order % 3);
    }

    private List<RubricItemEntity> persistRubric(AssignmentEntity assignment, int order) {
        RubricEntity rubric = new RubricEntity();
        rubric.assignmentId = assignment.id;
        rubric.versionNo = 1;
        rubric.active = true;
        assignmentMapper.insertRubric(rubric);

        List<RubricTemplate> templates = selectRubricTemplates(assignment.mode, order);
        List<RubricItemEntity> items = new ArrayList<>();
        for (RubricTemplate template : templates) {
            RubricItemEntity item = new RubricItemEntity();
            item.rubricId = rubric.id;
            item.itemName = template.name;
            item.description = template.description;
            item.weight = template.weight;
            items.add(item);
        }
        assignmentMapper.insertRubricItems(items);
        return assignmentMapper.findRubricItemsByAssignmentId(assignment.id);
    }

    private List<RubricTemplate> selectRubricTemplates(String mode, int order) {
        List<List<RubricTemplate>> profiles = "GROUP".equals(mode)
            ? List.of(GROUP_RUBRIC_A, GROUP_RUBRIC_B, GROUP_RUBRIC_C)
            : List.of(INDIVIDUAL_RUBRIC_A, INDIVIDUAL_RUBRIC_B, INDIVIDUAL_RUBRIC_C);
        return profiles.get(order % profiles.size());
    }

    private void seedSubmissions(List<GeneratedAssignment> assignments, Map<Long, UserEntity> usersById) {
        for (GeneratedAssignment generated : assignments) {
            if ("GROUP".equals(generated.assignment.mode)) {
                seedGroupAssignmentSubmissions(generated);
            } else {
                seedIndividualAssignmentSubmissions(generated, usersById);
            }
        }
    }

    private void seedGroupAssignmentSubmissions(GeneratedAssignment generated) {
        List<Long> orderedStudents = rotateList(generated.courseSeed.studentIds, generated.order * 2);
        List<Integer> layout = groupLayoutFor(generated.assignment.status, generated.order);
        int consumed = layout.stream().mapToInt(Integer::intValue).sum();
        List<Long> groupedStudents = orderedStudents.subList(0, Math.min(consumed, orderedStudents.size()));
        int cursor = 0;
        List<GroupBundle> groupBundles = new ArrayList<>();
        for (int groupIndex = 0; groupIndex < layout.size(); groupIndex++) {
            int memberCount = layout.get(groupIndex);
            List<Long> memberIds = groupedStudents.subList(cursor, cursor + memberCount);
            cursor += memberCount;
            String stem = nextProjectStem(generated, groupIndex);

            com.demo.courseplatform.domain.entity.PersistenceModels.AssignmentGroupEntity group =
                new com.demo.courseplatform.domain.entity.PersistenceModels.AssignmentGroupEntity();
            group.assignmentId = generated.assignment.id;
            group.groupName = stem;
            submissionMapper.insertGroup(group);

            List<com.demo.courseplatform.domain.entity.PersistenceModels.AssignmentGroupMemberEntity> members = memberIds.stream()
                .map(memberId -> {
                    com.demo.courseplatform.domain.entity.PersistenceModels.AssignmentGroupMemberEntity entity =
                        new com.demo.courseplatform.domain.entity.PersistenceModels.AssignmentGroupMemberEntity();
                    entity.assignmentId = generated.assignment.id;
                    entity.groupId = group.id;
                    entity.userId = memberId;
                    return entity;
                })
                .toList();
            submissionMapper.insertGroupMembers(members);
            groupBundles.add(new GroupBundle(groupIndex, group.id, stem, List.copyOf(memberIds)));
        }

        int submittedCount = submittedGroupCount(generated.assignment.status, groupBundles.size());
        for (int index = 0; index < submittedCount; index++) {
            GroupBundle bundle = groupBundles.get(index);
            SubmissionEntity submission = new SubmissionEntity();
            submission.assignmentId = generated.assignment.id;
            submission.groupId = bundle.groupId;
            submission.projectName = BULK_PROJECT_NAME_PREFIX + bundle.groupName;
            submission.repoUrl = buildRepoUrl(generated, index);
            submission.videoUrl = buildAssetUrl("videos", generated, index);
            submission.previewUrl = buildAssetUrl("preview", generated, index);
            submission.docUrl = buildAssetUrl("docs", generated, index);
            submission.attachmentUrl = buildAssetUrl("files", generated, index);
            submission.description = "围绕 " + generated.assignment.title + " 构建的多成员协作项目，用于覆盖提交、互评与治理联调。";
            submission.submittedBy = bundle.memberIds.get(0);
            submission.late = isLateSubmission(generated.assignment, index);
            submission.submittedAt = buildSubmittedAt(generated.assignment, index, submission.late);
            submissionMapper.insertSubmission(submission);
            generated.submissions.add(new SubmissionBundle(index, submission, bundle.memberIds));
        }
    }

    private void seedIndividualAssignmentSubmissions(GeneratedAssignment generated, Map<Long, UserEntity> usersById) {
        List<Long> orderedStudents = rotateList(generated.courseSeed.studentIds, generated.order * 3);
        int submittedCount = switch (generated.assignment.status) {
            case "SUBMITTING" -> Math.min(orderedStudents.size(), 12 + generated.order % 2);
            case "REVIEWING" -> Math.min(orderedStudents.size(), 16 + generated.order % 2);
            case "CLOSED" -> orderedStudents.size();
            default -> orderedStudents.size();
        };
        for (int index = 0; index < submittedCount; index++) {
            Long studentId = orderedStudents.get(index);
            UserEntity student = usersById.get(studentId);
            com.demo.courseplatform.domain.entity.PersistenceModels.AssignmentGroupEntity group =
                new com.demo.courseplatform.domain.entity.PersistenceModels.AssignmentGroupEntity();
            group.assignmentId = generated.assignment.id;
            group.groupName = (student == null ? "个人组" : student.displayName + "个人组");
            submissionMapper.insertGroup(group);

            com.demo.courseplatform.domain.entity.PersistenceModels.AssignmentGroupMemberEntity member =
                new com.demo.courseplatform.domain.entity.PersistenceModels.AssignmentGroupMemberEntity();
            member.assignmentId = generated.assignment.id;
            member.groupId = group.id;
            member.userId = studentId;
            submissionMapper.insertGroupMembers(List.of(member));

            SubmissionEntity submission = new SubmissionEntity();
            submission.assignmentId = generated.assignment.id;
            submission.groupId = group.id;
            submission.projectName = BULK_PROJECT_NAME_PREFIX + nextProjectStem(generated, index);
            submission.repoUrl = buildRepoUrl(generated, index);
            submission.videoUrl = buildAssetUrl("videos", generated, index);
            submission.previewUrl = buildAssetUrl("preview", generated, index);
            submission.docUrl = buildAssetUrl("docs", generated, index);
            submission.attachmentUrl = buildAssetUrl("files", generated, index);
            submission.description = "围绕 " + generated.assignment.title + " 产出的个人作品页，用于覆盖个人提交、互评和结果看板场景。";
            submission.submittedBy = studentId;
            submission.late = isLateSubmission(generated.assignment, index);
            submission.submittedAt = buildSubmittedAt(generated.assignment, index, submission.late);
            submissionMapper.insertSubmission(submission);
            generated.submissions.add(new SubmissionBundle(index, submission, List.of(studentId)));
        }
    }

    private List<Integer> groupLayoutFor(String status, int order) {
        if ("SUBMITTING".equals(status) && order % 2 == 0) {
            return List.of(4, 3, 3, 3, 3);
        }
        if (order % 3 == 0) {
            return List.of(3, 3, 3, 3, 3, 3);
        }
        return List.of(4, 4, 4, 3, 3);
    }

    private int submittedGroupCount(String status, int totalGroups) {
        if ("SUBMITTING".equals(status)) {
            return Math.max(3, totalGroups - 1);
        }
        return totalGroups;
    }

    private String nextProjectStem(GeneratedAssignment generated, int index) {
        String template = PROJECT_NAME_TEMPLATES[(generated.order + index) % PROJECT_NAME_TEMPLATES.length];
        return template + " " + String.format(Locale.ROOT, "%02d-%02d", generated.courseSeed.order + 1, index + 1);
    }

    private String buildRepoUrl(GeneratedAssignment generated, int index) {
        return "https://github.com/demo/" + slugify(generated.assignment.title) + "-" + String.format(Locale.ROOT, "%02d", index + 1);
    }

    private String buildAssetUrl(String kind, GeneratedAssignment generated, int index) {
        return "https://example.com/" + kind + "/" + slugify(generated.assignment.title) + "/" + (index + 1);
    }

    private String slugify(String value) {
        String normalized = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        return normalized.replaceAll("(^-+|-+$)", "");
    }

    private boolean isLateSubmission(AssignmentEntity assignment, int ordinal) {
        return assignment.allowLate
            && !"SUBMITTING".equals(assignment.status)
            && (ordinal + assignment.id) % 6 == 0;
    }

    private LocalDateTime buildSubmittedAt(AssignmentEntity assignment, int ordinal, boolean late) {
        if (late) {
            return assignment.deadline.plusHours(6L + ordinal % 12);
        }
        return assignment.deadline.minusDays(1L + ordinal % 3)
            .withHour(18 - ordinal % 3)
            .withMinute((ordinal * 7) % 60)
            .withSecond(0)
            .withNano(0);
    }

    private void seedBlacklists(List<GeneratedAssignment> assignments) {
        int reviewableAssignmentsWithBlacklist = 0;
        for (GeneratedAssignment generated : assignments) {
            if (!isReviewable(generated.assignment.status) || generated.submissions.isEmpty()) {
                continue;
            }
            if (reviewableAssignmentsWithBlacklist >= 9) {
                break;
            }
            SubmissionBundle target = generated.submissions.get(reviewableAssignmentsWithBlacklist % generated.submissions.size());
            LinkedHashSet<Long> candidates = new LinkedHashSet<>(generated.courseSeed.studentIds);
            candidates.removeAll(target.memberIds);
            if (candidates.isEmpty()) {
                continue;
            }
            Long evaluatorId = candidates.iterator().next();
            com.demo.courseplatform.domain.entity.PersistenceModels.EvaluationBlacklistEntity blacklist =
                new com.demo.courseplatform.domain.entity.PersistenceModels.EvaluationBlacklistEntity();
            blacklist.assignmentId = generated.assignment.id;
            blacklist.evaluatorUserId = evaluatorId;
            blacklist.targetSubmissionId = target.submission.id;
            evaluationMapper.insertBlacklist(blacklist);
            generated.blacklistedEvaluatorsBySubmissionId
                .computeIfAbsent(target.submission.id, ignored -> new LinkedHashSet<>())
                .add(evaluatorId);
            reviewableAssignmentsWithBlacklist++;
        }
    }

    private void seedEvaluations(List<GeneratedAssignment> assignments) {
        int reviewableAssignmentIndex = 0;
        for (GeneratedAssignment generated : assignments) {
            if (!isReviewable(generated.assignment.status) || generated.submissions.isEmpty()) {
                continue;
            }
            AnomalyProfile profile = anomalyProfileFor(reviewableAssignmentIndex);
            for (SubmissionBundle submissionBundle : generated.submissions) {
                double baseScore = baseSubmissionScore(generated, submissionBundle.ordinal);
                List<Long> reviewerIds = pickStudentReviewers(generated, submissionBundle, generated.studentReviewTarget());
                List<AnomalyPlan> anomalyPlans = profile.plansForSubmission(submissionBundle.ordinal, reviewerIds.size());
                Map<Integer, AnomalyPlan> anomalyByReviewerIndex = anomalyPlans.stream()
                    .collect(Collectors.toMap(plan -> plan.reviewerOrdinal, plan -> plan, (left, right) -> left, LinkedHashMap::new));

                for (int reviewerIndex = 0; reviewerIndex < reviewerIds.size(); reviewerIndex++) {
                    Long reviewerId = reviewerIds.get(reviewerIndex);
                    AnomalyPlan anomaly = anomalyByReviewerIndex.get(reviewerIndex);
                    double totalScore = anomaly == null
                        ? normalStudentScore(baseScore, reviewerIndex, generated.order)
                        : anomaly.totalScore;
                    com.demo.courseplatform.domain.entity.PersistenceModels.EvaluationEntity evaluation =
                        new com.demo.courseplatform.domain.entity.PersistenceModels.EvaluationEntity();
                    evaluation.assignmentId = generated.assignment.id;
                    evaluation.submissionId = submissionBundle.submission.id;
                    evaluation.evaluatorUserId = reviewerId;
                    evaluation.evaluatorRole = "STUDENT";
                    evaluation.totalScore = decimal(totalScore);
                    evaluation.comment = anomaly == null
                        ? studentComment(totalScore, reviewerIndex)
                        : studentComment(totalScore, reviewerIndex) + "（系统已标记为异常样本）";
                    evaluation.abnormal = anomaly != null;
                    evaluation.abnormalReason = anomaly == null ? null : buildAbnormalReason(totalScore, baseScore);
                    evaluation.excluded = anomaly != null && anomaly.excluded;
                    evaluation.reviewStatus = anomaly == null ? "PENDING" : anomaly.reviewStatus;
                    evaluationMapper.insert(evaluation);
                    evaluationMapper.insertItems(buildEvaluationItems(evaluation.id, generated.rubricItems, totalScore));
                    generated.generatedEvaluationCount++;
                }

                List<Long> teacherReviewers = rotateList(generated.courseSeed.teacherIds, generated.order + submissionBundle.ordinal)
                    .stream()
                    .limit(generated.teacherReviewTarget())
                    .toList();
                for (int teacherIndex = 0; teacherIndex < teacherReviewers.size(); teacherIndex++) {
                    double teacherScore = normalTeacherScore(baseScore, teacherIndex);
                    com.demo.courseplatform.domain.entity.PersistenceModels.EvaluationEntity evaluation =
                        new com.demo.courseplatform.domain.entity.PersistenceModels.EvaluationEntity();
                    evaluation.assignmentId = generated.assignment.id;
                    evaluation.submissionId = submissionBundle.submission.id;
                    evaluation.evaluatorUserId = teacherReviewers.get(teacherIndex);
                    evaluation.evaluatorRole = "TEACHER";
                    evaluation.totalScore = decimal(teacherScore);
                    evaluation.comment = teacherComment(teacherScore);
                    evaluation.abnormal = false;
                    evaluation.abnormalReason = null;
                    evaluation.excluded = false;
                    evaluation.reviewStatus = "PENDING";
                    evaluationMapper.insert(evaluation);
                    evaluationMapper.insertItems(buildEvaluationItems(evaluation.id, generated.rubricItems, teacherScore));
                    generated.generatedEvaluationCount++;
                }
            }
            reviewableAssignmentIndex++;
        }
    }

    private boolean isReviewable(String status) {
        return "REVIEWING".equals(status) || "CLOSED".equals(status);
    }

    private List<Long> pickStudentReviewers(GeneratedAssignment generated, SubmissionBundle submissionBundle, int targetCount) {
        Set<Long> blockedEvaluators = generated.blacklistedEvaluatorsBySubmissionId
            .getOrDefault(submissionBundle.submission.id, Set.of());
        return rotateList(generated.courseSeed.studentIds, generated.order + submissionBundle.ordinal)
            .stream()
            .filter(studentId -> !submissionBundle.memberIds.contains(studentId))
            .filter(studentId -> !blockedEvaluators.contains(studentId))
            .limit(targetCount)
            .toList();
    }

    private double baseSubmissionScore(GeneratedAssignment generated, int submissionOrdinal) {
        double base = 72D + ((generated.order * 5 + submissionOrdinal * 3) % 17);
        if ("CLOSED".equals(generated.assignment.status)) {
            base += 2D;
        }
        if ("GROUP".equals(generated.assignment.mode)) {
            base += 1.5D;
        }
        return clamp(base, 65D, 96D);
    }

    private double normalStudentScore(double baseScore, int reviewerIndex, int assignmentOrder) {
        double[] offsets = {-3D, -2D, -1D, 0D, 1D, 2D, 3D, 0.5D};
        return clamp(baseScore + offsets[(assignmentOrder + reviewerIndex) % offsets.length], 45D, 99D);
    }

    private double normalTeacherScore(double baseScore, int teacherIndex) {
        double[] offsets = {1.5D, 3D};
        return clamp(baseScore + offsets[teacherIndex % offsets.length], 50D, 99D);
    }

    private String studentComment(double totalScore, int index) {
        return commentOpener(totalScore, index)
            + COMMENT_TEMPLATES[index % COMMENT_TEMPLATES.length]
            + COMMENT_TRAILERS[(index + (int) Math.round(totalScore)) % COMMENT_TRAILERS.length]
            + " 综合分约 " + decimal(totalScore) + "。";
    }

    private String teacherComment(double totalScore) {
        int scoreBucket = Math.floorMod((int) Math.round(totalScore), TEACHER_COMMENT_PREFIXES.length);
        return TEACHER_COMMENT_PREFIXES[scoreBucket]
            + " 建议继续围绕边界质量、表达节奏和结果闭环做下一轮优化。"
            + " 评分：" + decimal(totalScore) + "。";
    }

    private String commentOpener(double totalScore, int index) {
        String[] source = totalScore >= 88D
            ? HIGH_SCORE_OPENERS
            : (totalScore >= 74D ? MID_SCORE_OPENERS : LOW_SCORE_OPENERS);
        return source[index % source.length] + " ";
    }

    private List<com.demo.courseplatform.domain.entity.PersistenceModels.EvaluationItemEntity> buildEvaluationItems(Long evaluationId,
                                                                                                                     List<RubricItemEntity> rubricItems,
                                                                                                                     double totalScore) {
        double itemScore = clamp(totalScore / 10D, 0D, 10D);
        List<com.demo.courseplatform.domain.entity.PersistenceModels.EvaluationItemEntity> items = new ArrayList<>();
        for (int index = 0; index < rubricItems.size(); index++) {
            RubricItemEntity rubricItem = rubricItems.get(index);
            com.demo.courseplatform.domain.entity.PersistenceModels.EvaluationItemEntity item =
                new com.demo.courseplatform.domain.entity.PersistenceModels.EvaluationItemEntity();
            item.evaluationId = evaluationId;
            item.rubricItemId = rubricItem.id;
            item.score = decimal(itemScore);
            item.comment = ITEM_COMMENT_TEMPLATES[index % ITEM_COMMENT_TEMPLATES.length];
            items.add(item);
        }
        return items;
    }

    private String buildAbnormalReason(double abnormalScore, double baseScore) {
        double gap = round(Math.abs(abnormalScore - baseScore));
        return "与其他学生评分均值偏差 " + gap + " 分，与其他学生评分中位数偏差 " + gap + " 分";
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private long countExistingSubmissions() {
        return BASE_COURSE_IDS.stream()
            .mapToLong(courseId -> assignmentMapper.findByCourseId(courseId).stream()
                .filter(assignment -> !assignment.title.startsWith(BULK_ASSIGNMENT_TITLE_PREFIX))
                .mapToLong(assignment -> submissionMapper.findSubmissionsByAssignmentId(assignment.id).size())
                .sum())
            .sum();
    }

    private long countExistingEvaluations() {
        return BASE_COURSE_IDS.stream()
            .flatMap(courseId -> assignmentMapper.findByCourseId(courseId).stream())
            .filter(assignment -> !assignment.title.startsWith(BULK_ASSIGNMENT_TITLE_PREFIX))
            .mapToLong(assignment -> evaluationMapper.findByAssignmentId(assignment.id, null, null, null, false).size())
            .sum();
    }

    private <T> List<T> rotateList(List<T> source, int offset) {
        if (source.isEmpty()) {
            return List.of();
        }
        List<T> rotated = new ArrayList<>(source.size());
        int start = Math.floorMod(offset, source.size());
        for (int index = 0; index < source.size(); index++) {
            rotated.add(source.get((start + index) % source.size()));
        }
        return rotated;
    }

    private AnomalyProfile anomalyProfileFor(int reviewableAssignmentIndex) {
        int abnormalQuota = reviewableAssignmentIndex < 10 ? 5 : 4;
        if (reviewableAssignmentIndex < 5) {
            return new AnomalyProfile(AnomalyType.IGNORED_IMPACT, abnormalQuota);
        }
        if (reviewableAssignmentIndex < 10) {
            return new AnomalyProfile(AnomalyType.RESTORED_ONLY, abnormalQuota);
        }
        return new AnomalyProfile(AnomalyType.PENDING_ONLY, abnormalQuota);
    }

    private record RubricTemplate(String name, String description, int weight) {
    }

    private record CourseSeed(int order, CourseEntity course, List<Long> teacherIds, List<Long> studentIds) {
    }

    private record SubmissionBundle(int ordinal, SubmissionEntity submission, List<Long> memberIds) {
    }

    private record GroupBundle(int ordinal, Long groupId, String groupName, List<Long> memberIds) {
    }

    private record AnomalyPlan(int submissionOrdinal, int reviewerOrdinal, double totalScore,
                               boolean excluded, String reviewStatus) {
    }

    private enum AnomalyType {
        IGNORED_IMPACT,
        RESTORED_ONLY,
        PENDING_ONLY
    }

    private static final class AnomalyProfile {
        private final AnomalyType type;
        private final int abnormalQuota;

        private AnomalyProfile(AnomalyType type, int abnormalQuota) {
            this.type = type;
            this.abnormalQuota = abnormalQuota;
        }

        private List<AnomalyPlan> plansForSubmission(int submissionOrdinal, int reviewerCount) {
            if (reviewerCount == 0) {
                return List.of();
            }
            List<AnomalyPlan> plans = new ArrayList<>();
            if (submissionOrdinal == 0 && type == AnomalyType.IGNORED_IMPACT && reviewerCount >= 2) {
                plans.add(new AnomalyPlan(0, 0, 50D, true, "IGNORED"));
                plans.add(new AnomalyPlan(0, 1, 52D, true, "IGNORED"));
            }
            if (submissionOrdinal == 0 && type == AnomalyType.RESTORED_ONLY && reviewerCount >= 2) {
                plans.add(new AnomalyPlan(0, 0, 95D, false, "RESTORED"));
                plans.add(new AnomalyPlan(0, 1, 93D, false, "RESTORED"));
            }

            int seededCount = plans.size();
            int remaining = abnormalQuota - seededCount;
            if (remaining <= 0) {
                return plans;
            }

            int startReviewer = seededCount == 0 ? 0 : 2;
            for (int index = 0; index < remaining && startReviewer + index < reviewerCount; index++) {
                int reviewerOrdinal = startReviewer + index;
                double score = ((submissionOrdinal + reviewerOrdinal) % 2 == 0) ? 96D : 58D;
                plans.add(new AnomalyPlan(submissionOrdinal, reviewerOrdinal, score, false, "PENDING"));
            }
            return plans;
        }
    }

    private static final class GeneratedAssignment {
        private final int order;
        private final CourseSeed courseSeed;
        private final AssignmentEntity assignment;
        private final List<RubricItemEntity> rubricItems;
        private final int studentReviewTarget;
        private final int teacherReviewTarget;
        private final List<SubmissionBundle> submissions = new ArrayList<>();
        private final Map<Long, Set<Long>> blacklistedEvaluatorsBySubmissionId = new LinkedHashMap<>();
        private long generatedEvaluationCount = 0;

        private GeneratedAssignment(int order,
                                    CourseSeed courseSeed,
                                    AssignmentEntity assignment,
                                    List<RubricItemEntity> rubricItems,
                                    int studentReviewTarget,
                                    int teacherReviewTarget) {
            this.order = order;
            this.courseSeed = courseSeed;
            this.assignment = assignment;
            this.rubricItems = rubricItems;
            this.studentReviewTarget = studentReviewTarget;
            this.teacherReviewTarget = teacherReviewTarget;
        }

        private int studentReviewTarget() {
            return studentReviewTarget;
        }

        private int teacherReviewTarget() {
            return teacherReviewTarget;
        }
    }
}
