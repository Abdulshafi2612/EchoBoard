package com.echoboard.repository;

import com.echoboard.entity.RefreshToken;
import com.echoboard.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    long deleteByExpiresAtBefore(LocalDateTime now);
}
