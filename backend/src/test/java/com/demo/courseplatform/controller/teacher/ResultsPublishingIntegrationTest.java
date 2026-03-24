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
    void shouldFreezeScoresAutomaticallyAfterTeacherWindowEnds() throws Exception {
        setAssignmentDeadlineHoursFromNow(1003L, -2);

        mockMvc.perform(get("/api/v1/student/assignments/1003/my-dashboard")
                .header("Authorization", bearer(4L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.summary.published").value(false))
            .andExpect(jsonPath("$.data.summary.finalScore").isEmpty())
            .andExpect(jsonPath("$.data.leaderboardType").value("REALTIME"))
            .andExpect(jsonPath("$.data.displayStatus").value("学生互评与教师评分中"));

        setAssignmentDeadlineHoursFromNow(1003L, -50);

        mockMvc.perform(get("/api/v1/student/assignments/1003/my-dashboard")
                .header("Authorization", bearer(4L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.summary.published").value(true))
            .andExpect(jsonPath("$.data.summary.finalScore").isNumber())
            .andExpect(jsonPath("$.data.leaderboardType").value("FINAL"))
            .andExpect(jsonPath("$.data.displayStatus").value("最终成绩已生成"));

        mockMvc.perform(get("/api/v1/teacher/assignments/1003/stats")
                .header("Authorization", bearer(2L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.resultsPublished").value(true))
            .andExpect(jsonPath("$.data.leaderboardType").value("FINAL"));
    }

    @Test
    void shouldRejectTeacherReviewAfterAutomaticFinalization() throws Exception {
        setAssignmentDeadlineHoursFromNow(1003L, -50);

        mockMvc.perform(patch("/api/v1/teacher/evaluations/8021/review")
                .header("Authorization", bearer(2L))
                .param("excluded", "true"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(4090));
    }

    @Test
    void shouldBuildScoreDistributionFromProjectScores() throws Exception {
        setAssignmentDeadlineHoursFromNow(1001L, -2);

        mockMvc.perform(get("/api/v1/teacher/assignments/1001/stats")
                .header("Authorization", bearer(2L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalSubmissions").value(3))
            .andExpect(jsonPath("$.data.scoreDistribution[0].name").value("90-100"))
            .andExpect(jsonPath("$.data.scoreDistribution[0].value").value(1.0))
            .andExpect(jsonPath("$.data.scoreDistribution[1].name").value("80-89"))
            .andExpect(jsonPath("$.data.scoreDistribution[1].value").value(2.0))
            .andExpect(jsonPath("$.data.scoreDistribution[2].name").value("<80"))
            .andExpect(jsonPath("$.data.scoreDistribution[2].value").value(0.0));
    }

    private String bearer(Long userId) {
        return "Bearer " + tokenService.issueToken(userId);
    }
}
