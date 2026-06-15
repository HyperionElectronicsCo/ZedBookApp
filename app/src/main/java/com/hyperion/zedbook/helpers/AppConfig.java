package com.hyperion.zedbook.helpers;

import android.graphics.Color;

public final class AppConfig {
    private AppConfig() {
    }

    /*
       FIREBASE SETUP:
       1) Create a Firebase project.
       2) Enable Authentication > Email/Password.
       3) Enable Realtime Database while developing.
       4) Replace the values below if you move to another Firebase project.

       This project uses Firebase REST calls, so it stays AIDE-friendly
       and does not require Firebase SDK jars or Gradle plugins.
    */
    public static final String FIREBASE_WEB_API_KEY = "yourapikey-CixXGr9XFk";
    public static final String FIREBASE_DATABASE_URL = "https://yourappname-default-rtdb.europe-west1.firebasedatabase.app";

    public static final String VISIBILITY_PUBLIC = "public";
    public static final String VISIBILITY_FRIENDS = "friends";
    public static final String VISIBILITY_ONLY_ME = "onlyme";

    public static final long NAME_CHANGE_INTERVAL_MS = 183L * 24L * 60L * 60L * 1000L;

    public static final int BLUE = Color.rgb(24, 119, 242);
    public static final int BLUE_DARK = Color.rgb(16, 84, 180);
    public static final int GREEN = Color.rgb(44, 196, 38);
    public static final int DARK = Color.rgb(22, 24, 26);
    public static final int PANEL_DARK = Color.rgb(48, 51, 56);
    public static final int FEED = Color.rgb(240, 242, 245);
    public static final int LINE = Color.rgb(220, 224, 228);
    public static final int TEXT = Color.rgb(28, 30, 33);
    public static final int MUTED = Color.rgb(100, 105, 112);

    public static final int PICK_PROFILE = 40;
    public static final int PICK_STATUS_IMAGE = 41;
    public static final int PICK_STATUS_VIDEO = 42;

    public static boolean firebaseReady() {
        return FIREBASE_WEB_API_KEY.indexOf("PUT_YOUR") < 0 && FIREBASE_DATABASE_URL.indexOf("YOUR_PROJECT_ID") < 0;
    }
}
