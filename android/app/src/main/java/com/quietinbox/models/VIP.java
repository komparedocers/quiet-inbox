package com.quietinbox.models;

public class VIP {
    public long id;
    public long user_id;
    public String app_package;
    public String identifier;
    public int priority;
    public boolean bypass_quiet_hours;
    public String created_at;

    public VIP() {}

    public VIP(String appPackage, String identifier, int priority) {
        this.app_package = appPackage;
        this.identifier = identifier;
        this.priority = priority;
        this.bypass_quiet_hours = true;
    }
}
