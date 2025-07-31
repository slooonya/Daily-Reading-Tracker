package com.cpt202.dailyreadingtracker.violationlog;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.cpt202.dailyreadingtracker.readinglog.ReadingLog;
import com.cpt202.dailyreadingtracker.readinglog.ReadingLogRepository;
import com.cpt202.dailyreadingtracker.user.User;
import com.cpt202.dailyreadingtracker.user.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Service responsible for managing violation logs, which track flagged reading logs for policy violations.
 * <ul>
 *     <li>Create violation logs</li>
 *     <li>Restore flagged logs back to the reading logs</li>
 *     <li>Update violation logs</li>
 *     <li>Retrieve violation logs</li>
 * </ul>
 */

@Service 
@RequiredArgsConstructor
public class ViolationLogService {

    private final ReadingLogRepository readingLogRepository;
    private final ViolationLogRepository violationLogRepository;
    private final UserRepository userRepository;

    /**
     * Creates a new violation log for a user.
     *
     * @param userId the ID of the user associated with the violation log
     * @param dto    the data transfer object containing violation log details
     * @return the created violation log
     */
    public ViolationLog createLog(Long userId, ViolationLogDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        ViolationLog log = new ViolationLog();
        log.setUser(user);
        log.setTitle(dto.getTitle());
        log.setAuthor(dto.getAuthor());
        log.setNotes(dto.getNotes());

        return violationLogRepository.save(log);
    }

    /**
     * Restores a flagged violation log back to the reading logs.
     * Only admins are authorized to perform this action.
     *
     * @param userId the ID of the admin performing the action
     * @param logId  the ID of the violation log to restore
     * @throws ResponseStatusException if the user is unauthorized or the log does not exist
     */
    @Transactional
    public void restoreViolationLog(long userId, Long logId) {
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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can restore any violation logs!");
        }

        ViolationLog violog = violationLogRepository.findById(logId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reading log not found"));

        User flaggedUser = violog.getUser();

        ReadingLog newLog = new ReadingLog();
        newLog.setTitle(violog.getTitle());
        newLog.setAuthor(violog.getAuthor());
        newLog.setDate(violog.getDate());
        newLog.setTimeSpent(violog.getTimeSpent());
        newLog.setNotes(violog.getNotes());
        newLog.setUser(violog.getUser());
        newLog.setCurrentPage(violog.getCurrentPage());
        newLog.setTotalPages(violog.getTotalPages());
        newLog.setCreatedAt(violog.getCreatedAt());

        flaggedUser.removeTimesFlagged();

        readingLogRepository.save(newLog);
        violationLogRepository.delete(violog);
    }

    /**
     * Checks whether a user has the "ROLE_ADMIN" role.
     *
     * @param userId the ID of the user to check
     * @return {@code true} if the user has the "ROLE_ADMIN" role, {@code false} otherwise
     * @throws EntityNotFoundException if the user does not exist
     */
    private boolean isAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        return user.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ROLE_ADMIN"));
    }

    /**
     * Updates the details of a violation log.
     * Only admins or the owner of the log are authorized to perform this action.
     *
     * @param userId the ID of the user attempting to update the log
     * @param logId  the ID of the violation log to update
     * @param dto    the data transfer object containing updated log details
     * @return the updated violation log
     * @throws SecurityException if the user is unauthorized
     * @throws ResponseStatusException if there is a mismatch in page count
     */
    @Transactional
    public ViolationLog updateLog(Long userId, Long logId, ViolationLogDto dto) {
        ViolationLog log = violationLogRepository.findById(logId)
        .orElseThrow(() -> new IllegalArgumentException("Violation log not found"));
    
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new SecurityException("User not found"));
        
        boolean isAdmin = user.getRoles().stream()
            .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
        
        if (!isAdmin && log.getUser().getId() == userId) {
            throw new SecurityException("Unauthorized to edit this log");
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
        log.setNotes(dto.getNotes());

        return violationLogRepository.save(log); 
    }

    /**
     * Retrieves a violation log by its ID.
     * Only admins are authorized to access violation logs.
     *
     * @param userId the ID of the user attempting to access the log
     * @param logId  the ID of the violation log to retrieve
     * @return the violation log
     * @throws SecurityException if the user is unauthorized
     */
    public ViolationLog getLogById(Long userId, Long logId) {
        ViolationLog log = violationLogRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("Violation log not found"));
                
        if (!isAdmin(userId)) {       
            throw new SecurityException("Access denied");
        }

        return log;
    } 
}
