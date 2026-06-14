package com.hyperion.zedbook.helpers;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private SharedPreferences prefs;

    public String uid = "";
    public String idToken = "";
    public String email = "";
    public String displayName = "";
    public String profileImage64 = "";

    public SessionManager(Context c) {
        prefs = c.getSharedPreferences("zedbook", Context.MODE_PRIVATE);
        load();
    }

    public void load() {
        uid = prefs.getString("uid", "");
        idToken = prefs.getString("token", "");
        email = prefs.getString("email", "");
        displayName = prefs.getString("name", "");
        profileImage64 = prefs.getString("profile", "");
    }

    public boolean isLoggedIn() {
        return uid.length() > 0 && idToken.length() > 0;
    }

    public void saveAuth(String newUid, String newToken, String newEmail, String newName) {
        uid = newUid;
        idToken = newToken;
        email = newEmail;
        displayName = newName;
        prefs.edit().putString("uid", uid).putString("token", idToken).putString("email", email).putString("name", displayName).commit();
    }

    public void saveProfile(String newName, String newProfile) {
        displayName = newName;
        profileImage64 = newProfile;
        prefs.edit().putString("name", displayName).putString("profile", profileImage64).commit();
    }

    public void clear() {
        prefs.edit().clear().commit();
        uid = "";
        idToken = "";
        email = "";
        displayName = "";
        profileImage64 = "";
    }
}
