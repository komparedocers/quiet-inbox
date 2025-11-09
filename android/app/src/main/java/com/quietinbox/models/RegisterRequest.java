package com.quietinbox.models;

public class RegisterRequest {
    public String email;
    public String password;
    public String device_id;

    public RegisterRequest(String email, String password, String deviceId) {
        this.email = email;
        this.password = password;
        this.device_id = deviceId;
    }
}
