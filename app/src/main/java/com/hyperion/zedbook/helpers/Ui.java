package com.hyperion.zedbook.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hyperion.zedbook.R;

public final class Ui {
    private Ui() {
    }

    public static int dp(Context c, int v) {
        return (int)(v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static TextView text(Context c, String text, int sp, int color, int style) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.DEFAULT, style);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    public static TextView label(Context c, String text) {
        TextView t = text(c, text, 13, AppConfig.MUTED, Typeface.NORMAL);
        t.setPadding(dp(c, 12), 0, dp(c, 12), 0);
        return t;
    }

    public static Button button(Context c, String text, int bg) {
        Button b = new Button(c);
        b.setText(text);
        b.setTextColor(android.graphics.Color.WHITE);
        b.setTextSize(12);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setAllCaps(false);
        b.setBackgroundResource(bg);
        return b;
    }

    public static LinearLayout iconButton(Context c, int icon, String title) {
        LinearLayout cell = new LinearLayout(c);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setPadding(dp(c, 4), dp(c, 4), dp(c, 4), dp(c, 3));
        cell.setClickable(true);
        cell.setBackgroundResource(R.drawable.bg_icon_chip);
        ImageView iv = new ImageView(c);
        iv.setImageResource(icon);
        iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        TextView tv = text(c, title, 10, AppConfig.MUTED, Typeface.BOLD);
        tv.setGravity(Gravity.CENTER);
        cell.addView(iv, new LinearLayout.LayoutParams(dp(c, 26), dp(c, 24)));
        cell.addView(tv, new LinearLayout.LayoutParams(-1, dp(c, 18)));
        return cell;
    }

    public static ImageView avatar(Context c, int sizeDp, String base64) {
        ImageView iv = new ImageView(c);
        iv.setPadding(dp(c, 2), dp(c, 2), dp(c, 2), dp(c, 2));
        iv.setBackgroundResource(R.drawable.bg_avatar_circle);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Bitmap bm = decodeBase64(base64);
        if (bm != null) {
            iv.setImageBitmap(bm);
        } else {
            iv.setImageResource(R.drawable.ic_avatar);
        }
        return iv;
    }

    public static ImageView feedImage(Context c, String base64) {
        ImageView iv = new ImageView(c);
        iv.setBackgroundResource(R.drawable.bg_media_frame);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Bitmap bm = decodeBase64(base64);
        if (bm != null) {
            iv.setImageBitmap(bm);
        } else {
            iv.setImageResource(R.drawable.ic_picture);
        }
        return iv;
    }

    public static Bitmap decodeBase64(String base64) {
        try {
            if (base64 == null || base64.length() < 10) {
                return null;
            }
            byte[] data = Base64.decode(base64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(data, 0, data.length);
        } catch (Exception e) {
            return null;
        }
    }

    public static void gap(LinearLayout l, Context c, int h) {
        TextView g = new TextView(c);
        l.addView(g, new LinearLayout.LayoutParams(1, dp(c, h)));
    }

    public static View line(Context c) {
        TextView v = new TextView(c);
        v.setBackgroundColor(AppConfig.LINE);
        return v;
    }
}
