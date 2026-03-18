package com.demo.courseplatform.mapper;

import com.demo.courseplatform.domain.entity.PersistenceModels.EvaluationBlacklistEntity;
import com.demo.courseplatform.domain.entity.PersistenceModels.EvaluationEntity;
import com.demo.courseplatform.domain.entity.PersistenceModels.EvaluationItemEntity;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface EvaluationMapper {

    @Select("""
        SELECT id, assignment_id, submission_id, evaluator_user_id, evaluator_role, total_score,
               comment, is_abnormal AS abnormal, abnormal_reason, is_excluded AS excluded,
               review_status, created_at
        FROM evaluation
        WHERE submission_id = #{submissionId}
          AND evaluator_user_id = #{evaluatorUserId}
          AND evaluator_role = #{evaluatorRole}
        LIMIT 1
        """)
    EvaluationEntity findBySubmissionAndEvaluator(@Param("submissionId") Long submissionId,
                                                  @Param("evaluatorUserId") Long evaluatorUserId,
                                                  @Param("evaluatorRole") String evaluatorRole);

    @Select("""
        <script>
        SELECT id, assignment_id, submission_id, evaluator_user_id, evaluator_role, total_score,
               comment, is_abnormal AS abnormal, abnormal_reason, is_excluded AS excluded,
               review_status, created_at
        FROM evaluation
        WHERE submission_id IN
        <foreach collection="submissionIds" item="submissionId" open="(" separator="," close=")">
            #{submissionId}
        </foreach>
        ORDER BY created_at DESC, id DESC
        </script>
        """)
    List<EvaluationEntity> findBySubmissionIds(@Param("submissionIds") List<Long> submissionIds);

    @Select("""
        <script>
        SELECT id, assignment_id, submission_id, evaluator_user_id, evaluator_role, total_score,
               comment, is_abnormal AS abnormal, abnormal_reason, is_excluded AS excluded,
               review_status, created_at
        FROM evaluation
        WHERE assignment_id = #{assignmentId}
        <if test="submissionId != null">
          AND submission_id = #{submissionId}
        </if>
        <if test="evaluatorUserId != null">
          AND evaluator_user_id = #{evaluatorUserId}
        </if>
        <if test="reviewStatus != null and reviewStatus != ''">
          AND review_status = #{reviewStatus}
        </if>
        <if test="abnormalOnly != null and abnormalOnly">
          AND is_abnormal = 1
        </if>
        ORDER BY created_at DESC, id DESC
        </script>
        """)
    List<EvaluationEntity> findByAssignmentId(@Param("assignmentId") Long assignmentId,
                                              @Param("submissionId") Long submissionId,
                                              @Param("evaluatorUserId") Long evaluatorUserId,
                                              @Param("reviewStatus") String reviewStatus,
                                              @Param("abnormalOnly") Boolean abnormalOnly);

    @Select("""
        SELECT id, assignment_id, submission_id, evaluator_user_id, evaluator_role, total_score,
               comment, is_abnormal AS abnormal, abnormal_reason, is_excluded AS excluded,
               review_status, created_at
        FROM evaluation
        WHERE id = #{id}
        """)
    EvaluationEntity findById(@Param("id") Long id);

    @Insert("""
        INSERT INTO evaluation (
            assignment_id, submission_id, evaluator_user_id, evaluator_role, total_score,
            comment, is_abnormal, abnormal_reason, is_excluded, review_status
        ) VALUES (
            #{assignmentId}, #{submissionId}, #{evaluatorUserId}, #{evaluatorRole}, #{totalScore},
            #{comment}, #{abnormal}, #{abnormalReason}, #{excluded}, #{reviewStatus}
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(EvaluationEntity entity);

    @Update("""
        UPDATE evaluation
        SET total_score = #{totalScore},
            comment = #{comment},
            is_abnormal = #{abnormal},
            abnormal_reason = #{abnormalReason},
            is_excluded = #{excluded},
            review_status = #{reviewStatus}
        WHERE id = #{id}
        """)
    int update(EvaluationEntity entity);

    @Update("""
        UPDATE evaluation
        SET is_excluded = #{excluded},
            review_status = #{reviewStatus}
        WHERE id = #{evaluationId}
        """)
    int updateExcluded(@Param("evaluationId") Long evaluationId,
                       @Param("excluded") boolean excluded,
                       @Param("reviewStatus") String reviewStatus);

    @Update("""
        UPDATE evaluation
        SET is_abnormal = #{abnormal},
            abnormal_reason = #{abnormalReason}
        WHERE id = #{id}
        """)
    int updateAbnormalState(EvaluationEntity entity);

    @Delete("""
        DELETE FROM evaluation_item
        WHERE evaluation_id = #{evaluationId}
        """)
    int deleteItemsByEvaluationId(@Param("evaluationId") Long evaluationId);

    @Insert("""
        <script>
        INSERT INTO evaluation_item (evaluation_id, rubric_item_id, score, comment)
        VALUES
        <foreach collection="items" item="item" separator=",">
            (#{item.evaluationId}, #{item.rubricItemId}, #{item.score}, #{item.comment})
        </foreach>
        </script>
        """)
    int insertItems(@Param("items") List<EvaluationItemEntity> items);

    @Select("""
        <script>
        SELECT id, evaluation_id, rubric_item_id, score, comment
        FROM evaluation_item
        WHERE evaluation_id IN
        <foreach collection="evaluationIds" item="evaluationId" open="(" separator="," close=")">
            #{evaluationId}
        </foreach>
        ORDER BY id
        </script>
        """)
    List<EvaluationItemEntity> findItemsByEvaluationIds(@Param("evaluationIds") List<Long> evaluationIds);

    @Select("""
        SELECT id, assignment_id, evaluator_user_id, target_submission_id, created_at
        FROM evaluation_blacklist
        WHERE assignment_id = #{assignmentId}
          AND evaluator_user_id = #{evaluatorUserId}
          AND target_submission_id = #{targetSubmissionId}
        LIMIT 1
        """)
    EvaluationBlacklistEntity findBlacklist(@Param("assignmentId") Long assignmentId,
                                            @Param("evaluatorUserId") Long evaluatorUserId,
                                            @Param("targetSubmissionId") Long targetSubmissionId);

    @Insert("""
        INSERT INTO evaluation_blacklist (assignment_id, evaluator_user_id, target_submission_id)
        VALUES (#{assignmentId}, #{evaluatorUserId}, #{targetSubmissionId})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertBlacklist(EvaluationBlacklistEntity entity);

    @Delete("""
        DELETE FROM evaluation_blacklist
        WHERE assignment_id = #{assignmentId}
          AND evaluator_user_id = #{evaluatorUserId}
          AND target_submission_id = #{targetSubmissionId}
        """)
    int deleteBlacklist(@Param("assignmentId") Long assignmentId,
                        @Param("evaluatorUserId") Long evaluatorUserId,
                        @Param("targetSubmissionId") Long targetSubmissionId);

    @Select("""
        SELECT id, assignment_id, evaluator_user_id, target_submission_id, created_at
        FROM evaluation_blacklist
        WHERE assignment_id = #{assignmentId}
        ORDER BY created_at DESC, id DESC
        """)
    List<EvaluationBlacklistEntity> findBlacklistsByAssignmentId(@Param("assignmentId") Long assignmentId);
}
