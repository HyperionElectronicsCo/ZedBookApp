package com.hyperion.zedbook.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hyperion.zedbook.R;
import com.hyperion.zedbook.helpers.AppConfig;
import com.hyperion.zedbook.helpers.TimeHelper;
import com.hyperion.zedbook.helpers.Ui;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class NotificationAdapter {
    public interface NotificationListener {
        void onOpenNotification(String id, JSONObject n);
        void onMarkRead(String id);
    }

    public static void render(final Context c, LinearLayout list, final JSONObject all, final NotificationListener listener) {
        list.removeAllViews();
        try {
            if (all == null) {
                empty(c, list, "Nothing to see here.");
                return;
            }
            ArrayList<String> keys = new ArrayList<String>();
            Iterator<String> it = all.keys();
            while (it.hasNext()) {
                keys.add(it.next());
            }
            Collections.sort(keys, new Comparator<String>() {
                public int compare(String a, String b) {
                    JSONObject na = all.optJSONObject(a);
                    JSONObject nb = all.optJSONObject(b);
                    long ta = na == null ? 0 : na.optLong("time", 0);
                    long tb = nb == null ? 0 : nb.optLong("time", 0);
                    if (tb > ta) return 1;
                    if (tb < ta) return -1;
                    return 0;
                }
            });
            if (keys.size() == 0) {
                empty(c, list, "Nothing to see here.");
                return;
            }
            for (int i = 0; i < keys.size(); i++) {
                addCard(c, list, keys.get(i), all.getJSONObject(keys.get(i)), listener);
            }
        } catch (Exception e) {
            empty(c, list, "Nothing to see here.");
        }
    }

    private static void empty(Context c, LinearLayout list, String msg) {
        TextView t = Ui.text(c, msg, 15, AppConfig.MUTED, Typeface.NORMAL);
        t.setGravity(Gravity.CENTER);
        list.addView(t, new LinearLayout.LayoutParams(-1, Ui.dp(c, 120)));
    }

    private static void addCard(final Context c, LinearLayout list, final String id, final JSONObject n, final NotificationListener listener) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Ui.dp(c, 10), Ui.dp(c, 8), Ui.dp(c, 10), Ui.dp(c, 8));
        card.setBackgroundResource(n.optBoolean("read", false) ? R.drawable.bg_card : R.drawable.bg_notification_unread);

        TextView msg = Ui.text(c, n.optString("message", "Notification"), 16, AppConfig.TEXT, Typeface.BOLD);
        if ("tag".equals(n.optString("type", ""))) {
            msg.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_notifications, 0, 0, 0);
        } else if ("comment".equals(n.optString("type", ""))) {
            msg.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_comment, 0, 0, 0);
        } else {
            msg.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_friend_add, 0, 0, 0);
        }
        card.addView(msg, new LinearLayout.LayoutParams(-1, Ui.dp(c, 34)));

        TextView time = Ui.text(c, TimeHelper.friendly(n.optLong("time", 0)), 12, Color.GRAY, Typeface.NORMAL);
        card.addView(time, new LinearLayout.LayoutParams(-1, Ui.dp(c, 22)));
        TextView snippet = Ui.text(c, n.optString("postText", ""), 14, Color.DKGRAY, Typeface.NORMAL);
        snippet.setGravity(Gravity.LEFT);
        card.addView(snippet, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout row = new LinearLayout(c);
        Button open = Ui.button(c, "Open Feed", R.drawable.bg_blue_button);
        Button read = Ui.button(c, "Mark Read", R.drawable.bg_grey_button);
        row.addView(open, new LinearLayout.LayoutParams(0, Ui.dp(c, 42), 1));
        row.addView(read, new LinearLayout.LayoutParams(0, Ui.dp(c, 42), 1));
        card.addView(row, new LinearLayout.LayoutParams(-1, Ui.dp(c, 48)));

        open.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                listener.onOpenNotification(id, n);
            }
        });
        read.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                listener.onMarkRead(id);
            }
        });

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(Ui.dp(c, 8), Ui.dp(c, 8), Ui.dp(c, 8), 0);
        list.addView(card, cp);
    }
}
