package com.hyperion.zedbook;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

public class MainActivity extends Activity {
    /*
       FIREBASE SETUP:
       1) Create Firebase project.
       2) Enable Authentication / Email and Password.
       3) Enable Realtime Database in test mode while developing.
       4) Put your Web API key and Realtime Database URL below.
       This uses Firebase REST APIs, so it compiles in AIDE without Firebase SDK jars.
    */
    private static final String FIREBASE_WEB_API_KEY = "PUT_YOUR_FIREBASE_WEB_API_KEY_HERE";
    private static final String FIREBASE_DATABASE_URL = "https://YOUR_PROJECT_ID-default-rtdb.firebaseio.com";

    private static final int BLUE = Color.rgb(30, 136, 245);
    private static final int GREEN = Color.rgb(44, 196, 38);
    private static final int DARK = Color.rgb(22, 24, 26);
    private static final int GREY = Color.rgb(240, 242, 245);
    private static final int MID_GREY = Color.rgb(220, 224, 228);
    private static final int TEXT = Color.rgb(28, 30, 33);

    private static final int PICK_PROFILE = 40;
    private static final int PICK_STATUS_IMAGE = 41;
    private static final int PICK_STATUS_VIDEO = 42;

    private static final int TAB_HOME = 0;
    private static final int TAB_PROFILE = 1;
    private static final int TAB_NOTIFICATIONS = 2;

    private FrameLayout root;
    private SharedPreferences prefs;
    private Handler handler = new Handler();

    private String idToken = "";
    private String uid = "";
    private String email = "";
    private String displayName = "";
    private String profileImage64 = "";

    private int currentTab = TAB_HOME;
    private LinearLayout feedList;
    private LinearLayout notificationList;
    private EditText statusBox;
    private Button activePostButton;

    private String pendingImage64 = "";
    private String pendingVideoUri = "";
    private String pendingLocation = "";
    private String editingPostId = null;

