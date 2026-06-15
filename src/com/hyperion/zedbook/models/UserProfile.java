package com.hyperion.zedbook.models;

import org.json.JSONObject;

public class UserProfile {
    public String uid = "";
    public String email = "";
    public String name = "";
    public String profile = "";

    public static UserProfile fromJson(String id, JSONObject o) {
        UserProfile u = new UserProfile();
        if (o == null) {
            return u;
        }
        u.uid = o.optString("uid", id);
        u.email = o.optString("email", "");
        u.name = o.optString("name", "ZedBook User");
        u.profile = o.optString("profile", "");
        return u;
    }
}
