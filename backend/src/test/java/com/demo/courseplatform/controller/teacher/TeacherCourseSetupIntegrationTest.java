package com.demo.courseplatform.controller.teacher;

import com.demo.courseplatform.AbstractIntegrationTest;
import com.demo.courseplatform.security.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeacherCourseSetupIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TokenService tokenService;

    @Test
    void shouldCreateCourseAndAutoJoinTeacher() throws Exception {
        mockMvc.perform(post("/api/v1/teacher/courses")
                .header("Authorization", bearer(2L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "AI2026",
                      "name": "智能系统工程",
                      "term": "2026 春",
                      "courseDeadline": "2026-06-30T23:59:00"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.code").value("AI2026"))
            .andExpect(jsonPath("$.data.roleInCourse").value("TEACHER"))
            .andExpect(jsonPath("$.data.courseDeadline").value("2026-06-30 23:59:00"));

        Long createdCourseId = jdbcTemplate.queryForObject("SELECT id FROM course WHERE code = 'AI2026'", Long.class);
        Integer memberCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM course_member WHERE course_id = ? AND user_id = 2 AND course_role = 'TEACHER'",
            Integer.class,
            createdCourseId
        );

        org.junit.jupiter.api.Assertions.assertEquals(1, memberCount);

        mockMvc.perform(get("/api/v1/teacher/courses")
                .header("Authorization", bearer(2L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.list[?(@.code == 'AI2026')].courseDeadline").value("2026-06-30 23:59:00"));
    }

    @Test
    void shouldCreateAssignmentForTeacherOwnedCourse() throws Exception {
        mockMvc.perform(post("/api/v1/teacher/courses")
                .header("Authorization", bearer(2L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "SELAB2026",
                      "name": "软件工程实验",
                      "term": "2026 春",
                      "courseDeadline": "2026-06-30T23:59:00"
                    }
                    """))
            .andExpect(status().isOk());

        Long createdCourseId = jdbcTemplate.queryForObject("SELECT id FROM course WHERE code = 'SELAB2026'", Long.class);

        mockMvc.perform(post("/api/v1/teacher/courses/{courseId}/assignments", createdCourseId)
                .header("Authorization", bearer(2L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "阶段项目一",
                      "mode": "GROUP",
                      "description": "用于课程项目阶段验收",
                      "deadline": "2026-06-20T23:59:00",
                      "allowLate": true,
                      "peerWeight": 40,
                      "teacherWeight": 60,
                      "status": "SUBMITTING"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.courseId").value(createdCourseId))
            .andExpect(jsonPath("$.data.title").value("阶段项目一"))
            .andExpect(jsonPath("$.data.deadline").value("2026-06-20 23:59:00"))
            .andExpect(jsonPath("$.data.status").value("SUBMITTING"));
    }

    @Test
    void shouldRejectAssignmentCreationForForeignCourse() throws Exception {
        mockMvc.perform(post("/api/v1/teacher/courses/103/assignments")
                .header("Authorization", bearer(2L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "越权作业",
                      "mode": "GROUP",
                      "description": "不应创建成功",
                      "deadline": "2026-03-30T23:59:00",
                      "allowLate": true,
                      "peerWeight": 40,
                      "teacherWeight": 60,
                      "status": "SUBMITTING"
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(4030))
            .andExpect(jsonPath("$.message").value("当前用户不是该课程教师，不能创建作业"));
    }

    @Test
    void shouldRejectAssignmentDeadlineAfterCourseDeadline() throws Exception {
        mockMvc.perform(post("/api/v1/teacher/courses")
                .header("Authorization", bearer(2L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "UX2026",
                      "name": "用户体验设计",
                      "term": "2026 春",
                      "courseDeadline": "2026-05-01T23:59:00"
                    }
                    """))
            .andExpect(status().isOk());

        Long createdCourseId = jdbcTemplate.queryForObject("SELECT id FROM course WHERE code = 'UX2026'", Long.class);

        mockMvc.perform(post("/api/v1/teacher/courses/{courseId}/assignments", createdCourseId)
                .header("Authorization", bearer(2L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "课程结课项目",
                      "mode": "INDIVIDUAL",
                      "description": "截止时间超过课程时间",
                      "deadline": "2026-05-02T00:00:00",
                      "allowLate": false,
                      "peerWeight": 30,
                      "teacherWeight": 70,
                      "status": "SUBMITTING"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(4000))
            .andExpect(jsonPath("$.message").value("作业截止时间不能晚于课程截止时间"));
    }

    private String bearer(Long userId) {
        return "Bearer " + tokenService.issueToken(userId);
    }
}
