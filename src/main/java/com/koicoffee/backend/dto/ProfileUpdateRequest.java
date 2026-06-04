package com.koicoffee.backend.dto;

public class ProfileUpdateRequest {
    private String fullName;
    private String oldPassword;
    private String newPassword;

    // Constructors
    public ProfileUpdateRequest() {}

    public ProfileUpdateRequest(String fullName, String oldPassword, String newPassword) {
        this.fullName = fullName;
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }

    // Getters and Setters
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}