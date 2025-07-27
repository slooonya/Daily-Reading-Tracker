package com.cpt202.dailyreadingtracker.readinglog;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadingLogDto {
    
    @NotBlank
    private String title;

    @NotBlank
    private String author;

    @NotNull
    @PastOrPresent
    private LocalDate date;

    @Min(value = 0, message = "The time spent must be greater than or equal to 0")
    private int timeSpent;

    @Min(value = 1, message = "The current page number must be greater than 0")
    private Integer currentPage;

    @Min(value = 1, message = "The total pages number must be greater than 0")
    private Integer totalPages;

    @Size(max = 65535, message = "Notes are too long")
    private String notes;
}
