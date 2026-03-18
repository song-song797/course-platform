package com.demo.courseplatform.service;

import com.demo.courseplatform.common.ForbiddenException;
import com.demo.courseplatform.domain.dto.DemoRequests;
import com.demo.courseplatform.domain.entity.PersistenceModels.AssignmentEntity;
import com.demo.courseplatform.domain.entity.PersistenceModels.AssignmentGroupEntity;
import com.demo.courseplatform.domain.entity.PersistenceModels.AssignmentGroupMemberEntity;
import com.demo.courseplatform.domain.entity.PersistenceModels.CourseEntity;
import com.demo.courseplatform.domain.entity.PersistenceModels.CourseMemberEntity;
import com.demo.courseplatform.domain.entity.PersistenceModels.EvaluationBlacklistEntity;
import com.demo.courseplatform.domain.entity.PersistenceModels.EvaluationEntity;
import com.demo.courseplatform.domain.entity.PersistenceModels.EvaluationItemEntity;
import com.demo.courseplatform.domain.entity.PersistenceModels.RubricEntity;
import com.demo.courseplatform.domain.entity.PersistenceModels.RubricItemEntity;
import com.demo.courseplatform.domain.entity.PersistenceModels.SubmissionEntity;
import com.demo.courseplatform.domain.entity.PersistenceModels.UserEntity;
import com.demo.courseplatform.domain.vo.DemoViews;
import com.demo.courseplatform.mapper.AssignmentMapper;
import com.demo.courseplatform.mapper.CourseMapper;
import com.demo.courseplatform.mapper.EvaluationMapper;
import com.demo.courseplatform.mapper.SubmissionMapper;
import com.demo.courseplatform.mapper.UserMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DemoPlatformService {

    private static final Logger log = LoggerFactory.getLogger(DemoPlatformService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String COURSE_EVENT_PREFIX = "[COURSE_EVENT]";
    private static final String REVIEW_STATUS_PENDING = "PENDING";
    private static final String REVIEW_STATUS_IGNORED = "IGNORED";
    private static final String REVIEW_STATUS_RESTORED = "RESTORED";

    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final AssignmentMapper assignmentMapper;
    private final SubmissionMapper submissionMapper;
    private final EvaluationMapper evaluationMapper;
    private final PasswordService passwordService;
    private final ScoreCalculator scoreCalculator;

    public DemoPlatformService(UserMapper userMapper,
                               CourseMapper courseMapper,
                               AssignmentMapper assignmentMapper,
                               SubmissionMapper submissionMapper,
                               EvaluationMapper evaluationMapper,
                               PasswordService passwordService,
                               ScoreCalculator scoreCalculator) {
        this.userMapper = userMapper;
        this.courseMapper = courseMapper;
        this.assignmentMapper = assignmentMapper;
        this.submissionMapper = submissionMapper;
        this.evaluationMapper = evaluationMapper;
        this.passwordService = passwordService;
        this.scoreCalculator = scoreCalculator;
    }

    public DemoViews.UserProfileVo authenticate(String username, String password) {
        UserEntity user = userMapper.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("账号不存在");
        }
        if (!passwordService.matches(password, user.passwordHash)) {
            throw new IllegalArgumentException("密码错误");
        }
        return toUserProfile(user);
    }

    public DemoViews.UserProfileVo getUserProfile(Long userId) {
        return toUserProfile(requireUser(userId));
    }

    @Transactional
    public void changePassword(Long userId, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("新密码不能为空");
        }
        UserEntity user = requireUser(userId);
        user.passwordHash = passwordService.encode(newPassword);
        user.firstLoginResetRequired = false;
        userMapper.updatePassword(user);
    }

    public List<DemoViews.CourseCardVo> getCoursesForUser(Long userId) {
        UserEntity user = requireUser(userId);
        if ("ADMIN".equals(user.role)) {
            return getAllCourses();
        }

        List<CourseEntity> courses = courseMapper.findByUserId(userId);
        Map<Long, CourseMemberEntity> myMemberships = courseMapper.findMembersByUserId(userId).stream()
            .collect(Collectors.toMap(item -> item.courseId, item -> item, (left, right) -> left, LinkedHashMap::new));
        return buildCourseCards(courses, course -> {
            CourseMemberEntity membership = myMemberships.get(course.id);
            return membership == null ? user.role : membership.courseRole;
        });
    }

    public List<DemoViews.CourseCardVo> getAllCourses() {
        return buildCourseCards(courseMapper.findAll(), course -> "ADMIN");
    }

    public DemoViews.StudentHomeVo getStudentHome(Long userId) {
        UserEntity user = requireUser(userId);
        if (!"STUDENT".equals(user.role)) {
            throw new ForbiddenException("仅学生可访问学生大厅");
        }

        List<CourseEntity> courses = courseMapper.findByUserId(userId);
        if (courses.isEmpty()) {
            return new DemoViews.StudentHomeVo(
                new DemoViews.StudentOverviewVo(0, 0, 0, 0, 0, 0),
                List.of(),
                List.of(),
                List.of(),
                List.of()
            );
        }

        Map<Long, CourseEntity> courseById = courses.stream()
            .collect(Collectors.toMap(item -> item.id, item -> item, (left, right) -> left, LinkedHashMap::new));
        List<AssignmentEntity> assignments = assignmentMapper.findByCourseIds(courses.stream().map(item -> item.id).toList()).stream()
            .sorted(Comparator.comparing(item -> item.deadline))
            .toList();

        List<DemoViews.StudentTaskCardVo> dueSoonTasks = new ArrayList<>();
        List<DemoViews.StudentTaskCardVo> pendingTasks = new ArrayList<>();
        List<DemoViews.StudentTaskCardVo> reviewTasks = new ArrayList<>();
        List<HomeResultTask> resultTaskCandidates = new ArrayList<>();
        List<HomeReviewHighlight> reviewHighlights = new ArrayList<>();
        List<HomeResultHighlight> resultHighlights = new ArrayList<>();

        int pendingSubmissionCount = 0;
        int reviewingAssignmentCount = 0;
        int publishedResultCount = 0;
        int availableReviewCount = 0;

        for (AssignmentEntity assignment : assignments) {
            CourseEntity course = courseById.get(assignment.courseId);
            SubmissionEntity mySubmission = submissionMapper.findSubmissionByAssignmentAndUser(assignment.id, userId);
            String displayStatus = resolveDisplayStatus(assignment);
            String deadline = formatDateTime(assignment.deadline);

            if ("SUBMITTING".equals(assignment.status) && mySubmission == null) {
                pendingSubmissionCount++;
                DemoViews.StudentTaskCardVo task = new DemoViews.StudentTaskCardVo(
                    assignment.id,
                    assignment.courseId,
                    course == null ? "未知课程" : course.name,
                    assignment.title,
                    assignment.mode,
                    displayStatus,
                    deadline,
                    isDueSoon(assignment.deadline) ? "DUE_SOON" : "TODO_SUBMIT",
                    "去提交"
                );
                if (isDueSoon(assignment.deadline)) {
                    dueSoonTasks.add(task);
                } else {
                    pendingTasks.add(task);
                }
            }

            if ("REVIEWING".equals(assignment.status)) {
                reviewingAssignmentCount++;
                List<DemoViews.ProjectCardVo> projects = getProjects(userId, assignment.id);
                int reviewableProjects = (int) projects.stream().filter(DemoViews.ProjectCardVo::canEvaluate).count();
                availableReviewCount += reviewableProjects;
                if (!projects.isEmpty()) {
                    reviewHighlights.add(new HomeReviewHighlight(
                        reviewableProjects,
                        projects.size(),
                        assignment.deadline,
                        new DemoViews.StudentReviewHighlightVo(
                            assignment.id,
                            course == null ? "未知课程" : course.name,
                            assignment.title,
                            projects.size(),
                            reviewableProjects,
                            isResultsPublished(assignment) ? "FINAL" : "REALTIME",
                            displayStatus
                        )
                    ));
                }
                if (reviewableProjects > 0) {
                    reviewTasks.add(new DemoViews.StudentTaskCardVo(
                        assignment.id,
                        assignment.courseId,
                        course == null ? "未知课程" : course.name,
                        assignment.title,
                        assignment.mode,
                        displayStatus,
                        deadline,
                        "GO_REVIEW",
                        "去互评"
                    ));
                }
            }

            if (isResultsPublished(assignment)) {
                publishedResultCount++;
                List<SubmissionEntity> submissions = submissionMapper.findSubmissionsByAssignmentId(assignment.id);
                AssignmentScoreSnapshot snapshot = buildAssignmentScoreSnapshot(assignment, submissions);
                Integer currentRank = null;
                Double finalScore = null;
                if (mySubmission != null) {
                    DemoViews.SubmissionSummaryVo summary = buildSubmissionSummary(assignment, mySubmission.id, snapshot);
                    currentRank = summary.currentRank();
                    finalScore = summary.finalScore();
                }
                resultHighlights.add(new HomeResultHighlight(
                    resolvePublishedAt(assignment),
                    new DemoViews.StudentResultHighlightVo(
                        assignment.id,
                        course == null ? "未知课程" : course.name,
                        assignment.title,
                        formatDateTime(resolvePublishedAt(assignment)),
                        currentRank,
                        finalScore
                    )
                ));
                resultTaskCandidates.add(new HomeResultTask(
                    resolvePublishedAt(assignment),
                    new DemoViews.StudentTaskCardVo(
                        assignment.id,
                        assignment.courseId,
                        course == null ? "未知课程" : course.name,
                        assignment.title,
                        assignment.mode,
                        "最近放榜",
                        formatDateTime(resolvePublishedAt(assignment)),
                        "RESULT_AVAILABLE",
                        "看结果"
                    )
                ));
            }
        }

        List<DemoViews.StudentResultHighlightVo> topResultHighlights = resultHighlights.stream()
            .sorted(Comparator.comparing(HomeResultHighlight::publishedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(6)
            .map(HomeResultHighlight::card)
            .toList();
        List<DemoViews.StudentTaskCardVo> resultTasks = resultTaskCandidates.stream()
            .sorted(Comparator.comparing(HomeResultTask::publishedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(6)
            .map(HomeResultTask::card)
            .toList();

        List<DemoViews.StudentTaskCardVo> taskQueue = new ArrayList<>();
        interleaveTasks(taskQueue, dueSoonTasks, reviewTasks, resultTasks, pendingTasks, 12);

        List<DemoViews.StudentReviewHighlightVo> topReviewHighlights = reviewHighlights.stream()
            .sorted(Comparator.comparing(HomeReviewHighlight::reviewableProjects, Comparator.reverseOrder())
                .thenComparing(HomeReviewHighlight::totalProjects, Comparator.reverseOrder())
                .thenComparing(HomeReviewHighlight::deadline))
            .limit(6)
            .map(HomeReviewHighlight::card)
            .toList();

        DemoViews.StudentOverviewVo overview = new DemoViews.StudentOverviewVo(
            courses.size(),
            assignments.size(),
            pendingSubmissionCount,
            reviewingAssignmentCount,
            publishedResultCount,
            availableReviewCount
        );

        return new DemoViews.StudentHomeVo(
            overview,
            taskQueue,
            topReviewHighlights,
            topResultHighlights,
            buildActivityBanners(overview, topReviewHighlights, topResultHighlights)
        );
    }

    @Transactional
    public DemoViews.CourseCardVo createCourse(DemoRequests.CreateCourseRequest request) {
        CourseEntity course = new CourseEntity();
        course.code = request.code().trim();
        course.name = request.name().trim();
        course.term = blankToDefault(request.term(), "2026 春");
        courseMapper.insert(course);
        return new DemoViews.CourseCardVo(course.id, course.code, course.name, course.term, "ADMIN", 0, List.of());
    }

    public DemoViews.CourseMemberManageVo getCourseMemberManage(Long courseId) {
        requireCourse(courseId);
        List<CourseMemberEntity> existingMembers = courseMapper.findMembersByCourseId(courseId);
        Map<Long, UserEntity> usersById = loadUsers(existingMembers.stream().map(item -> item.userId).toList());

        List<DemoViews.CourseMemberVo> members = existingMembers.stream()
            .map(item -> toCourseMemberVo(usersById.get(item.userId), item.courseRole))
            .filter(Objects::nonNull)
            .toList();

        Set<Long> teacherIds = existingMembers.stream()
            .filter(item -> "TEACHER".equals(item.courseRole))
            .map(item -> item.userId)
            .collect(Collectors.toSet());
        Set<Long> studentIds = existingMembers.stream()
            .filter(item -> "STUDENT".equals(item.courseRole))
            .map(item -> item.userId)
            .collect(Collectors.toSet());

        List<DemoViews.CourseMemberVo> teacherCandidates = userMapper.findByRole("TEACHER").stream()
            .filter(user -> !teacherIds.contains(user.id))
            .map(user -> toCourseMemberVo(user, "TEACHER"))
            .toList();
        List<DemoViews.CourseMemberVo> studentCandidates = userMapper.findByRole("STUDENT").stream()
            .filter(user -> !studentIds.contains(user.id))
            .map(user -> toCourseMemberVo(user, "STUDENT"))
            .toList();

        return new DemoViews.CourseMemberManageVo(members, teacherCandidates, studentCandidates);
    }

    @Transactional
    public DemoViews.CourseMemberVo addCourseMember(Long courseId, DemoRequests.CourseMemberRequest request) {
        requireCourse(courseId);
        String courseRole = normalizeCourseRole(request.courseRole());
        UserEntity user = requireUser(request.userId());
        validateUserRoleForCourseMember(user, courseRole);
        if (courseMapper.findMember(courseId, user.id) != null) {
            throw new IllegalStateException("该成员已在当前课程中");
        }

        CourseMemberEntity member = new CourseMemberEntity();
        member.courseId = courseId;
        member.userId = user.id;
        member.courseRole = courseRole;
        courseMapper.insertMember(member);
        return toCourseMemberVo(user, courseRole);
    }

    @Transactional
    public Map<String, Object> removeCourseMember(Long courseId, Long userId) {
        requireCourse(courseId);
        CourseMemberEntity member = courseMapper.findMember(courseId, userId);
        if (member == null) {
            throw new IllegalArgumentException("课程成员不存在");
        }
        courseMapper.deleteMember(courseId, userId);
        return Map.of("removed", true);
    }

    @Transactional
    public Map<String, Object> importUsers(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择 CSV 文件");
        }

        List<Map<String, String>> items = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean headerSkipped = false;
            while ((line = reader.readLine()) != null) {
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }
                if (line.isBlank()) {
                    continue;
                }
                String[] cells = line.split(",");
                if (cells.length < 3) {
                    continue;
                }
                String username = cells[0].trim();
                String displayName = cells[1].trim();
                String role = cells[2].trim().toUpperCase(Locale.ROOT);
                if (username.isBlank() || displayName.isBlank() || !Set.of("ADMIN", "TEACHER", "STUDENT").contains(role)) {
                    continue;
                }

                UserEntity entity = new UserEntity();
                entity.username = username;
                entity.displayName = displayName;
                entity.role = role;
                entity.passwordHash = passwordService.encode(username);
                entity.firstLoginResetRequired = true;
                try {
                    userMapper.insert(entity);
                    items.add(Map.of("username", entity.username, "displayName", entity.displayName, "role", entity.role));
                } catch (DuplicateKeyException ignored) {
                    // Duplicate usernames are skipped to keep CSV import idempotent.
                }
            }
        }
        return Map.of("count", items.size(), "items", items);
    }

    public DemoViews.AssignmentDetailVo getStudentAssignmentDetail(Long userId, Long assignmentId) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        ensureStudentAssignmentAccess(userId, assignment);

        List<SubmissionEntity> submissions = submissionMapper.findSubmissionsByAssignmentId(assignmentId);
        AssignmentScoreSnapshot snapshot = buildAssignmentScoreSnapshot(assignment, submissions);
        SubmissionEntity submission = submissionMapper.findSubmissionByAssignmentAndUser(assignmentId, userId);
        DemoViews.SubmissionSummaryVo summary = submission == null ? null : buildSubmissionSummary(assignment, submission.id, snapshot);
        return toAssignmentDetail(assignment, summary, userId);
    }

    public DemoViews.AssignmentDetailVo getTeacherAssignmentDetail(Long teacherUserId, Long assignmentId) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        ensureTeacherAssignmentAccess(teacherUserId, assignment);
        return toAssignmentDetail(assignment, null, null);
    }

    @Transactional
    public DemoViews.AssignmentDetailVo createAssignment(Long courseId, DemoRequests.CreateAssignmentRequest request) {
        requireCourse(courseId);
        validateAssignmentWeights(request.peerWeight(), request.teacherWeight());

        AssignmentEntity assignment = new AssignmentEntity();
        assignment.courseId = courseId;
        assignment.title = request.title().trim();
        assignment.mode = normalizeMode(request.mode());
        assignment.description = blankToDefault(request.description(), "课程项目 demo 作业");
        assignment.deadline = parseDeadline(request.deadline(), LocalDateTime.now().plusDays(5));
        assignment.allowLate = request.allowLate();
        assignment.peerWeight = request.peerWeight();
        assignment.teacherWeight = request.teacherWeight();
        assignment.status = blankToDefault(request.status(), "SUBMITTING");
        assignment.resultsPublished = false;
        assignment.resultsPublishedAt = null;
        assignmentMapper.insert(assignment);

        persistRubric(assignment.id, defaultRubricItems());
        return toAssignmentDetail(assignment, null, null);
    }

    @Transactional
    public DemoViews.AssignmentDetailVo updateAssignment(Long assignmentId, DemoRequests.CreateAssignmentRequest request) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        validateAssignmentWeights(request.peerWeight(), request.teacherWeight());

        assignment.title = request.title().trim();
        assignment.mode = normalizeMode(request.mode());
        assignment.description = blankToDefault(request.description(), assignment.description);
        assignment.deadline = parseDeadline(request.deadline(), assignment.deadline);
        assignment.allowLate = request.allowLate();
        assignment.peerWeight = request.peerWeight();
        assignment.teacherWeight = request.teacherWeight();
        assignment.status = blankToDefault(request.status(), assignment.status);
        assignmentMapper.update(assignment);
        return toAssignmentDetail(assignment, null, null);
    }

    public List<DemoViews.SubmissionVo> getTeacherSubmissions(Long teacherUserId, Long assignmentId) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        ensureTeacherAssignmentAccess(teacherUserId, assignment);
        return buildSubmissionVos(submissionMapper.findSubmissionsByAssignmentId(assignmentId));
    }

    public DemoViews.AssignmentGroupManageVo getAssignmentGroups(Long teacherUserId, Long assignmentId) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        ensureTeacherGroupManageAccessible(teacherUserId, assignment);

        List<AssignmentGroupEntity> groups = submissionMapper.findGroupsByAssignmentId(assignmentId);
        List<AssignmentGroupMemberEntity> groupMembers = submissionMapper.findGroupMembersByAssignmentId(assignmentId);
        Map<Long, List<AssignmentGroupMemberEntity>> groupMembersByGroupId = groupMembers.stream()
            .collect(Collectors.groupingBy(item -> item.groupId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<DemoViews.MemberVo>> memberVosByGroupId = buildGroupMemberVosByGroupId(groups, groupMembersByGroupId);
        Map<Long, SubmissionEntity> submissionsByGroupId = groups.stream()
            .collect(Collectors.toMap(item -> item.id, item -> submissionMapper.findSubmissionByGroupId(item.id),
                (left, right) -> left, LinkedHashMap::new));

        Set<Long> groupedStudentIds = groupMembers.stream().map(item -> item.userId).collect(Collectors.toSet());
        List<DemoViews.MemberVo> ungroupedStudents = loadCourseStudentMemberVos(assignment.courseId).stream()
            .filter(item -> !groupedStudentIds.contains(item.id()))
            .toList();

        CourseEntity course = requireCourse(assignment.courseId);
        return new DemoViews.AssignmentGroupManageVo(
            assignment.id,
            assignment.title,
            assignment.status,
            resolveDisplayStatus(assignment),
            isResultsPublished(assignment),
            formatDateTime(resolvePublishedAt(assignment)),
            groups.stream()
                .map(group -> buildAssignmentGroupVo(group, memberVosByGroupId.getOrDefault(group.id, List.of()),
                    submissionsByGroupId.get(group.id)))
                .toList(),
            ungroupedStudents
        );
    }

    @Transactional
    public DemoViews.AssignmentGroupVo createAssignmentGroup(Long teacherUserId, Long assignmentId,
                                                             DemoRequests.UpsertAssignmentGroupRequest request) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        ensureTeacherGroupManageEditable(teacherUserId, assignment);
        List<Long> memberIds = normalizeDistinctMemberIds(request.memberUserIds());
        ensureMembersBelongToCourse(assignment.courseId, memberIds);
        ensureGroupMembersAvailable(assignmentId, memberIds, null);

        AssignmentGroupEntity group = new AssignmentGroupEntity();
        group.assignmentId = assignmentId;
        group.groupName = request.groupName().trim();
        submissionMapper.insertGroup(group);
        submissionMapper.insertGroupMembers(buildGroupMembers(assignmentId, group.id, memberIds));

        Map<Long, UserEntity> usersById = loadUsers(memberIds);
        return buildAssignmentGroupVo(group, memberIds.stream()
            .map(usersById::get)
            .filter(Objects::nonNull)
            .map(user -> new DemoViews.MemberVo(user.id, user.displayName, user.username))
            .toList(), null);
    }

    @Transactional
    public DemoViews.AssignmentGroupVo updateAssignmentGroup(Long teacherUserId, Long assignmentId, Long groupId,
                                                             DemoRequests.UpsertAssignmentGroupRequest request) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        ensureTeacherGroupManageEditable(teacherUserId, assignment);
        AssignmentGroupEntity group = requireAssignmentGroup(assignmentId, groupId);
        List<Long> memberIds = normalizeDistinctMemberIds(request.memberUserIds());
        ensureMembersBelongToCourse(assignment.courseId, memberIds);

        List<Long> existingMemberIds = submissionMapper.findGroupMembersByGroupIds(List.of(groupId)).stream()
            .map(item -> item.userId)
            .toList();
        if (isGroupMemberLocked(group.id) && !new LinkedHashSet<>(existingMemberIds).equals(new LinkedHashSet<>(memberIds))) {
            throw new IllegalStateException("该小组已有提交或评分，不能再调整组员");
        }

        ensureGroupMembersAvailable(assignmentId, memberIds, group.id);
        group.groupName = request.groupName().trim();
        submissionMapper.updateGroup(group);
        if (!isGroupMemberLocked(group.id)) {
            submissionMapper.deleteGroupMembersByGroupId(group.id);
            submissionMapper.insertGroupMembers(buildGroupMembers(assignmentId, group.id, memberIds));
        }

        Map<Long, UserEntity> usersById = loadUsers(memberIds);
        SubmissionEntity submission = submissionMapper.findSubmissionByGroupId(group.id);
        return buildAssignmentGroupVo(group, memberIds.stream()
            .map(usersById::get)
            .filter(Objects::nonNull)
            .map(user -> new DemoViews.MemberVo(user.id, user.displayName, user.username))
            .toList(), submission);
    }

    @Transactional
    public Map<String, Object> deleteAssignmentGroup(Long teacherUserId, Long assignmentId, Long groupId) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        ensureTeacherGroupManageEditable(teacherUserId, assignment);
        requireAssignmentGroup(assignmentId, groupId);
        if (isGroupMemberLocked(groupId)) {
            throw new IllegalStateException("该小组已有提交或评分，不能删除");
        }
        submissionMapper.deleteGroupMembersByGroupId(groupId);
        submissionMapper.deleteGroup(groupId);
        return Map.of("removed", true);
    }

    public DemoViews.SubmissionVo getMySubmission(Long userId, Long assignmentId) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        ensureStudentAssignmentAccess(userId, assignment);
        SubmissionEntity submission = submissionMapper.findSubmissionByAssignmentAndUser(assignmentId, userId);
        return submission == null ? null : buildSubmissionVos(List.of(submission)).get(0);
    }

    @Transactional
    public DemoViews.SubmissionVo upsertSubmission(Long userId, Long assignmentId, DemoRequests.UpsertSubmissionRequest request) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        ensureStudentAssignmentAccess(userId, assignment);
        ensureSubmissionEditable(assignment);
        validateSubmissionRequest(request);

        LocalDateTime now = LocalDateTime.now();
        boolean late = now.isAfter(assignment.deadline);
        if (late && !assignment.allowLate) {
            throw new IllegalStateException("当前作业已截止，不允许补交");
        }

        AssignmentGroupEntity group = resolveSubmissionGroup(userId, assignment);
        SubmissionEntity submission = submissionMapper.findSubmissionByGroupId(group.id);
        boolean created = submission == null;
        if (submission == null) {
            submission = new SubmissionEntity();
            submission.assignmentId = assignmentId;
            submission.groupId = group.id;
        }
        submission.projectName = request.projectName().trim();
        submission.repoUrl = request.repoUrl().trim();
        submission.videoUrl = blankToNull(request.videoUrl());
        submission.previewUrl = blankToNull(request.previewUrl());
        submission.docUrl = blankToNull(request.docUrl());
        submission.attachmentUrl = blankToNull(request.attachmentUrl());
        submission.description = blankToNull(request.description());
        submission.submittedBy = userId;
        submission.submittedAt = now;
        submission.late = late;

        if (submission.id == null) {
            submissionMapper.insertSubmission(submission);
        } else {
            submissionMapper.updateSubmission(submission);
        }
        logSubmissionEvent(created ? "created" : "updated", userId, assignment, submission, group.id);
        return buildSubmissionVos(List.of(submission)).get(0);
    }

    public List<DemoViews.ProjectCardVo> getProjects(Long userId, Long assignmentId) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        ensureStudentAssignmentAccess(userId, assignment);

        List<SubmissionEntity> submissions = submissionMapper.findSubmissionsByAssignmentId(assignmentId);
        AssignmentScoreSnapshot snapshot = buildAssignmentScoreSnapshot(assignment, submissions);
        Map<Long, List<Long>> memberIdsBySubmission = findMemberIdsBySubmission(submissions);
        Set<Long> blacklistedSubmissionIds = evaluationMapper.findBlacklistsByAssignmentId(assignmentId).stream()
            .filter(item -> Objects.equals(item.evaluatorUserId, userId))
            .map(item -> item.targetSubmissionId)
            .collect(Collectors.toSet());
        Set<Long> evaluatedSubmissionIds = evaluationMapper.findByAssignmentId(assignmentId, null, null, null, false).stream()
            .filter(item -> "STUDENT".equals(item.evaluatorRole))
            .filter(item -> Objects.equals(item.evaluatorUserId, userId))
            .map(item -> item.submissionId)
            .collect(Collectors.toSet());
        Map<Long, UserEntity> usersById = loadUsers(flattenMemberIds(memberIdsBySubmission.values()));

        return submissions.stream()
            .sorted(Comparator.comparing(item -> item.projectName.toLowerCase(Locale.ROOT)))
            .map(submission -> {
                List<Long> submissionMemberIds = memberIdsBySubmission.getOrDefault(submission.id, List.of());
                ProjectScoreBundle bundle = snapshot.projectScores().get(submission.id);
                boolean isSelfProject = submissionMemberIds.contains(userId);
                boolean evaluated = evaluatedSubmissionIds.contains(submission.id);
                boolean blocked = blacklistedSubmissionIds.contains(submission.id);
                boolean canEvaluate = "REVIEWING".equals(assignment.status) && !isSelfProject && !evaluated && !blocked;
                return new DemoViews.ProjectCardVo(
                    submission.id,
                    submission.projectName,
                    submission.repoUrl,
                    submissionMemberIds.stream()
                        .map(usersById::get)
                        .filter(Objects::nonNull)
                        .map(user -> user.displayName)
                        .toList(),
                    bundle == null ? 0D : bundle.displayScore(snapshot.published()),
                    snapshot.leaderboardType(),
                    canEvaluate,
                    evaluated,
                    submission.late
                );
            })
            .toList();
    }

    public DemoViews.DashboardVo getDashboard(Long userId, Long assignmentId) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        ensureStudentAssignmentAccess(userId, assignment);
        List<SubmissionEntity> submissions = submissionMapper.findSubmissionsByAssignmentId(assignmentId);
        AssignmentScoreSnapshot snapshot = buildAssignmentScoreSnapshot(assignment, submissions);
        SubmissionEntity submission = submissionMapper.findSubmissionByAssignmentAndUser(assignmentId, userId);
        DemoViews.SubmissionSummaryVo summary = submission == null
            ? buildEmptySubmissionSummary(assignment, snapshot)
            : buildSubmissionSummary(assignment, submission.id, snapshot);
        return new DemoViews.DashboardVo(
            assignment.title,
            requireCourse(assignment.courseId).name,
            assignment.status,
            resolveDisplayStatus(assignment),
            snapshot.leaderboardType(),
            submission == null ? null : buildSubmissionVos(List.of(submission)).get(0),
            summary,
            snapshot.leaderboard()
        );
    }

    public List<DemoViews.LeaderboardItemVo> getLeaderboard(Long userId, Long assignmentId) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        ensureStudentAssignmentAccess(userId, assignment);
        List<SubmissionEntity> submissions = submissionMapper.findSubmissionsByAssignmentId(assignmentId);
        return buildAssignmentScoreSnapshot(assignment, submissions).leaderboard();
    }

    @Transactional
    public DemoViews.SubmissionVo updateSubmission(Long userId, Long submissionId, DemoRequests.UpsertSubmissionRequest request) {
        SubmissionEntity submission = requireSubmission(submissionId);
        List<Long> memberIds = findMemberIdsBySubmission(List.of(submission)).getOrDefault(submissionId, List.of());
        if (!memberIds.contains(userId)) {
            throw new ForbiddenException("只能修改自己的项目提交");
        }
        return upsertSubmission(userId, submission.assignmentId, request);
    }

    @Transactional
    public Map<String, Object> createEvaluation(Long userId, Long submissionId, DemoRequests.CreateEvaluationRequest request) {
        SubmissionEntity submission = requireSubmission(submissionId);
        AssignmentEntity assignment = requireAssignment(submission.assignmentId);
        ensureStudentAssignmentAccess(userId, assignment);
        if (!"REVIEWING".equals(assignment.status)) {
            throw new IllegalStateException("当前作业不在互评阶段");
        }

        List<Long> memberIds = findMemberIdsBySubmission(List.of(submission)).getOrDefault(submissionId, List.of());
        if (memberIds.contains(userId)) {
            throw new IllegalStateException("不能评价自己或自己所在小组的项目");
        }
        if (evaluationMapper.findBlacklist(assignment.id, userId, submissionId) != null) {
            throw new IllegalStateException("该项目已被加入黑名单");
        }
        EvaluationEntity existing = evaluationMapper.findBySubmissionAndEvaluator(submissionId, userId, "STUDENT");
        if (existing != null) {
            throw new IllegalStateException("同一用户对同一项目只能评分一次");
        }

        List<RubricItemEntity> rubricItems = assignmentMapper.findRubricItemsByAssignmentId(assignment.id);
        BigDecimal totalScore = scoreCalculator.calculateWeightedTotal(request.itemScores(), rubricItems);

        EvaluationEntity evaluation = new EvaluationEntity();
        evaluation.assignmentId = assignment.id;
        evaluation.submissionId = submissionId;
        evaluation.evaluatorUserId = userId;
        evaluation.evaluatorRole = "STUDENT";
        evaluation.totalScore = totalScore;
        evaluation.comment = blankToNull(request.overallComment());
        evaluation.abnormal = false;
        evaluation.abnormalReason = null;
        evaluation.excluded = false;
        evaluation.reviewStatus = REVIEW_STATUS_PENDING;
        evaluationMapper.insert(evaluation);

        saveEvaluationItems(evaluation.id, request.itemScores());
        recomputeStudentAbnormalities(submissionId);
        logStudentEvaluationEvent(userId, assignment, submission, evaluation, request.itemScores().size());
        return Map.of("saved", true, "evaluationId", evaluation.id, "totalScore", evaluation.totalScore);
    }

    @Transactional
    public Map<String, Object> createTeacherScore(Long userId, Long submissionId, DemoRequests.TeacherScoreRequest request) {
        SubmissionEntity submission = requireSubmission(submissionId);
        AssignmentEntity assignment = requireAssignment(submission.assignmentId);
        ensureTeacherAssignmentAccess(userId, assignment);
        ensureResultsUnpublished(assignment, "最终成绩已发布，不能再修改教师评分");
        if ("SUBMITTING".equals(assignment.status)) {
            throw new IllegalStateException("当前作业还未进入评分阶段");
        }

        List<RubricItemEntity> rubricItems = assignmentMapper.findRubricItemsByAssignmentId(assignment.id);
        BigDecimal totalScore = scoreCalculator.calculateWeightedTotal(request.itemScores(), rubricItems);

        EvaluationEntity evaluation = evaluationMapper.findBySubmissionAndEvaluator(submissionId, userId, "TEACHER");
        boolean created = evaluation == null;
        if (evaluation == null) {
            evaluation = new EvaluationEntity();
            evaluation.assignmentId = assignment.id;
            evaluation.submissionId = submissionId;
            evaluation.evaluatorUserId = userId;
            evaluation.evaluatorRole = "TEACHER";
            evaluation.excluded = false;
            evaluation.abnormal = false;
            evaluation.reviewStatus = REVIEW_STATUS_PENDING;
        }
        evaluation.totalScore = totalScore;
        evaluation.comment = blankToNull(request.overallComment());
        evaluation.abnormalReason = null;
        evaluation.reviewStatus = blankToDefault(evaluation.reviewStatus, REVIEW_STATUS_PENDING);
        if (evaluation.id == null) {
            evaluationMapper.insert(evaluation);
        } else {
            evaluationMapper.update(evaluation);
            evaluationMapper.deleteItemsByEvaluationId(evaluation.id);
        }
        saveEvaluationItems(evaluation.id, request.itemScores());
        logTeacherScoreEvent(created ? "created" : "updated", userId, assignment, submission, evaluation, request.itemScores().size());
        return Map.of("saved", true, "evaluationId", evaluation.id, "totalScore", evaluation.totalScore);
    }

    private void logSubmissionEvent(String action, Long userId, AssignmentEntity assignment, SubmissionEntity submission, Long groupId) {
        UserEntity actor = requireUser(userId);
        log.info(
            "{} submission action={} actorUserId={} actorUsername={} actorRole={} assignmentId={} assignmentTitle={} submissionId={} groupId={} projectName={} late={}",
            COURSE_EVENT_PREFIX,
            action,
            actor.id,
            actor.username,
            actor.role,
            assignment.id,
            assignment.title,
            submission.id,
            groupId,
            submission.projectName,
            submission.late
        );
    }

    private void logStudentEvaluationEvent(Long userId, AssignmentEntity assignment, SubmissionEntity submission,
                                           EvaluationEntity evaluation, int itemCount) {
        UserEntity actor = requireUser(userId);
        log.info(
            "{} peer_evaluation action=created actorUserId={} actorUsername={} assignmentId={} assignmentTitle={} submissionId={} projectName={} evaluationId={} totalScore={} rubricItemCount={}",
            COURSE_EVENT_PREFIX,
            actor.id,
            actor.username,
            assignment.id,
            assignment.title,
            submission.id,
            submission.projectName,
            evaluation.id,
            evaluation.totalScore,
            itemCount
        );
    }

    private void logTeacherScoreEvent(String action, Long userId, AssignmentEntity assignment, SubmissionEntity submission,
                                      EvaluationEntity evaluation, int itemCount) {
        UserEntity actor = requireUser(userId);
        log.info(
            "{} teacher_score action={} actorUserId={} actorUsername={} assignmentId={} assignmentTitle={} submissionId={} projectName={} evaluationId={} totalScore={} rubricItemCount={}",
            COURSE_EVENT_PREFIX,
            action,
            actor.id,
            actor.username,
            assignment.id,
            assignment.title,
            submission.id,
            submission.projectName,
            evaluation.id,
            evaluation.totalScore,
            itemCount
        );
    }

    public List<DemoViews.EvaluationRecordVo> getEvaluations(Long teacherUserId, Long assignmentId,
                                                             Long submissionId, Long evaluatorUserId,
                                                             String reviewStatus, boolean abnormalOnly) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        ensureTeacherAssignmentAccess(teacherUserId, assignment);
        List<SubmissionEntity> submissions = submissionMapper.findSubmissionsByAssignmentId(assignmentId);
        Map<Long, SubmissionEntity> submissionById = submissions.stream()
            .collect(Collectors.toMap(item -> item.id, item -> item));
        List<EvaluationEntity> evaluations = evaluationMapper.findByAssignmentId(
            assignmentId,
            submissionId,
            evaluatorUserId,
            normalizeReviewStatus(reviewStatus),
            abnormalOnly
        );
        Map<Long, UserEntity> usersById = loadUsers(evaluations.stream().map(item -> item.evaluatorUserId).toList());
        Map<Long, List<DemoViews.EvaluationItemScoreVo>> itemScoresByEvaluationId = buildEvaluationItemScoreViews(evaluations);

        return evaluations.stream()
            .map(evaluation -> {
                SubmissionEntity submission = submissionById.get(evaluation.submissionId);
                UserEntity evaluator = usersById.get(evaluation.evaluatorUserId);
                return new DemoViews.EvaluationRecordVo(
                    evaluation.id,
                    evaluation.submissionId,
                    submission == null ? "未知项目" : submission.projectName,
                    evaluation.evaluatorUserId,
                    evaluator == null ? "未知用户" : evaluator.displayName,
                    evaluation.evaluatorRole,
                    scoreCalculator.round(evaluation.totalScore.doubleValue()),
                    evaluation.comment,
                    evaluation.abnormal,
                    evaluation.abnormalReason,
                    evaluation.excluded,
                    blankToDefault(evaluation.reviewStatus, REVIEW_STATUS_PENDING),
                    itemScoresByEvaluationId.getOrDefault(evaluation.id, List.of()),
                    formatDateTime(evaluation.createdAt)
                );
            })
            .toList();
    }

    public DemoViews.TeacherStatsVo getTeacherStats(Long teacherUserId, Long assignmentId) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        ensureTeacherAssignmentAccess(teacherUserId, assignment);

        List<SubmissionEntity> submissions = submissionMapper.findSubmissionsByAssignmentId(assignmentId);
        List<EvaluationEntity> evaluations = evaluationMapper.findByAssignmentId(assignmentId, null, null, null, false);
        AssignmentScoreSnapshot snapshot = buildAssignmentScoreSnapshot(assignment, submissions);
        List<CourseMemberEntity> courseMembers = courseMapper.findMembersByCourseId(assignment.courseId);
        Map<Long, UserEntity> studentsById = loadUsers(courseMembers.stream()
            .filter(item -> "STUDENT".equals(item.courseRole))
            .map(item -> item.userId)
            .toList());
        List<DemoViews.ProjectScoreVo> projectScoreViews = submissions.stream()
            .map(submission -> {
                ProjectScoreBundle bundle = snapshot.projectScores().get(submission.id);
                return new DemoViews.ProjectScoreVo(
                    submission.id,
                    submission.projectName,
                    bundle == null ? null : bundle.peerScore(),
                    bundle == null ? null : bundle.teacherScore(),
                    bundle == null ? null : bundle.realtimeFinalScore(),
                    bundle == null ? null : bundle.finalScore(),
                    submission.late
                );
            })
            .toList();

        Map<Long, List<Long>> memberIdsBySubmission = findMemberIdsBySubmission(submissions);
        List<EvaluationBlacklistEntity> blacklists = evaluationMapper.findBlacklistsByAssignmentId(assignmentId);
        Map<Long, Set<Long>> blacklistedByEvaluator = blacklists.stream()
            .collect(Collectors.groupingBy(item -> item.evaluatorUserId,
                Collectors.mapping(item -> item.targetSubmissionId, Collectors.toCollection(HashSet::new))));
        Map<Long, Set<Long>> evaluatedByStudent = evaluations.stream()
            .filter(item -> "STUDENT".equals(item.evaluatorRole))
            .collect(Collectors.groupingBy(item -> item.evaluatorUserId,
                Collectors.mapping(item -> item.submissionId, Collectors.toCollection(HashSet::new))));

        List<DemoViews.ReviewProgressVo> reviewProgress = studentsById.values().stream()
            .sorted(Comparator.comparing(item -> item.id))
            .map(student -> {
                Set<Long> blacklistedTargets = blacklistedByEvaluator.getOrDefault(student.id, Set.of());
                int totalCount = (int) submissions.stream()
                    .filter(submission -> !memberIdsBySubmission.getOrDefault(submission.id, List.of()).contains(student.id))
                    .filter(submission -> !blacklistedTargets.contains(submission.id))
                    .count();
                int completedCount = evaluatedByStudent.getOrDefault(student.id, Set.of()).size();
                double rate = totalCount == 0 ? 100D : scoreCalculator.round((double) completedCount / totalCount * 100D);
                return new DemoViews.ReviewProgressVo(student.id, student.displayName, completedCount, totalCount, rate);
            })
            .toList();

        double completionRate = reviewProgress.isEmpty()
            ? 0D
            : scoreCalculator.round(reviewProgress.stream().mapToDouble(DemoViews.ReviewProgressVo::completionRate).average().orElse(0D));

        Map<Long, SubmissionEntity> submissionById = submissions.stream()
            .collect(Collectors.toMap(item -> item.id, item -> item));
        Map<Long, UserEntity> blacklistUsers = loadUsers(blacklists.stream().map(item -> item.evaluatorUserId).toList());
        Map<Long, UserEntity> abnormalUsers = loadUsers(evaluations.stream().map(item -> item.evaluatorUserId).toList());

        List<DemoViews.AbnormalHintVo> abnormalHints = evaluations.stream()
            .filter(this::isStudentEvaluation)
            .filter(item -> item.abnormal)
            .map(item -> new DemoViews.AbnormalHintVo(
                item.id,
                submissionById.get(item.submissionId) == null ? "未知项目" : submissionById.get(item.submissionId).projectName,
                abnormalUsers.get(item.evaluatorUserId) == null ? "未知用户" : abnormalUsers.get(item.evaluatorUserId).displayName,
                scoreCalculator.round(item.totalScore.doubleValue()),
                blankToDefault(item.abnormalReason, "异常评分"),
                item.excluded
            ))
            .toList();

        List<DemoViews.BlacklistRuleVo> blacklistRules = blacklists.stream()
            .map(item -> new DemoViews.BlacklistRuleVo(
                item.id,
                item.evaluatorUserId,
                blacklistUsers.get(item.evaluatorUserId) == null ? "未知用户" : blacklistUsers.get(item.evaluatorUserId).displayName,
                item.targetSubmissionId,
                submissionById.get(item.targetSubmissionId) == null ? "未知项目" : submissionById.get(item.targetSubmissionId).projectName,
                formatDateTime(item.createdAt)
            ))
            .toList();

        List<EvaluationEntity> abnormalStudentEvaluations = evaluations.stream()
            .filter(this::isStudentEvaluation)
            .filter(item -> item.abnormal)
            .toList();
        int abnormalPendingCount = (int) abnormalStudentEvaluations.stream()
            .filter(item -> REVIEW_STATUS_PENDING.equals(blankToDefault(item.reviewStatus, REVIEW_STATUS_PENDING)))
            .count();
        int abnormalIgnoredCount = (int) abnormalStudentEvaluations.stream()
            .filter(item -> REVIEW_STATUS_IGNORED.equals(blankToDefault(item.reviewStatus, REVIEW_STATUS_PENDING)))
            .count();
        int abnormalRestoredCount = (int) abnormalStudentEvaluations.stream()
            .filter(item -> REVIEW_STATUS_RESTORED.equals(blankToDefault(item.reviewStatus, REVIEW_STATUS_PENDING)))
            .count();
        int abnormalHandledCount = abnormalIgnoredCount + abnormalRestoredCount;

        List<DemoViews.MetricVo> scoreDistribution = List.of(
            new DemoViews.MetricVo("90-100", (double) evaluations.stream().filter(item -> !item.excluded && item.totalScore.doubleValue() >= 90D).count()),
            new DemoViews.MetricVo("80-89", (double) evaluations.stream().filter(item -> !item.excluded && item.totalScore.doubleValue() >= 80D && item.totalScore.doubleValue() < 90D).count()),
            new DemoViews.MetricVo("<80", (double) evaluations.stream().filter(item -> !item.excluded && item.totalScore.doubleValue() < 80D).count())
        );

        CourseEntity course = requireCourse(assignment.courseId);
        return new DemoViews.TeacherStatsVo(
            assignment.courseId,
            course.name,
            assignment.id,
            assignment.title,
            assignment.status,
            resolveDisplayStatus(assignment),
            snapshot.published(),
            formatDateTime(resolvePublishedAt(assignment)),
            submissions.size(),
            (int) submissions.stream().filter(item -> item.late).count(),
            evaluations.size(),
            abnormalStudentEvaluations.size(),
            abnormalPendingCount,
            abnormalHandledCount,
            abnormalIgnoredCount,
            abnormalRestoredCount,
            completionRate,
            scoreDistribution,
            snapshot.dimensionAverages(),
            snapshot.leaderboardType(),
            snapshot.leaderboard(),
            projectScoreViews,
            reviewProgress,
            abnormalHints,
            buildAbnormalImpacts(assignment, submissions, evaluations),
            blacklistRules
        );
    }

    @Transactional
    public List<DemoViews.RubricItemVo> updateRubric(Long teacherUserId, Long assignmentId, DemoRequests.UpdateRubricRequest request) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        ensureTeacherAssignmentAccess(teacherUserId, assignment);
        ensureResultsUnpublished(assignment, "最终成绩已发布，不能再修改 Rubric");
        validateRubricItems(request.items());
        persistRubric(assignmentId, request.items());
        return assignmentMapper.findRubricItemsByAssignmentId(assignmentId).stream()
            .map(this::toRubricItemVo)
            .toList();
    }

    @Transactional
    public Map<String, Object> addBlacklist(Long teacherUserId, Long assignmentId, Long evaluatorUserId, Long targetSubmissionId) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        ensureTeacherAssignmentAccess(teacherUserId, assignment);
        ensureResultsUnpublished(assignment, "最终成绩已发布，不能再修改黑名单");
        ensureStudentMember(assignment.courseId, evaluatorUserId);
        SubmissionEntity submission = requireSubmission(targetSubmissionId);
        if (!Objects.equals(submission.assignmentId, assignmentId)) {
            throw new IllegalArgumentException("目标项目不属于当前作业");
        }
        if (evaluationMapper.findBlacklist(assignmentId, evaluatorUserId, targetSubmissionId) == null) {
            EvaluationBlacklistEntity blacklist = new EvaluationBlacklistEntity();
            blacklist.assignmentId = assignmentId;
            blacklist.evaluatorUserId = evaluatorUserId;
            blacklist.targetSubmissionId = targetSubmissionId;
            evaluationMapper.insertBlacklist(blacklist);
        }
        return Map.of("saved", true);
    }

    @Transactional
    public Map<String, Object> removeBlacklist(Long teacherUserId, Long assignmentId, Long evaluatorUserId, Long targetSubmissionId) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        ensureTeacherAssignmentAccess(teacherUserId, assignment);
        ensureResultsUnpublished(assignment, "最终成绩已发布，不能再修改黑名单");
        evaluationMapper.deleteBlacklist(assignmentId, evaluatorUserId, targetSubmissionId);
        return Map.of("removed", true);
    }

    @Transactional
    public Map<String, Object> reviewEvaluation(Long teacherUserId, Long evaluationId, boolean excluded) {
        EvaluationEntity evaluation = evaluationMapper.findById(evaluationId);
        if (evaluation == null) {
            throw new IllegalArgumentException("评分记录不存在");
        }
        AssignmentEntity assignment = requireAssignment(evaluation.assignmentId);
        ensureTeacherAssignmentAccess(teacherUserId, assignment);
        ensureResultsUnpublished(assignment, "最终成绩已发布，不能再调整评分有效性");
        String reviewStatus = excluded ? REVIEW_STATUS_IGNORED : REVIEW_STATUS_RESTORED;
        evaluationMapper.updateExcluded(evaluationId, excluded, reviewStatus);
        if (isStudentEvaluation(evaluation)) {
            recomputeStudentAbnormalities(evaluation.submissionId);
        }
        return Map.of("reviewed", true, "excluded", excluded, "reviewStatus", reviewStatus);
    }

    @Transactional
    public Map<String, Object> publishResults(Long teacherUserId, Long assignmentId) {
        AssignmentEntity assignment = requireAssignment(assignmentId);
        ensureTeacherAssignmentAccess(teacherUserId, assignment);
        if (isResultsPublished(assignment)) {
            throw new IllegalStateException("当前作业的最终成绩已发布");
        }
        assignment.status = "CLOSED";
        assignment.resultsPublished = true;
        assignment.resultsPublishedAt = LocalDateTime.now();
        assignmentMapper.updatePublishStatus(assignment);
        return Map.of(
            "published", true,
            "publishedAt", formatDateTime(assignment.resultsPublishedAt),
            "displayStatus", resolveDisplayStatus(assignment)
        );
    }

    private List<DemoViews.CourseCardVo> buildCourseCards(List<CourseEntity> courses,
                                                          java.util.function.Function<CourseEntity, String> roleResolver) {
        if (courses.isEmpty()) {
            return List.of();
        }
        List<Long> courseIds = courses.stream().map(item -> item.id).toList();
        Map<Long, List<AssignmentEntity>> assignmentsByCourse = assignmentMapper.findByCourseIds(courseIds).stream()
            .collect(Collectors.groupingBy(item -> item.courseId, LinkedHashMap::new, Collectors.toList()));

        return courses.stream()
            .sorted(Comparator.comparing(item -> item.id))
            .map(course -> {
                List<DemoViews.AssignmentSummaryVo> assignments = assignmentsByCourse.getOrDefault(course.id, List.of()).stream()
                    .sorted(Comparator.comparing(item -> item.id))
                    .map(item -> new DemoViews.AssignmentSummaryVo(
                        item.id,
                        item.title,
                        item.mode,
                        formatDateTime(item.deadline),
                        item.status,
                        isResultsPublished(item),
                        formatDateTime(resolvePublishedAt(item)),
                        resolveDisplayStatus(item)
                    ))
                    .toList();
                return new DemoViews.CourseCardVo(
                    course.id,
                    course.code,
                    course.name,
                    course.term,
                    roleResolver.apply(course),
                    assignments.size(),
                    assignments
                );
            })
            .toList();
    }

    private DemoViews.AssignmentDetailVo toAssignmentDetail(AssignmentEntity assignment, DemoViews.SubmissionSummaryVo summary,
                                                            Long currentStudentUserId) {
        CourseEntity course = requireCourse(assignment.courseId);
        List<DemoViews.RubricItemVo> rubric = assignmentMapper.findRubricItemsByAssignmentId(assignment.id).stream()
            .map(this::toRubricItemVo)
            .toList();
        List<DemoViews.MemberVo> studentMembers = loadCourseStudentMemberVos(assignment.courseId);
        AssignmentGroupEntity myGroupEntity = currentStudentUserId == null
            ? null
            : submissionMapper.findGroupByAssignmentAndUser(assignment.id, currentStudentUserId);
        DemoViews.AssignmentGroupVo myGroup = myGroupEntity == null
            ? null
            : buildAssignmentGroupVo(myGroupEntity,
                buildGroupMemberVosByGroupId(List.of(myGroupEntity),
                    submissionMapper.findGroupMembersByGroupIds(List.of(myGroupEntity.id)).stream()
                        .collect(Collectors.groupingBy(item -> item.groupId, LinkedHashMap::new, Collectors.toList())))
                    .getOrDefault(myGroupEntity.id, List.of()),
                submissionMapper.findSubmissionByGroupId(myGroupEntity.id));

        return new DemoViews.AssignmentDetailVo(
            assignment.id,
            assignment.courseId,
            course.name,
            assignment.title,
            assignment.mode,
            assignment.description,
            formatDateTime(assignment.deadline),
            assignment.allowLate,
            assignment.peerWeight,
            assignment.teacherWeight,
            assignment.status,
            isResultsPublished(assignment),
            formatDateTime(resolvePublishedAt(assignment)),
            resolveDisplayStatus(assignment),
            rubric,
            studentMembers,
            myGroup,
            currentStudentUserId != null && "GROUP".equals(assignment.mode) && myGroup == null,
            summary
        );
    }

    private List<DemoViews.SubmissionVo> buildSubmissionVos(List<SubmissionEntity> submissions) {
        if (submissions.isEmpty()) {
            return List.of();
        }
        Map<Long, List<Long>> memberIdsBySubmission = findMemberIdsBySubmission(submissions);
        Map<Long, UserEntity> usersById = loadUsers(flattenMemberIds(memberIdsBySubmission.values()));
        Map<Long, AssignmentGroupEntity> groupsById = submissions.stream()
            .map(item -> item.groupId)
            .distinct()
            .map(submissionMapper::findGroupById)
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(item -> item.id, item -> item, (left, right) -> left, LinkedHashMap::new));
        return submissions.stream()
            .map(submission -> new DemoViews.SubmissionVo(
                submission.id,
                submission.assignmentId,
                submission.groupId,
                groupsById.get(submission.groupId) == null ? null : groupsById.get(submission.groupId).groupName,
                submission.projectName,
                submission.repoUrl,
                memberIdsBySubmission.getOrDefault(submission.id, List.of()).stream()
                    .map(usersById::get)
                    .filter(Objects::nonNull)
                    .map(user -> new DemoViews.MemberVo(user.id, user.displayName, user.username))
                    .toList(),
                submission.videoUrl,
                submission.previewUrl,
                submission.docUrl,
                submission.attachmentUrl,
                submission.description,
                formatDateTime(submission.submittedAt),
                submission.late
            ))
            .toList();
    }

    private DemoViews.SubmissionSummaryVo buildSubmissionSummary(AssignmentEntity assignment, Long submissionId,
                                                                 AssignmentScoreSnapshot snapshot) {
        ProjectScoreBundle bundle = snapshot.projectScores().get(submissionId);
        return new DemoViews.SubmissionSummaryVo(
            bundle == null ? null : bundle.peerScore(),
            bundle == null ? null : bundle.teacherScore(),
            bundle == null ? null : bundle.realtimeFinalScore(),
            bundle == null || !snapshot.published() ? null : bundle.finalScore(),
            snapshot.published(),
            formatDateTime(resolvePublishedAt(assignment)),
            snapshot.rankBySubmissionId().get(submissionId),
            snapshot.totalProjects(),
            snapshot.dimensionAverages(),
            bundle == null ? List.of() : bundle.radar(),
            bundle == null ? List.of() : bundle.comments()
        );
    }

    private DemoViews.SubmissionSummaryVo buildEmptySubmissionSummary(AssignmentEntity assignment,
                                                                      AssignmentScoreSnapshot snapshot) {
        return new DemoViews.SubmissionSummaryVo(
            null,
            null,
            null,
            null,
            snapshot.published(),
            formatDateTime(resolvePublishedAt(assignment)),
            null,
            snapshot.totalProjects(),
            snapshot.dimensionAverages(),
            List.of(),
            List.of()
        );
    }

    private Map<Long, ProjectScoreBundle> buildProjectScoreBundles(AssignmentEntity assignment, List<SubmissionEntity> submissions) {
        if (submissions.isEmpty()) {
            return Map.of();
        }
        List<Long> submissionIds = submissions.stream().map(item -> item.id).toList();
        List<EvaluationEntity> evaluations = evaluationMapper.findBySubmissionIds(submissionIds);
        Map<Long, List<EvaluationEntity>> evaluationsBySubmission = evaluations.stream()
            .collect(Collectors.groupingBy(item -> item.submissionId, LinkedHashMap::new, Collectors.toList()));
        List<Long> evaluationIds = evaluations.stream().map(item -> item.id).toList();
        Map<Long, List<EvaluationItemEntity>> evaluationItemsBySubmission = new HashMap<>();
        if (!evaluationIds.isEmpty()) {
            Map<Long, Long> submissionIdByEvaluationId = evaluations.stream()
                .collect(Collectors.toMap(item -> item.id, item -> item.submissionId));
            for (EvaluationItemEntity item : evaluationMapper.findItemsByEvaluationIds(evaluationIds)) {
                Long submissionId = submissionIdByEvaluationId.get(item.evaluationId);
                if (submissionId != null) {
                    evaluationItemsBySubmission.computeIfAbsent(submissionId, ignored -> new ArrayList<>()).add(item);
                }
            }
        }
        List<RubricItemEntity> rubricItems = assignmentMapper.findRubricItemsByAssignmentId(assignment.id);

        Map<Long, ProjectScoreBundle> bundles = new LinkedHashMap<>();
        for (SubmissionEntity submission : submissions) {
            List<EvaluationEntity> scopedEvaluations = evaluationsBySubmission.getOrDefault(submission.id, List.of());
            List<EvaluationItemEntity> scopedItems = evaluationItemsBySubmission.getOrDefault(submission.id, List.of());
            Double peerScore = scoreCalculator.calculatePeerScore(scopedEvaluations);
            Double teacherScore = scoreCalculator.calculateTeacherScore(scopedEvaluations);
            Double realtimeFinalScore = scoreCalculator.calculateRealtimeFinalScore(assignment, peerScore, teacherScore);
            Double finalScore = scoreCalculator.calculateFinalScore(assignment, peerScore, teacherScore);
            bundles.put(submission.id, new ProjectScoreBundle(
                submission.id,
                submission.projectName,
                peerScore,
                teacherScore,
                realtimeFinalScore,
                finalScore,
                scoreCalculator.buildDimensionMetrics(rubricItems, scopedEvaluations, scopedItems),
                scoreCalculator.buildComments(scopedEvaluations),
                submission.late
            ));
        }
        return bundles;
    }

    private AssignmentScoreSnapshot buildAssignmentScoreSnapshot(AssignmentEntity assignment, List<SubmissionEntity> submissions) {
        boolean published = isResultsPublished(assignment);
        Map<Long, ProjectScoreBundle> projectScores = buildProjectScoreBundles(assignment, submissions);
        List<ProjectScoreBundle> ordered = orderBundles(projectScores, published);
        List<DemoViews.LeaderboardItemVo> leaderboard = new ArrayList<>();
        Map<Long, Integer> rankBySubmissionId = new LinkedHashMap<>();
        int rank = 1;
        for (ProjectScoreBundle bundle : ordered) {
            leaderboard.add(new DemoViews.LeaderboardItemVo(
                rank,
                bundle.projectName(),
                defaultScore(bundle.displayScore(published)),
                published ? "FINAL" : "REALTIME"
            ));
            rankBySubmissionId.put(bundle.submissionId(), rank);
            rank++;
        }
        return new AssignmentScoreSnapshot(
            published,
            published ? "FINAL" : "REALTIME",
            projectScores,
            leaderboard,
            rankBySubmissionId,
            ordered.size(),
            buildAssignmentDimensionAverages(assignment, submissions)
        );
    }

    private List<DemoViews.MetricVo> buildAssignmentDimensionAverages(AssignmentEntity assignment, List<SubmissionEntity> submissions) {
        if (submissions.isEmpty()) {
            return List.of();
        }
        List<Long> submissionIds = submissions.stream().map(item -> item.id).toList();
        List<EvaluationEntity> evaluations = evaluationMapper.findBySubmissionIds(submissionIds);
        List<Long> evaluationIds = evaluations.stream().map(item -> item.id).toList();
        List<EvaluationItemEntity> evaluationItems = evaluationIds.isEmpty()
            ? List.of()
            : evaluationMapper.findItemsByEvaluationIds(evaluationIds);
        List<RubricItemEntity> rubricItems = assignmentMapper.findRubricItemsByAssignmentId(assignment.id);
        return scoreCalculator.buildDimensionMetrics(rubricItems, evaluations, evaluationItems);
    }

    private void persistRubric(Long assignmentId, List<DemoRequests.RubricItemRequest> items) {
        RubricEntity rubric = assignmentMapper.findRubricByAssignmentId(assignmentId);
        if (rubric == null) {
            rubric = new RubricEntity();
            rubric.assignmentId = assignmentId;
            rubric.versionNo = 1;
            rubric.active = true;
            assignmentMapper.insertRubric(rubric);
        } else {
            rubric.versionNo = rubric.versionNo + 1;
            rubric.active = true;
            assignmentMapper.updateRubric(rubric);
            assignmentMapper.deleteRubricItemsByRubricId(rubric.id);
        }

        Long rubricId = rubric.id;
        List<RubricItemEntity> rubricItems = items.stream()
            .map(item -> {
                RubricItemEntity entity = new RubricItemEntity();
                entity.rubricId = rubricId;
                entity.itemName = item.name().trim();
                entity.description = blankToNull(item.description());
                entity.weight = item.weight();
                return entity;
            })
            .toList();
        assignmentMapper.insertRubricItems(rubricItems);
    }

    private void saveEvaluationItems(Long evaluationId, List<DemoRequests.ItemScoreRequest> itemScores) {
        if (itemScores.isEmpty()) {
            return;
        }
        List<EvaluationItemEntity> entities = itemScores.stream()
            .map(item -> {
                EvaluationItemEntity entity = new EvaluationItemEntity();
                entity.evaluationId = evaluationId;
                entity.rubricItemId = item.rubricItemId();
                entity.score = item.score();
                entity.comment = blankToNull(item.comment());
                return entity;
            })
            .toList();
        evaluationMapper.insertItems(entities);
    }

    // `abnormal` is only the detection result. `excluded` alone determines whether a score is aggregated.
    private void recomputeStudentAbnormalities(Long submissionId) {
        List<EvaluationEntity> evaluations = evaluationMapper.findBySubmissionIds(List.of(submissionId));
        Map<Long, ScoreCalculator.AbnormalDetectionResult> detectionByEvaluationId =
            scoreCalculator.detectStudentAbnormalities(evaluations);
        for (EvaluationEntity evaluation : evaluations) {
            if (!isStudentEvaluation(evaluation)) {
                continue;
            }
            ScoreCalculator.AbnormalDetectionResult detection = detectionByEvaluationId.get(evaluation.id);
            evaluation.abnormal = detection != null && detection.abnormal();
            evaluation.abnormalReason = detection == null ? null : detection.reason();
            evaluationMapper.updateAbnormalState(evaluation);
        }
    }

    private Map<Long, List<DemoViews.EvaluationItemScoreVo>> buildEvaluationItemScoreViews(List<EvaluationEntity> evaluations) {
        List<Long> evaluationIds = evaluations.stream()
            .map(item -> item.id)
            .filter(Objects::nonNull)
            .toList();
        if (evaluationIds.isEmpty()) {
            return Map.of();
        }
        List<EvaluationItemEntity> items = evaluationMapper.findItemsByEvaluationIds(evaluationIds);
        Map<Long, RubricItemEntity> rubricItemsById = items.isEmpty() ? Map.of() : assignmentMapper.findRubricItemsByAssignmentId(
            evaluations.get(0).assignmentId
        ).stream().collect(Collectors.toMap(item -> item.id, item -> item, (left, right) -> left, LinkedHashMap::new));
        return items.stream()
            .collect(Collectors.groupingBy(item -> item.evaluationId, LinkedHashMap::new, Collectors.mapping(item ->
                new DemoViews.EvaluationItemScoreVo(
                    item.rubricItemId,
                    rubricItemsById.get(item.rubricItemId) == null ? "未知评分项" : rubricItemsById.get(item.rubricItemId).itemName,
                    scoreCalculator.round(item.score.doubleValue()),
                    item.comment
                ),
                Collectors.toList()
            )));
    }

    private List<DemoViews.AbnormalImpactVo> buildAbnormalImpacts(AssignmentEntity assignment,
                                                                  List<SubmissionEntity> submissions,
                                                                  List<EvaluationEntity> evaluations) {
        if (submissions.isEmpty() || evaluations.isEmpty()) {
            return List.of();
        }
        Map<Long, List<EvaluationEntity>> evaluationsBySubmission = evaluations.stream()
            .collect(Collectors.groupingBy(item -> item.submissionId, LinkedHashMap::new, Collectors.toList()));
        return submissions.stream()
            .map(submission -> buildAbnormalImpact(assignment, submission, evaluationsBySubmission.getOrDefault(submission.id, List.of())))
            .filter(Objects::nonNull)
            .toList();
    }

    private DemoViews.AbnormalImpactVo buildAbnormalImpact(AssignmentEntity assignment, SubmissionEntity submission,
                                                           List<EvaluationEntity> evaluations) {
        boolean hasIgnoredEvaluation = evaluations.stream()
            .filter(this::isStudentEvaluation)
            .anyMatch(item -> item.excluded
                && REVIEW_STATUS_IGNORED.equals(blankToDefault(item.reviewStatus, REVIEW_STATUS_PENDING)));
        if (!hasIgnoredEvaluation) {
            return null;
        }

        Double currentPeerScore = scoreCalculator.calculatePeerScore(evaluations);
        Double teacherScore = scoreCalculator.calculateTeacherScore(evaluations);
        List<EvaluationEntity> rawEvaluations = evaluations.stream()
            .map(this::copyEvaluation)
            .peek(item -> {
                if (isStudentEvaluation(item)
                    && REVIEW_STATUS_IGNORED.equals(blankToDefault(item.reviewStatus, REVIEW_STATUS_PENDING))) {
                    item.excluded = false;
                }
            })
            .toList();
        Double rawPeerScore = scoreCalculator.calculatePeerScore(rawEvaluations);
        Double currentFinalScore = scoreCalculator.calculateFinalScore(assignment, currentPeerScore, teacherScore);
        Double rawFinalScore = scoreCalculator.calculateFinalScore(assignment, rawPeerScore, teacherScore);
        double peerDelta = defaultScore(rawPeerScore) - defaultScore(currentPeerScore);
        double finalDelta = defaultScore(rawFinalScore) - defaultScore(currentFinalScore);
        if (Math.abs(peerDelta) < 0.01D && Math.abs(finalDelta) < 0.01D) {
            return null;
        }
        return new DemoViews.AbnormalImpactVo(
            submission.id,
            submission.projectName,
            currentPeerScore,
            rawPeerScore,
            scoreCalculator.round(peerDelta),
            currentFinalScore,
            rawFinalScore,
            scoreCalculator.round(finalDelta)
        );
    }

    private EvaluationEntity copyEvaluation(EvaluationEntity source) {
        EvaluationEntity copy = new EvaluationEntity();
        copy.id = source.id;
        copy.assignmentId = source.assignmentId;
        copy.submissionId = source.submissionId;
        copy.evaluatorUserId = source.evaluatorUserId;
        copy.evaluatorRole = source.evaluatorRole;
        copy.totalScore = source.totalScore;
        copy.comment = source.comment;
        copy.abnormal = source.abnormal;
        copy.abnormalReason = source.abnormalReason;
        copy.excluded = source.excluded;
        copy.reviewStatus = source.reviewStatus;
        copy.createdAt = source.createdAt;
        return copy;
    }

    private boolean isStudentEvaluation(EvaluationEntity evaluation) {
        return "STUDENT".equals(evaluation.evaluatorRole);
    }

    private String normalizeReviewStatus(String reviewStatus) {
        if (reviewStatus == null || reviewStatus.isBlank()) {
            return null;
        }
        String normalized = reviewStatus.trim().toUpperCase(Locale.ROOT);
        if (!Set.of(REVIEW_STATUS_PENDING, REVIEW_STATUS_IGNORED, REVIEW_STATUS_RESTORED).contains(normalized)) {
            throw new IllegalArgumentException("reviewStatus 只支持 PENDING / IGNORED / RESTORED");
        }
        return normalized;
    }

    private void ensureTeacherGroupManageAccessible(Long userId, AssignmentEntity assignment) {
        ensureTeacherAssignmentAccess(userId, assignment);
        if (!"GROUP".equals(assignment.mode)) {
            throw new IllegalStateException("个人作业不支持教师小组管理");
        }
    }

    private void ensureTeacherGroupManageEditable(Long userId, AssignmentEntity assignment) {
        ensureTeacherGroupManageAccessible(userId, assignment);
        ensureResultsUnpublished(assignment, "最终成绩已发布，不能再修改小组");
    }

    private void ensureStudentAssignmentAccess(Long userId, AssignmentEntity assignment) {
        if (!hasCourseRole(assignment.courseId, userId, "STUDENT")) {
            throw new ForbiddenException("无权访问该作业");
        }
    }

    private void ensureTeacherAssignmentAccess(Long userId, AssignmentEntity assignment) {
        UserEntity user = requireUser(userId);
        if ("ADMIN".equals(user.role)) {
            return;
        }
        if (!hasCourseRole(assignment.courseId, userId, "TEACHER")) {
            throw new ForbiddenException("无权访问该教师作业");
        }
    }

    private void ensureResultsUnpublished(AssignmentEntity assignment, String message) {
        if (isResultsPublished(assignment)) {
            throw new IllegalStateException(message);
        }
    }

    private boolean hasCourseRole(Long courseId, Long userId, String role) {
        return courseMapper.findMembersByUserId(userId).stream()
            .anyMatch(item -> Objects.equals(item.courseId, courseId) && role.equals(item.courseRole));
    }

    private void ensureMembersBelongToCourse(Long courseId, List<Long> memberIds) {
        Set<Long> studentIds = courseMapper.findMembersByCourseId(courseId).stream()
            .filter(item -> "STUDENT".equals(item.courseRole))
            .map(item -> item.userId)
            .collect(Collectors.toSet());
        for (Long memberId : memberIds) {
            if (!studentIds.contains(memberId)) {
                throw new IllegalArgumentException("成员必须来自当前课程学生名单");
            }
        }
    }

    private void ensureStudentMember(Long courseId, Long userId) {
        boolean matched = courseMapper.findMembersByCourseId(courseId).stream()
            .anyMatch(item -> Objects.equals(item.userId, userId) && "STUDENT".equals(item.courseRole));
        if (!matched) {
            throw new IllegalArgumentException("指定用户不是当前课程学生");
        }
    }

    private void ensureGroupMembersAvailable(Long assignmentId, List<Long> memberIds, Long currentGroupId) {
        for (AssignmentGroupMemberEntity member : submissionMapper.findGroupMembersByAssignmentId(assignmentId)) {
            if (currentGroupId != null && Objects.equals(member.groupId, currentGroupId)) {
                continue;
            }
            if (memberIds.contains(member.userId)) {
                throw new IllegalStateException("成员 " + requireUser(member.userId).displayName + " 已在其他小组中");
            }
        }
    }

    private void ensureSubmissionEditable(AssignmentEntity assignment) {
        if (!"SUBMITTING".equals(assignment.status)) {
            throw new IllegalStateException("当前作业不允许提交或修改项目");
        }
    }

    private AssignmentGroupEntity resolveSubmissionGroup(Long userId, AssignmentEntity assignment) {
        if ("GROUP".equals(assignment.mode)) {
            AssignmentGroupEntity group = submissionMapper.findGroupByAssignmentAndUser(assignment.id, userId);
            if (group == null) {
                throw new IllegalStateException("你还未被教师分配到小组，暂时不能提交项目");
            }
            return group;
        }
        return resolveOrCreateIndividualGroup(userId, assignment);
    }

    private AssignmentGroupEntity resolveOrCreateIndividualGroup(Long userId, AssignmentEntity assignment) {
        AssignmentGroupEntity group = submissionMapper.findGroupByAssignmentAndUser(assignment.id, userId);
        if (group != null) {
            return group;
        }
        UserEntity user = requireUser(userId);
        AssignmentGroupEntity entity = new AssignmentGroupEntity();
        entity.assignmentId = assignment.id;
        entity.groupName = user.displayName + "个人组";
        submissionMapper.insertGroup(entity);
        submissionMapper.insertGroupMembers(buildGroupMembers(assignment.id, entity.id, List.of(userId)));
        return entity;
    }

    private List<Long> normalizeMemberIds(Long userId, AssignmentEntity assignment, List<Long> requestedMemberIds) {
        LinkedHashSet<Long> normalized = new LinkedHashSet<>();
        if ("INDIVIDUAL".equals(assignment.mode)) {
            normalized.add(userId);
        } else {
            if (requestedMemberIds != null) {
                normalized.addAll(requestedMemberIds);
            }
            normalized.add(userId);
        }
        return new ArrayList<>(normalized);
    }

    private void validateAssignmentWeights(int peerWeight, int teacherWeight) {
        if (peerWeight < 0 || teacherWeight < 0 || peerWeight + teacherWeight != 100) {
            throw new IllegalArgumentException("学生互评分和教师评分权重之和必须为 100%");
        }
    }

    private void validateRubricItems(List<DemoRequests.RubricItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Rubric 不能为空");
        }
        int totalWeight = items.stream().mapToInt(DemoRequests.RubricItemRequest::weight).sum();
        if (totalWeight != 100) {
            throw new IllegalArgumentException("Rubric 权重总和必须为 100%");
        }
    }

    private void validateSubmissionRequest(DemoRequests.UpsertSubmissionRequest request) {
        if (request.projectName() == null || request.projectName().isBlank()) {
            throw new IllegalArgumentException("项目名称不能为空");
        }
        if (request.repoUrl() == null || request.repoUrl().isBlank()) {
            throw new IllegalArgumentException("代码仓库链接不能为空");
        }
    }

    private AssignmentGroupEntity requireAssignmentGroup(Long assignmentId, Long groupId) {
        AssignmentGroupEntity group = submissionMapper.findGroupById(groupId);
        if (group == null || !Objects.equals(group.assignmentId, assignmentId)) {
            throw new IllegalArgumentException("小组不存在");
        }
        return group;
    }

    private boolean isGroupMemberLocked(Long groupId) {
        return submissionMapper.findSubmissionByGroupId(groupId) != null;
    }

    private List<DemoViews.MemberVo> loadCourseStudentMemberVos(Long courseId) {
        List<CourseMemberEntity> studentMembers = courseMapper.findMembersByCourseId(courseId).stream()
            .filter(item -> "STUDENT".equals(item.courseRole))
            .toList();
        Map<Long, UserEntity> usersById = loadUsers(studentMembers.stream().map(item -> item.userId).toList());
        return studentMembers.stream()
            .map(item -> usersById.get(item.userId))
            .filter(Objects::nonNull)
            .map(user -> new DemoViews.MemberVo(user.id, user.displayName, user.username))
            .toList();
    }

    private List<Long> normalizeDistinctMemberIds(List<Long> memberUserIds) {
        if (memberUserIds == null) {
            throw new IllegalArgumentException("小组成员不能为空");
        }
        LinkedHashSet<Long> normalized = new LinkedHashSet<>(memberUserIds);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("小组成员不能为空");
        }
        return new ArrayList<>(normalized);
    }

    private List<AssignmentGroupMemberEntity> buildGroupMembers(Long assignmentId, Long groupId, List<Long> memberIds) {
        List<AssignmentGroupMemberEntity> groupMembers = new ArrayList<>();
        for (Long memberId : memberIds) {
            AssignmentGroupMemberEntity member = new AssignmentGroupMemberEntity();
            member.assignmentId = assignmentId;
            member.groupId = groupId;
            member.userId = memberId;
            groupMembers.add(member);
        }
        return groupMembers;
    }

    private DemoViews.UserProfileVo toUserProfile(UserEntity user) {
        return new DemoViews.UserProfileVo(user.id, user.username, user.displayName, user.role, user.firstLoginResetRequired);
    }

    private DemoViews.CourseMemberVo toCourseMemberVo(UserEntity user, String courseRole) {
        if (user == null) {
            return null;
        }
        return new DemoViews.CourseMemberVo(user.id, user.username, user.displayName, courseRole);
    }

    private DemoViews.RubricItemVo toRubricItemVo(RubricItemEntity item) {
        return new DemoViews.RubricItemVo(item.id, item.itemName, item.description, item.weight);
    }

    private Map<Long, List<DemoViews.MemberVo>> buildGroupMemberVosByGroupId(List<AssignmentGroupEntity> groups,
                                                                              Map<Long, List<AssignmentGroupMemberEntity>> groupMembersByGroupId) {
        if (groups.isEmpty()) {
            return Map.of();
        }
        List<Long> userIds = groupMembersByGroupId.values().stream()
            .flatMap(List::stream)
            .map(item -> item.userId)
            .distinct()
            .toList();
        Map<Long, UserEntity> usersById = loadUsers(userIds);
        Map<Long, List<DemoViews.MemberVo>> result = new LinkedHashMap<>();
        for (AssignmentGroupEntity group : groups) {
            result.put(group.id, groupMembersByGroupId.getOrDefault(group.id, List.of()).stream()
                .map(item -> usersById.get(item.userId))
                .filter(Objects::nonNull)
                .map(user -> new DemoViews.MemberVo(user.id, user.displayName, user.username))
                .toList());
        }
        return result;
    }

    private DemoViews.AssignmentGroupVo buildAssignmentGroupVo(AssignmentGroupEntity group, List<DemoViews.MemberVo> members,
                                                               SubmissionEntity submission) {
        return new DemoViews.AssignmentGroupVo(
            group.id,
            group.groupName,
            members,
            submission == null ? null : new DemoViews.AssignmentGroupSubmissionVo(
                submission.id,
                submission.projectName,
                formatDateTime(submission.submittedAt),
                submission.late
            ),
            submission != null
        );
    }

    private UserEntity requireUser(Long userId) {
        UserEntity user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return user;
    }

    private CourseEntity requireCourse(Long courseId) {
        CourseEntity course = courseMapper.findById(courseId);
        if (course == null) {
            throw new IllegalArgumentException("课程不存在");
        }
        return course;
    }

    private AssignmentEntity requireAssignment(Long assignmentId) {
        AssignmentEntity assignment = assignmentMapper.findById(assignmentId);
        if (assignment == null) {
            throw new IllegalArgumentException("作业不存在");
        }
        return assignment;
    }

    private SubmissionEntity requireSubmission(Long submissionId) {
        SubmissionEntity submission = submissionMapper.findSubmissionById(submissionId);
        if (submission == null) {
            throw new IllegalArgumentException("项目不存在");
        }
        return submission;
    }

    private Map<Long, List<Long>> findMemberIdsBySubmission(List<SubmissionEntity> submissions) {
        if (submissions.isEmpty()) {
            return Map.of();
        }
        List<Long> groupIds = submissions.stream().map(item -> item.groupId).distinct().toList();
        Map<Long, List<Long>> membersByGroup = submissionMapper.findGroupMembersByGroupIds(groupIds).stream()
            .collect(Collectors.groupingBy(item -> item.groupId, LinkedHashMap::new,
                Collectors.mapping(item -> item.userId, Collectors.toList())));
        Map<Long, List<Long>> result = new LinkedHashMap<>();
        for (SubmissionEntity submission : submissions) {
            result.put(submission.id, membersByGroup.getOrDefault(submission.groupId, List.of()));
        }
        return result;
    }

    private Map<Long, UserEntity> loadUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.findByIds(userIds.stream().distinct().toList()).stream()
            .collect(Collectors.toMap(item -> item.id, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private List<Long> flattenMemberIds(Iterable<List<Long>> memberGroups) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        for (List<Long> group : memberGroups) {
            ids.addAll(group);
        }
        return new ArrayList<>(ids);
    }

    private String normalizeMode(String mode) {
        return "GROUP".equalsIgnoreCase(mode) ? "GROUP" : "INDIVIDUAL";
    }

    private boolean isResultsPublished(AssignmentEntity assignment) {
        return assignment.resultsPublished || "CLOSED".equals(assignment.status);
    }

    private LocalDateTime resolvePublishedAt(AssignmentEntity assignment) {
        if (assignment.resultsPublishedAt != null) {
            return assignment.resultsPublishedAt;
        }
        return isResultsPublished(assignment) ? assignment.createdAt : null;
    }

    private String resolveDisplayStatus(AssignmentEntity assignment) {
        if (isResultsPublished(assignment)) {
            return "已发布最终成绩";
        }
        return switch (assignment.status) {
            case "SUBMITTING" -> "提交中";
            case "REVIEWING" -> "互评中";
            case "CLOSED" -> "已封榜待发布";
            default -> assignment.status;
        };
    }

    private String normalizeCourseRole(String courseRole) {
        if (courseRole == null || courseRole.isBlank()) {
            throw new IllegalArgumentException("课程成员角色不能为空");
        }
        String normalized = courseRole.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("TEACHER", "STUDENT").contains(normalized)) {
            throw new IllegalArgumentException("课程成员角色只支持 TEACHER 或 STUDENT");
        }
        return normalized;
    }

    private void validateUserRoleForCourseMember(UserEntity user, String courseRole) {
        if (!courseRole.equals(user.role)) {
            throw new IllegalArgumentException("用户角色与课程成员角色不匹配");
        }
    }

    private String buildGroupName(String projectName, String mode, List<Long> memberIds) {
        if ("INDIVIDUAL".equals(mode)) {
            return blankToDefault(projectName, "个人项目");
        }
        return blankToDefault(projectName, "项目小组") + "（" + memberIds.size() + "人）";
    }

    private LocalDateTime parseDeadline(String raw, LocalDateTime fallback) {
        return raw == null || raw.isBlank() ? fallback : LocalDateTime.parse(raw);
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(FORMATTER);
    }

    private double defaultScore(Double value) {
        return value == null ? 0D : scoreCalculator.round(value);
    }

    private List<ProjectScoreBundle> orderBundles(Map<Long, ProjectScoreBundle> bundles, boolean published) {
        return bundles.values().stream()
            .sorted(Comparator
                .comparing((ProjectScoreBundle bundle) -> bundle.displayScore(published),
                    Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ProjectScoreBundle::projectName))
            .toList();
    }

    private boolean isDueSoon(LocalDateTime deadline) {
        if (deadline == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return !deadline.isBefore(now) && !deadline.isAfter(now.plusHours(72));
    }

    private void interleaveTasks(List<DemoViews.StudentTaskCardVo> target,
                                 List<DemoViews.StudentTaskCardVo> dueSoonTasks,
                                 List<DemoViews.StudentTaskCardVo> reviewTasks,
                                 List<DemoViews.StudentTaskCardVo> resultTasks,
                                 List<DemoViews.StudentTaskCardVo> pendingTasks,
                                 int limit) {
        List<List<DemoViews.StudentTaskCardVo>> buckets = List.of(dueSoonTasks, reviewTasks, resultTasks, pendingTasks);
        int[] cursors = new int[buckets.size()];
        while (target.size() < limit) {
            boolean progressed = false;
            for (int index = 0; index < buckets.size() && target.size() < limit; index++) {
                List<DemoViews.StudentTaskCardVo> bucket = buckets.get(index);
                if (cursors[index] < bucket.size()) {
                    target.add(bucket.get(cursors[index]++));
                    progressed = true;
                }
            }
            if (!progressed) {
                return;
            }
        }
    }

    private List<DemoViews.StudentActivityBannerVo> buildActivityBanners(DemoViews.StudentOverviewVo overview,
                                                                         List<DemoViews.StudentReviewHighlightVo> reviewHighlights,
                                                                         List<DemoViews.StudentResultHighlightVo> resultHighlights) {
        List<DemoViews.StudentActivityBannerVo> banners = new ArrayList<>();
        if (overview.pendingSubmissionCount() > 0) {
            banners.add(new DemoViews.StudentActivityBannerVo(
                "今日优先完成",
                "还有 " + overview.pendingSubmissionCount() + " 个待提交任务，建议优先处理临近截止的作业。",
                "warning"
            ));
        }
        if (overview.availableReviewCount() > 0 && !reviewHighlights.isEmpty()) {
            DemoViews.StudentReviewHighlightVo hottest = reviewHighlights.get(0);
            banners.add(new DemoViews.StudentActivityBannerVo(
                "互评热区",
                hottest.assignmentTitle() + " 当前有 " + hottest.reviewableProjects() + " 个可评项目，适合直接进入广场完成互评。",
                "primary"
            ));
        }
        if (overview.publishedResultCount() > 0 && !resultHighlights.isEmpty()) {
            DemoViews.StudentResultHighlightVo latest = resultHighlights.get(0);
            banners.add(new DemoViews.StudentActivityBannerVo(
                "最近放榜",
                latest.assignmentTitle() + " 已发布结果，可直接查看你的排名和最终得分。",
                "success"
            ));
        }
        return banners.stream().limit(3).toList();
    }

    private List<DemoRequests.RubricItemRequest> defaultRubricItems() {
        return List.of(
            new DemoRequests.RubricItemRequest(null, "完成度", "功能是否完整", 30),
            new DemoRequests.RubricItemRequest(null, "工程质量", "代码结构与协作规范", 25),
            new DemoRequests.RubricItemRequest(null, "创新性", "设计亮点与创意", 20),
            new DemoRequests.RubricItemRequest(null, "展示表达", "说明文档和演示效果", 25)
        );
    }

    private record ProjectScoreBundle(Long submissionId, String projectName, Double peerScore, Double teacherScore,
                                      Double realtimeFinalScore, Double finalScore, List<DemoViews.MetricVo> radar,
                                      List<DemoViews.CommentVo> comments, boolean late) {
        private Double displayScore(boolean published) {
            return published ? finalScore : realtimeFinalScore;
        }
    }

    private record AssignmentScoreSnapshot(boolean published, String leaderboardType,
                                           Map<Long, ProjectScoreBundle> projectScores,
                                           List<DemoViews.LeaderboardItemVo> leaderboard,
                                           Map<Long, Integer> rankBySubmissionId,
                                           int totalProjects,
                                           List<DemoViews.MetricVo> dimensionAverages) {
    }

    private record HomeReviewHighlight(int reviewableProjects, int totalProjects, LocalDateTime deadline,
                                       DemoViews.StudentReviewHighlightVo card) {
    }

    private record HomeResultHighlight(LocalDateTime publishedAt, DemoViews.StudentResultHighlightVo card) {
    }

    private record HomeResultTask(LocalDateTime publishedAt, DemoViews.StudentTaskCardVo card) {
    }

}
