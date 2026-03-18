package com.demo.courseplatform.controller.admin;

import com.demo.courseplatform.AbstractIntegrationTest;
import com.demo.courseplatform.security.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CourseMemberManagementIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TokenService tokenService;

    @Test
    void shouldAddCourseMemberAndExposeCourseToStudent() throws Exception {
        mockMvc.perform(post("/api/v1/admin/courses/102/members")
                .header("Authorization", bearer(1L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId":12,"courseRole":"STUDENT"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.userId").value(12))
            .andExpect(jsonPath("$.data.courseRole").value("STUDENT"));

        mockMvc.perform(get("/api/v1/student/courses")
                .header("Authorization", bearer(12L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.list[?(@.id == 102)].name").isNotEmpty());
    }

    @Test
    void shouldRejectDuplicateCourseMember() throws Exception {
        String payload = """
            {"userId":12,"courseRole":"STUDENT"}
            """;

        mockMvc.perform(post("/api/v1/admin/courses/102/members")
                .header("Authorization", bearer(1L))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/courses/102/members")
                .header("Authorization", bearer(1L))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(4090));
    }

    @Test
    void shouldRemoveCourseMemberAndHideCourseFromStudent() throws Exception {
        mockMvc.perform(post("/api/v1/admin/courses/102/members")
                .header("Authorization", bearer(1L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId":12,"courseRole":"STUDENT"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/admin/courses/102/members/12")
                .header("Authorization", bearer(1L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.removed").value(true));

        mockMvc.perform(get("/api/v1/student/courses")
                .header("Authorization", bearer(12L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.list[?(@.id == 102)]").isEmpty());
    }

    private String bearer(Long userId) {
        return "Bearer " + tokenService.issueToken(userId);
    }
}
