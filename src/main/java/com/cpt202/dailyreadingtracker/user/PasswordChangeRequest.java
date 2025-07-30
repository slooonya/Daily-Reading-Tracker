package com.cpt202.dailyreadingtracker.user;

import lombok.Data;

@Data
class PasswordChangeRequest {
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;
}
