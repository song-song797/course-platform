package com.demo.courseplatform.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class PersistenceModels {

    private PersistenceModels() {
    }

    public static class UserEntity {
        public Long id;
        public String username;
        public String passwordHash;
        public String displayName;
        public String role;
        public boolean firstLoginResetRequired;
        public LocalDateTime createdAt;
    }

    public static class CourseEntity {
        public Long id;
        public String code;
        public String name;
        public String term;
        public LocalDateTime courseDeadline;
    }

    public static class CourseMemberEntity {
        public Long id;
        public Long courseId;
        public Long userId;
        public String courseRole;
        public LocalDateTime createdAt;
    }

    public static class AssignmentEntity {
        public Long id;
        public Long courseId;
        public String title;
        public String mode;
        public String description;
        public LocalDateTime deadline;
        public boolean allowLate;
        public int peerWeight;
        public int teacherWeight;
        public String status;
        public boolean resultsPublished;
        public LocalDateTime resultsPublishedAt;
        public LocalDateTime createdAt;
    }

    public static class RubricEntity {
        public Long id;
        public Long assignmentId;
        public int versionNo;
        public boolean active;
        public LocalDateTime createdAt;
    }

    public static class RubricItemEntity {
        public Long id;
        public Long rubricId;
        public String itemName;
        public String description;
        public int weight;
    }

    public static class AssignmentGroupEntity {
        public Long id;
        public Long assignmentId;
        public String groupName;
        public LocalDateTime createdAt;
    }

    public static class AssignmentGroupMemberEntity {
        public Long id;
        public Long assignmentId;
        public Long groupId;
        public Long userId;
        public LocalDateTime createdAt;
    }

    public static class SubmissionEntity {
        public Long id;
        public Long assignmentId;
        public Long groupId;
        public String projectName;
        public String repoUrl;
        public String videoUrl;
        public String previewUrl;
        public String docUrl;
        public String attachmentUrl;
        public String description;
        public Long submittedBy;
        public LocalDateTime submittedAt;
        public boolean late;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;
    }

    public static class EvaluationEntity {
        public Long id;
        public Long assignmentId;
        public Long submissionId;
        public Long evaluatorUserId;
        public String evaluatorRole;
        public BigDecimal totalScore;
        public String comment;
        public boolean abnormal;
        public String abnormalReason;
        public boolean excluded;
        public String reviewStatus;
        public LocalDateTime createdAt;
    }

    public static class EvaluationItemEntity {
        public Long id;
        public Long evaluationId;
        public Long rubricItemId;
        public BigDecimal score;
        public String comment;
    }

    public static class EvaluationBlacklistEntity {
        public Long id;
        public Long assignmentId;
        public Long evaluatorUserId;
        public Long targetSubmissionId;
        public LocalDateTime createdAt;
    }
}
