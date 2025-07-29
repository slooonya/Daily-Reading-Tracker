package com.cpt202.dailyreadingtracker.utils;

import com.cpt202.dailyreadingtracker.readinglog.ReadingLog;
import com.cpt202.dailyreadingtracker.user.User;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Mock
    private MimeMessage mimeMessage;

    @Mock
    private MimeMessageHelper mimeMessageHelper;

    private User testUser;
    private ReadingLog testLog;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@test.com");

        testLog = new ReadingLog();
        testLog.setTitle("Test Book");
        testLog.setAuthor("Test Author");
        testLog.setDate(LocalDate.now());
        testLog.setNotes("Test notes");

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    // ES_001
    @Test
    public void testSendVerificationEmail() throws Exception {
        boolean result = emailService.sendVerificationEmail(testUser, "http://verify.com");

        assertTrue(result);
        verify(mailSender).send(mimeMessage);
    }

    // ES_002
    @Test
    public void testSendPasswordResetEmail() {
        assertDoesNotThrow(() -> {
            emailService.sendPasswordResetEmail(testUser, "http://reset.com");
        });
        verify(mailSender).send(mimeMessage);
    }

    // ES_003
    @Test
    public void testSendAccountFrozenEmail() {
        boolean result = emailService.sendAccountFrozenEmail(testUser);

        assertTrue(result);
        verify(mailSender).send(mimeMessage);
    }

    // ES_004
    @Test
    public void testSendViolationNotificationEmail() {
        assertDoesNotThrow(() -> {
            emailService.sendViolationNotificationEmail("test@example.com", testLog);
        });

        verify(mailSender).send(mimeMessage);
    }

    // ES_005
    @Test
    public void testSendPasswordChangeNotification() {
        assertDoesNotThrow(() -> {
            emailService.sendPasswordChangeNotification(testUser);
        });

        verify(mailSender).send(mimeMessage);
    }
}
