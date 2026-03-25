package com.demo.courseplatform.domain.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public final class DemoRequests {

    private DemoRequests() {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record ChangePasswordRequest(@NotBlank String newPassword) {
    }

    public record CreateCourseRequest(@NotBlank String code, @NotBlank String name, String term, String courseDeadline) {
    }

    public record CourseMemberRequest(@NotNull Long userId, @NotBlank String courseRole) {
    }

    public record CreateAssignmentRequest(@NotBlank String title, @NotBlank String mode, String description, String deadline,
                                          boolean allowLate, @Min(0) @Max(100) int peerWeight,
                                          @Min(0) @Max(100) int teacherWeight, String status) {
    }

    public record RubricItemRequest(Long id, @NotBlank String name, String description,
                                    @Min(0) @Max(100) int weight) {
    }

    public record UpdateRubricRequest(@NotEmpty List<RubricItemRequest> items) {
    }

    public record UpsertSubmissionRequest(@NotBlank String projectName, @NotBlank String repoUrl, List<Long> memberUserIds,
                                          String videoUrl, String previewUrl, String docUrl,
                                          String attachmentUrl, String description) {
    }

    public record UpsertAssignmentGroupRequest(@NotBlank String groupName, @NotEmpty List<Long> memberUserIds) {
    }

    public record ItemScoreRequest(@NotNull Long rubricItemId,
                                   @NotNull @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal score,
                                   String comment) {
    }

    public record CreateEvaluationRequest(@NotEmpty List<ItemScoreRequest> itemScores, String overallComment) {
    }

    public record TeacherScoreRequest(@NotEmpty List<ItemScoreRequest> itemScores, String overallComment) {
    }
}
