package com.hyperion.zedbook.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
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

public class PostAdapter {
    public interface PostActionListener {
        void onEditPost(String postId, JSONObject post);
        void onReactPost(String postId, String type);
        void onCommentPost(String postId, JSONObject post);
        void onOpenVideo(String uri);
        void onOpenLocation(String locationText);
    }

    public static void render(final Context c, LinearLayout list, final JSONObject all, final String currentUid, boolean onlyMine, final PostActionListener listener) {
        list.removeAllViews();
        try {
            if (all == null) {
                empty(c, list, onlyMine ? "You have not posted a status yet." : "No statuses yet. Be the first to post.");
                return;
            }
            ArrayList<String> keys = new ArrayList<String>();
            Iterator<String> it = all.keys();
            while (it.hasNext()) {
                String k = it.next();
                JSONObject p = all.optJSONObject(k);
                if (p != null) {
                    if (!onlyMine || currentUid.equals(p.optString("uid", ""))) {
                        keys.add(k);
                    }
                }
            }
            Collections.sort(keys, new Comparator<String>() {
                public int compare(String a, String b) {
                    JSONObject pa = all.optJSONObject(a);
                    JSONObject pb = all.optJSONObject(b);
                    long ta = pa == null ? 0 : pa.optLong("time", 0);
                    long tb = pb == null ? 0 : pb.optLong("time", 0);
                    if (tb > ta) return 1;
                    if (tb < ta) return -1;
                    return 0;
                }
            });
            if (keys.size() == 0) {
                empty(c, list, onlyMine ? "You have not posted a status yet." : "No statuses yet. Be the first to post.");
                return;
            }
            for (int i = 0; i < keys.size(); i++) {
                String postId = keys.get(i);
                addCard(c, list, postId, all.getJSONObject(postId), currentUid, listener);
            }
        } catch (Exception e) {
            empty(c, list, "Feed error: " + e.toString());
        }
    }

    private static void empty(Context c, LinearLayout list, String msg) {
        TextView t = Ui.text(c, msg, 15, AppConfig.MUTED, Typeface.NORMAL);
        t.setGravity(Gravity.CENTER);
        list.addView(t, new LinearLayout.LayoutParams(-1, Ui.dp(c, 120)));
    }

    private static void addCard(final Context c, LinearLayout list, final String postId, final JSONObject p, String currentUid, final PostActionListener listener) throws Exception {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Ui.dp(c, 10), Ui.dp(c, 10), Ui.dp(c, 10), Ui.dp(c, 10));
        card.setBackgroundResource(R.drawable.bg_card);

        LinearLayout head = new LinearLayout(c);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(Ui.avatar(c, 44, p.optString("profile", "")), new LinearLayout.LayoutParams(Ui.dp(c, 44), Ui.dp(c, 44)));

        LinearLayout headText = new LinearLayout(c);
        headText.setOrientation(LinearLayout.VERTICAL);
        TextView name = Ui.text(c, p.optString("name", "User"), 16, AppConfig.TEXT, Typeface.BOLD);
        String timeText = TimeHelper.friendly(p.optLong("time", 0));
        if (p.optBoolean("edited", false)) {
            timeText = timeText + "  Edited";
        }
        TextView time = Ui.text(c, timeText, 12, Color.GRAY, Typeface.NORMAL);
        headText.addView(name, new LinearLayout.LayoutParams(-1, Ui.dp(c, 25)));
        headText.addView(time, new LinearLayout.LayoutParams(-1, Ui.dp(c, 22)));
        head.addView(headText, new LinearLayout.LayoutParams(0, Ui.dp(c, 50), 1));

