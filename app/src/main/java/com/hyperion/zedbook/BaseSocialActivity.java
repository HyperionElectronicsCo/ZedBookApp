package com.hyperion.zedbook;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.hyperion.zedbook.adapters.PostAdapter;
import com.hyperion.zedbook.adapters.UserAdapter;
import com.hyperion.zedbook.helpers.AppConfig;
import com.hyperion.zedbook.helpers.FirebaseHelper;
import com.hyperion.zedbook.helpers.LocationHelper;
import com.hyperion.zedbook.helpers.MediaHelper;
import com.hyperion.zedbook.helpers.PostService;
import com.hyperion.zedbook.helpers.SessionManager;
import com.hyperion.zedbook.helpers.Ui;

import org.json.JSONObject;

public class BaseSocialActivity extends Activity implements PostAdapter.PostActionListener, UserAdapter.UserActionListener {
    protected SessionManager session;
    protected LinearLayout root;
    protected LinearLayout main;
    protected LinearLayout feedList;
    protected EditText statusBox;
    protected Button activePostButton;

    protected String pendingImage64 = "";
    protected String pendingVideoUri = "";
    protected String pendingLocation = "";
    protected String editingPostId = null;
    protected boolean feedOnlyMine = false;

    public void onCreate(Bundle b) {
        super.onCreate(b);
    }

