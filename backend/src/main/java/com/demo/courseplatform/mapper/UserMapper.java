package com.demo.courseplatform.mapper;

import com.demo.courseplatform.domain.entity.PersistenceModels.UserEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {

    @Select("""
        SELECT id, username, password_hash, display_name, role, first_login_reset_required, created_at
        FROM sys_user
        WHERE id = #{id}
        """)
    UserEntity findById(@Param("id") Long id);

    @Select("""
        SELECT id, username, password_hash, display_name, role, first_login_reset_required, created_at
        FROM sys_user
        WHERE username = #{username}
        """)
    UserEntity findByUsername(@Param("username") String username);

    @Select("""
        SELECT id, username, password_hash, display_name, role, first_login_reset_required, created_at
        FROM sys_user
        WHERE role = #{role}
        ORDER BY id
        """)
    List<UserEntity> findByRole(@Param("role") String role);

    @Select("""
        <script>
        SELECT id, username, password_hash, display_name, role, first_login_reset_required, created_at
        FROM sys_user
        WHERE id IN
        <foreach collection="ids" item="id" open="(" separator="," close=")">
            #{id}
        </foreach>
        ORDER BY id
        </script>
        """)
    List<UserEntity> findByIds(@Param("ids") List<Long> ids);

    @Insert("""
        INSERT INTO sys_user (username, password_hash, display_name, role, first_login_reset_required)
        VALUES (#{username}, #{passwordHash}, #{displayName}, #{role}, #{firstLoginResetRequired})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserEntity entity);

    @Update("""
        UPDATE sys_user
        SET password_hash = #{passwordHash},
            first_login_reset_required = #{firstLoginResetRequired}
        WHERE id = #{id}
        """)
    int updatePassword(UserEntity entity);
}
