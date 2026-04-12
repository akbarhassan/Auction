package com.ga.warehouse.services;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String fromAddress;

    /**
     *
     * @param to
     * @param subject
     * @param templateName
     * @param model
     */
    @Async
    public void sendEmail(String to, String subject, String templateName, Map<String, Object> model) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom(fromAddress);

            Context context = new Context();
            if (model != null) {
                context.setVariables(model);
            }

            String htmlContent = templateEngine.process("email/" + templateName, context);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("✅ Email sent successfully to {}", to);

        } catch (MessagingException ex) {
            log.error("❌ Failed to send email to {}: {}", to, ex.getMessage(), ex);
            throw new RuntimeException("Email sending failed", ex);
        }
    }
}
