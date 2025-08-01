package com.cpt202.dailyreadingtracker.user;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserVo implements Serializable {

    private Long id;
    private String username;
    private String email;
    private String avatarFileName;
    private Boolean isEnabled;
    private Boolean isFreezed;
    private Integer timesFlagged;
    private String contactInfo;
    private String aboutMe;

    public UserVo(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.avatarFileName = user.getAvatarFileName();
        this.isEnabled = user.isEnabled();
        this.isFreezed = user.isFreezed();
        this.timesFlagged = user.getTimesFlagged();
        this.contactInfo = user.getContactInfo();
        this.aboutMe = user.getAboutMe();
    }

    @Override
    public String toString() {
        return "UserVo{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", avatarFileName='" + avatarFileName + '\'' +
                ", isEnabled=" + isEnabled +
                ", isFreezed=" + isFreezed +
                ", timesFlagged=" + timesFlagged +
                ", contactInfo='" + contactInfo + '\'' +
                ", aboutMe='" + aboutMe + '\'' +
                '}';
    }
}