package com.demo.courseplatform.controller.teacher;

import com.demo.courseplatform.AbstractIntegrationTest;
import com.demo.courseplatform.security.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeacherGroupManagementIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TokenService tokenService;

    @Test
    void shouldExposeGroupsAndUngroupedStudents() throws Exception {
        mockMvc.perform(get("/api/v1/teacher/assignments/1001/groups")
                .header("Authorization", bearer(2L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.groups.length()").value(3))
            .andExpect(jsonPath("$.data.ungroupedStudents.length()").value(2));
    }

    @Test
    void shouldCreateUpdateAndDeleteUnlockedGroup() throws Exception {
        mockMvc.perform(post("/api/v1/teacher/assignments/1001/groups")
                .header("Authorization", bearer(2L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "groupName": "Fresh Group",
                      "memberUserIds": [9, 10]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.groupName").value("Fresh Group"))
            .andExpect(jsonPath("$.data.members.length()").value(2));

        Long createdGroupId = jdbcTemplate.queryForObject(
            "SELECT id FROM assignment_group WHERE assignment_id = 1001 AND group_name = 'Fresh Group'",
            Long.class
        );

        mockMvc.perform(put("/api/v1/teacher/assignments/1001/groups/{groupId}", createdGroupId)
                .header("Authorization", bearer(2L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "groupName": "Fresh Group Renamed",
                      "memberUserIds": [9]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.groupName").value("Fresh Group Renamed"))
            .andExpect(jsonPath("$.data.members.length()").value(1));

        mockMvc.perform(delete("/api/v1/teacher/assignments/1001/groups/{groupId}", createdGroupId)
                .header("Authorization", bearer(2L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.removed").value(true));
    }

    @Test
    void shouldFreezeMembershipChangesAfterSubmissionButAllowRename() throws Exception {
        mockMvc.perform(put("/api/v1/teacher/assignments/1001/groups/3001")
                .header("Authorization", bearer(2L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "groupName": "Campus Pair",
                      "memberUserIds": [3]
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(4090));

        mockMvc.perform(put("/api/v1/teacher/assignments/1001/groups/3001")
                .header("Authorization", bearer(2L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "groupName": "Campus Pair Final",
                      "memberUserIds": [3, 4]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.groupName").value("Campus Pair Final"))
            .andExpect(jsonPath("$.data.memberLocked").value(true));

        mockMvc.perform(delete("/api/v1/teacher/assignments/1001/groups/3001")
                .header("Authorization", bearer(2L)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(4090));
    }

    private String bearer(Long userId) {
        return "Bearer " + tokenService.issueToken(userId);
    }
}
