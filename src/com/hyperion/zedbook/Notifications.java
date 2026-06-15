package com.hyperion.zedbook;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.hyperion.zedbook.adapters.NotificationAdapter;
import com.hyperion.zedbook.helpers.AppConfig;
import com.hyperion.zedbook.helpers.FirebaseHelper;
import com.hyperion.zedbook.helpers.SessionManager;
import com.hyperion.zedbook.helpers.Ui;

import org.json.JSONObject;

import java.util.Iterator;

public class Notifications extends BaseSocialActivity implements NotificationAdapter.NotificationListener {
    private LinearLayout notificationList;

    public void onCreate(Bundle b) {
        super.onCreate(b);
        if (!prepareSession()) {
            return;
        }
        buildChrome("Notifications", 2);
        buildNotificationHeader();
        buildNotificationList();
        loadNotifications();
    }

    private void buildNotificationHeader() {
        LinearLayout top = new LinearLayout(this);
        top.setPadding(Ui.dp(this, 10), Ui.dp(this, 8), Ui.dp(this, 10), Ui.dp(this, 8));
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setBackgroundResource(R.drawable.bg_card);
        TextView title = Ui.text(this, "Notifications", 19, AppConfig.TEXT, Typeface.BOLD);
        title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_notifications, 0, 0, 0);
        top.addView(title, new LinearLayout.LayoutParams(0, Ui.dp(this, 50), 1));
        Button refresh = Ui.button(this, "Refresh", R.drawable.bg_blue_button);
        Button mark = Ui.button(this, "Mark read", R.drawable.bg_grey_button);
        top.addView(refresh, new LinearLayout.LayoutParams(Ui.dp(this, 94), Ui.dp(this, 44)));
        top.addView(mark, new LinearLayout.LayoutParams(Ui.dp(this, 104), Ui.dp(this, 44)));
        root.addView(top, new LinearLayout.LayoutParams(-1, Ui.dp(this, 66)));

        TextView help = Ui.text(this, "Friend, comment and @tag alerts show here.", 13, AppConfig.MUTED, Typeface.NORMAL);
        help.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 10), 0);
        help.setBackgroundResource(R.drawable.bg_card);
        root.addView(help, new LinearLayout.LayoutParams(-1, Ui.dp(this, 38)));

        refresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                loadNotifications();
            }
        });
        mark.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                markAllNotificationsRead();
            }
        });
    }

    private void buildNotificationList() {
        ScrollView sv = new ScrollView(this);
        installPullToRefresh(sv);
        notificationList = new LinearLayout(this);
        notificationList.setOrientation(LinearLayout.VERTICAL);
        sv.addView(notificationList);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    protected void loadNotifications() {
        if (notificationList == null) {
            return;
        }
        notificationList.removeAllViews();
        TextView t = Ui.text(this, "Loading notifications...", 15, AppConfig.MUTED, Typeface.NORMAL);
        t.setGravity(Gravity.CENTER);
        notificationList.addView(t, new LinearLayout.LayoutParams(-1, Ui.dp(this, 100)));
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void[] v) {
                try {
                    return FirebaseHelper.getFirebase("notifications/" + session.uid, session.idToken);
                } catch (Exception e) {
                    if (refreshAfterAuthFailure(e.toString())) {
                        try {
                            return FirebaseHelper.getFirebase("notifications/" + session.uid, session.idToken);
                        } catch (Exception second) {
                            return FirebaseHelper.isAuthFailure(second.toString()) ? "AUTH_REQUIRED" : "ERR:" + second.toString();
                        }
                    }
                    return FirebaseHelper.isAuthFailure(e.toString()) ? "AUTH_REQUIRED" : "ERR:" + e.toString();
                }
            }
            protected void onPostExecute(String s) {
                renderNotifications(s);
            }
        }.execute(new Void[0]);
    }

    protected void onPullRefresh() {
        loadNotifications();
    }

    private void renderNotifications(String s) {
        try {
            if (isAuthRequiredResult(s)) {
                NotificationAdapter.render(this, notificationList, null, this);
                handleAuthRequired();
                return;
            }
            if (FirebaseHelper.isEmptyFirebaseValue(s)) {
                NotificationAdapter.render(this, notificationList, null, this);
                return;
            }
            if (s.startsWith("ERR:")) {
                NotificationAdapter.render(this, notificationList, null, this);
                Toast.makeText(this, "Nothing to see here.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!FirebaseHelper.looksLikeJsonObject(s)) {
                NotificationAdapter.render(this, notificationList, null, this);
                return;
            }
            NotificationAdapter.render(this, notificationList, new JSONObject(s), this);
        } catch (Exception e) {
            NotificationAdapter.render(this, notificationList, null, this);
        }
    }

    public void onOpenNotification(String id, JSONObject n) {
        markNotificationRead(id, false);
        startActivity(new Intent(this, Homescreen.class));
        finish();
    }

    public void onMarkRead(String id) {
        markNotificationRead(id, true);
    }

    private void markNotificationRead(final String nid, final boolean reload) {
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void[] v) {
                try {
                    return FirebaseHelper.putFirebase("notifications/" + session.uid + "/" + nid + "/read", session.idToken, "true");
                } catch (Exception e) {
                    if (refreshAfterAuthFailure(e.toString())) {
                        try {
                            return FirebaseHelper.putFirebase("notifications/" + session.uid + "/" + nid + "/read", session.idToken, "true");
                        } catch (Exception second) {
                            return FirebaseHelper.isAuthFailure(second.toString()) ? "AUTH_REQUIRED" : "ERR:" + second.toString();
                        }
                    }
                    return FirebaseHelper.isAuthFailure(e.toString()) ? "AUTH_REQUIRED" : "ERR:" + e.toString();
                }
            }
            protected void onPostExecute(String s) {
                if (reload) {
                    loadNotifications();
                }
            }
        }.execute(new Void[0]);
    }

    private String markAllNotificationsReadRaw() throws Exception {
        String s = FirebaseHelper.getFirebase("notifications/" + session.uid, session.idToken);
        if (FirebaseHelper.isEmptyFirebaseValue(s) || s.length() < 3 || !FirebaseHelper.looksLikeJsonObject(s)) {
            return "OK";
        }
        JSONObject all = new JSONObject(s);
        Iterator<String> it = all.keys();
        while (it.hasNext()) {
            String nid = it.next();
            FirebaseHelper.putFirebase("notifications/" + session.uid + "/" + nid + "/read", session.idToken, "true");
        }
        return "OK";
    }

    private void markAllNotificationsRead() {
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void[] v) {
                try {
                    return markAllNotificationsReadRaw();
                } catch (Exception e) {
                    if (refreshAfterAuthFailure(e.toString())) {
                        try {
                            return markAllNotificationsReadRaw();
                        } catch (Exception second) {
                            return FirebaseHelper.isAuthFailure(second.toString()) ? "AUTH_REQUIRED" : "ERR:" + second.toString();
                        }
                    }
                    return FirebaseHelper.isAuthFailure(e.toString()) ? "AUTH_REQUIRED" : "ERR:" + e.toString();
                }
            }
            protected void onPostExecute(String s) {
                if (isAuthRequiredResult(s)) {
                    handleAuthRequired();
                    return;
                }
                loadNotifications();
            }
        }.execute(new Void[0]);
    }
}
