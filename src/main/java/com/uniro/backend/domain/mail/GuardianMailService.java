package com.uniro.backend.mail;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GuardianMailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${guardian.consent.base-url}")
    private String baseUrl;

    public void sendConsentEmail(
            String guardianName,
            String guardianEmail,
            String token
    ) {
        String link = baseUrl + "/" + token;

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(senderEmail);
        message.setTo(guardianEmail);
        message.setSubject("[Uniro] 보호자 동의 요청");

        message.setText(
                guardianName + " 보호자님, 안녕하세요.\n\n"
                        + "Uniro 대학 탐방 서비스에서 보호자 동의를 요청했습니다.\n\n"
                        + "아래 링크에 접속하여 동의 여부를 확인해 주세요.\n\n"
                        + link + "\n\n"
                        + "본 링크는 발급 후 7일 동안 유효합니다.\n"
                        + "본인이 요청받은 내용이 아니라면 링크를 이용하지 마세요.\n\n"
                        + "Uniro 드림"
        );

        mailSender.send(message);
    }
}