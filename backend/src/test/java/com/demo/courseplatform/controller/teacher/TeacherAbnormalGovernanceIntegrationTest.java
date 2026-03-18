package com.demo.courseplatform.controller.teacher;

import com.demo.courseplatform.AbstractIntegrationTest;
import com.demo.courseplatform.security.TokenService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeacherAbnormalGovernanceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TokenService tokenService;

    @Test
    void shouldRecomputeSubmissionAbnormalitiesAfterThirdStudentReview() throws Exception {
        jdbcTemplate.update("UPDATE sys_user SET first_login_reset_required = 0 WHERE id IN (3, 4, 9)");
        deleteStudentEvaluations(5003L);

        createStudentEvaluation(3L, 5003L, 8.5);
        createStudentEvaluation(4L, 5003L, 8.5);
        createStudentEvaluation(9L, 5003L, 10.0);

        mockMvc.perform(get("/api/v1/teacher/assignments/1001/evaluations")
                .header("Authorization", bearer(2L))
                .param("submissionId", "5003")
                .param("evaluatorUserId", "9")
                .param("abnormalOnly", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.list.length()").value(1))
            .andExpect(jsonPath("$.data.list[0].abnormal").value(true))
            .andExpect(jsonPath("$.data.list[0].itemScores.length()").value(4))
            .andExpect(jsonPath("$.data.list[0].abnormalReason").value(org.hamcrest.Matchers.containsString("均值偏差")))
            .andExpect(jsonPath("$.data.list[0].abnormalReason").value(org.hamcrest.Matchers.containsString("中位数偏差")));

        assertAbnormal(5003L, 3L, false);
        assertAbnormal(5003L, 4L, false);
        assertAbnormal(5003L, 9L, true);
    }

    @Test
    void shouldFilterEvaluationsAndSyncScoresAfterIgnoreAndRestore() throws Exception {
        mockMvc.perform(get("/api/v1/student/assignments/1001/my-dashboard")
                .header("Authorization", bearer(5L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.summary.peerScore").value(89.0))
            .andExpect(jsonPath("$.data.summary.realtimeFinalScore").value(90.2));

        mockMvc.perform(patch("/api/v1/teacher/evaluations/8007/review")
                .header("Authorization", bearer(2L))
                .param("excluded", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.excluded").value(true))
            .andExpect(jsonPath("$.data.reviewStatus").value("IGNORED"));

        mockMvc.perform(get("/api/v1/student/assignments/1001/my-dashboard")
                .header("Authorization", bearer(5L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.summary.peerScore").value(90.0))
            .andExpect(jsonPath("$.data.summary.realtimeFinalScore").value(90.6));

        mockMvc.perform(get("/api/v1/teacher/assignments/1001/evaluations")
                .header("Authorization", bearer(2L))
                .param("submissionId", "5002")
                .param("evaluatorUserId", "6")
                .param("reviewStatus", "IGNORED")
                .param("abnormalOnly", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.list.length()").value(1))
            .andExpect(jsonPath("$.data.list[0].reviewStatus").value("IGNORED"))
            .andExpect(jsonPath("$.data.list[0].excluded").value(true));

        mockMvc.perform(get("/api/v1/teacher/assignments/1001/stats")
                .header("Authorization", bearer(2L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.abnormalIgnoredCount").value(1))
            .andExpect(jsonPath("$.data.abnormalHandledCount").value(1))
            .andExpect(jsonPath("$.data.abnormalImpacts.length()").value(1));

        mockMvc.perform(patch("/api/v1/teacher/evaluations/8007/review")
                .header("Authorization", bearer(2L))
                .param("excluded", "false"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.excluded").value(false))
            .andExpect(jsonPath("$.data.reviewStatus").value("RESTORED"));

        mockMvc.perform(get("/api/v1/student/assignments/1001/my-dashboard")
                .header("Authorization", bearer(5L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.summary.peerScore").value(89.0))
            .andExpect(jsonPath("$.data.summary.realtimeFinalScore").value(90.2));

        mockMvc.perform(get("/api/v1/teacher/assignments/1001/stats")
                .header("Authorization", bearer(2L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.abnormalRestoredCount").value(1))
            .andExpect(jsonPath("$.data.abnormalImpacts.length()").value(0));
    }

    private void createStudentEvaluation(Long userId, Long submissionId, double score) throws Exception {
        mockMvc.perform(post("/api/v1/student/projects/{submissionId}/evaluations", submissionId)
                .header("Authorization", bearer(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "itemScores": [
                        { "rubricItemId": 2001, "score": SCORE_VALUE, "comment": "自动化测试" },
                        { "rubricItemId": 2002, "score": SCORE_VALUE, "comment": "自动化测试" },
                        { "rubricItemId": 2003, "score": SCORE_VALUE, "comment": "自动化测试" },
                        { "rubricItemId": 2004, "score": SCORE_VALUE, "comment": "自动化测试" }
                      ],
                      "overallComment": "异常重算测试"
                    }
                    """.replace("SCORE_VALUE", Double.toString(score))))
            .andExpect(status().isOk());
    }

    private void deleteStudentEvaluations(Long submissionId) {
        List<Long> evaluationIds = jdbcTemplate.queryForList(
            "SELECT id FROM evaluation WHERE submission_id = ? AND evaluator_role = 'STUDENT'",
            Long.class,
            submissionId
        );
        if (!evaluationIds.isEmpty()) {
            String idList = evaluationIds.stream().map(String::valueOf).reduce((left, right) -> left + "," + right).orElse("");
            jdbcTemplate.execute("DELETE FROM evaluation_item WHERE evaluation_id IN (" + idList + ")");
            jdbcTemplate.execute("DELETE FROM evaluation WHERE id IN (" + idList + ")");
        }
    }

    private void assertAbnormal(Long submissionId, Long evaluatorUserId, boolean abnormal) {
        Boolean actual = jdbcTemplate.queryForObject(
            "SELECT is_abnormal FROM evaluation WHERE submission_id = ? AND evaluator_user_id = ? AND evaluator_role = 'STUDENT'",
            Boolean.class,
            submissionId,
            evaluatorUserId
        );
        org.junit.jupiter.api.Assertions.assertEquals(abnormal, actual);
    }

    private String bearer(Long userId) {
        return "Bearer " + tokenService.issueToken(userId);
    }
}
