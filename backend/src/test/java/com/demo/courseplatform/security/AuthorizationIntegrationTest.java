package com.demo.courseplatform.security;

import com.demo.courseplatform.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    void adminShouldAccessTeacherApi() throws Exception {
        mockMvc.perform(get("/api/v1/teacher/assignments/1001/stats")
                .header("Authorization", bearer(1L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.totalSubmissions").value(3));
    }

    private String bearer(Long userId) {
        return "Bearer " + tokenService.issueToken(userId);
    }
}
