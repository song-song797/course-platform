package com.demo.courseplatform.controller.teacher;

import com.demo.courseplatform.AbstractIntegrationTest;
import com.demo.courseplatform.security.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResultsPublishingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TokenService tokenService;

    @Test
    void shouldHideFinalScoreBeforePublishAndFreezeAfterPublish() throws Exception {
        mockMvc.perform(get("/api/v1/student/assignments/1003/my-dashboard")
                .header("Authorization", bearer(4L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.summary.published").value(false))
            .andExpect(jsonPath("$.data.summary.finalScore").isEmpty())
            .andExpect(jsonPath("$.data.leaderboardType").value("REALTIME"))
            .andExpect(jsonPath("$.data.displayStatus").value("互评中"));

        mockMvc.perform(patch("/api/v1/teacher/assignments/1003/publish-results")
                .header("Authorization", bearer(2L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.published").value(true))
            .andExpect(jsonPath("$.data.displayStatus").value("已发布最终成绩"));

        mockMvc.perform(get("/api/v1/student/assignments/1003/my-dashboard")
                .header("Authorization", bearer(4L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.summary.published").value(true))
            .andExpect(jsonPath("$.data.summary.finalScore").isNumber())
            .andExpect(jsonPath("$.data.leaderboardType").value("FINAL"))
            .andExpect(jsonPath("$.data.displayStatus").value("已发布最终成绩"));

        mockMvc.perform(get("/api/v1/teacher/assignments/1003/stats")
                .header("Authorization", bearer(2L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.resultsPublished").value(true))
            .andExpect(jsonPath("$.data.leaderboardType").value("FINAL"));
    }

    @Test
    void shouldRejectTeacherScoreChangeAfterPublish() throws Exception {
        mockMvc.perform(patch("/api/v1/teacher/assignments/1003/publish-results")
                .header("Authorization", bearer(2L)))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/teacher/evaluations/8021/review")
                .header("Authorization", bearer(2L))
                .param("excluded", "true"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(4090));
    }

    private String bearer(Long userId) {
        return "Bearer " + tokenService.issueToken(userId);
    }
}
