package com.hyperion.zedbook.helpers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class TimeHelper {
    private TimeHelper() {
    }

    public static String friendly(long millis) {
        try {
            if (millis <= 0) {
                return "";
            }
            long age = System.currentTimeMillis() - millis;
            if (age < 60000) {
                return "Just now";
            }
            if (age < 3600000) {
                return String.valueOf(age / 60000) + " min ago";
            }
            if (age < 86400000) {
                return String.valueOf(age / 3600000) + " hr ago";
            }
            SimpleDateFormat f = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
            return f.format(new Date(millis));
        } catch (Exception e) {
            return "";
        }
    }

    public static String trim(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, max) + "...";
    }
}
