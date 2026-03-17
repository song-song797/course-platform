package com.demo.courseplatform.controller.student;

import com.demo.courseplatform.AbstractIntegrationTest;
import com.demo.courseplatform.security.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentGroupWorkflowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TokenService tokenService;

    @Test
    void shouldRejectGroupSubmissionForUngroupedStudent() throws Exception {
        jdbcTemplate.update("UPDATE assignment SET status = 'SUBMITTING' WHERE id = 1001");

        mockMvc.perform(post("/api/v1/student/assignments/1001/submit")
                .header("Authorization", bearer(10L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "projectName": "Ungrouped Demo",
                      "repoUrl": "https://github.com/demo/ungrouped-demo",
                      "memberUserIds": [10]
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(4090));
    }

    @Test
    void shouldIgnoreForgedMemberListAndBindSubmissionToRealGroup() throws Exception {
        jdbcTemplate.update("UPDATE assignment SET status = 'SUBMITTING' WHERE id = 1001");

        mockMvc.perform(post("/api/v1/student/assignments/1001/submit")
                .header("Authorization", bearer(4L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "projectName": "Campus Pair Updated",
                      "repoUrl": "https://github.com/demo/campus-pair-updated",
                      "memberUserIds": [3, 9, 10]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.groupId").value(3001))
            .andExpect(jsonPath("$.data.groupName").value("Campus Pair"))
            .andExpect(jsonPath("$.data.members.length()").value(2))
            .andExpect(jsonPath("$.data.members[0].id").value(3))
            .andExpect(jsonPath("$.data.members[1].id").value(4));
    }

    @Test
    void shouldCreateSingleMemberGroupForIndividualAssignmentSubmission() throws Exception {
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
    }

    @Test
    void shouldAllowUngroupedStudentToEvaluateOtherGroups() throws Exception {
        mockMvc.perform(post("/api/v1/student/projects/5002/evaluations")
                .header("Authorization", bearer(10L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "itemScores": [
                        { "rubricItemId": 2001, "score": 8.5, "comment": "完成度不错" },
                        { "rubricItemId": 2002, "score": 8.0, "comment": "结构清晰" },
                        { "rubricItemId": 2003, "score": 8.5, "comment": "有亮点" },
                        { "rubricItemId": 2004, "score": 8.0, "comment": "演示稳定" }
                      ],
                      "overallComment": "未分组学生也可正常互评"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.saved").value(true));
    }

    private String bearer(Long userId) {
        return "Bearer " + tokenService.issueToken(userId);
    }
}
