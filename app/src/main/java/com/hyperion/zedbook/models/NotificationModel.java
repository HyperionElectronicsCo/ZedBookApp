package com.hyperion.zedbook.models;

import org.json.JSONObject;

public class NotificationModel {
    public String id = "";
    public JSONObject json;

    public NotificationModel(String notificationId, JSONObject notificationJson) {
        id = notificationId;
        json = notificationJson;
    }
}
