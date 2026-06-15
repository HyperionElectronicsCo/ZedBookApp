package com.hyperion.zedbook.helpers;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;

public final class LocationHelper {
    private LocationHelper() {
    }

    public static String getCurrentLocationText(Activity a) {
        try {
            if (Build.VERSION.SDK_INT >= 23) {
                if (a.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        a.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    a.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 500);
                    return "";
                }
            }
            LocationManager lm = (LocationManager)a.getSystemService(Context.LOCATION_SERVICE);
            Location l = null;
            try {
                l = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            } catch (Exception e) {
                l = null;
            }
            if (l == null) {
                try {
                    l = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                } catch (Exception e2) {
                    l = null;
                }
            }
            if (l == null) {
                return "";
            }
            return String.valueOf(l.getLatitude()) + "," + String.valueOf(l.getLongitude());
        } catch (Exception e3) {
            return "";
        }
    }
}
