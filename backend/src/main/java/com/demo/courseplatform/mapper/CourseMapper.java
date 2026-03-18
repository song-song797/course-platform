package com.demo.courseplatform.mapper;

import com.demo.courseplatform.domain.entity.PersistenceModels.CourseEntity;
import com.demo.courseplatform.domain.entity.PersistenceModels.CourseMemberEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

@Mapper
public interface CourseMapper {

    @Select("""
        SELECT id, code, name, term
        FROM course
        ORDER BY id
        """)
    List<CourseEntity> findAll();

    @Select("""
        SELECT c.id, c.code, c.name, c.term
        FROM course c
        INNER JOIN course_member cm ON cm.course_id = c.id
        WHERE cm.user_id = #{userId}
        ORDER BY c.id
        """)
    List<CourseEntity> findByUserId(@Param("userId") Long userId);

    @Select("""
        SELECT id, code, name, term
        FROM course
        WHERE id = #{id}
        """)
    CourseEntity findById(@Param("id") Long id);

    @Insert("""
        INSERT INTO course (code, name, term)
        VALUES (#{code}, #{name}, #{term})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CourseEntity entity);

    @Select("""
        SELECT id, course_id, user_id, course_role, created_at
        FROM course_member
        WHERE course_id = #{courseId}
        ORDER BY id
        """)
    List<CourseMemberEntity> findMembersByCourseId(@Param("courseId") Long courseId);

    @Select("""
        SELECT id, course_id, user_id, course_role, created_at
        FROM course_member
        WHERE user_id = #{userId}
        ORDER BY course_id, id
        """)
    List<CourseMemberEntity> findMembersByUserId(@Param("userId") Long userId);

    @Select("""
        SELECT id, course_id, user_id, course_role, created_at
        FROM course_member
        WHERE course_id = #{courseId}
          AND user_id = #{userId}
        LIMIT 1
        """)
    CourseMemberEntity findMember(@Param("courseId") Long courseId, @Param("userId") Long userId);

    @Insert("""
        INSERT INTO course_member (course_id, user_id, course_role)
        VALUES (#{courseId}, #{userId}, #{courseRole})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMember(CourseMemberEntity member);

    @Delete("""
        DELETE FROM course_member
        WHERE course_id = #{courseId}
          AND user_id = #{userId}
        """)
    int deleteMember(@Param("courseId") Long courseId, @Param("userId") Long userId);

    @Insert("""
        <script>
        INSERT INTO course_member (course_id, user_id, course_role)
        VALUES
        <foreach collection="members" item="item" separator=",">
            (#{item.courseId}, #{item.userId}, #{item.courseRole})
        </foreach>
        </script>
        """)
    int insertMembers(@Param("members") List<CourseMemberEntity> members);
}
