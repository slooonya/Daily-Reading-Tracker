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

/**
 * Service responsible for managing reading logs for users.
 * <ul>
 *     <li>Create, update, and delete reading logs</li>
 *     <li>Retrieve reading log history and details</li>
 *     <li>Handle log versioning to track changes over time</li>
 *     <li>Allow admins to delete inappropriate logs</li>
 * </ul>
 */

@Service
@RequiredArgsConstructor
public class ReadingLogService {

    private final ReadingLogRepository readingLogRepository;
    private final UserRepository userRepository;
    private final ViolationLogRepository violationLogRepository;
    private final EmailService emailService;

    /**
     * Creates a new reading log for a user.
     * If a current log with the same title exists, it is marked as non-current.
     *
     * @param userId the ID of the user creating the log
     * @param dto    the data transfer object containing log details
     * @return the created reading log
     */
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

    /**
     * Retrieves the history of reading logs for a specific book by a user.
     *
     * @param userId       the ID of the user
     * @param title        the title of the book
     * @param author       the author of the book
     * @param currentLogId the ID of the current log (optional)
     * @return a list of reading log history DTOs
     */
    public List<ReadingLogHistoryDto> getLogHistory(Long userId, String title, String author, Long currentLogId) {
        String trimmedTitle = title.trim().toLowerCase();
        String trimmedAuthor = author.trim().toLowerCase();

        return readingLogRepository.findByUserIdAndTitleIgnoreCaseAndAuthorIgnoreCase(userId, trimmedTitle, trimmedAuthor)
                                                        .stream()
                                                        .sorted(Comparator.comparing(ReadingLog::getDate).reversed())
                                                        .map(log -> new ReadingLogHistoryDto(log, currentLogId))
                                                        .collect(Collectors.toList());
    }

    /**
     * Deletes a reading log by its ID.
     * Ensures that only the owner of the log or an admin can delete it.
     *
     * @param userId the ID of the user attempting to delete the log
     * @param logId  the ID of the log to delete
     */
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

    /**
     * Retrieves all reading logs for a user, sorted by date in descending order.
     *
     * @param userId the ID of the user
     * @return a list of reading logs
     */
    public List<ReadingLog> getAllLogsByUser(Long userId) {
        return readingLogRepository.findByUserId(userId)
                .stream()
                .sorted(Comparator.comparing(ReadingLog::getDate).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Updates an existing reading log with new details.
     * Ensures that only the owner of the log or an admin can update it.
     *
     * @param userId the ID of the user attempting to update the log
     * @param logId  the ID of the log to update
     * @param dto    the data transfer object containing updated log details
     * @return the updated reading log
     */
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

    /**
     * Deletes an inappropriate reading log and records it in the violation log.
     * Only admins are allowed to perform this action.
     *
     * @param userId the ID of the admin performing the action
     * @param logId  the ID of the log to delete
     */
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

    /**
     * Retrieves a specific reading log by its ID.
     * Ensures that only the owner of the log or an admin can access it.
     *
     * @param userId the ID of the user attempting to access the log
     * @param logId  the ID of the log to retrieve
     * @return the reading log
     */
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

    /**
     * Retrieves the history of all logs for a specific book, sorted by date.
     *
     * @param title  the title of the book
     * @param author the author of the book
     * @return a list of reading logs for the specified book
     */
    public List<ReadingLog> getAllLogHistory(String title, String author) {
            return readingLogRepository.findByTitleIgnoreCaseAndAuthorIgnoreCaseOrderByDateDesc(title, author);
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
     * Retrieves the user ID from the authenticated principal.
     *
     * @param principal the authenticated principal
     * @return the user ID
     */
    public Long getUserIdFromPrincipal(Principal principal) {
        String email = principal.getName(); 
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new SecurityException("User not found"));

        return user.getId();
    }
    
}