    public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("zedbook", MODE_PRIVATE);
        idToken = prefs.getString("token", "");
        uid = prefs.getString("uid", "");
        email = prefs.getString("email", "");
        displayName = prefs.getString("name", "");
        profileImage64 = prefs.getString("profile", "");
        root = new FrameLayout(this);
        setContentView(root);
        showSplash();
    }

    private int dp(int v) {
        return (int)(v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView tv(String text, int sp, int color, int style) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.DEFAULT, style);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    private Button btn(String text, int color) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setTextSize(12);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackgroundColor(color);
        b.setAllCaps(false);
        return b;
    }

    private EditText edit(String hint, boolean password) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setTextColor(Color.WHITE);
        e.setHintTextColor(Color.rgb(150, 150, 150));
        e.setTextSize(16);
        e.setPadding(dp(14), 0, dp(14), 0);
        e.setBackgroundColor(Color.rgb(64, 66, 69));
        if (password) {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        return e;
    }

    private void clear() {
        root.removeAllViews();
    }

    private void addGap(LinearLayout l, int h) {
        TextView g = new TextView(this);
        l.addView(g, new LinearLayout.LayoutParams(1, dp(h)));
    }

    private void showSplash() {
        clear();
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER);
        l.setBackgroundColor(DARK);
        TextView logo = tv("ZedBook", 40, BLUE, Typeface.BOLD);
        ProgressBar p = new ProgressBar(this);
        l.addView(logo, new LinearLayout.LayoutParams(-2, dp(80)));
        l.addView(p, new LinearLayout.LayoutParams(dp(48), dp(48)));
        root.addView(l, new FrameLayout.LayoutParams(-1, -1));
        handler.postDelayed(new Runnable() {
            public void run() {
                if (uid.length() > 0 && idToken.length() > 0) {
                    loadMyProfileThenHome();
                } else {
                    showLogin();
                }
            }
        }, 1500);
    }

    private void showLogin() {
        clear();
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER);
        l.setPadding(dp(28), 0, dp(28), 0);
        l.setBackgroundColor(DARK);
        TextView logo = tv("ZedBook", 34, BLUE, Typeface.NORMAL);
        final EditText mail = edit("Email", false);
        final EditText pass = edit("Password", true);
        Button login = btn("LOG IN", BLUE);
        Button signup = btn("CREATE NEW ACCOUNT", GREEN);
        l.addView(logo, new LinearLayout.LayoutParams(-1, dp(85)));
        l.addView(mail, new LinearLayout.LayoutParams(-1, dp(54)));
        addGap(l, 8);
        l.addView(pass, new LinearLayout.LayoutParams(-1, dp(54)));
        addGap(l, 16);
        l.addView(login, new LinearLayout.LayoutParams(-1, dp(54)));
        addGap(l, 8);
        l.addView(signup, new LinearLayout.LayoutParams(-1, dp(54)));
        root.addView(l, new FrameLayout.LayoutParams(-1, -1));
        login.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doAuth(mail.getText().toString(), pass.getText().toString(), false);
            }
        });
        signup.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showSignup();
            }
        });
    }

    private void showSignup() {
        clear();
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER);
        l.setPadding(dp(28), 0, dp(28), 0);
        l.setBackgroundColor(DARK);
        TextView logo = tv("Create ZedBook", 30, BLUE, Typeface.BOLD);
        final EditText name = edit("Display name", false);
        final EditText mail = edit("Email", false);
        final EditText pass = edit("Password", true);
        Button create = btn("SIGN UP", GREEN);
        Button back = btn("BACK TO LOGIN", BLUE);
        l.addView(logo, new LinearLayout.LayoutParams(-1, dp(80)));
        l.addView(name, new LinearLayout.LayoutParams(-1, dp(54)));
        addGap(l, 8);
        l.addView(mail, new LinearLayout.LayoutParams(-1, dp(54)));
        addGap(l, 8);
        l.addView(pass, new LinearLayout.LayoutParams(-1, dp(54)));
        addGap(l, 16);
        l.addView(create, new LinearLayout.LayoutParams(-1, dp(54)));
        addGap(l, 8);
        l.addView(back, new LinearLayout.LayoutParams(-1, dp(54)));
        root.addView(l, new FrameLayout.LayoutParams(-1, -1));
        create.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                displayName = name.getText().toString().trim();
                doAuth(mail.getText().toString(), pass.getText().toString(), true);
            }
        });
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showLogin();
            }
        });
    }

    private void doAuth(final String m, final String p, final boolean signup) {
        if (FIREBASE_WEB_API_KEY.indexOf("PUT_YOUR") >= 0) {
            toast("Add your Firebase Web API key in MainActivity.java first.");
            return;
        }
        if (m.length() < 5 || p.length() < 6) {
            toast("Enter a valid email and 6+ character password.");
            return;
        }
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void[] v) {
                try {
                    String endpoint = signup ? "signUp" : "signInWithPassword";
                    String url = "https://identitytoolkit.googleapis.com/v1/accounts:" + endpoint + "?key=" + FIREBASE_WEB_API_KEY;
                    JSONObject o = new JSONObject();
                    o.put("email", m);
                    o.put("password", p);
                    o.put("returnSecureToken", true);
                    return postJson(url, o.toString());
                } catch (Exception e) {
                    return "ERR:" + e.toString();
                }
            }
            protected void onPostExecute(String s) {
                try {
                    if (s.startsWith("ERR:")) {
                        toast(s);
                        return;
                    }
                    JSONObject r = new JSONObject(s);
                    if (r.has("error")) {
                        toast(r.getJSONObject("error").getString("message"));
                        return;
                    }
                    uid = r.getString("localId");
                    idToken = r.getString("idToken");
                    email = m;
                    if (displayName.length() == 0) {
                        int at = email.indexOf("@");
                        displayName = email.substring(0, at > 0 ? at : email.length());
                    }
                    prefs.edit().putString("uid", uid).putString("token", idToken).putString("email", email).putString("name", displayName).commit();
                    currentTab = TAB_HOME;
                    if (signup) {
                        saveMyProfile();
                    } else {
                        loadMyProfileThenHome();
                    }
                } catch (Exception e) {
                    toast("Auth parse error: " + e.toString());
                }
            }
        }.execute(new Void[0]);
    }

    private void showMainPage(int tab) {
        currentTab = tab;
        editingPostId = null;
        pendingImage64 = "";
        pendingVideoUri = "";
        pendingLocation = "";
        clear();

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setBackgroundColor(GREY);
        root.addView(main, new FrameLayout.LayoutParams(-1, -1));

        buildTopBar(main);
        buildTabs(main);

        if (tab == TAB_HOME) {
            buildHomeTab(main);
        } else if (tab == TAB_PROFILE) {
            buildProfileTab(main);
        } else {
            buildNotificationsTab(main);
        }
    }

    private void buildTopBar(LinearLayout main) {
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(10), 0, dp(6), 0);
        top.setBackgroundColor(Color.WHITE);
        TextView title = tv("ZedBook", 24, BLUE, Typeface.BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(62), 1));

        String[] names = {"Search", "Invite", "Logout"};
        for (int i = 0; i < names.length; i++) {
            int c = i == 2 ? Color.rgb(90, 90, 90) : Color.rgb(230, 235, 240);
            Button b = btn(names[i], c);
            b.setTextColor(i == 2 ? Color.WHITE : Color.rgb(90, 100, 108));
            final int ix = i;
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    topAction(ix);
                }
            });
            top.addView(b, new LinearLayout.LayoutParams(dp(72), dp(42)));
        }
        main.addView(top, new LinearLayout.LayoutParams(-1, dp(62)));
    }

    private void buildTabs(LinearLayout main) {
        LinearLayout tabs = new LinearLayout(this);
        tabs.setBackgroundColor(Color.WHITE);
        String[] names = {"Home", "Profile", "Notifications"};
        for (int i = 0; i < names.length; i++) {
            Button b = btn(names[i], i == currentTab ? BLUE : Color.WHITE);
            b.setTextColor(i == currentTab ? Color.WHITE : Color.rgb(90, 100, 108));
            final int tab = i;
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    showMainPage(tab);
                }
            });
            tabs.addView(b, new LinearLayout.LayoutParams(0, dp(44), 1));
        }
        main.addView(tabs, new LinearLayout.LayoutParams(-1, dp(44)));
        TextView line = new TextView(this);
        line.setBackgroundColor(MID_GREY);
        main.addView(line, new LinearLayout.LayoutParams(-1, 1));
    }

    private void buildHomeTab(LinearLayout main) {
        TextView title = tv("News Feed", 17, TEXT, Typeface.BOLD);
        title.setPadding(dp(10), 0, 0, 0);
        title.setBackgroundColor(Color.WHITE);
        main.addView(title, new LinearLayout.LayoutParams(-1, dp(40)));

        buildComposer(main, "What's on your mind?  Use @name to tag friends.");
        buildFeedHolder(main);
        loadPosts(false);
    }

    private void buildProfileTab(LinearLayout main) {
        LinearLayout profile = new LinearLayout(this);
        profile.setOrientation(LinearLayout.HORIZONTAL);
        profile.setGravity(Gravity.CENTER_VERTICAL);
        profile.setPadding(dp(10), dp(8), dp(10), dp(8));
        profile.setBackgroundColor(Color.WHITE);
        profile.addView(avatarView(72, profileImage64), new LinearLayout.LayoutParams(dp(72), dp(72)));

        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        TextView n = tv(displayName.length() == 0 ? "ZedBook User" : displayName, 19, TEXT, Typeface.BOLD);
        TextView em = tv(email, 13, Color.GRAY, Typeface.NORMAL);
        TextView helper = tv("Your profile and your own statuses", 12, Color.GRAY, Typeface.NORMAL);
        names.addView(n, new LinearLayout.LayoutParams(-1, dp(28)));
        names.addView(em, new LinearLayout.LayoutParams(-1, dp(24)));
        names.addView(helper, new LinearLayout.LayoutParams(-1, dp(24)));
        profile.addView(names, new LinearLayout.LayoutParams(0, dp(78), 1));

        Button edit = btn("Edit", BLUE);
        edit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showProfileDialog();
            }
        });
        profile.addView(edit, new LinearLayout.LayoutParams(dp(72), dp(44)));
        main.addView(profile, new LinearLayout.LayoutParams(-1, dp(96)));

        buildComposer(main, "Post something to your profile...");
        TextView mine = tv("My Statuses", 16, TEXT, Typeface.BOLD);
        mine.setPadding(dp(10), 0, 0, 0);
        mine.setBackgroundColor(Color.WHITE);
        main.addView(mine, new LinearLayout.LayoutParams(-1, dp(38)));
        buildFeedHolder(main);
        loadPosts(true);
    }

    private void buildNotificationsTab(LinearLayout main) {
        LinearLayout top = new LinearLayout(this);
        top.setPadding(dp(10), dp(8), dp(10), dp(8));
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setBackgroundColor(Color.WHITE);
        TextView title = tv("Notifications", 19, TEXT, Typeface.BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(50), 1));
        Button refresh = btn("Refresh", BLUE);
        Button mark = btn("Mark read", Color.rgb(100, 100, 100));
        top.addView(refresh, new LinearLayout.LayoutParams(dp(92), dp(44)));
        top.addView(mark, new LinearLayout.LayoutParams(dp(104), dp(44)));
        main.addView(top, new LinearLayout.LayoutParams(-1, dp(66)));

        TextView help = tv("Tag friends in statuses with @DisplayName or @emailname. Tag alerts show here.", 13, Color.GRAY, Typeface.NORMAL);
        help.setPadding(dp(10), 0, dp(10), 0);
        help.setBackgroundColor(Color.WHITE);
        main.addView(help, new LinearLayout.LayoutParams(-1, dp(44)));

        ScrollView sv = new ScrollView(this);
        notificationList = new LinearLayout(this);
        notificationList.setOrientation(LinearLayout.VERTICAL);
        sv.addView(notificationList);
        main.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));

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
        loadNotifications();
    }

    private void buildComposer(LinearLayout main, String hint) {
        LinearLayout composer = new LinearLayout(this);
        composer.setPadding(dp(8), dp(6), dp(8), dp(6));
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setBackgroundColor(Color.WHITE);
        ImageView avatar = avatarView(42, profileImage64);
        composer.addView(avatar, new LinearLayout.LayoutParams(dp(42), dp(42)));

        statusBox = new EditText(this);
        statusBox.setHint(hint);
        statusBox.setTextSize(15);
        statusBox.setSingleLine(false);
        statusBox.setMinLines(1);
        statusBox.setMaxLines(3);
        statusBox.setTextColor(TEXT);
        statusBox.setHintTextColor(Color.rgb(120, 125, 130));
        statusBox.setBackgroundColor(GREY);
        statusBox.setPadding(dp(12), 0, dp(12), 0);
        composer.addView(statusBox, new LinearLayout.LayoutParams(0, dp(50), 1));

        activePostButton = btn("Post", BLUE);
        composer.addView(activePostButton, new LinearLayout.LayoutParams(dp(72), dp(50)));
        main.addView(composer, new LinearLayout.LayoutParams(-1, dp(64)));

        LinearLayout attach = new LinearLayout(this);
        attach.setPadding(dp(8), 0, dp(8), dp(6));
        attach.setBackgroundColor(Color.WHITE);
        Button img = btn("Picture", Color.rgb(120, 150, 180));
        Button vid = btn("Video", Color.rgb(120, 150, 180));
        Button loc = btn("Location", Color.rgb(120, 150, 180));
        Button clear = btn("Clear", Color.rgb(100, 100, 100));
        attach.addView(img, new LinearLayout.LayoutParams(0, dp(40), 1));
        attach.addView(vid, new LinearLayout.LayoutParams(0, dp(40), 1));
        attach.addView(loc, new LinearLayout.LayoutParams(0, dp(40), 1));
        attach.addView(clear, new LinearLayout.LayoutParams(0, dp(40), 1));
        main.addView(attach, new LinearLayout.LayoutParams(-1, dp(48)));

        activePostButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                createPost();
            }
        });
        img.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pick(PICK_STATUS_IMAGE, "image/*");
            }
        });
        vid.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pick(PICK_STATUS_VIDEO, "video/*");
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

    private void buildFeedHolder(LinearLayout main) {
        ScrollView sv = new ScrollView(this);
        feedList = new LinearLayout(this);
        feedList.setOrientation(LinearLayout.VERTICAL);
        sv.addView(feedList);
        main.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void topAction(int i) {
        if (i == 0) {
            showSearchDialog();
        }
        if (i == 1) {
            inviteFriends();
        }
        if (i == 2) {
            prefs.edit().clear().commit();
            uid = "";
            idToken = "";
            email = "";
            displayName = "";
            profileImage64 = "";
            showLogin();
        }
    }

    private void resetComposer() {
        editingPostId = null;
        pendingImage64 = "";
        pendingVideoUri = "";
        pendingLocation = "";
        if (statusBox != null) {
            statusBox.setText("");
        }
        if (activePostButton != null) {
            activePostButton.setText("Post");
            activePostButton.setBackgroundColor(BLUE);
        }
    }

    private void showProfileDialog() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(16), dp(8), dp(16), 0);
        final ImageView av = avatarView(100, profileImage64);
        l.addView(av, new LinearLayout.LayoutParams(dp(100), dp(100)));
        final EditText name = new EditText(this);
        name.setHint("Display name");
        name.setSingleLine(true);
        name.setText(displayName);
        l.addView(name, new LinearLayout.LayoutParams(-1, dp(54)));
        Button pic = btn("Change profile picture", BLUE);
        l.addView(pic, new LinearLayout.LayoutParams(-1, dp(48)));
        pic.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pick(PICK_PROFILE, "image/*");
            }
        });
        new AlertDialog.Builder(this).setTitle("Profile setup").setView(l).setPositiveButton("Save", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int w) {
                displayName = name.getText().toString().trim();
                if (displayName.length() == 0) {
                    displayName = "ZedBook User";
                }
                saveMyProfile();
            }
        }).setNegativeButton("Cancel", null).show();
    }

    private void showSearchDialog() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(16), dp(8), dp(16), 0);
        final EditText q = new EditText(this);
        q.setHint("Search registered users");
        q.setSingleLine(true);
        l.addView(q, new LinearLayout.LayoutParams(-1, dp(54)));
        final LinearLayout results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        ScrollView sv = new ScrollView(this);
        sv.addView(results);
        l.addView(sv, new LinearLayout.LayoutParams(-1, dp(300)));
        final AlertDialog dlg = new AlertDialog.Builder(this).setTitle("Find friends").setView(l).setPositiveButton("Search", null).setNegativeButton("Close", null).show();
        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                searchUsers(q.getText().toString(), results);
            }
        });
    }

    private void inviteFriends() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, "Join me on ZedBook. Create an account and search for " + displayName + ".");
        startActivity(Intent.createChooser(i, "Invite friends"));
    }

    private void pick(int code, String type) {
        Intent i = new Intent(Intent.ACTION_PICK);
        i.setType(type);
        try {
            startActivityForResult(i, code);
        } catch (Exception e) {
            toast("No picker found");
        }
    }

    public void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null) {
            return;
        }
        Uri u = data.getData();
        if (u == null) {
            return;
        }
        if (req == PICK_PROFILE) {
            profileImage64 = imageToBase64(u);
            prefs.edit().putString("profile", profileImage64).commit();
            saveMyProfile();
            toast("Profile picture saved");
        }
        if (req == PICK_STATUS_IMAGE) {
            pendingImage64 = imageToBase64(u);
            toast("Picture attached");
        }
        if (req == PICK_STATUS_VIDEO) {
            pendingVideoUri = u.toString();
            toast("Video attached. For cloud video upload, add Firebase Storage rules.");
        }
    }

    private String imageToBase64(Uri uri) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            Bitmap bm = BitmapFactory.decodeStream(in);
            if (bm == null) {
                return "";
            }
            int max = 700;
            int w = bm.getWidth();
            int h = bm.getHeight();
            if (w > max || h > max) {
                float s = Math.min((float)max / (float)w, (float)max / (float)h);
                bm = Bitmap.createScaledBitmap(bm, (int)(w * s), (int)(h * s), true);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.JPEG, 72, out);
            return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            toast("Image failed: " + e.toString());
            return "";
        }
    }

    private ImageView avatarView(int size, String b64) {
        ImageView v = new ImageView(this);
        v.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (b64 != null && b64.length() > 10) {
            try {
                byte[] d = Base64.decode(b64, Base64.DEFAULT);
                v.setImageBitmap(BitmapFactory.decodeByteArray(d, 0, d.length));
            } catch (Exception e) {
                v.setBackgroundColor(BLUE);
            }
        } else {
            v.setBackgroundColor(BLUE);
        }
        return v;
    }

    private void attachLocation() {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 500);
            return;
        }
        try {
            LocationManager lm = (LocationManager)getSystemService(LOCATION_SERVICE);
            Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null) {
                loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (loc == null) {
                toast("Turn location on, then try again.");
                return;
            }
            pendingLocation = loc.getLatitude() + "," + loc.getLongitude();
            toast("Location attached");
        } catch (Exception e) {
            toast("Location failed: " + e.toString());
        }
    }

    private void createPost() {
        final String text = statusBox == null ? "" : statusBox.getText().toString();
        final String editId = editingPostId;
        if (text.trim().length() == 0 && pendingImage64.length() == 0 && pendingVideoUri.length() == 0 && pendingLocation.length() == 0) {
            toast("Write a status or attach media.");
            return;
        }
        if (activePostButton != null) {
            activePostButton.setText(editId == null ? "Posting..." : "Saving...");
        }
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void[] v) {
                try {
                    JSONObject p = new JSONObject();
                    p.put("uid", uid);
                    p.put("name", displayName);
                    p.put("profile", profileImage64);
                    p.put("text", text);
                    p.put("image", pendingImage64);
                    p.put("video", pendingVideoUri);
                    p.put("location", pendingLocation);
                    p.put("time", System.currentTimeMillis());
                    p.put("edited", editId == null ? false : true);

                    String postId = editId;
                    String writeResult;
                    if (editId == null) {
                        String url = FIREBASE_DATABASE_URL + "/posts.json?auth=" + enc(idToken);
                        writeResult = postJson(url, p.toString());
                        JSONObject created = new JSONObject(writeResult);
                        postId = created.optString("name", "");
                    } else {
                        String url = FIREBASE_DATABASE_URL + "/posts/" + editId + ".json?auth=" + enc(idToken);
                        writeResult = putJson(url, p.toString());
                    }
                    if (postId != null && postId.length() > 0) {
                        createTagNotifications(postId, text);
                    }
                    return writeResult;
                } catch (Exception e) {
                    return "ERR:" + e.toString();
                }
            }
            protected void onPostExecute(String s) {
                if (s != null && s.startsWith("ERR:")) {
                    toast(s);
                } else {
                    toast(editId == null ? "Status posted" : "Status updated");
                }
                resetComposer();
                if (currentTab == TAB_PROFILE) {
                    loadPosts(true);
                } else if (currentTab == TAB_HOME) {
                    loadPosts(false);
                }
            }
        }.execute(new Void[0]);
    }

    private void loadPosts(final boolean onlyMine) {
        if (feedList == null) {
            return;
        }
        feedList.removeAllViews();
        feedList.addView(tv("Loading statuses...", 15, Color.GRAY, Typeface.NORMAL), new LinearLayout.LayoutParams(-1, dp(56)));
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void[] v) {
                try {
                    return getUrl(FIREBASE_DATABASE_URL + "/posts.json?auth=" + enc(idToken));
                } catch (Exception e) {
                    return "ERR:" + e.toString();
                }
            }
            protected void onPostExecute(String s) {
                renderPosts(s, onlyMine);
            }
        }.execute(new Void[0]);
    }

    private void renderPosts(String s, boolean onlyMine) {
        feedList.removeAllViews();
        try {
            if (s == null || s.equals("null") || s.startsWith("ERR:")) {
                String msg = s != null && s.startsWith("ERR:") ? s : "No statuses yet.";
                feedList.addView(tv(msg, 15, Color.GRAY, Typeface.NORMAL), new LinearLayout.LayoutParams(-1, dp(80)));
                return;
            }
            final JSONObject all = new JSONObject(s);
            ArrayList<String> keys = new ArrayList<String>();
            Iterator<String> it = all.keys();
            while (it.hasNext()) {
                String k = it.next();
                JSONObject p = all.optJSONObject(k);
                if (p != null) {
                    if (!onlyMine || uid.equals(p.optString("uid", ""))) {
                        keys.add(k);
                    }
                }
            }
            Collections.sort(keys, new Comparator<String>() {
                public int compare(String a, String b) {
                    long ta = all.optJSONObject(a) == null ? 0 : all.optJSONObject(a).optLong("time", 0);
                    long tb = all.optJSONObject(b) == null ? 0 : all.optJSONObject(b).optLong("time", 0);
                    if (tb > ta) return 1;
                    if (tb < ta) return -1;
                    return 0;
                }
            });
            if (keys.size() == 0) {
                feedList.addView(tv(onlyMine ? "You have not posted a profile status yet." : "No statuses yet.", 15, Color.GRAY, Typeface.NORMAL), new LinearLayout.LayoutParams(-1, dp(80)));
                return;
            }
            for (int i = 0; i < keys.size(); i++) {
                String id = keys.get(i);
                addPostCard(id, all.getJSONObject(id));
            }
        } catch (Exception e) {
            feedList.addView(tv("Feed error: " + e.toString(), 14, Color.RED, Typeface.NORMAL));
        }
    }

    private void addPostCard(final String postId, final JSONObject p) throws Exception {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(10), dp(10), dp(10), dp(10));
        card.setBackgroundColor(Color.WHITE);

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(avatarView(44, p.optString("profile", "")), new LinearLayout.LayoutParams(dp(44), dp(44)));
        LinearLayout headText = new LinearLayout(this);
        headText.setOrientation(LinearLayout.VERTICAL);
        TextView name = tv(p.optString("name", "User"), 16, TEXT, Typeface.BOLD);
        TextView time = tv(friendlyTime(p.optLong("time", 0)) + (p.optBoolean("edited", false) ? "  Edited" : ""), 12, Color.GRAY, Typeface.NORMAL);
        headText.addView(name, new LinearLayout.LayoutParams(-1, dp(26)));
        headText.addView(time, new LinearLayout.LayoutParams(-1, dp(22)));
        head.addView(headText, new LinearLayout.LayoutParams(0, dp(50), 1));
        if (uid.equals(p.optString("uid", ""))) {
            Button edit = btn("Edit", BLUE);
            head.addView(edit, new LinearLayout.LayoutParams(dp(70), dp(42)));
            edit.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    editPost(postId, p);
                }
            });
        }
        card.addView(head);

        TextView text = tv(p.optString("text", ""), 16, TEXT, Typeface.NORMAL);
        text.setGravity(Gravity.LEFT);
        text.setPadding(0, dp(8), 0, dp(8));
        card.addView(text, new LinearLayout.LayoutParams(-1, -2));

        String img = p.optString("image", "");
        if (img.length() > 10) {
            card.addView(avatarView(260, img), new LinearLayout.LayoutParams(-1, dp(260)));
        }

        final String video = p.optString("video", "");
        if (video.length() > 0) {
            TextView vv = tv("Video attached - tap to open", 14, BLUE, Typeface.BOLD);
            vv.setPadding(0, dp(8), 0, dp(8));
            vv.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(video));
                        startActivity(i);
                    } catch (Exception e) {
                        toast("Could not open video");
                    }
                }
            });
            card.addView(vv, new LinearLayout.LayoutParams(-1, dp(44)));
        }

        final String loc = p.optString("location", "");
        if (loc.length() > 0) {
            TextView lt = tv("Location: " + loc + "  - tap for map", 14, BLUE, Typeface.NORMAL);
            lt.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:" + loc + "?q=" + loc));
                        startActivity(i);
                    } catch (Exception e) {
                        toast("Could not open map");
                    }
                }
            });
            card.addView(lt, new LinearLayout.LayoutParams(-1, dp(40)));
        }

        LinearLayout reacts = new LinearLayout(this);
        Button up = btn("Like " + count(p, "up"), Color.rgb(80, 150, 230));
        Button down = btn("Dislike " + count(p, "down"), Color.rgb(100, 100, 100));
        Button com = btn("Comment", Color.rgb(80, 150, 230));
        reacts.addView(up, new LinearLayout.LayoutParams(0, dp(42), 1));
        reacts.addView(down, new LinearLayout.LayoutParams(0, dp(42), 1));
        reacts.addView(com, new LinearLayout.LayoutParams(0, dp(42), 1));
        card.addView(reacts);
        up.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                react(postId, "up");
            }
        });
        down.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                react(postId, "down");
            }
        });
        com.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                comment(postId, p);
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
                JSONObject c = comments.getJSONObject(ckeys.get(i));
                TextView ct = tv(c.optString("name", "User") + ": " + c.optString("text", ""), 14, Color.DKGRAY, Typeface.NORMAL);
                ct.setPadding(dp(14), 0, 0, 0);
                card.addView(ct, new LinearLayout.LayoutParams(-1, dp(32)));
            }
        }

        feedList.addView(card, new LinearLayout.LayoutParams(-1, -2));
        TextView line = new TextView(this);
        line.setBackgroundColor(GREY);
        feedList.addView(line, new LinearLayout.LayoutParams(-1, dp(8)));
    }

    private int count(JSONObject p, String type) {
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

    private void editPost(final String id, JSONObject p) {
        if (statusBox == null) {
            return;
        }
        editingPostId = id;
        statusBox.setText(p.optString("text", ""));
        pendingImage64 = p.optString("image", "");
        pendingVideoUri = p.optString("video", "");
        pendingLocation = p.optString("location", "");
        if (activePostButton != null) {
            activePostButton.setText("Save Edit");
            activePostButton.setBackgroundColor(GREEN);
        }
        toast("Edit the status, then press Save Edit.");
    }

    private void react(final String postId, final String type) {
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void[] v) {
                try {
                    return putJson(FIREBASE_DATABASE_URL + "/posts/" + postId + "/reactions/" + uid + ".json?auth=" + enc(idToken), JSONObject.quote(type));
                } catch (Exception e) {
                    return "ERR:" + e.toString();
                }
            }
            protected void onPostExecute(String s) {
                if (currentTab == TAB_PROFILE) {
                    loadPosts(true);
                } else {
                    loadPosts(false);
                }
            }
        }.execute(new Void[0]);
    }

    private void comment(final String postId, final JSONObject post) {
        final EditText e = new EditText(this);
        e.setHint("Write comment");
        e.setMinLines(2);
        final String ownerUid = post.optString("uid", "");
        final String postText = post.optString("text", "");
        new AlertDialog.Builder(this).setTitle("Comment").setView(e).setPositiveButton("Post", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int w) {
                sendComment(postId, e.getText().toString(), ownerUid, postText);
            }
        }).setNegativeButton("Cancel", null).show();
    }

    private void sendComment(final String postId, final String text, final String ownerUid, final String postText) {
        if (text.trim().length() == 0) {
            return;
        }
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void[] v) {
                try {
                    JSONObject c = new JSONObject();
                    c.put("uid", uid);
                    c.put("name", displayName);
                    c.put("text", text);
                    c.put("time", System.currentTimeMillis());
                    String result = postJson(FIREBASE_DATABASE_URL + "/posts/" + postId + "/comments.json?auth=" + enc(idToken), c.toString());
                    if (ownerUid != null && ownerUid.length() > 0 && !uid.equals(ownerUid)) {
                        createNotification(ownerUid, "comment", postId, displayName + " commented on your status", postText);
                    }
                    return result;
                } catch (Exception e) {
                    return "ERR:" + e.toString();
                }
            }
            protected void onPostExecute(String s) {
                if (currentTab == TAB_PROFILE) {
                    loadPosts(true);
                } else {
                    loadPosts(false);
                }
            }
        }.execute(new Void[0]);
    }

    private void saveMyProfile() {
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void[] v) {
                try {
                    JSONObject u = new JSONObject();
                    u.put("uid", uid);
                    u.put("email", email);
                    u.put("name", displayName);
                    u.put("profile", profileImage64);
                    u.put("name_lc", displayName.toLowerCase(Locale.US));
                    return putJson(FIREBASE_DATABASE_URL + "/users/" + uid + ".json?auth=" + enc(idToken), u.toString());
                } catch (Exception e) {
                    return "ERR:" + e.toString();
                }
            }
            protected void onPostExecute(String s) {
                prefs.edit().putString("name", displayName).putString("profile", profileImage64).commit();
                showMainPage(currentTab);
            }
        }.execute(new Void[0]);
    }

    private void loadMyProfileThenHome() {
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void[] v) {
                try {
                    return getUrl(FIREBASE_DATABASE_URL + "/users/" + uid + ".json?auth=" + enc(idToken));
                } catch (Exception e) {
                    return "";
                }
            }
            protected void onPostExecute(String s) {
                try {
                    if (s != null && !s.equals("null") && s.length() > 2) {
                        JSONObject o = new JSONObject(s);
                        displayName = o.optString("name", displayName);
                        profileImage64 = o.optString("profile", "");
                        prefs.edit().putString("name", displayName).putString("profile", profileImage64).commit();
                    }
                } catch (Exception e) {
                }
                currentTab = TAB_HOME;
                showMainPage(TAB_HOME);
            }
        }.execute(new Void[0]);
    }

    private void searchUsers(final String q, final LinearLayout results) {
        results.removeAllViews();
        results.addView(tv("Searching...", 14, Color.GRAY, Typeface.NORMAL));
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void[] v) {
                try {
                    return getUrl(FIREBASE_DATABASE_URL + "/users.json?auth=" + enc(idToken));
                } catch (Exception e) {
                    return "ERR:" + e.toString();
                }
            }
            protected void onPostExecute(String s) {
                results.removeAllViews();
                try {
                    if (s == null || s.equals("null") || s.startsWith("ERR:")) {
                        results.addView(tv("No users found", 14, Color.GRAY, Typeface.NORMAL));
                        return;
                    }
                    JSONObject all = new JSONObject(s);
                    Iterator<String> it = all.keys();
                    String qq = q.toLowerCase(Locale.US);
                    int found = 0;
                    while (it.hasNext()) {
                        final String targetUid = it.next();
                        final JSONObject u = all.getJSONObject(targetUid);
                        if (targetUid.equals(uid)) {
                            continue;
                        }
                        String name = u.optString("name", "");
                        String mail = u.optString("email", "");
                        if (qq.length() == 0 || name.toLowerCase(Locale.US).indexOf(qq) >= 0 || mail.toLowerCase(Locale.US).indexOf(qq) >= 0) {
                            found++;
                            LinearLayout row = new LinearLayout(MainActivity.this);
                            row.setGravity(Gravity.CENTER_VERTICAL);
                            row.setPadding(0, dp(4), 0, dp(4));
                            row.addView(avatarView(48, u.optString("profile", "")), new LinearLayout.LayoutParams(dp(48), dp(48)));
                            TextView r = tv("  " + name + "\n  " + mail, 14, Color.DKGRAY, Typeface.BOLD);
                            row.addView(r, new LinearLayout.LayoutParams(0, dp(58), 1));
                            Button add = btn("Add", BLUE);
                            row.addView(add, new LinearLayout.LayoutParams(dp(76), dp(44)));
                            final String targetName = name;
                            add.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    addFriend(targetUid, targetName);
                                }
                            });
                            results.addView(row, new LinearLayout.LayoutParams(-1, dp(66)));
                        }
                    }
                    if (found == 0) {
                        results.addView(tv("No users found", 14, Color.GRAY, Typeface.NORMAL));
                    }
                } catch (Exception e) {
                    results.addView(tv("Search error: " + e.toString(), 14, Color.RED, Typeface.NORMAL));
                }
            }
        }.execute(new Void[0]);
    }

    private void addFriend(final String targetUid, final String targetName) {
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void[] v) {
                try {
                    JSONObject detail = new JSONObject();
                    detail.put("uid", targetUid);
                    detail.put("name", targetName);
                    detail.put("time", System.currentTimeMillis());
                    putJson(FIREBASE_DATABASE_URL + "/friends/" + uid + "/" + targetUid + ".json?auth=" + enc(idToken), "true");
                    return putJson(FIREBASE_DATABASE_URL + "/friendDetails/" + uid + "/" + targetUid + ".json?auth=" + enc(idToken), detail.toString());
                } catch (Exception e) {
                    return "ERR:" + e.toString();
                }
            }
            protected void onPostExecute(String s) {
                if (s != null && s.startsWith("ERR:")) {
                    toast(s);
                } else {
                    toast(targetName + " added. You will get tag alerts from this user.");
                }
            }
        }.execute(new Void[0]);
    }

    private void createTagNotifications(String postId, String text) throws Exception {
        if (text == null || text.indexOf("@") < 0) {
            return;
        }
        String usersText = getUrl(FIREBASE_DATABASE_URL + "/users.json?auth=" + enc(idToken));
        if (usersText == null || usersText.equals("null") || usersText.length() < 3) {
            return;
        }
        String friendsText = getUrl(FIREBASE_DATABASE_URL + "/friends.json?auth=" + enc(idToken));
        JSONObject users = new JSONObject(usersText);
        JSONObject friends = null;
        try {
            if (friendsText != null && !friendsText.equals("null") && friendsText.length() > 2) {
                friends = new JSONObject(friendsText);
            }
        } catch (Exception e) {
            friends = null;
        }
        Iterator<String> it = users.keys();
        while (it.hasNext()) {
            String targetUid = it.next();
            if (targetUid.equals(uid)) {
                continue;
            }
            JSONObject target = users.optJSONObject(targetUid);
            if (target == null) {
                continue;
            }
            if (matchesTag(text, target.optString("name", ""), target.optString("email", ""))) {
                boolean addedAuthor = false;
                if (friends != null) {
                    JSONObject targetFriends = friends.optJSONObject(targetUid);
                    if (targetFriends != null && targetFriends.optBoolean(uid, false)) {
                        addedAuthor = true;
                    }
                }
                if (addedAuthor) {
                    createNotification(targetUid, "tag", postId, displayName + " tagged you in a status", text);
                }
            }
        }
    }

    private boolean matchesTag(String text, String name, String mail) {
        String low = text.toLowerCase(Locale.US);
        if (name != null && name.length() > 0) {
            String n = name.toLowerCase(Locale.US).trim();
            String noSpace = n.replace(" ", "");
            if (low.indexOf("@" + n) >= 0 || low.indexOf("@" + noSpace) >= 0) {
                return true;
            }
        }
        if (mail != null && mail.length() > 0) {
            String m = mail.toLowerCase(Locale.US).trim();
            int at = m.indexOf("@");
            String prefix = at > 0 ? m.substring(0, at) : m;
            if (low.indexOf("@" + prefix) >= 0 || low.indexOf("@" + m) >= 0) {
                return true;
            }
        }
        return false;
    }

    private void createNotification(String targetUid, String type, String postId, String message, String postText) throws Exception {
        JSONObject n = new JSONObject();
        n.put("type", type);
        n.put("postId", postId);
        n.put("fromUid", uid);
        n.put("fromName", displayName);
        n.put("message", message);
        n.put("postText", trimText(postText, 160));
        n.put("read", false);
        n.put("time", System.currentTimeMillis());
        postJson(FIREBASE_DATABASE_URL + "/notifications/" + targetUid + ".json?auth=" + enc(idToken), n.toString());
    }

    private void loadNotifications() {
        if (notificationList == null) {
            return;
        }
        notificationList.removeAllViews();
        notificationList.addView(tv("Loading notifications...", 15, Color.GRAY, Typeface.NORMAL), new LinearLayout.LayoutParams(-1, dp(56)));
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void[] v) {
                try {
                    return getUrl(FIREBASE_DATABASE_URL + "/notifications/" + uid + ".json?auth=" + enc(idToken));
                } catch (Exception e) {
                    return "ERR:" + e.toString();
                }
            }
            protected void onPostExecute(String s) {
                renderNotifications(s);
            }
        }.execute(new Void[0]);
    }

    private void renderNotifications(String s) {
        notificationList.removeAllViews();
        try {
            if (s == null || s.equals("null") || s.startsWith("ERR:")) {
                String msg = s != null && s.startsWith("ERR:") ? s : "No notifications yet.";
                notificationList.addView(tv(msg, 15, Color.GRAY, Typeface.NORMAL), new LinearLayout.LayoutParams(-1, dp(80)));
                return;
            }
            final JSONObject all = new JSONObject(s);
            ArrayList<String> keys = new ArrayList<String>();
            Iterator<String> it = all.keys();
            while (it.hasNext()) {
                keys.add(it.next());
            }
            Collections.sort(keys, new Comparator<String>() {
                public int compare(String a, String b) {
                    long ta = all.optJSONObject(a) == null ? 0 : all.optJSONObject(a).optLong("time", 0);
                    long tb = all.optJSONObject(b) == null ? 0 : all.optJSONObject(b).optLong("time", 0);
                    if (tb > ta) return 1;
                    if (tb < ta) return -1;
                    return 0;
                }
            });
            for (int i = 0; i < keys.size(); i++) {
                addNotificationCard(keys.get(i), all.getJSONObject(keys.get(i)));
            }
        } catch (Exception e) {
            notificationList.addView(tv("Notification error: " + e.toString(), 14, Color.RED, Typeface.NORMAL));
        }
    }

    private void addNotificationCard(final String nid, final JSONObject n) throws Exception {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(10), dp(8), dp(10), dp(8));
        card.setBackgroundColor(n.optBoolean("read", false) ? Color.WHITE : Color.rgb(232, 244, 255));
        TextView msg = tv(n.optString("message", "Notification"), 16, TEXT, Typeface.BOLD);
        card.addView(msg, new LinearLayout.LayoutParams(-1, dp(30)));
        TextView time = tv(friendlyTime(n.optLong("time", 0)), 12, Color.GRAY, Typeface.NORMAL);
        card.addView(time, new LinearLayout.LayoutParams(-1, dp(22)));
        TextView snippet = tv(n.optString("postText", ""), 14, Color.DKGRAY, Typeface.NORMAL);
        snippet.setGravity(Gravity.LEFT);
        card.addView(snippet, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout row = new LinearLayout(this);
        Button view = btn("Open Home", BLUE);
        Button read = btn("Mark read", Color.rgb(100, 100, 100));
        row.addView(view, new LinearLayout.LayoutParams(0, dp(42), 1));
        row.addView(read, new LinearLayout.LayoutParams(0, dp(42), 1));
        card.addView(row, new LinearLayout.LayoutParams(-1, dp(48)));
        view.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                markNotificationRead(nid, false);
                showMainPage(TAB_HOME);
            }
        });
        read.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                markNotificationRead(nid, true);
            }
        });
        notificationList.addView(card, new LinearLayout.LayoutParams(-1, -2));
        TextView line = new TextView(this);
        line.setBackgroundColor(GREY);
        notificationList.addView(line, new LinearLayout.LayoutParams(-1, dp(8)));
    }

    private void markNotificationRead(final String nid, final boolean reload) {
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void[] v) {
                try {
                    return putJson(FIREBASE_DATABASE_URL + "/notifications/" + uid + "/" + nid + "/read.json?auth=" + enc(idToken), "true");
                } catch (Exception e) {
                    return "ERR:" + e.toString();
                }
            }
            protected void onPostExecute(String s) {
                if (reload) {
                    loadNotifications();
                }
            }
        }.execute(new Void[0]);
    }

    private void markAllNotificationsRead() {
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void[] v) {
                try {
                    String s = getUrl(FIREBASE_DATABASE_URL + "/notifications/" + uid + ".json?auth=" + enc(idToken));
                    if (s == null || s.equals("null") || s.length() < 3) {
                        return "OK";
                    }
                    JSONObject all = new JSONObject(s);
                    Iterator<String> it = all.keys();
                    while (it.hasNext()) {
                        String nid = it.next();
                        putJson(FIREBASE_DATABASE_URL + "/notifications/" + uid + "/" + nid + "/read.json?auth=" + enc(idToken), "true");
                    }
                    return "OK";
                } catch (Exception e) {
                    return "ERR:" + e.toString();
                }
            }
            protected void onPostExecute(String s) {
                loadNotifications();
            }
        }.execute(new Void[0]);
    }

    private String trimText(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, max) + "...";
    }

    private String friendlyTime(long millis) {
        try {
            if (millis <= 0) {
                return "";
            }
            SimpleDateFormat f = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
            return f.format(new Date(millis));
        } catch (Exception e) {
            return "";
        }
    }

    private String enc(String s) throws Exception {
        return URLEncoder.encode(s, "UTF-8");
    }

    private String getUrl(String u) throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(u).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(15000);
        InputStream is = c.getResponseCode() >= 400 ? c.getErrorStream() : c.getInputStream();
        return read(is);
    }

    private String postJson(String u, String body) throws Exception {
        return sendJson(u, body, "POST");
    }

    private String putJson(String u, String body) throws Exception {
        return sendJson(u, body, "PUT");
    }

    private String sendJson(String u, String body, String method) throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(u).openConnection();
        c.setRequestMethod(method);
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json");
        OutputStream os = c.getOutputStream();
        os.write(body.getBytes("UTF-8"));
        os.close();
        InputStream is = c.getResponseCode() >= 400 ? c.getErrorStream() : c.getInputStream();
        return read(is);
    }

    private String read(InputStream is) throws Exception {
        if (is == null) {
            return "";
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        return sb.toString();
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }
}
