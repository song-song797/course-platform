package com.demo.courseplatform.controller.teacher;

import com.demo.courseplatform.common.ApiResponse;
import com.demo.courseplatform.common.PageResult;
import com.demo.courseplatform.domain.dto.DemoRequests;
import com.demo.courseplatform.domain.vo.DemoViews;
import com.demo.courseplatform.service.DemoPlatformService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teacher")
public class TeacherController {

    private final DemoPlatformService demoPlatformService;

    public TeacherController(DemoPlatformService demoPlatformService) {
        this.demoPlatformService = demoPlatformService;
    }

    @GetMapping("/courses")
    public ApiResponse<PageResult<DemoViews.CourseCardVo>> getCourses(HttpServletRequest request,
                                                                       @RequestParam(defaultValue = "1") int pageNo,
                                                                       @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ApiResponse.success(PageResult.of(demoPlatformService.getCoursesForUser(userId), pageNo, pageSize));
    }

    @GetMapping("/assignments/{assignmentId}")
    public ApiResponse<DemoViews.AssignmentDetailVo> getAssignment(HttpServletRequest request, @PathVariable Long assignmentId) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ApiResponse.success(demoPlatformService.getTeacherAssignmentDetail(userId, assignmentId));
    }

    @GetMapping("/assignments/{assignmentId}/groups")
    public ApiResponse<DemoViews.AssignmentGroupManageVo> getGroups(HttpServletRequest request, @PathVariable Long assignmentId) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ApiResponse.success(demoPlatformService.getAssignmentGroups(userId, assignmentId));
    }

    @PostMapping("/assignments/{assignmentId}/groups")
    public ApiResponse<DemoViews.AssignmentGroupVo> createGroup(HttpServletRequest request, @PathVariable Long assignmentId,
                                                                @Valid @RequestBody DemoRequests.UpsertAssignmentGroupRequest requestBody) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ApiResponse.success(demoPlatformService.createAssignmentGroup(userId, assignmentId, requestBody));
    }

    @PutMapping("/assignments/{assignmentId}/groups/{groupId}")
    public ApiResponse<DemoViews.AssignmentGroupVo> updateGroup(HttpServletRequest request, @PathVariable Long assignmentId,
                                                                @PathVariable Long groupId,
                                                                @Valid @RequestBody DemoRequests.UpsertAssignmentGroupRequest requestBody) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ApiResponse.success(demoPlatformService.updateAssignmentGroup(userId, assignmentId, groupId, requestBody));
    }

    @DeleteMapping("/assignments/{assignmentId}/groups/{groupId}")
    public ApiResponse<Map<String, Object>> deleteGroup(HttpServletRequest request, @PathVariable Long assignmentId,
                                                        @PathVariable Long groupId) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ApiResponse.success(demoPlatformService.deleteAssignmentGroup(userId, assignmentId, groupId));
    }

    @PutMapping("/assignments/{assignmentId}/rubric")
    public ApiResponse<PageResult<DemoViews.RubricItemVo>> updateRubric(HttpServletRequest request, @PathVariable Long assignmentId,
                                                                        @Valid @RequestBody DemoRequests.UpdateRubricRequest requestBody) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ApiResponse.success(PageResult.of(demoPlatformService.updateRubric(userId, assignmentId, requestBody), 1, 20));
    }

    @GetMapping("/assignments/{assignmentId}/submissions")
    public ApiResponse<PageResult<DemoViews.SubmissionVo>> getSubmissions(HttpServletRequest request, @PathVariable Long assignmentId,
                                                                          @RequestParam(defaultValue = "1") int pageNo,
                                                                          @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ApiResponse.success(PageResult.of(demoPlatformService.getTeacherSubmissions(userId, assignmentId), pageNo, pageSize));
    }

    @PostMapping("/submissions/{submissionId}/scores")
    public ApiResponse<Map<String, Object>> createTeacherScore(HttpServletRequest request,
                                                               @PathVariable Long submissionId,
                                                               @Valid @RequestBody DemoRequests.TeacherScoreRequest requestBody) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ApiResponse.success(demoPlatformService.createTeacherScore(userId, submissionId, requestBody));
    }

    @GetMapping("/assignments/{assignmentId}/evaluations")
    public ApiResponse<PageResult<DemoViews.EvaluationRecordVo>> getEvaluations(HttpServletRequest request, @PathVariable Long assignmentId,
                                                                                 @RequestParam(required = false) Long submissionId,
                                                                                 @RequestParam(required = false) Long evaluatorUserId,
                                                                                 @RequestParam(required = false) String reviewStatus,
                                                                                 @RequestParam(defaultValue = "false") boolean abnormalOnly,
                                                                                 @RequestParam(defaultValue = "1") int pageNo,
                                                                                 @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ApiResponse.success(PageResult.of(
            demoPlatformService.getEvaluations(userId, assignmentId, submissionId, evaluatorUserId, reviewStatus, abnormalOnly),
            pageNo,
            pageSize
        ));
    }

    @PatchMapping("/evaluations/{evaluationId}/review")
    public ApiResponse<Map<String, Object>> reviewEvaluation(HttpServletRequest request, @PathVariable Long evaluationId,
                                                             @RequestParam(defaultValue = "true") boolean excluded) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ApiResponse.success(demoPlatformService.reviewEvaluation(userId, evaluationId, excluded));
    }

    @PostMapping("/assignments/{assignmentId}/blacklist")
    public ApiResponse<Map<String, Object>> addBlacklist(HttpServletRequest request, @PathVariable Long assignmentId,
                                                         @RequestParam Long evaluatorUserId,
                                                         @RequestParam Long targetSubmissionId) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ApiResponse.success(demoPlatformService.addBlacklist(userId, assignmentId, evaluatorUserId, targetSubmissionId));
    }

    @DeleteMapping("/assignments/{assignmentId}/blacklist")
    public ApiResponse<Map<String, Object>> removeBlacklist(HttpServletRequest request, @PathVariable Long assignmentId,
                                                            @RequestParam Long evaluatorUserId,
                                                            @RequestParam Long targetSubmissionId) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ApiResponse.success(demoPlatformService.removeBlacklist(userId, assignmentId, evaluatorUserId, targetSubmissionId));
    }

    @PatchMapping("/assignments/{assignmentId}/publish-results")
    public ApiResponse<Map<String, Object>> publishResults(HttpServletRequest request, @PathVariable Long assignmentId) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ApiResponse.success(demoPlatformService.publishResults(userId, assignmentId));
    }

    @GetMapping("/assignments/{assignmentId}/stats")
    public ApiResponse<DemoViews.TeacherStatsVo> getStats(HttpServletRequest request, @PathVariable Long assignmentId) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ApiResponse.success(demoPlatformService.getTeacherStats(userId, assignmentId));
    }
}
