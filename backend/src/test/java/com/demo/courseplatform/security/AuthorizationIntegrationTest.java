package com.demo.courseplatform.security;

import com.demo.courseplatform.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthorizationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TokenService tokenService;

    @Test
    void studentShouldNotAccessTeacherApi() throws Exception {
        mockMvc.perform(get("/api/v1/teacher/courses")
                .header("Authorization", bearer(4L)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(4030));
    }

    @Test
    void teacherShouldNotAccessAdminApi() throws Exception {
        mockMvc.perform(get("/api/v1/admin/courses")
                .header("Authorization", bearer(2L)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(4030));
    }

    @Test
    void adminShouldNotAccessTeacherApi() throws Exception {
        mockMvc.perform(get("/api/v1/teacher/assignments/1001/stats")
                .header("Authorization", bearer(1L)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(4030));
    }

    @Test
    void adminAssignmentApiShouldBeUnavailable() throws Exception {
        mockMvc.perform(post("/api/v1/admin/courses/101/assignments")
                .header("Authorization", bearer(1L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "不应存在的管理员作业入口",
                      "mode": "GROUP",
                      "description": "管理员不再负责创建作业",
                      "deadline": "2026-03-30T23:59:00",
                      "allowLate": true,
                      "peerWeight": 40,
                      "teacherWeight": 60,
                      "status": "SUBMITTING"
                    }
                    """))
            .andExpect(status().isNotFound());
    }

    private String bearer(Long userId) {
        return "Bearer " + tokenService.issueToken(userId);
    }
}
