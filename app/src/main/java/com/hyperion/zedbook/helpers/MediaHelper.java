package com.hyperion.zedbook.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public final class MediaHelper {
    private MediaHelper() {
    }

    public static String imageUriToBase64(Context c, Uri uri, int quality) {
        try {
            InputStream is = c.getContentResolver().openInputStream(uri);
            Bitmap b = BitmapFactory.decodeStream(is);
            if (is != null) {
                is.close();
            }
            if (b == null) {
                return "";
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            b.compress(Bitmap.CompressFormat.JPEG, quality, bos);
            return Base64.encodeToString(bos.toByteArray(), Base64.DEFAULT);
        } catch (Exception e) {
            return "";
        }
    }
}
