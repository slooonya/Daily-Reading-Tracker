package com.cpt202.dailyreadingtracker.utils;

import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.cpt202.dailyreadingtracker.user.User;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

// Service for sending email messages for account verification, status update, and password reset, 

@Service
public class EmailService {
    
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    private final String senderName = "Daily Reading Tracker Team";

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    public EmailService(JavaMailSender mailSender){
        this.mailSender = mailSender;
    }

    public void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException,
                                                                                    UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        try {
            helper.setFrom(new InternetAddress(senderEmail, senderName));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (UnsupportedEncodingException e) {
            helper.setFrom(senderEmail);
            mailSender.send(message);
            logger.warn("Used simple from address to encoding issues");
        }
    }

    public boolean sendVerificationEmail(User user, String verificationUrl){
        String subject = "Verify Your Email Address";
        String content = String.format("""
                <html>
                    <body>
                        <p>Dear %s,</p>
                        <p>Thank you for registering with Daily Reading Tracker!</p>
                        <p>Please verify your email address by clicking the link below:</p>
                        <p><a href="%s>Verify Email</a></p>
                        <p>Or copy this URL to your browser: %s</p>
                        <p>This link will expire in 24 hours.</p>
                        <p>If you didn't create this account, please ignore this email.</p>
                        <p>Best regards, <br>%s Team</p>
                    </body>
                </html>
                """, user.getUsername(), verificationUrl, verificationUrl, senderName);
        
        try {
            sendHtmlEmail(user.getEmail(), subject, content);
            logger.info("Verification email sent to {} with link: {}", user.getEmail(), verificationUrl);
            return true;
        } catch (MessagingException | UnsupportedEncodingException e){
            logger.error("Failed to send verification email to {}", user.getUsername(), e);
            return false;
        }
    }

    public void sendPasswordResetEmail(User user, String url){
        String subject = "Password Reset Request";
        String content = String.format("""
                <html>
                    <body>
                        <p>Dear %s,</p>
                        <p>We received a request to reset your password.</p>
                        <p>Click <a href="%s">here</a> to reset your password.</p>
                        <p>If you didn't request this, please ignore this email.</p>
                        <p>Best regards, <br>%s Team</p>
                    </body>
                </html>
                """, user.getUsername(), url, senderName);

        try {
            sendHtmlEmail(user.getEmail(), subject, content);
            logger.info("Reset email sent to {} with link: {}", user.getEmail(), url);
        } catch (MessagingException | UnsupportedEncodingException e){
            logger.error("Failed to send password reset email to {}", user.getEmail(), e);
            throw new EmailException("Failed to send passoword reset email", e);
        }
    }

    public class EmailException extends RuntimeException {
        public EmailException(String message, Throwable cause){
            super(message, cause);
        }
    }

}
