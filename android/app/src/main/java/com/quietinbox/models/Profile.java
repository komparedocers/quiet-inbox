package com.quietinbox.models;

public class Profile {
    public long id;
    public long user_id;
    public String name;
    public String quiet_hours_start;
    public String quiet_hours_end;
    public String rules_json;
    public boolean is_active;
    public String created_at;

    public Profile() {}

    public Profile(String name, String quietHoursStart, String quietHoursEnd) {
        this.name = name;
        this.quiet_hours_start = quietHoursStart;
        this.quiet_hours_end = quietHoursEnd;
        this.rules_json = "{}";
    }
}