    protected boolean prepareSession() {
        session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, Loginsignup.class));
            finish();
            return false;
        }
        return true;
    }

    protected LinearLayout buildChrome(String pageTitle, int tabIndex) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.bg_feed);
        setContentView(root);
        buildTopBar(pageTitle);
        buildTabs(tabIndex);
        main = root;
        return root;
    }

    protected void buildTopBar(String pageTitle) {
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(Ui.dp(this, 8), Ui.dp(this, 5), Ui.dp(this, 6), Ui.dp(this, 5));
        top.setBackgroundResource(R.drawable.bg_top_bar);

        TextView title = Ui.text(this, "ZedBook", 24, AppConfig.BLUE, Typeface.BOLD);
        title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_zedbook_mark, 0, 0, 0);
        top.addView(title, new LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1));

        LinearLayout search = Ui.iconButton(this, R.drawable.ic_search, "Search");
        LinearLayout invite = Ui.iconButton(this, R.drawable.ic_invite, "Invite");
        LinearLayout settings = Ui.iconButton(this, R.drawable.ic_settings, "Settings");
        top.addView(search, new LinearLayout.LayoutParams(Ui.dp(this, 58), Ui.dp(this, 52)));
        top.addView(invite, new LinearLayout.LayoutParams(Ui.dp(this, 58), Ui.dp(this, 52)));
        top.addView(settings, new LinearLayout.LayoutParams(Ui.dp(this, 62), Ui.dp(this, 52)));

        search.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showSearchDialog();
            }
        });
        invite.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                inviteFriends();
            }
        });
        settings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(BaseSocialActivity.this, Settings.class));
            }
        });
        root.addView(top, new LinearLayout.LayoutParams(-1, Ui.dp(this, 62)));
    }

    protected void buildTabs(int tabIndex) {
        LinearLayout tabs = new LinearLayout(this);
        tabs.setBackgroundResource(R.drawable.bg_top_bar);
        addTab(tabs, "Home", R.drawable.ic_home, tabIndex == 0, Homescreen.class);
        addTab(tabs, "Profile", R.drawable.ic_profile, tabIndex == 1, Profile.class);
        addTab(tabs, "Alerts", R.drawable.ic_notifications, tabIndex == 2, Notifications.class);
        root.addView(tabs, new LinearLayout.LayoutParams(-1, Ui.dp(this, 50)));
        root.addView(Ui.line(this), new LinearLayout.LayoutParams(-1, 1));
    }

    private void addTab(LinearLayout tabs, String text, int icon, boolean active, final Class cls) {
        Button b = Ui.button(this, text, active ? R.drawable.bg_tab_selected : R.drawable.bg_tab_normal);
        b.setTextColor(active ? android.graphics.Color.WHITE : AppConfig.MUTED);
        b.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0);
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!BaseSocialActivity.this.getClass().equals(cls)) {
                    startActivity(new Intent(BaseSocialActivity.this, cls));
                    finish();
                }
            }
        });
        tabs.addView(b, new LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1));
    }

    protected void addSectionHeader(String title, String subtitle) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(this, 12), Ui.dp(this, 6), Ui.dp(this, 12), Ui.dp(this, 4));
        box.setBackgroundResource(R.drawable.bg_card);
        box.addView(Ui.text(this, title, 18, AppConfig.TEXT, Typeface.BOLD), new LinearLayout.LayoutParams(-1, Ui.dp(this, 30)));
        if (subtitle != null && subtitle.length() > 0) {
            box.addView(Ui.text(this, subtitle, 13, AppConfig.MUTED, Typeface.NORMAL), new LinearLayout.LayoutParams(-1, Ui.dp(this, 24)));
        }
        root.addView(box, new LinearLayout.LayoutParams(-1, subtitle == null || subtitle.length() == 0 ? Ui.dp(this, 42) : Ui.dp(this, 66)));
    }

    protected void buildComposer(String hint) {
        LinearLayout composer = new LinearLayout(this);
        composer.setPadding(Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 8), Ui.dp(this, 6));
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setBackgroundResource(R.drawable.bg_card);
        composer.addView(Ui.avatar(this, 42, session.profileImage64), new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 42)));

        statusBox = new EditText(this);
        statusBox.setHint(hint);
        statusBox.setTextSize(15);
        statusBox.setSingleLine(false);
        statusBox.setMinLines(1);
        statusBox.setMaxLines(3);
        statusBox.setTextColor(AppConfig.TEXT);
        statusBox.setHintTextColor(android.graphics.Color.rgb(120, 125, 130));
        statusBox.setBackgroundResource(R.drawable.bg_input_light);
        statusBox.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);
        composer.addView(statusBox, new LinearLayout.LayoutParams(0, Ui.dp(this, 50), 1));

        activePostButton = Ui.button(this, "Post", R.drawable.bg_blue_button);
        activePostButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_status, 0, 0, 0);
        composer.addView(activePostButton, new LinearLayout.LayoutParams(Ui.dp(this, 78), Ui.dp(this, 50)));
        root.addView(composer, new LinearLayout.LayoutParams(-1, Ui.dp(this, 64)));

        LinearLayout attach = new LinearLayout(this);
        attach.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 8), Ui.dp(this, 6));
        attach.setBackgroundResource(R.drawable.bg_card);
        Button img = Ui.button(this, "Picture", R.drawable.bg_attach_button);
        Button vid = Ui.button(this, "Video", R.drawable.bg_attach_button);
        Button loc = Ui.button(this, "Location", R.drawable.bg_attach_button);
        Button clear = Ui.button(this, "Clear", R.drawable.bg_grey_button);
        img.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_picture, 0, 0, 0);
        vid.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_video, 0, 0, 0);
        loc.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_location, 0, 0, 0);
        attach.addView(img, new LinearLayout.LayoutParams(0, Ui.dp(this, 40), 1));
        attach.addView(vid, new LinearLayout.LayoutParams(0, Ui.dp(this, 40), 1));
        attach.addView(loc, new LinearLayout.LayoutParams(0, Ui.dp(this, 40), 1));
        attach.addView(clear, new LinearLayout.LayoutParams(0, Ui.dp(this, 40), 1));
        root.addView(attach, new LinearLayout.LayoutParams(-1, Ui.dp(this, 48)));

        activePostButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                saveStatus();
            }
        });
        img.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pick(AppConfig.PICK_STATUS_IMAGE, "image/*");
            }
        });
        vid.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pick(AppConfig.PICK_STATUS_VIDEO, "video/*");
            }
        });
        loc.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                attachLocation();
            }
        });
        clear.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                resetComposer();
                toast("Composer cleared");
            }
        });
    }

    protected void buildFeedHolder() {
        ScrollView sv = new ScrollView(this);
        feedList = new LinearLayout(this);
        feedList.setOrientation(LinearLayout.VERTICAL);
        sv.addView(feedList);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    protected void loadPosts(final boolean onlyMine) {
        feedOnlyMine = onlyMine;
        if (feedList == null) {
            return;
        }
        feedList.removeAllViews();
        TextView loading = Ui.text(this, "Loading feed...", 15, AppConfig.MUTED, Typeface.NORMAL);
        loading.setGravity(Gravity.CENTER);
        feedList.addView(loading, new LinearLayout.LayoutParams(-1, Ui.dp(this, 100)));
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void[] v) {
                try {
                    return FirebaseHelper.getFirebase("posts", session.idToken);
                } catch (Exception e) {
                    return "ERR:" + e.toString();
                }
            }
            protected void onPostExecute(String s) {
                renderPostString(s, onlyMine);
            }
        }.execute(new Void[0]);
    }

    protected void renderPostString(String s, boolean onlyMine) {
        try {
            if (s == null || s.equals("null") || s.startsWith("ERR:")) {
                PostAdapter.render(this, feedList, null, session.uid, onlyMine, this);
                if (s != null && s.startsWith("ERR:")) {
                    toast(s);
                }
                return;
            }
            JSONObject all = new JSONObject(s);
            PostAdapter.render(this, feedList, all, session.uid, onlyMine, this);
        } catch (Exception e) {
            toast("Feed parse error: " + e.toString());
        }
    }

    protected void saveStatus() {
        if (statusBox == null) {
            return;
        }
        final String text = statusBox.getText().toString().trim();
        if (text.length() == 0 && pendingImage64.length() == 0 && pendingVideoUri.length() == 0 && pendingLocation.length() == 0) {
            toast("Write a status or attach something first.");
            return;
        }
        final String postId = editingPostId;
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void[] v) {
                try {
                    if (postId != null && postId.length() > 0) {
                        return PostService.updatePost(session, postId, text, pendingImage64, pendingVideoUri, pendingLocation);
                    }
                    return PostService.createPost(session, text, pendingImage64, pendingVideoUri, pendingLocation);
                } catch (Exception e) {
                    return "ERR:" + e.toString();
                }
            }
            protected void onPostExecute(String s) {
                if (s != null && s.startsWith("ERR:")) {
                    toast(s);
                    return;
                }
                resetComposer();
                loadPosts(feedOnlyMine);
            }
        }.execute(new Void[0]);
    }

    protected void resetComposer() {
        editingPostId = null;
        pendingImage64 = "";
        pendingVideoUri = "";
        pendingLocation = "";
        if (statusBox != null) {
            statusBox.setText("");
        }
        if (activePostButton != null) {
            activePostButton.setText("Post");
            activePostButton.setBackgroundResource(R.drawable.bg_blue_button);
        }
    }

    protected void pick(int code, String type) {
        try {
            Intent i = new Intent(Intent.ACTION_PICK);
            i.setType(type);
            startActivityForResult(i, code);
        } catch (Exception e) {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType(type);
            startActivityForResult(Intent.createChooser(i, "Choose file"), code);
        }
    }

    public void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        if (request == AppConfig.PICK_STATUS_IMAGE) {
            pendingImage64 = MediaHelper.imageUriToBase64(this, data.getData(), 70);
            toast(pendingImage64.length() > 0 ? "Picture attached" : "Could not attach picture");
        } else if (request == AppConfig.PICK_STATUS_VIDEO) {
            pendingVideoUri = data.getData().toString();
            toast("Video attached");
        } else if (request == AppConfig.PICK_PROFILE) {
            String img = MediaHelper.imageUriToBase64(this, data.getData(), 70);
            profileImagePicked(img);
        }
    }

    protected void profileImagePicked(String base64) {
    }

    protected void attachLocation() {
        String loc = LocationHelper.getCurrentLocationText(this);
        if (loc.length() == 0) {
            toast("Turn on location and allow permission, then press Location again.");
            return;
        }
        pendingLocation = loc;
        toast("Location attached: " + loc);
    }

    public void onEditPost(String postId, JSONObject post) {
        editingPostId = postId;
        statusBox.setText(post.optString("text", ""));
        pendingImage64 = post.optString("image", "");
        pendingVideoUri = post.optString("video", "");
        pendingLocation = post.optString("location", "");
        activePostButton.setText("Save Edit");
        activePostButton.setBackgroundResource(R.drawable.bg_green_button);
        toast("Edit the status, then press Save Edit.");
    }

    public void onReactPost(final String postId, final String type) {
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void[] v) {
                try {
                    return PostService.react(session, postId, type);
                } catch (Exception e) {
                    return "ERR:" + e.toString();
                }
            }
            protected void onPostExecute(String s) {
                loadPosts(feedOnlyMine);
            }
        }.execute(new Void[0]);
    }

    public void onCommentPost(final String postId, final JSONObject post) {
        final EditText e = new EditText(this);
        e.setHint("Write comment");
        e.setMinLines(2);
        new AlertDialog.Builder(this).setTitle("Comment").setView(e).setPositiveButton("Post", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int w) {
                sendComment(postId, post, e.getText().toString());
            }
        }).setNegativeButton("Cancel", null).show();
    }

    protected void sendComment(final String postId, final JSONObject post, final String text) {
        if (text == null || text.trim().length() == 0) {
            return;
        }
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void[] v) {
                try {
                    return PostService.comment(session, postId, text.trim(), post.optString("uid", ""), post.optString("text", ""));
                } catch (Exception e) {
                    return "ERR:" + e.toString();
                }
            }
            protected void onPostExecute(String s) {
                loadPosts(feedOnlyMine);
            }
        }.execute(new Void[0]);
    }

    public void onOpenVideo(String uri) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            startActivity(i);
        } catch (Exception e) {
            toast("Could not open video");
        }
    }

    public void onOpenLocation(String locationText) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:" + locationText + "?q=" + locationText));
            startActivity(i);
        } catch (Exception e) {
            toast("Could not open map");
        }
    }

    protected void showSearchDialog() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), 0);
        final EditText q = new EditText(this);
        q.setHint("Search registered users");
        q.setSingleLine(true);
        l.addView(q, new LinearLayout.LayoutParams(-1, Ui.dp(this, 54)));
        final LinearLayout results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        ScrollView sv = new ScrollView(this);
        sv.addView(results);
        l.addView(sv, new LinearLayout.LayoutParams(-1, Ui.dp(this, 310)));
        final AlertDialog dlg = new AlertDialog.Builder(this).setTitle("Find friends").setView(l).setPositiveButton("Search", null).setNegativeButton("Close", null).show();
        dlg.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                searchUsers(q.getText().toString(), results);
            }
        });
    }

    protected void searchUsers(final String q, final LinearLayout results) {
        results.removeAllViews();
        results.addView(Ui.text(this, "Searching...", 15, AppConfig.MUTED, Typeface.NORMAL), new LinearLayout.LayoutParams(-1, Ui.dp(this, 80)));
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void[] v) {
                try {
                    return FirebaseHelper.getFirebase("users", session.idToken);
                } catch (Exception e) {
                    return "ERR:" + e.toString();
                }
            }
            protected void onPostExecute(String s) {
                try {
                    if (s == null || s.equals("null") || s.startsWith("ERR:")) {
                        UserAdapter.render(BaseSocialActivity.this, results, null, q, session.uid, BaseSocialActivity.this);
                        return;
                    }
                    UserAdapter.render(BaseSocialActivity.this, results, new JSONObject(s), q, session.uid, BaseSocialActivity.this);
                } catch (Exception e) {
                    toast("Search error: " + e.toString());
                }
            }
        }.execute(new Void[0]);
    }

    public void onAddFriend(final String targetUid, final String targetName) {
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void[] v) {
                try {
                    return PostService.addFriend(session, targetUid, targetName);
                } catch (Exception e) {
                    return "ERR:" + e.toString();
                }
            }
            protected void onPostExecute(String s) {
                toast(s != null && s.startsWith("ERR:") ? s : targetName + " added");
            }
        }.execute(new Void[0]);
    }

    protected void inviteFriends() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, "Join me on ZedBook");
        i.putExtra(Intent.EXTRA_TEXT, "Join me on ZedBook so we can share statuses, pictures, videos, comments and location updates.");
        startActivity(Intent.createChooser(i, "Invite friends"));
    }

    protected void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }
}
