package com.quietinbox.models;

public class User {
    public long id;
    public String email;
    public String device_id;
    public boolean is_pro;
    public String created_at;
    public String last_sync;

    public User() {}

    public User(String email, String deviceId) {
        this.email = email;
        this.device_id = deviceId;
    }
}
