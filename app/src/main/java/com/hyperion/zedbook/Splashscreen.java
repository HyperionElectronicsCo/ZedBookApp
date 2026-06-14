package com.hyperion.zedbook;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.hyperion.zedbook.helpers.AppConfig;
import com.hyperion.zedbook.helpers.SessionManager;
import com.hyperion.zedbook.helpers.Ui;

public class Splashscreen extends Activity {
    public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER);
        l.setBackgroundResource(R.drawable.bg_dark);

        ImageView logoIcon = new ImageView(this);
        logoIcon.setImageResource(R.drawable.ic_zedbook_mark);
        l.addView(logoIcon, new LinearLayout.LayoutParams(Ui.dp(this, 84), Ui.dp(this, 84)));

        TextView logo = Ui.text(this, "ZedBook", 40, AppConfig.BLUE, Typeface.BOLD);
        logo.setGravity(Gravity.CENTER);
        l.addView(logo, new LinearLayout.LayoutParams(-1, Ui.dp(this, 70)));

        TextView tagline = Ui.text(this, "Connect. Share. React.", 14, android.graphics.Color.GRAY, Typeface.NORMAL);
        tagline.setGravity(Gravity.CENTER);
        l.addView(tagline, new LinearLayout.LayoutParams(-1, Ui.dp(this, 28)));

        ProgressBar p = new ProgressBar(this);
        l.addView(p, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));
        setContentView(l);

        new Handler().postDelayed(new Runnable() {
            public void run() {
                SessionManager session = new SessionManager(Splashscreen.this);
                if (session.isLoggedIn()) {
                    startActivity(new Intent(Splashscreen.this, Homescreen.class));
                } else {
                    startActivity(new Intent(Splashscreen.this, Loginsignup.class));
                }
                finish();
            }
        }, 1400);
    }
}
