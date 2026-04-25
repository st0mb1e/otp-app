package ru.yartsev_vladislav.otp_app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yartsev_vladislav.otp_app.domain.OtpConfig;

@Repository
public interface OtpConfigRepository extends JpaRepository<OtpConfig, Long> {
}
