package com.project.digitalwallet.repository;

import com.project.digitalwallet.entity.Otp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class OtpRepositoryTest {

    @Autowired
    private OtpRepository otpRepository;

    @Test
    void findTopByEmailOrderByCreatedAtDesc_shouldReturnLatestOtp() {
        LocalDateTime now = LocalDateTime.now();

        Otp olderOtp = new Otp();
        olderOtp.setEmail("test@gmail.com");
        olderOtp.setCreatedAt(now.minusMinutes(10));
        olderOtp.setExpiresAt(now.plusMinutes(5));

        Otp latestOtp = new Otp();
        latestOtp.setEmail("test@gmail.com");
        latestOtp.setCreatedAt(now.minusMinutes(2));
        latestOtp.setExpiresAt(now.plusMinutes(5));

        otpRepository.save(olderOtp);
        otpRepository.save(latestOtp);

        Optional<Otp> result =
                otpRepository.findTopByEmailOrderByCreatedAtDesc("test@gmail.com");

        assertThat(result).isPresent();
        assertThat(result.get().getCreatedAt())
                .isEqualTo(latestOtp.getCreatedAt());
    }

    @Test
    void findTopByEmailOrderByCreatedAtDesc_shouldReturnEmptyWhenEmailDoesNotExist() {
        Optional<Otp> result =
                otpRepository.findTopByEmailOrderByCreatedAtDesc("unknown@gmail.com");

        assertThat(result).isEmpty();
    }

    @Test
    void deleteByEmail_shouldDeleteOtpsForGivenEmail() {
        LocalDateTime now = LocalDateTime.now();

        Otp otp1 = new Otp();
        otp1.setEmail("test@gmail.com");
        otp1.setCreatedAt(now);
        otp1.setExpiresAt(now.plusMinutes(5));

        Otp otp2 = new Otp();
        otp2.setEmail("test@gmail.com");
        otp2.setCreatedAt(now.plusMinutes(1));
        otp2.setExpiresAt(now.plusMinutes(5));

        Otp otherOtp = new Otp();
        otherOtp.setEmail("other@gmail.com");
        otherOtp.setCreatedAt(now);
        otherOtp.setExpiresAt(now.plusMinutes(5));

        otpRepository.save(otp1);
        otpRepository.save(otp2);
        otpRepository.save(otherOtp);

        otpRepository.deleteByEmail("test@gmail.com");

        assertThat(otpRepository.findById(otp1.getId())).isEmpty();
        assertThat(otpRepository.findById(otp2.getId())).isEmpty();
        assertThat(otpRepository.findById(otherOtp.getId())).isPresent();
    }

    @Test
    void deleteExpiredOtps_shouldDeleteOnlyExpiredOtps() {
        LocalDateTime now = LocalDateTime.now();

        Otp expiredOtp = new Otp();
        expiredOtp.setEmail("expired@gmail.com");
        expiredOtp.setCreatedAt(now.minusMinutes(10));
        expiredOtp.setExpiresAt(now.minusMinutes(1));

        Otp validOtp = new Otp();
        validOtp.setEmail("valid@gmail.com");
        validOtp.setCreatedAt(now);
        validOtp.setExpiresAt(now.plusMinutes(10));

        otpRepository.save(expiredOtp);
        otpRepository.save(validOtp);

        otpRepository.deleteExpiredOtps(now);

        assertThat(otpRepository.findById(expiredOtp.getId())).isEmpty();
        assertThat(otpRepository.findById(validOtp.getId())).isPresent();
    }
}