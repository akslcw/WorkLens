package com.su.worklens_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.su.worklens_backend.entity.AuthLoginAttempt;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

public interface AuthLoginAttemptMapper extends BaseMapper<AuthLoginAttempt> {

    @Insert("""
            INSERT INTO auth_login_attempts (username, failed_attempts, locked_until, updated_at)
            VALUES (#{username}, 0, NULL, #{updatedAt})
            ON CONFLICT (username) DO NOTHING
            """)
    int insertIfMissing(@Param("username") String username, @Param("updatedAt") LocalDateTime updatedAt);

    @Select("""
            SELECT username, failed_attempts, locked_until, updated_at
            FROM auth_login_attempts
            WHERE username = #{username}
            FOR UPDATE
            """)
    AuthLoginAttempt selectForUpdate(@Param("username") String username);
}
