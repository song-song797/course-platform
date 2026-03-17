package com.demo.courseplatform.controller.admin;

import com.demo.courseplatform.common.ApiResponse;
import com.demo.courseplatform.common.PageResult;
import com.demo.courseplatform.domain.dto.DemoRequests;
import com.demo.courseplatform.domain.vo.DemoViews;
import com.demo.courseplatform.service.DemoPlatformService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final DemoPlatformService demoPlatformService;

    public AdminController(DemoPlatformService demoPlatformService) {
        this.demoPlatformService = demoPlatformService;
    }

    @PostMapping("/users/import")
    public ApiResponse<Map<String, Object>> importUsers(@RequestParam("file") MultipartFile file) throws IOException {
        return ApiResponse.success(demoPlatformService.importUsers(file));
    }

    @GetMapping("/courses")
    public ApiResponse<PageResult<DemoViews.CourseCardVo>> getCourses(@RequestParam(defaultValue = "1") int pageNo,
                                                                      @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(PageResult.of(demoPlatformService.getAllCourses(), pageNo, pageSize));
    }

    @PostMapping("/courses")
    public ApiResponse<DemoViews.CourseCardVo> createCourse(@Valid @RequestBody DemoRequests.CreateCourseRequest request) {
        return ApiResponse.success(demoPlatformService.createCourse(request));
    }

    @GetMapping("/courses/{courseId}/members")
    public ApiResponse<DemoViews.CourseMemberManageVo> getCourseMembers(@PathVariable Long courseId) {
        return ApiResponse.success(demoPlatformService.getCourseMemberManage(courseId));
    }

    @PostMapping("/courses/{courseId}/members")
    public ApiResponse<DemoViews.CourseMemberVo> addCourseMember(@PathVariable Long courseId,
                                                                 @Valid @RequestBody DemoRequests.CourseMemberRequest request) {
        return ApiResponse.success(demoPlatformService.addCourseMember(courseId, request));
    }

    @DeleteMapping("/courses/{courseId}/members/{userId}")
    public ApiResponse<Map<String, Object>> removeCourseMember(@PathVariable Long courseId, @PathVariable Long userId) {
        return ApiResponse.success(demoPlatformService.removeCourseMember(courseId, userId));
    }

    @PostMapping("/courses/{courseId}/assignments")
    public ApiResponse<DemoViews.AssignmentDetailVo> createAssignment(@PathVariable Long courseId,
                                                                      @Valid @RequestBody DemoRequests.CreateAssignmentRequest request) {
        return ApiResponse.success(demoPlatformService.createAssignment(courseId, request));
    }

    @PutMapping("/assignments/{assignmentId}")
    public ApiResponse<DemoViews.AssignmentDetailVo> updateAssignment(@PathVariable Long assignmentId,
                                                                      @Valid @RequestBody DemoRequests.CreateAssignmentRequest request) {
        return ApiResponse.success(demoPlatformService.updateAssignment(assignmentId, request));
    }
}
