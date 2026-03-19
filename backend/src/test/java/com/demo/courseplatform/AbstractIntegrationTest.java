package com.demo.courseplatform;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected DataSource dataSource;

    @Autowired
    protected StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void resetState() {
        List<String> drops = List.of(
            "DROP TABLE IF EXISTS evaluation_blacklist",
            "DROP TABLE IF EXISTS evaluation_item",
            "DROP TABLE IF EXISTS evaluation",
            "DROP TABLE IF EXISTS rubric_item",
            "DROP TABLE IF EXISTS rubric",
            "DROP TABLE IF EXISTS submission",
            "DROP TABLE IF EXISTS assignment_group_member",
            "DROP TABLE IF EXISTS assignment_group",
            "DROP TABLE IF EXISTS assignment",
            "DROP TABLE IF EXISTS course_member",
            "DROP TABLE IF EXISTS course",
            "DROP TABLE IF EXISTS sys_user"
        );
        drops.forEach(jdbcTemplate::execute);

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
            new ClassPathResource("db/schema.sql"),
            new ClassPathResource("db/seed.sql")
        );
        populator.execute(dataSource);

        Set<String> tokenKeys = stringRedisTemplate.keys("course-platform-demo:test:token:*");
        if (tokenKeys != null && !tokenKeys.isEmpty()) {
            stringRedisTemplate.delete(tokenKeys);
        }
    }

    protected void setAssignmentDeadlineHoursFromNow(long assignmentId, long hoursOffset) {
        jdbcTemplate.update(
            "UPDATE assignment SET deadline = ?, results_published = 0, results_published_at = NULL, status = 'SUBMITTING' WHERE id = ?",
            LocalDateTime.now().plusHours(hoursOffset),
            assignmentId
        );
    }
}
