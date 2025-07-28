package com.cpt202.dailyreadingtracker.utils;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.cpt202.dailyreadingtracker.readinglog.ReadingLog;
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

    private final String senderName = "Daily Reading Tracker";
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    public EmailService(JavaMailSender mailSender){
        this.mailSender = mailSender;
    }

    public boolean sendVerificationEmail(User user, String verificationUrl){
        String subject = "Verify Your Email Address";
        String content = String.format("""
            <html>
                <body>
                    <p>Dear %s,</p>
                    <p>Thank you for registering with Daily Reading Tracker!</p>
                    <p>Please verify your email address by clicking the link below:</p>
                    <p><a href="%s">Verify Email</a></p>
                    <p>Or copy this URL to your browser: %s</p>
                    <p>This link will expire in 24 hours.</p>
                    <p>If you didn't create this account, please ignore this email.</p>
                    <p>Best regards,<br>%s Team</p>
                </body>
            </html>
            """, user.getUsername(), verificationUrl, verificationUrl, senderName);
        
        try {
            sendHtmlEmail(user.getEmail(), subject, content);
            logger.info("Verification email sent to {} with link: {}", user.getEmail(), verificationUrl);
            return true;
        } catch (MessagingException | UnsupportedEncodingException e) {
            logger.error("Failed to send verification email to {}", user.getEmail(), e);
            return false;
        }
    }

    public void sendPasswordResetEmail(User user, String url) {
        String subject = "Password Reset Request";
        String content = String.format("""
            <html>
                <body>
                    <p>Dear %s,</p>
                    <p>We received a request to reset your password.</p>
                    <p>Click <a href="%s">here</a> to reset your password.</p>
                    <p>If you didn't request this, please ignore this email.</p>
                    <p>Best regards,<br>%s Team</p>
                </body>
            </html>
            """, user.getUsername(), url, senderName);

        try {
            sendHtmlEmail(user.getEmail(), subject, content);
            logger.info("Reset email sent to {} with link: {}", user.getEmail(), url);
        } catch (MessagingException | UnsupportedEncodingException e) {
            logger.error("Failed to send password reset email to {}", user.getEmail(), e);
            throw new EmailException("Failed to send password reset email", e);
        }
    }

    public void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException,
                                                                                    UnsupportedEncodingException{
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
            logger.warn("Used simple from address due to encoding issues");
        }
    }

    public boolean sendAccountFrozenEmail(User user){
        String subject = "Your Daily Reading Tracker Acount Has Been Frozen!";
        String content = String.format("""
            <html>
                <body>
                    <p>Dear %s,</p>
                    <p>Due too many violations of our content policy (logs frequently flagged as inappropriate) by our admin: </p>
                    <p>We are sorry to inform you that we have to freeze your account for a week.</p>
                    <p>Please consider taking your time reading our policy and try not to violate it again.</p>
                    <p>Your account will be unfrozen by: %s</p>
                    <p>Thank you for using Daily Reading Tracker!</p>
                    <p>If this isn't your account, please ignore this email.</p>
                    <p>Best regards,<br>%s Team</p>
                </body>
            </html>
            """, user.getUsername(), 
            LocalDate.now().plusDays(7).format(DateTimeFormatter.ofPattern("MMMM d, yyyy")), 
            senderName);
        
        try {
            sendHtmlEmail(user.getEmail(), subject, content);
            logger.info("Account Frozen email sent to {}", user.getEmail());
            return true;
        } catch (MessagingException | UnsupportedEncodingException e) {
            logger.error("Failed to send account frozen email to {}", user.getEmail(), e);
            return false;
        }
    }

    public void sendViolationNotificationEmail(String userEmail, ReadingLog log) {
        try {
            String subject = "Notification: Your Reading Log Has Been Flagged";
            
            String content = String.format("""
                <html>
                    <body>
                        <p>Dear User,</p>
                        
                        <p>We would like to inform you that one of your reading logs has been flagged by our administrator 
                        for potential policy violation.</p>
                        
                        <p>Details of the flagged log:</p>
                        <ul>
                            <li><strong>Title:</strong> %s</li>
                            <li><strong>Author:</strong> %s</li>
                            <li><strong>Date:</strong> %s</li>
                            <li><strong>Notes:</strong> %s</li>
                        </ul>
                        
                        <p>Please review and modify the content within 24 hours to ensure it complies with our content policy. 
                        Our admin has marked the content that violates our policy for you.</p>
                        
                        <p>If you believe this was done in error, please contact our support team.</p>
                        
                        <p>Best regards,<br>
                        Daily Reading Tracker Team</p>
                    </body>
                </html>
                """,
                log.getTitle(),
                log.getAuthor(),
                log.getDate().toString(),
                log.getNotes() != null ? log.getNotes() : "No notes"
            );
            
            sendHtmlEmail(userEmail, subject, content);
        } catch (Exception e) {
            System.err.println("Failed to send notification email: " + e.getMessage());
        }
    }

    public void sendPasswordChangeNotification(User user) {
        String subject = "Password Changed Successfully";
        String content = String.format("""
            <html>
                <body>
                    <p>Dear %s,</p>
                    <p>This is to confirm that your password was successfully changed.</p>
                    <p>If you didn't make this change, please contact our support team immediately.</p>
                    <p>Date of change: %s</p>
                    <p>Best regards,<br>%s Team</p>
                </body>
            </html>
            """, 
            user.getUsername(),
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a")),
            senderName);
        
        try {
            sendHtmlEmail(user.getEmail(), subject, content);
            logger.info("Password change notification sent to {}", user.getEmail());
        } catch (MessagingException | UnsupportedEncodingException e) {
            logger.error("Failed to send password change notification to {}", user.getEmail(), e);
        }
    }

    public class EmailException extends RuntimeException {
        public EmailException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}