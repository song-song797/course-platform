package com.demo.courseplatform.mapper;

import com.demo.courseplatform.domain.entity.PersistenceModels.AssignmentGroupEntity;
import com.demo.courseplatform.domain.entity.PersistenceModels.AssignmentGroupMemberEntity;
import com.demo.courseplatform.domain.entity.PersistenceModels.SubmissionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SubmissionMapper {

    @Select("""
        SELECT ag.id, ag.assignment_id, ag.group_name, ag.created_at
        FROM assignment_group ag
        INNER JOIN assignment_group_member gm ON gm.group_id = ag.id
        WHERE ag.assignment_id = #{assignmentId}
          AND gm.user_id = #{userId}
        LIMIT 1
        """)
    AssignmentGroupEntity findGroupByAssignmentAndUser(@Param("assignmentId") Long assignmentId, @Param("userId") Long userId);

    @Select("""
        SELECT id, assignment_id, group_name, created_at
        FROM assignment_group
        WHERE assignment_id = #{assignmentId}
        ORDER BY id
        """)
    List<AssignmentGroupEntity> findGroupsByAssignmentId(@Param("assignmentId") Long assignmentId);

    @Select("""
        SELECT id, assignment_id, group_name, created_at
        FROM assignment_group
        WHERE id = #{id}
        """)
    AssignmentGroupEntity findGroupById(@Param("id") Long id);

    @Insert("""
        INSERT INTO assignment_group (assignment_id, group_name)
        VALUES (#{assignmentId}, #{groupName})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertGroup(AssignmentGroupEntity entity);

    @Update("""
        UPDATE assignment_group
        SET group_name = #{groupName}
        WHERE id = #{id}
        """)
    int updateGroup(AssignmentGroupEntity entity);

    @Delete("""
        DELETE FROM assignment_group
        WHERE id = #{groupId}
        """)
    int deleteGroup(@Param("groupId") Long groupId);

    @Select("""
        SELECT id, assignment_id, group_id, user_id, created_at
        FROM assignment_group_member
        WHERE assignment_id = #{assignmentId}
        ORDER BY group_id, id
        """)
    List<AssignmentGroupMemberEntity> findGroupMembersByAssignmentId(@Param("assignmentId") Long assignmentId);

    @Select("""
        <script>
        SELECT id, assignment_id, group_id, user_id, created_at
        FROM assignment_group_member
        WHERE group_id IN
        <foreach collection="groupIds" item="groupId" open="(" separator="," close=")">
            #{groupId}
        </foreach>
        ORDER BY group_id, id
        </script>
        """)
    List<AssignmentGroupMemberEntity> findGroupMembersByGroupIds(@Param("groupIds") List<Long> groupIds);

    @Delete("""
        DELETE FROM assignment_group_member
        WHERE group_id = #{groupId}
        """)
    int deleteGroupMembersByGroupId(@Param("groupId") Long groupId);

    @Insert("""
        <script>
        INSERT INTO assignment_group_member (assignment_id, group_id, user_id)
        VALUES
        <foreach collection="members" item="item" separator=",">
            (#{item.assignmentId}, #{item.groupId}, #{item.userId})
        </foreach>
        </script>
        """)
    int insertGroupMembers(@Param("members") List<AssignmentGroupMemberEntity> members);

    @Select("""
        SELECT id, assignment_id, group_id, project_name, repo_url, video_url, preview_url, doc_url, attachment_url,
               description, submitted_by, submitted_at, is_late AS late, created_at, updated_at
        FROM submission
        WHERE group_id = #{groupId}
        LIMIT 1
        """)
    SubmissionEntity findSubmissionByGroupId(@Param("groupId") Long groupId);

    @Select("""
        SELECT id, assignment_id, group_id, project_name, repo_url, video_url, preview_url, doc_url, attachment_url,
               description, submitted_by, submitted_at, is_late AS late, created_at, updated_at
        FROM submission
        WHERE id = #{id}
        """)
    SubmissionEntity findSubmissionById(@Param("id") Long id);

    @Select("""
        SELECT s.id, s.assignment_id, s.group_id, s.project_name, s.repo_url, s.video_url, s.preview_url, s.doc_url, s.attachment_url,
               s.description, s.submitted_by, s.submitted_at, s.is_late AS late, s.created_at, s.updated_at
        FROM submission s
        INNER JOIN assignment_group_member gm ON gm.group_id = s.group_id
        WHERE s.assignment_id = #{assignmentId}
          AND gm.user_id = #{userId}
        LIMIT 1
        """)
    SubmissionEntity findSubmissionByAssignmentAndUser(@Param("assignmentId") Long assignmentId, @Param("userId") Long userId);

    @Select("""
        SELECT id, assignment_id, group_id, project_name, repo_url, video_url, preview_url, doc_url, attachment_url,
               description, submitted_by, submitted_at, is_late AS late, created_at, updated_at
        FROM submission
        WHERE assignment_id = #{assignmentId}
        ORDER BY id
        """)
    List<SubmissionEntity> findSubmissionsByAssignmentId(@Param("assignmentId") Long assignmentId);

    @Insert("""
        INSERT INTO submission (
            assignment_id, group_id, project_name, repo_url, video_url, preview_url, doc_url,
            attachment_url, description, submitted_by, submitted_at, is_late
        ) VALUES (
            #{assignmentId}, #{groupId}, #{projectName}, #{repoUrl}, #{videoUrl}, #{previewUrl}, #{docUrl},
            #{attachmentUrl}, #{description}, #{submittedBy}, #{submittedAt}, #{late}
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertSubmission(SubmissionEntity entity);

    @Update("""
        UPDATE submission
        SET project_name = #{projectName},
            repo_url = #{repoUrl},
            video_url = #{videoUrl},
            preview_url = #{previewUrl},
            doc_url = #{docUrl},
            attachment_url = #{attachmentUrl},
            description = #{description},
            submitted_by = #{submittedBy},
            submitted_at = #{submittedAt},
            is_late = #{late}
        WHERE id = #{id}
        """)
    int updateSubmission(SubmissionEntity entity);
}
