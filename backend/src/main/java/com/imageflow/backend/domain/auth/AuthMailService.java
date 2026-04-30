package com.imageflow.backend.domain.auth;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class AuthMailService {

    private static final Logger log = LoggerFactory.getLogger(AuthMailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String verificationBaseUrl;
    private final boolean mailEnabled;

    public AuthMailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${spring.mail.host:}") String mailHost,
            @Value("${app.auth.mail.from:no-reply@imageflow.local}") String fromAddress,
            @Value("${app.auth.mail.verification-base-url:http://localhost:5173}") String verificationBaseUrl
    ) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.fromAddress = fromAddress;
        this.verificationBaseUrl = verificationBaseUrl;
        this.mailEnabled = mailHost != null && !mailHost.isBlank();
    }

    public void sendVerificationCode(String email, String code, LocalDateTime expiresAt) {
        String deepLink = verificationBaseUrl.replaceAll("/$", "") + "/?verifyEmail=" + email;
        String subject = "[ImageFlow] 이메일 인증 코드";
        String body = """
                ImageFlow 가입을 위한 이메일 인증 코드입니다.

                인증 코드: %s
                만료 시간: %s

                가입을 진행 중이 아니라면 이 메일을 무시하셔도 됩니다.
                서비스 진입 주소: %s
                """.formatted(code, expiresAt, deepLink);

        if (mailSender == null || !mailEnabled) {
            log.info("[auth-mail] mail sender not configured. verification code for {} => {}", email, code);
            return;
        }
        System.out.println("body :: " + body);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
