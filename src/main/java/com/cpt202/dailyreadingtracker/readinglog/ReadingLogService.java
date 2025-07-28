package com.cpt202.dailyreadingtracker.readinglog;

import java.security.Principal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.cpt202.dailyreadingtracker.user.User;
import com.cpt202.dailyreadingtracker.user.UserRepository;
import com.cpt202.dailyreadingtracker.utils.EmailService;
import com.cpt202.dailyreadingtracker.violationlog.ViolationLog;
import com.cpt202.dailyreadingtracker.violationlog.ViolationLogRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReadingLogService {

    private final ReadingLogRepository readingLogRepository;
    private final UserRepository userRepository;
    private final ViolationLogRepository violationLogRepository;
    private final EmailService emailService;

    @Transactional
    public ReadingLog createLog(Long userId, ReadingLogDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User is not existed."));

        List<ReadingLog> existingCurrentLogs = readingLogRepository
                            .findByUserIdAndTitleIgnoreCaseAndIsCurrent(userId, dto.getTitle().trim(), true);

        ReadingLog log = new ReadingLog();
        log.setUser(user);
        log.setTitle(dto.getTitle());
        log.setAuthor(dto.getAuthor());
        log.setDate(dto.getDate());
        log.setTimeSpent(dto.getTimeSpent());
        log.setCurrentPage(dto.getCurrentPage());
        log.setTotalPages(dto.getTotalPages());
        log.setNotes(dto.getNotes());
        log.setCurrent(true);

        if (!existingCurrentLogs.isEmpty()) {
            ReadingLog previousLog = existingCurrentLogs.get(0);
            
            if (previousLog.getTotalPages() != null && !previousLog.getTotalPages().equals(dto.getTotalPages())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Warning: The total number of pages previously recorded in this book is " + 
                        previousLog.getTotalPages() + " page, is inconsistency with this input " +
                        dto.getTotalPages() + ". Please check whether the book is the same version.");
            }
            
            previousLog.setCurrent(false);

            readingLogRepository.save(previousLog);
            
            log.setPreviousVersion(previousLog);
        }

        return readingLogRepository.save(log);
    }

    public List<ReadingLogHistoryDto> getLogHistory(Long userId, String title, String author, Long currentLogId) {
        String trimmedTitle = title.trim().toLowerCase();
        String trimmedAuthor = author.trim().toLowerCase();

        return readingLogRepository.findByUserIdAndTitleIgnoreCaseAndAuthorIgnoreCase(userId, trimmedTitle, trimmedAuthor)
                                                        .stream()
                                                        .sorted(Comparator.comparing(ReadingLog::getDate).reversed())
                                                        .map(log -> new ReadingLogHistoryDto(log, currentLogId))
                                                        .collect(Collectors.toList());
    }

    @Transactional
    public void deleteLog(long userId, long logId) {
        ReadingLog log = readingLogRepository.findById(logId)
                .orElseThrow(() -> new EntityNotFoundException("Log not found"));

        if (!isAdmin(userId) && log.getUser().getId() != userId) {
            throw new SecurityException("You can only delete your own logs");
        }

        ReadingLog nextVersion = readingLogRepository.findByPreviousVersionId(logId);
        if (nextVersion != null) {
            nextVersion.setPreviousVersion(log.getPreviousVersion());
            readingLogRepository.save(nextVersion);
            
            if (log.isCurrent()) {
                nextVersion.setCurrent(true);
                readingLogRepository.save(nextVersion);
            }
        }

        readingLogRepository.delete(log);
    }

    public List<ReadingLog> getAllLogsByUser(Long userId) {
        return readingLogRepository.findByUserId(userId)
                .stream()
                .sorted(Comparator.comparing(ReadingLog::getDate).reversed())
                .collect(Collectors.toList());
    }

    public ReadingLog updateLog(Long userId, Long logId, ReadingLogDto dto) {
        ReadingLog log = readingLogRepository.findById(logId)
            .orElseThrow(() -> new IllegalArgumentException("Reading log not found"));

        User currentUser = userRepository.findById(userId)
            .orElseThrow(() -> new SecurityException("User not found"));
        
        boolean isAdmin = currentUser.getRoles().stream()
            .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
        
        if (!isAdmin && log.getUser().getId() != userId) {
            throw new SecurityException("Access denied");
        }

        List<ReadingLog> otherLogs = readingLogRepository
        .findByUserIdAndTitleIgnoreCaseAndAuthorIgnoreCaseAndIdNot(
            userId, dto.getTitle().trim(), dto.getAuthor().trim(), logId);

        if (!otherLogs.isEmpty() && dto.getTotalPages() != null) {
            Integer existingTotalPages = otherLogs.stream()
                .filter(l -> l.getTotalPages() != null)
                .findFirst()
                .map(ReadingLog::getTotalPages)
                .orElse(null);
        
            if (existingTotalPages != null && !existingTotalPages.equals(dto.getTotalPages())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "PAGE_COUNT_MISMATCH:" + existingTotalPages + ":" + dto.getTotalPages());
            }
        }

        log.setTitle(dto.getTitle());
        log.setAuthor(dto.getAuthor());
        log.setDate(dto.getDate());
        log.setTimeSpent(dto.getTimeSpent());
        log.setCurrentPage(dto.getCurrentPage());
        log.setTotalPages(dto.getTotalPages());
        log.setNotes(dto.getNotes());

        return readingLogRepository.save(log);
    }

    @Transactional
    public void deleteInappropriateLog(long userId, Long logId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        String email = authentication.getName();

        User admin = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, 
                                                              "User not found with email: " + email));

        boolean isAdmin = admin.getRoles().stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
                System.out.println("Your role: " + admin.getRoles());

        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can delete any user's logs!");
        }

        ReadingLog log = readingLogRepository.findById(logId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reading log not found"));
        ViolationLog violog = new ViolationLog(log); 

        violationLogRepository.save(violog);
        readingLogRepository.delete(log); 

        emailService.sendViolationNotificationEmail(log.getUser().getEmail(), log);
    }

    public ReadingLog getLogById(Long userId, Long logId) {
        ReadingLog log = readingLogRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("Reading log not found"));

        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new SecurityException("User not found"));

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));

        if (!isAdmin && !(log.getUser().getId() == userId)) {
            throw new SecurityException("Access denied");
        }

        return log;
    }

    public List<ReadingLog> getAllLogHistory(String title, String author) {
            return readingLogRepository.findByTitleIgnoreCaseAndAuthorIgnoreCaseOrderByDateDesc(title, author);
    }

    private boolean isAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        return user.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ROLE_ADMIN"));
    }

    public Long getUserIdFromPrincipal(Principal principal) {
        String email = principal.getName(); 
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new SecurityException("User not found"));

        return user.getId();
    }
    
}
