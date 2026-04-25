package ru.yartsev_vladislav.otp_app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "otp_config")
public class OtpConfig {

    public static final long SINGLETON_ID = 1L;

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "code_length", nullable = false)
    private int codeLength;

    @Column(name = "ttl_seconds", nullable = false)
    private long ttlSeconds;

    public OtpConfig() {
    }

    public OtpConfig(Long id, int codeLength, long ttlSeconds) {
        this.id = id;
        this.codeLength = codeLength;
        this.ttlSeconds = ttlSeconds;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getCodeLength() {
        return codeLength;
    }

    public void setCodeLength(int codeLength) {
        this.codeLength = codeLength;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }
}
