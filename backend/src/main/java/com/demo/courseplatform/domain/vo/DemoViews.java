package com.demo.courseplatform.domain.vo;

import java.util.List;

public final class DemoViews {

    private DemoViews() {
    }

    public record UserProfileVo(Long id, String username, String displayName, String role, boolean firstLoginResetRequired) {
    }

    public record LoginVo(String token, UserProfileVo user) {
    }

    public record CourseCardVo(Long id, String code, String name, String term, String courseDeadline,
                               String roleInCourse, int assignmentCount,
                               List<AssignmentSummaryVo> assignments) {
    }

    public record StudentOverviewVo(int totalCourses, int totalAssignments, int pendingSubmissionCount,
                                    int reviewingAssignmentCount, int publishedResultCount, int availableReviewCount) {
    }

    public record StudentTaskCardVo(Long assignmentId, Long courseId, String courseName, String assignmentTitle,
                                    String mode, String displayStatus, String deadline, String submissionCloseAt,
                                    String taskType, String actionLabel) {
    }

    public record StudentReviewHighlightVo(Long assignmentId, String courseName, String assignmentTitle,
                                           int totalProjects, int reviewableProjects, String leaderboardType,
                                           String displayStatus) {
    }

    public record StudentResultHighlightVo(Long assignmentId, String courseName, String assignmentTitle,
                                           String publishedAt, Integer currentRank, Double finalScore) {
    }

    public record StudentActivityBannerVo(String title, String description, String tone) {
    }

    public record StudentHomeVo(StudentOverviewVo overview,
                                List<StudentTaskCardVo> taskQueue,
                                List<StudentReviewHighlightVo> reviewHighlights,
                                List<StudentResultHighlightVo> resultHighlights,
                                List<StudentActivityBannerVo> activityBanners) {
    }

    public record CourseMemberVo(Long userId, String username, String displayName, String courseRole) {
    }

    public record CourseMemberManageVo(List<CourseMemberVo> members, List<CourseMemberVo> teacherCandidates,
                                       List<CourseMemberVo> studentCandidates) {
    }

    public record AssignmentSummaryVo(Long id, String title, String mode, String deadline, String submissionCloseAt, String status,
                                      boolean resultsPublished, String resultsPublishedAt, String displayStatus) {
    }

    public record MemberVo(Long id, String name, String username) {
    }

    public record RubricItemVo(Long id, String name, String description, int weight) {
    }

    public record MetricVo(String name, Double value) {
    }

    public record CommentVo(String authorRole, String content) {
    }

    public record SubmissionSummaryVo(Double peerScore, Double teacherScore, Double realtimeFinalScore, Double finalScore,
                                      boolean published, String publishedAt, Integer currentRank, Integer totalProjects,
                                      List<MetricVo> dimensionAverages, List<MetricVo> radar, List<CommentVo> comments) {
    }

    public record AssignmentGroupSubmissionVo(Long submissionId, String projectName, String submittedAt, boolean late) {
    }

    public record AssignmentGroupVo(Long id, String groupName, List<MemberVo> members,
                                    AssignmentGroupSubmissionVo submission, boolean memberLocked) {
    }

    public record AssignmentGroupManageVo(Long assignmentId, String assignmentTitle, String assignmentStatus,
                                          String displayStatus, boolean resultsPublished, String resultsPublishedAt,
                                          List<AssignmentGroupVo> groups, List<MemberVo> ungroupedStudents) {
    }

    public record AssignmentDetailVo(Long id, Long courseId, String courseName, String title, String mode, String description,
                                     String deadline, String submissionCloseAt, boolean allowLate, int peerWeight, int teacherWeight, String status,
                                     boolean resultsPublished, String resultsPublishedAt, String displayStatus,
                                     List<RubricItemVo> rubric, List<MemberVo> studentMembers, AssignmentGroupVo myGroup,
                                     boolean ungroupedForGroupAssignment, SubmissionSummaryVo summary) {
    }

    public record SubmissionVo(Long id, Long assignmentId, Long groupId, String groupName, String projectName,
                               String repoUrl, List<MemberVo> members,
                               String videoUrl, String previewUrl, String docUrl, String attachmentUrl, String description,
                               String submittedAt, boolean late) {
    }

    public record ProjectCardVo(Long id, String projectName, String repoUrl, List<String> memberNames,
                                Double finalScore, String scoreType, boolean canEvaluate, boolean evaluated,
                                String ineligibleReason, boolean late) {
    }

    public record EvaluationItemScoreVo(Long rubricItemId, String rubricItemName, Double score, String comment) {
    }

    // `abnormal` is the system's detection result. `excluded` alone controls whether a score participates in aggregation.
    public record EvaluationRecordVo(Long id, Long submissionId, String projectName, Long evaluatorUserId, String evaluatorName,
                                     String evaluatorUsername, String evaluatorRole, Double totalScore, String comment, boolean abnormal,
                                     String abnormalReason, boolean excluded, String reviewStatus,
                                     List<EvaluationItemScoreVo> itemScores, String createdAt) {
    }

    public record LeaderboardItemVo(int rank, String projectName, Double finalScore, String scoreType) {
    }

    public record DashboardVo(String assignmentTitle, String courseName, String assignmentStatus, String displayStatus,
                              String leaderboardType, SubmissionVo submission, SubmissionSummaryVo summary,
                              List<LeaderboardItemVo> leaderboard) {
    }

    public record AbnormalImpactVo(Long submissionId, String projectName, Double currentPeerScore, Double rawPeerScore,
                                   Double peerScoreDelta, Double currentFinalScore, Double rawFinalScore,
                                   Double finalScoreDelta) {
    }

    public record TeacherStatsVo(Long courseId, String courseName, Long assignmentId, String assignmentTitle,
                                 String assignmentStatus, String displayStatus, boolean resultsPublished,
                                 String resultsPublishedAt, int totalSubmissions, int lateSubmissions,
                                 int totalEvaluations, int abnormalEvaluations, int abnormalPendingCount,
                                 int abnormalHandledCount, int abnormalIgnoredCount, int abnormalRestoredCount,
                                 double completionRate,
                                 List<MetricVo> scoreDistribution, List<MetricVo> dimensionAverages,
                                 String leaderboardType, List<LeaderboardItemVo> leaderboard,
                                 List<ProjectScoreVo> projectScores, List<ReviewProgressVo> reviewProgressByStudent,
                                 List<AbnormalHintVo> abnormalHints, List<AbnormalImpactVo> abnormalImpacts,
                                 List<BlacklistRuleVo> blacklistRules) {
    }

    public record ProjectScoreVo(Long submissionId, String projectName, Double peerScore,
                                 Double teacherScore, Double realtimeFinalScore, Double finalScore, boolean late) {
    }

    public record ReviewProgressVo(Long userId, String displayName, int completedCount,
                                   int totalCount, Double completionRate) {
    }

    public record AbnormalHintVo(Long evaluationId, String projectName, String evaluatorName,
                                 Double totalScore, String reason, boolean excluded) {
    }

    public record BlacklistRuleVo(Long id, Long evaluatorUserId, String evaluatorName,
                                  Long targetSubmissionId, String targetProjectName, String createdAt) {
    }
}
