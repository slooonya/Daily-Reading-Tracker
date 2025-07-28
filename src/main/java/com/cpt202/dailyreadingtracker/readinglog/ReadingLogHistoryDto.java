package com.cpt202.dailyreadingtracker.readinglog;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadingLogHistoryDto {
    
    private Long id;
    private String title;
    private String author;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    
    private Integer timeSpent;
    private Integer currentPage;
    private Integer totalPages;
    private String notes;
    private boolean isCurrent; 
    
    public ReadingLogHistoryDto(ReadingLog log, Long currentLogId) {
        this.id = log.getId();
        this.title = log.getTitle();
        this.author = log.getAuthor();
        this.date = log.getDate();
        this.timeSpent = log.getTimeSpent();
        this.currentPage = log.getCurrentPage();
        this.totalPages = log.getTotalPages();
        this.notes = log.getNotes();
        this.isCurrent = log.getId().equals(currentLogId);
    }
}

