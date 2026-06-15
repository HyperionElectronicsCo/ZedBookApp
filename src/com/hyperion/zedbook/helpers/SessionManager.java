package com.hyperion.zedbook.helpers;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private SharedPreferences prefs;

    public String uid = "";
    public String idToken = "";
    public String refreshToken = "";
    public String email = "";
    public String displayName = "";
    public String profileImage64 = "";
    public long tokenSavedAt = 0L;

    public SessionManager(Context c) {
        prefs = c.getSharedPreferences("zedbook", Context.MODE_PRIVATE);
        load();
    }

    public void load() {
        uid = prefs.getString("uid", "");
        idToken = prefs.getString("token", "");
        refreshToken = prefs.getString("refreshToken", "");
        email = prefs.getString("email", "");
        displayName = prefs.getString("name", "");
        profileImage64 = prefs.getString("profile", "");
        tokenSavedAt = prefs.getLong("tokenSavedAt", 0L);
    }

    public boolean isLoggedIn() {
        return uid.length() > 0 && (idToken.length() > 0 || refreshToken.length() > 0);
    }

    public boolean canRefreshToken() {
        return refreshToken != null && refreshToken.length() > 0;
    }

    public void saveAuth(String newUid, String newToken, String newEmail, String newName) {
        saveAuth(newUid, newToken, "", newEmail, newName);
    }

    public void saveAuth(String newUid, String newToken, String newRefreshToken, String newEmail, String newName) {
        uid = safe(newUid);
        idToken = safe(newToken);
        refreshToken = safe(newRefreshToken);
        email = safe(newEmail);
        displayName = safe(newName);
        tokenSavedAt = System.currentTimeMillis();
        prefs.edit()
                .putString("uid", uid)
                .putString("token", idToken)
                .putString("refreshToken", refreshToken)
                .putString("email", email)
                .putString("name", displayName)
                .putLong("tokenSavedAt", tokenSavedAt)
                .commit();
    }

    public void saveFreshToken(String newToken, String newRefreshToken) {
        saveFreshToken(uid, newToken, newRefreshToken);
    }

    public void saveFreshToken(String newUid, String newToken, String newRefreshToken) {
        if (newUid != null && newUid.length() > 0) {
            uid = newUid;
        }
        if (newToken != null && newToken.length() > 0) {
            idToken = newToken;
        }
        if (newRefreshToken != null && newRefreshToken.length() > 0) {
            refreshToken = newRefreshToken;
        }
        tokenSavedAt = System.currentTimeMillis();
        prefs.edit()
                .putString("uid", uid)
                .putString("token", idToken)
                .putString("refreshToken", refreshToken)
                .putLong("tokenSavedAt", tokenSavedAt)
                .commit();
    }

    public void saveProfile(String newName, String newProfile) {
        displayName = safe(newName);
        profileImage64 = safe(newProfile);
        prefs.edit().putString("name", displayName).putString("profile", profileImage64).commit();
    }

    public void clear() {
        prefs.edit().clear().commit();
        uid = "";
        idToken = "";
        refreshToken = "";
        email = "";
        displayName = "";
        profileImage64 = "";
        tokenSavedAt = 0L;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
