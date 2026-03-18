package com.demo.courseplatform.controller.student;

import com.demo.courseplatform.common.ApiResponse;
import com.demo.courseplatform.common.PageResult;
import com.demo.courseplatform.domain.dto.DemoRequests;
import com.demo.courseplatform.domain.vo.DemoViews;
import com.demo.courseplatform.service.DemoPlatformService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student")
public class StudentController {

    private final DemoPlatformService demoPlatformService;

    public StudentController(DemoPlatformService demoPlatformService) {
        this.demoPlatformService = demoPlatformService;
    }

    @GetMapping("/home")
    public ApiResponse<DemoViews.StudentHomeVo> getHome(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ApiResponse.success(demoPlatformService.getStudentHome(userId));
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
        return ApiResponse.success(demoPlatformService.getStudentAssignmentDetail(userId, assignmentId));
    }

    @GetMapping("/assignments/{assignmentId}/my-submission")
    public ApiResponse<DemoViews.SubmissionVo> getMySubmission(HttpServletRequest request, @PathVariable Long assignmentId) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ApiResponse.success(demoPlatformService.getMySubmission(userId, assignmentId));
    }

    @PostMapping("/assignments/{assignmentId}/submit")
    public ApiResponse<DemoViews.SubmissionVo> submit(HttpServletRequest request, @PathVariable Long assignmentId,
                                                      @Valid @RequestBody DemoRequests.UpsertSubmissionRequest requestBody) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ApiResponse.success(demoPlatformService.upsertSubmission(userId, assignmentId, requestBody));
    }

    @PutMapping("/submissions/{submissionId}")
    public ApiResponse<DemoViews.SubmissionVo> updateSubmission(HttpServletRequest request, @PathVariable Long submissionId,
                                                                @Valid @RequestBody DemoRequests.UpsertSubmissionRequest requestBody) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ApiResponse.success(demoPlatformService.updateSubmission(userId, submissionId, requestBody));
    }

    @GetMapping("/assignments/{assignmentId}/projects")
    public ApiResponse<PageResult<DemoViews.ProjectCardVo>> getProjects(HttpServletRequest request,
                                                                        @PathVariable Long assignmentId,
                                                                        @RequestParam(defaultValue = "1") int pageNo,
                                                                        @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ApiResponse.success(PageResult.of(demoPlatformService.getProjects(userId, assignmentId), pageNo, pageSize));
    }

    @PostMapping("/projects/{submissionId}/evaluations")
    public ApiResponse<Map<String, Object>> createEvaluation(HttpServletRequest request,
                                                             @PathVariable Long submissionId,
                                                             @Valid @RequestBody DemoRequests.CreateEvaluationRequest requestBody) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ApiResponse.success(demoPlatformService.createEvaluation(userId, submissionId, requestBody));
    }

    @GetMapping("/assignments/{assignmentId}/my-dashboard")
    public ApiResponse<DemoViews.DashboardVo> getDashboard(HttpServletRequest request, @PathVariable Long assignmentId) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ApiResponse.success(demoPlatformService.getDashboard(userId, assignmentId));
    }

    @GetMapping("/assignments/{assignmentId}/leaderboard")
    public ApiResponse<PageResult<DemoViews.LeaderboardItemVo>> getLeaderboard(HttpServletRequest request,
                                                                                @PathVariable Long assignmentId,
                                                                                @RequestParam(defaultValue = "1") int pageNo,
                                                                                @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ApiResponse.success(PageResult.of(demoPlatformService.getLeaderboard(userId, assignmentId), pageNo, pageSize));
    }
}
