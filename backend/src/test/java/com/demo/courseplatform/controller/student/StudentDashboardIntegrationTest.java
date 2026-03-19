package com.demo.courseplatform.controller.student;

import com.demo.courseplatform.AbstractIntegrationTest;
import com.demo.courseplatform.security.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentDashboardIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TokenService tokenService;

    @Test
    void shouldReturnEmptyDashboardWhenStudentHasNotSubmittedYet() throws Exception {
        setAssignmentDeadlineHoursFromNow(1002L, 24);

        mockMvc.perform(get("/api/v1/student/assignments/1002/my-dashboard")
                .header("Authorization", bearer(4L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.submission").isEmpty())
            .andExpect(jsonPath("$.data.summary.published").value(false))
            .andExpect(jsonPath("$.data.summary.totalProjects").value(0))
            .andExpect(jsonPath("$.data.displayStatus").value("提交中"));
    }

    private String bearer(Long userId) {
        return "Bearer " + tokenService.issueToken(userId);
    }
}
