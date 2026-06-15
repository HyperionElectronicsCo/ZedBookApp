package com.hyperion.zedbook;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.hyperion.zedbook.helpers.AppConfig;
import com.hyperion.zedbook.helpers.FirebaseHelper;
import com.hyperion.zedbook.helpers.SessionManager;
import com.hyperion.zedbook.helpers.Ui;

import org.json.JSONObject;

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

        TextView syncing = Ui.text(this, "Refreshing your feed...", 13, android.graphics.Color.GRAY, Typeface.NORMAL);
        syncing.setGravity(Gravity.CENTER);
        l.addView(syncing, new LinearLayout.LayoutParams(-1, Ui.dp(this, 28)));

        ProgressBar p = new ProgressBar(this);
        l.addView(p, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));
        setContentView(l);

        new Handler().postDelayed(new Runnable() {
            public void run() {
                refreshSessionThenOpen();
            }
        }, 1000);
    }

    private void refreshSessionThenOpen() {
        final SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            openLogin();
            return;
        }
        new AsyncTask<Void, Void, Boolean>() {
            protected Boolean doInBackground(Void[] v) {
                boolean refreshed = false;
                if (session.canRefreshToken()) {
                    try {
                        String r = FirebaseHelper.refreshIdToken(session.refreshToken);
                        JSONObject o = new JSONObject(r);
                        String token = o.optString("id_token", "");
                        String refresh = o.optString("refresh_token", session.refreshToken);
                        String uid = o.optString("user_id", session.uid);
                        if (token.length() > 0) {
                            session.saveFreshToken(uid, token, refresh);
                            refreshed = true;
                        }
                    } catch (Exception e) {
                        if (FirebaseHelper.isAuthFailure(e.toString())) {
                            return Boolean.FALSE;
                        }
                    }
                }

                if (session.idToken.length() == 0) {
                    return Boolean.FALSE;
                }

                try {
                    String user = FirebaseHelper.getFirebase("users/" + session.uid, session.idToken);
                    if (!FirebaseHelper.isEmptyFirebaseValue(user) && FirebaseHelper.looksLikeJsonObject(user)) {
                        JSONObject u = new JSONObject(user);
                        session.saveProfile(u.optString("name", session.displayName), u.optString("profile", session.profileImage64));
                    }
                } catch (Exception e) {
                    if (FirebaseHelper.isAuthFailure(e.toString())) {
                        if (!refreshed && session.canRefreshToken()) {
                            try {
                                String r = FirebaseHelper.refreshIdToken(session.refreshToken);
                                JSONObject o = new JSONObject(r);
                                String token = o.optString("id_token", "");
                                String refresh = o.optString("refresh_token", session.refreshToken);
                                String uid = o.optString("user_id", session.uid);
                                if (token.length() > 0) {
                                    session.saveFreshToken(uid, token, refresh);
                                    String user = FirebaseHelper.getFirebase("users/" + session.uid, session.idToken);
                                    if (!FirebaseHelper.isEmptyFirebaseValue(user) && FirebaseHelper.looksLikeJsonObject(user)) {
                                        JSONObject u = new JSONObject(user);
                                        session.saveProfile(u.optString("name", session.displayName), u.optString("profile", session.profileImage64));
                                    }
                                    return Boolean.TRUE;
                                }
                            } catch (Exception ignored) {
                            }
                        }
                        return Boolean.FALSE;
                    }
                }
                return Boolean.TRUE;
            }

            protected void onPostExecute(Boolean ok) {
                if (ok != null && ok.booleanValue()) {
                    openHome();
                } else {
                    session.clear();
                    openLogin();
                }
            }
        }.execute(new Void[0]);
    }

    private void openHome() {
        startActivity(new Intent(Splashscreen.this, Homescreen.class));
        finish();
    }

    private void openLogin() {
        startActivity(new Intent(Splashscreen.this, Loginsignup.class));
        finish();
    }
}
