package ru.yartsev_vladislav.otp_app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.yartsev_vladislav.otp_app.domain.OtpCode;
import ru.yartsev_vladislav.otp_app.domain.OtpStatus;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findFirstByUserIdAndCodeAndStatusOrderByCreatedAtDesc(
            Long userId,
            String code,
            OtpStatus status
    );

    Optional<OtpCode> findByIdAndUserId(Long id, Long userId);

    long deleteByUserId(Long userId);

    @Modifying
    @Query("""
            update OtpCode c
               set c.status = :expiredStatus
             where c.status = :activeStatus
               and c.expiresAt < :now
            """)
    int markExpired(
            @Param("activeStatus") OtpStatus activeStatus,
            @Param("expiredStatus") OtpStatus expiredStatus,
            @Param("now") Instant now
    );
}
