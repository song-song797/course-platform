package com.demo.courseplatform.controller.student;

import com.demo.courseplatform.AbstractIntegrationTest;
import com.demo.courseplatform.security.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentLeaderboardAuthorizationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TokenService tokenService;

    @Test
    void courseStudentShouldAccessLeaderboard() throws Exception {
        mockMvc.perform(get("/api/v1/student/assignments/1001/leaderboard")
                .header("Authorization", bearer(4L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.list.length()").value(3));
    }

    @Test
    void outsiderStudentShouldBeRejected() throws Exception {
        mockMvc.perform(get("/api/v1/student/assignments/1001/leaderboard")
                .header("Authorization", bearer(12L)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(4030));
    }

    private String bearer(Long userId) {
        return "Bearer " + tokenService.issueToken(userId);
    }
}