        if (currentUid.equals(p.optString("uid", ""))) {
            Button edit = Ui.button(c, "Edit", R.drawable.bg_blue_button);
            edit.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_edit, 0, 0, 0);
            edit.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    listener.onEditPost(postId, p);
                }
            });
            head.addView(edit, new LinearLayout.LayoutParams(Ui.dp(c, 82), Ui.dp(c, 42)));
        }
        card.addView(head, new LinearLayout.LayoutParams(-1, Ui.dp(c, 52)));

        TextView text = Ui.text(c, p.optString("text", ""), 16, AppConfig.TEXT, Typeface.NORMAL);
        text.setGravity(Gravity.LEFT);
        text.setPadding(0, Ui.dp(c, 8), 0, Ui.dp(c, 8));
        card.addView(text, new LinearLayout.LayoutParams(-1, -2));

        String img = p.optString("image", "");
        if (img.length() > 10) {
            card.addView(Ui.feedImage(c, img), new LinearLayout.LayoutParams(-1, Ui.dp(c, 260)));
        }

        final String video = p.optString("video", "");
        if (video.length() > 0) {
            TextView vv = Ui.text(c, "Video attached - tap to open", 14, AppConfig.BLUE, Typeface.BOLD);
            vv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_video, 0, 0, 0);
            vv.setPadding(0, Ui.dp(c, 8), 0, Ui.dp(c, 8));
            vv.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    listener.onOpenVideo(video);
                }
            });
            card.addView(vv, new LinearLayout.LayoutParams(-1, Ui.dp(c, 44)));
        }

        final String loc = p.optString("location", "");
        if (loc.length() > 0) {
            TextView lt = Ui.text(c, "Location: " + loc + "  - tap for map", 14, AppConfig.BLUE, Typeface.NORMAL);
            lt.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_location, 0, 0, 0);
            lt.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    listener.onOpenLocation(loc);
                }
            });
            card.addView(lt, new LinearLayout.LayoutParams(-1, Ui.dp(c, 40)));
        }

        LinearLayout reacts = new LinearLayout(c);
        Button up = Ui.button(c, "Up " + count(p, "up"), R.drawable.bg_action_button);
        Button down = Ui.button(c, "Down " + count(p, "down"), R.drawable.bg_grey_button);
        Button com = Ui.button(c, "Comment", R.drawable.bg_action_button);
        up.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_thumb_up, 0, 0, 0);
        down.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_thumb_down, 0, 0, 0);
        com.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_comment, 0, 0, 0);
        reacts.addView(up, new LinearLayout.LayoutParams(0, Ui.dp(c, 42), 1));
        reacts.addView(down, new LinearLayout.LayoutParams(0, Ui.dp(c, 42), 1));
        reacts.addView(com, new LinearLayout.LayoutParams(0, Ui.dp(c, 42), 1));
        card.addView(reacts, new LinearLayout.LayoutParams(-1, Ui.dp(c, 48)));
        up.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                listener.onReactPost(postId, "up");
            }
        });
        down.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                listener.onReactPost(postId, "down");
            }
        });
        com.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                listener.onCommentPost(postId, p);
            }
        });

        JSONObject comments = p.optJSONObject("comments");
        if (comments != null) {
            ArrayList<String> ckeys = new ArrayList<String>();
            Iterator<String> cit = comments.keys();
            while (cit.hasNext()) {
                ckeys.add(cit.next());
            }
            for (int i = 0; i < ckeys.size(); i++) {
                JSONObject cm = comments.optJSONObject(ckeys.get(i));
                if (cm != null) {
                    TextView ct = Ui.text(c, cm.optString("name", "User") + ": " + cm.optString("text", ""), 14, Color.DKGRAY, Typeface.NORMAL);
                    ct.setPadding(Ui.dp(c, 14), 0, 0, 0);
                    card.addView(ct, new LinearLayout.LayoutParams(-1, -2));
                }
            }
        }

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(Ui.dp(c, 8), Ui.dp(c, 8), Ui.dp(c, 8), 0);
        list.addView(card, cp);
    }

    private static int count(JSONObject p, String type) {
        JSONObject r = p.optJSONObject("reactions");
        if (r == null) {
            return 0;
        }
        int c = 0;
        Iterator<String> it = r.keys();
        while (it.hasNext()) {
            if (type.equals(r.optString(it.next()))) {
                c++;
            }
        }
        return c;
    }

    public static void openVideo(Context c, String uri) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            c.startActivity(i);
        } catch (Exception ignored) {
        }
    }
}
