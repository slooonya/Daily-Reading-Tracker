package com.cpt202.dailyreadingtracker.violationlog;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.cpt202.dailyreadingtracker.readinglog.ReadingLog;
import com.cpt202.dailyreadingtracker.user.User;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ViolationLog {
    
    @Id
    private Long id;

    private String username;
    private String title;
    private String author;
    private LocalDate date;
    private int timeSpent;
    private Integer currentPage;
    private Integer totalPages;
    private String reason;
    private String notes;
    private LocalDateTime createdAt;

    @CreatedDate
    private LocalDateTime deletedAt;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    public ViolationLog(ReadingLog log) {
        this.id = log.getId();
        this.user = log.getUser();
        this.username = log.getUser().getUsername();
        this.author = log.getAuthor();
        this.date = log.getDate(); 
        this.timeSpent = log.getTimeSpent();
        this.currentPage = log.getCurrentPage();
        this.totalPages = log.getTotalPages();
        this.title = log.getTitle();
        this.reason = "Violation of content policy";
        this.notes = log.getNotes();
        this.createdAt = log.getCreatedAt();
        this.deletedAt = LocalDateTime.now();
        log.getUser().addTimesFlagged();
    }

}