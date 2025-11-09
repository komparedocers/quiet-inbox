package com.quietinbox.models;

import java.util.List;
import java.util.Map;

public class SyncPullResponse {
    public boolean success;
    public List<Map<String, Object>> items;
    public String server_timestamp;

    public SyncPullResponse() {}
}
