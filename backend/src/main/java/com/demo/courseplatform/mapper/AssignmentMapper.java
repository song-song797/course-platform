package com.demo.courseplatform.mapper;

import com.demo.courseplatform.domain.entity.PersistenceModels.AssignmentEntity;
import com.demo.courseplatform.domain.entity.PersistenceModels.RubricEntity;
import com.demo.courseplatform.domain.entity.PersistenceModels.RubricItemEntity;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AssignmentMapper {

    @Select("""
        SELECT id, course_id, title, mode, description, deadline, allow_late, peer_weight, teacher_weight, status,
               results_published AS resultsPublished, results_published_at AS resultsPublishedAt, created_at
        FROM assignment
        WHERE id = #{id}
        """)
    AssignmentEntity findById(@Param("id") Long id);

    @Select("""
        <script>
        SELECT id, course_id, title, mode, description, deadline, allow_late, peer_weight, teacher_weight, status,
               results_published AS resultsPublished, results_published_at AS resultsPublishedAt, created_at
        FROM assignment
        WHERE course_id IN
        <foreach collection="courseIds" item="courseId" open="(" separator="," close=")">
            #{courseId}
        </foreach>
        ORDER BY id
        </script>
        """)
    List<AssignmentEntity> findByCourseIds(@Param("courseIds") List<Long> courseIds);

    @Select("""
        SELECT id, course_id, title, mode, description, deadline, allow_late, peer_weight, teacher_weight, status,
               results_published AS resultsPublished, results_published_at AS resultsPublishedAt, created_at
        FROM assignment
        WHERE course_id = #{courseId}
        ORDER BY id
        """)
    List<AssignmentEntity> findByCourseId(@Param("courseId") Long courseId);

    @Insert("""
        INSERT INTO assignment (
            course_id, title, mode, description, deadline, allow_late, peer_weight, teacher_weight, status,
            results_published, results_published_at
        )
        VALUES (
            #{courseId}, #{title}, #{mode}, #{description}, #{deadline}, #{allowLate}, #{peerWeight}, #{teacherWeight}, #{status},
            #{resultsPublished}, #{resultsPublishedAt}
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AssignmentEntity entity);

    @Update("""
        UPDATE assignment
        SET title = #{title},
            mode = #{mode},
            description = #{description},
            deadline = #{deadline},
            allow_late = #{allowLate},
            peer_weight = #{peerWeight},
            teacher_weight = #{teacherWeight},
            status = #{status},
            results_published = #{resultsPublished},
            results_published_at = #{resultsPublishedAt}
        WHERE id = #{id}
        """)
    int update(AssignmentEntity entity);

    @Update("""
        UPDATE assignment
        SET status = #{status},
            results_published = #{resultsPublished},
            results_published_at = #{resultsPublishedAt}
        WHERE id = #{id}
        """)
    int updatePublishStatus(AssignmentEntity entity);

    @Select("""
        SELECT id, assignment_id, version_no, is_active AS active, created_at
        FROM rubric
        WHERE assignment_id = #{assignmentId}
        LIMIT 1
        """)
    RubricEntity findRubricByAssignmentId(@Param("assignmentId") Long assignmentId);

    @Insert("""
        INSERT INTO rubric (assignment_id, version_no, is_active)
        VALUES (#{assignmentId}, #{versionNo}, #{active})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRubric(RubricEntity entity);

    @Update("""
        UPDATE rubric
        SET version_no = #{versionNo},
            is_active = #{active}
        WHERE id = #{id}
        """)
    int updateRubric(RubricEntity entity);

    @Select("""
        SELECT ri.id, ri.rubric_id, ri.item_name, ri.description, ri.weight
        FROM rubric_item ri
        INNER JOIN rubric r ON r.id = ri.rubric_id
        WHERE r.assignment_id = #{assignmentId}
        ORDER BY ri.id
        """)
    List<RubricItemEntity> findRubricItemsByAssignmentId(@Param("assignmentId") Long assignmentId);

    @Delete("""
        DELETE FROM rubric_item
        WHERE rubric_id = #{rubricId}
        """)
    int deleteRubricItemsByRubricId(@Param("rubricId") Long rubricId);

    @Insert("""
        <script>
        INSERT INTO rubric_item (rubric_id, item_name, description, weight)
        VALUES
        <foreach collection="items" item="item" separator=",">
            (#{item.rubricId}, #{item.itemName}, #{item.description}, #{item.weight})
        </foreach>
        </script>
        """)
    int insertRubricItems(@Param("items") List<RubricItemEntity> items);
}
