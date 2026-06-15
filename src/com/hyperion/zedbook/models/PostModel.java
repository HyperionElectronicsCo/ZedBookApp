package com.hyperion.zedbook.models;

import org.json.JSONObject;

public class PostModel {
    public String id = "";
    public JSONObject json;

    public PostModel(String postId, JSONObject postJson) {
        id = postId;
        json = postJson;
    }
}
