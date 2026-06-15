package com.hyperion.zedbook.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hyperion.zedbook.R;
import com.hyperion.zedbook.helpers.AppConfig;
import com.hyperion.zedbook.helpers.Ui;

import org.json.JSONObject;

import java.util.Iterator;
import java.util.Locale;

public class UserAdapter {
    public interface UserActionListener {
        void onAddFriend(String targetUid, String targetName);
        void onViewProfile(String targetUid, String targetName, String targetEmail, String targetProfile);
    }

    public static void render(Context c, LinearLayout results, JSONObject users, String query, String myUid, final UserActionListener listener) {
        results.removeAllViews();
        try {
            if (users == null) {
                empty(c, results, "No registered users found.");
                return;
            }
            String q = query == null ? "" : query.toLowerCase(Locale.US).trim();
            int count = 0;
            Iterator<String> it = users.keys();
            while (it.hasNext()) {
                final String uid = it.next();
                if (myUid.equals(uid)) {
                    continue;
                }
                JSONObject u = users.optJSONObject(uid);
                if (u == null) {
                    continue;
                }
                final String name = u.optString("name", "ZedBook User");
                final String email = u.optString("email", "");
                final String profile = u.optString("profile", "");
                String hay = (name + " " + email).toLowerCase(Locale.US);
                if (q.length() == 0 || hay.indexOf(q) >= 0) {
                    count++;
                    addRow(c, results, uid, name, email, profile, listener);
                }
            }
            if (count == 0) {
                empty(c, results, "No matching users.");
            }
        } catch (Exception e) {
            empty(c, results, "Search error: " + e.toString());
        }
    }

    private static void addRow(Context c, LinearLayout results, final String uid, final String name, final String email, final String profile, final UserActionListener listener) {
        LinearLayout row = new LinearLayout(c);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.dp(c, 8), Ui.dp(c, 8), Ui.dp(c, 8), Ui.dp(c, 8));
        row.setBackgroundResource(R.drawable.bg_card);
        row.addView(Ui.avatar(c, 44, profile), new LinearLayout.LayoutParams(Ui.dp(c, 44), Ui.dp(c, 44)));

        LinearLayout texts = new LinearLayout(c);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(Ui.text(c, name, 16, AppConfig.TEXT, Typeface.BOLD), new LinearLayout.LayoutParams(-1, Ui.dp(c, 25)));
        texts.addView(Ui.text(c, email, 12, AppConfig.MUTED, Typeface.NORMAL), new LinearLayout.LayoutParams(-1, Ui.dp(c, 22)));
        row.addView(texts, new LinearLayout.LayoutParams(0, Ui.dp(c, 48), 1));

        Button view = Ui.button(c, "View", R.drawable.bg_grey_button);
        view.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_profile, 0, 0, 0);
        view.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                listener.onViewProfile(uid, name, email, profile);
            }
        });
        row.addView(view, new LinearLayout.LayoutParams(Ui.dp(c, 76), Ui.dp(c, 42)));

        Button add = Ui.button(c, "Add", R.drawable.bg_blue_button);
        add.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_friend_add, 0, 0, 0);
        add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                listener.onAddFriend(uid, name);
            }
        });
        row.addView(add, new LinearLayout.LayoutParams(Ui.dp(c, 74), Ui.dp(c, 42)));

        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, Ui.dp(c, 66));
        rp.setMargins(0, Ui.dp(c, 6), 0, 0);
        results.addView(row, rp);
    }

    private static void empty(Context c, LinearLayout results, String msg) {
        TextView t = Ui.text(c, msg, 14, AppConfig.MUTED, Typeface.NORMAL);
        t.setGravity(Gravity.CENTER);
        results.addView(t, new LinearLayout.LayoutParams(-1, Ui.dp(c, 90)));
    }
}
