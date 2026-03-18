package com.demo.courseplatform.controller.student;

import com.demo.courseplatform.AbstractIntegrationTest;
import com.demo.courseplatform.security.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IndividualAssignmentWorkflowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TokenService tokenService;

    @Test
    void shouldKeepIndividualAssignmentWorkflowWorkingAfterRealGroupUpgrade() throws Exception {
        mockMvc.perform(post("/api/v1/student/assignments/1002/submit")
                .header("Authorization", bearer(4L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "projectName": "Personal Portfolio",
                      "repoUrl": "https://github.com/demo/personal-portfolio"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.groupId").isNumber())
            .andExpect(jsonPath("$.data.members.length()").value(1))
            .andExpect(jsonPath("$.data.members[0].id").value(4));

        Long submissionId = jdbcTemplate.queryForObject(
            "SELECT id FROM submission WHERE assignment_id = 1002 AND submitted_by = 4",
            Long.class
        );

        jdbcTemplate.update("UPDATE assignment SET status = 'REVIEWING' WHERE id = 1002");

        mockMvc.perform(post("/api/v1/teacher/submissions/" + submissionId + "/scores")
                .header("Authorization", bearer(2L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "itemScores": [
                        { "rubricItemId": 2101, "score": 8.5, "comment": "完成度稳定" },
                        { "rubricItemId": 2102, "score": 8.0, "comment": "结构清晰" },
                        { "rubricItemId": 2103, "score": 8.5, "comment": "有自己的亮点" },
                        { "rubricItemId": 2104, "score": 8.0, "comment": "表达完整" }
                      ],
                      "overallComment": "个人作业链路正常"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.saved").value(true));

        mockMvc.perform(patch("/api/v1/teacher/assignments/1002/publish-results")
                .header("Authorization", bearer(2L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.published").value(true));

        mockMvc.perform(get("/api/v1/student/assignments/1002/my-dashboard")
                .header("Authorization", bearer(4L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.summary.published").value(true))
            .andExpect(jsonPath("$.data.summary.finalScore").isNumber())
            .andExpect(jsonPath("$.data.summary.currentRank").value(1))
            .andExpect(jsonPath("$.data.summary.totalProjects").value(1))
            .andExpect(jsonPath("$.data.submission.members.length()").value(1))
            .andExpect(jsonPath("$.data.submission.members[0].id").value(4))
            .andExpect(jsonPath("$.data.leaderboardType").value("FINAL"))
            .andExpect(jsonPath("$.data.displayStatus").value("已发布最终成绩"));
    }

    private String bearer(Long userId) {
        return "Bearer " + tokenService.issueToken(userId);
    }
}
