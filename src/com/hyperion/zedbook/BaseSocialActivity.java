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
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.text.InputType;
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
    protected Button publicButton;
    protected Button friendsButton;
    protected Button onlyMeButton;

    protected String pendingImage64 = "";
    protected String pendingVideoUri = "";
    protected String pendingLocation = "";
    protected String pendingVisibility = AppConfig.VISIBILITY_PUBLIC;
    protected String editingPostId = null;
    protected boolean feedOnlyMine = false;
    protected String feedProfileUid = "";
    protected JSONObject currentFriends = new JSONObject();

    private float pullStartY = 0;
    private boolean pullArmed = false;

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

    protected boolean refreshSessionBlocking() {
        if (session == null) {
            session = new SessionManager(this);
        } else {
            session.load();
        }
        if (!session.canRefreshToken()) {
            return false;
        }
        try {
            String r = FirebaseHelper.refreshIdToken(session.refreshToken);
            JSONObject o = new JSONObject(r);
            String token = o.optString("id_token", "");
            String refresh = o.optString("refresh_token", session.refreshToken);
            String uid = o.optString("user_id", session.uid);
            if (token.length() > 0) {
                session.saveFreshToken(uid, token, refresh);
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    protected boolean refreshAfterAuthFailure(String errorText) {
        if (!FirebaseHelper.isAuthFailure(errorText)) {
            return false;
        }
        return refreshSessionBlocking();
    }

    protected boolean isAuthRequiredResult(String s) {
        return s != null && s.startsWith("AUTH_REQUIRED");
    }

    protected void handleAuthRequired() {
        if (session != null) {
            session.clear();
        }
        Toast.makeText(this, "Please log in again to refresh your secure session.", Toast.LENGTH_LONG).show();
        startActivity(new Intent(this, Loginsignup.class));
        finish();
    }

    protected String friendlyError(String s) {
        if (s == null) {
            return "Network error";
        }
        if (FirebaseHelper.isAuthFailure(s)) {
            return "Could not refresh your secure session. Please log in again.";
        }
        if (s.startsWith("ERR:")) {
            return s.substring(4);
        }
        return s;
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
                boolean open = !BaseSocialActivity.this.getClass().equals(cls);
                if (!open && cls.equals(Profile.class) && feedProfileUid != null && feedProfileUid.length() > 0) {
                    open = true;
                }
                if (open) {
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
        statusBox.setRawInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        statusBox.setImeOptions(EditorInfo.IME_ACTION_SEND);
        statusBox.setTextColor(AppConfig.TEXT);
        statusBox.setHintTextColor(android.graphics.Color.rgb(120, 125, 130));
        statusBox.setBackgroundResource(R.drawable.bg_input_light);
        statusBox.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);
        statusBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean enterReleased = false;
                if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                    enterReleased = true;
                }
                if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE || enterReleased) {
                    saveStatus();
                    return true;
                }
                return false;
            }
        });
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

        buildVisibilityChooser();

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

    private void buildVisibilityChooser() {
        LinearLayout row = new LinearLayout(this);
        row.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 8), Ui.dp(this, 6));
        row.setBackgroundResource(R.drawable.bg_card);
        TextView label = Ui.text(this, "Visibility", 12, AppConfig.MUTED, Typeface.BOLD);
        publicButton = Ui.button(this, "Public", R.drawable.bg_blue_button);
        friendsButton = Ui.button(this, "Friends", R.drawable.bg_grey_button);
        onlyMeButton = Ui.button(this, "Only me", R.drawable.bg_grey_button);
        row.addView(label, new LinearLayout.LayoutParams(Ui.dp(this, 76), Ui.dp(this, 40)));
        row.addView(publicButton, new LinearLayout.LayoutParams(0, Ui.dp(this, 40), 1));
        row.addView(friendsButton, new LinearLayout.LayoutParams(0, Ui.dp(this, 40), 1));
        row.addView(onlyMeButton, new LinearLayout.LayoutParams(0, Ui.dp(this, 40), 1));
        root.addView(row, new LinearLayout.LayoutParams(-1, Ui.dp(this, 48)));
        publicButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                selectVisibility(AppConfig.VISIBILITY_PUBLIC);
            }
        });
        friendsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                selectVisibility(AppConfig.VISIBILITY_FRIENDS);
            }
        });
        onlyMeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                selectVisibility(AppConfig.VISIBILITY_ONLY_ME);
            }
        });
        selectVisibility(AppConfig.VISIBILITY_PUBLIC);
    }

    protected void selectVisibility(String visibility) {
        if (visibility == null || visibility.length() == 0) {
            visibility = AppConfig.VISIBILITY_PUBLIC;
        }
        if (!AppConfig.VISIBILITY_FRIENDS.equals(visibility) && !AppConfig.VISIBILITY_ONLY_ME.equals(visibility)) {
            visibility = AppConfig.VISIBILITY_PUBLIC;
        }
        pendingVisibility = visibility;
        if (publicButton != null) {
            publicButton.setBackgroundResource(AppConfig.VISIBILITY_PUBLIC.equals(visibility) ? R.drawable.bg_blue_button : R.drawable.bg_grey_button);
        }
        if (friendsButton != null) {
            friendsButton.setBackgroundResource(AppConfig.VISIBILITY_FRIENDS.equals(visibility) ? R.drawable.bg_blue_button : R.drawable.bg_grey_button);
        }
        if (onlyMeButton != null) {
            onlyMeButton.setBackgroundResource(AppConfig.VISIBILITY_ONLY_ME.equals(visibility) ? R.drawable.bg_blue_button : R.drawable.bg_grey_button);
        }
    }

    protected void buildFeedHolder() {
        ScrollView sv = new ScrollView(this);
        installPullToRefresh(sv);
        feedList = new LinearLayout(this);
        feedList.setOrientation(LinearLayout.VERTICAL);
        sv.addView(feedList);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    protected void installPullToRefresh(final ScrollView sv) {
        sv.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    pullStartY = event.getY();
                    pullArmed = sv.getScrollY() == 0;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (pullArmed && sv.getScrollY() == 0 && event.getY() - pullStartY > Ui.dp(BaseSocialActivity.this, 70)) {
                        toast("Refreshing...");
                        onPullRefresh();
                    }
                    pullArmed = false;
                } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                    pullArmed = false;
                }
                return false;
            }
        });
    }

    protected void onPullRefresh() {
        loadPosts(feedOnlyMine);
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
                    return loadPostsRaw();
                } catch (Exception e) {
                    if (refreshAfterAuthFailure(e.toString())) {
                        try {
                            return loadPostsRaw();
                        } catch (Exception second) {
                            return FirebaseHelper.isAuthFailure(second.toString()) ? "AUTH_REQUIRED" : "ERR:" + second.toString();
                        }
                    }
                    return FirebaseHelper.isAuthFailure(e.toString()) ? "AUTH_REQUIRED" : "ERR:" + e.toString();
                }
            }
            protected void onPostExecute(String s) {
                renderPostString(s, onlyMine);
            }
        }.execute(new Void[0]);
    }

    protected String loadPostsRaw() throws Exception {
        String posts = FirebaseHelper.getFirebase("posts", session.idToken);
        String friends = FirebaseHelper.getFirebase("friends/" + session.uid, session.idToken);
        JSONObject wrap = new JSONObject();
        wrap.put("posts", posts == null ? "null" : posts);
        wrap.put("friends", friends == null ? "null" : friends);
        return wrap.toString();
    }

    protected void renderPostString(String s, boolean onlyMine) {
        try {
            if (isAuthRequiredResult(s)) {
                PostAdapter.render(this, feedList, null, session.uid, onlyMine, currentFriends, feedProfileUid, this);
                handleAuthRequired();
                return;
            }
            if (s == null || s.startsWith("ERR:")) {
                PostAdapter.render(this, feedList, null, session.uid, onlyMine, currentFriends, feedProfileUid, this);
                if (s != null && s.startsWith("ERR:") && !FirebaseHelper.isAuthFailure(s)) {
                    Toast.makeText(this, "Feed could not refresh. Pull down to try again.", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            JSONObject wrap = new JSONObject(s);
            String friendsRaw = wrap.optString("friends", "null");
            currentFriends = new JSONObject();
            if (!FirebaseHelper.isEmptyFirebaseValue(friendsRaw) && FirebaseHelper.looksLikeJsonObject(friendsRaw)) {
                currentFriends = new JSONObject(friendsRaw);
            }
            String postsRaw = wrap.optString("posts", "null");
            if (FirebaseHelper.isEmptyFirebaseValue(postsRaw)) {
                PostAdapter.render(this, feedList, null, session.uid, onlyMine, currentFriends, feedProfileUid, this);
                return;
            }
            if (!FirebaseHelper.looksLikeJsonObject(postsRaw)) {
                PostAdapter.render(this, feedList, null, session.uid, onlyMine, currentFriends, feedProfileUid, this);
                toast("Feed could not load. Check the Firebase Realtime Database URL and rules.");
                return;
            }
            JSONObject all = new JSONObject(postsRaw);
            PostAdapter.render(this, feedList, all, session.uid, onlyMine, currentFriends, feedProfileUid, this);
        } catch (Exception e) {
            PostAdapter.render(this, feedList, null, session.uid, onlyMine, currentFriends, feedProfileUid, this);
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
        final String vis = pendingVisibility;
        if (activePostButton != null) {
            activePostButton.setEnabled(false);
            activePostButton.setText(postId != null && postId.length() > 0 ? "Saving..." : "Posting...");
        }
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void[] v) {
                try {
                    return saveStatusRaw(postId, text, vis);
                } catch (Exception e) {
                    if (refreshAfterAuthFailure(e.toString())) {
                        try {
                            return saveStatusRaw(postId, text, vis);
                        } catch (Exception second) {
                            return FirebaseHelper.isAuthFailure(second.toString()) ? "AUTH_REQUIRED" : "ERR:" + second.toString();
                        }
                    }
                    return FirebaseHelper.isAuthFailure(e.toString()) ? "AUTH_REQUIRED" : "ERR:" + e.toString();
                }
            }
            protected void onPostExecute(String s) {
                if (activePostButton != null) {
                    activePostButton.setEnabled(true);
                }
                if (isAuthRequiredResult(s)) {
                    if (activePostButton != null) {
                        activePostButton.setText(postId != null && postId.length() > 0 ? "Save" : "Post");
                    }
                    handleAuthRequired();
                    return;
                }
                if (s != null && s.startsWith("ERR:")) {
                    if (activePostButton != null) {
                        activePostButton.setText(postId != null && postId.length() > 0 ? "Save" : "Post");
                    }
                    toast(friendlyError(s));
                    return;
                }
                resetComposer();
                loadPosts(feedOnlyMine);
            }
        }.execute(new Void[0]);
    }

    protected String saveStatusRaw(String postId, String text, String vis) throws Exception {
        if (postId != null && postId.length() > 0) {
            return PostService.updatePost(session, postId, text, pendingImage64, pendingVideoUri, pendingLocation, vis);
        }
        return PostService.createPost(session, text, pendingImage64, pendingVideoUri, pendingLocation, vis);
    }

    protected void resetComposer() {
        editingPostId = null;
        pendingImage64 = "";
        pendingVideoUri = "";
        pendingLocation = "";
        selectVisibility(AppConfig.VISIBILITY_PUBLIC);
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
        selectVisibility(post.optString("visibility", AppConfig.VISIBILITY_PUBLIC));
        activePostButton.setText("Save");
        activePostButton.setBackgroundResource(R.drawable.bg_green_button);
        toast("Edit the status, then press Save.");
    }

    public void onDeletePost(final String postId, JSONObject post) {
        new AlertDialog.Builder(this).setTitle("Delete status?").setMessage("This will remove the status and its comments permanently.").setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int w) {
                deletePostNow(postId);
            }
        }).setNegativeButton("Cancel", null).show();
    }

    private void deletePostNow(final String postId) {
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void[] v) {
                try {
                    return PostService.deletePost(session, postId);
                } catch (Exception e) {
                    if (refreshAfterAuthFailure(e.toString())) {
                        try {
                            return PostService.deletePost(session, postId);
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
                if (s != null && s.startsWith("ERR:")) {
                    toast(friendlyError(s));
                    return;
                }
                resetComposer();
                loadPosts(feedOnlyMine);
            }
        }.execute(new Void[0]);
    }

    public void onReactPost(final String postId, final String type) {
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void[] v) {
                try {
                    return PostService.react(session, postId, type);
                } catch (Exception e) {
                    if (refreshAfterAuthFailure(e.toString())) {
                        try {
                            return PostService.react(session, postId, type);
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
                    if (refreshAfterAuthFailure(e.toString())) {
                        try {
                            return PostService.comment(session, postId, text.trim(), post.optString("uid", ""), post.optString("text", ""));
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
                    if (refreshAfterAuthFailure(e.toString())) {
                        try {
                            return FirebaseHelper.getFirebase("users", session.idToken);
                        } catch (Exception second) {
                            return FirebaseHelper.isAuthFailure(second.toString()) ? "AUTH_REQUIRED" : "ERR:" + second.toString();
                        }
                    }
                    return FirebaseHelper.isAuthFailure(e.toString()) ? "AUTH_REQUIRED" : "ERR:" + e.toString();
                }
            }
            protected void onPostExecute(String s) {
                try {
                    if (isAuthRequiredResult(s)) {
                        handleAuthRequired();
                        return;
                    }
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
                    if (refreshAfterAuthFailure(e.toString())) {
                        try {
                            return PostService.addFriend(session, targetUid, targetName);
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
                toast(s != null && s.startsWith("ERR:") ? friendlyError(s) : targetName + " added");
                loadPosts(feedOnlyMine);
            }
        }.execute(new Void[0]);
    }

    public void onViewProfile(String targetUid, String targetName, String targetEmail, String targetProfile) {
        Intent i = new Intent(this, Profile.class);
        i.putExtra("profileUid", targetUid);
        i.putExtra("profileName", targetName);
        i.putExtra("profileEmail", targetEmail);
        i.putExtra("profileImage", targetProfile);
        startActivity(i);
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
