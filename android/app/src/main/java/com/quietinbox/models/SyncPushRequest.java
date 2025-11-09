package com.quietinbox.models;

import java.util.List;
import java.util.Map;

public class SyncPushRequest {
    public List<Map<String, Object>> items;
    public String device_timestamp;

    public SyncPushRequest(List<Map<String, Object>> items, String deviceTimestamp) {
        this.items = items;
        this.device_timestamp = deviceTimestamp;
    }
}
