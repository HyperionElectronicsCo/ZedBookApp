package com.hyperion.zedbook;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hyperion.zedbook.helpers.AppConfig;
import com.hyperion.zedbook.helpers.PostService;
import com.hyperion.zedbook.helpers.Ui;

public class Profile extends BaseSocialActivity {
    public void onCreate(Bundle b) {
        super.onCreate(b);
        if (!prepareSession()) {
            return;
        }
        buildChrome("Profile", 1);
        buildProfileHeader();
        buildComposer("Post something to your profile...");
        addSectionHeader("My Statuses", "Everything you have posted from home or profile.");
        buildFeedHolder();
        loadPosts(true);
    }

    private void buildProfileHeader() {
        LinearLayout profile = new LinearLayout(this);
        profile.setOrientation(LinearLayout.HORIZONTAL);
        profile.setGravity(Gravity.CENTER_VERTICAL);
        profile.setPadding(Ui.dp(this, 10), Ui.dp(this, 8), Ui.dp(this, 10), Ui.dp(this, 8));
        profile.setBackgroundResource(R.drawable.bg_card);
        profile.addView(Ui.avatar(this, 76, session.profileImage64), new LinearLayout.LayoutParams(Ui.dp(this, 76), Ui.dp(this, 76)));

        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        TextView n = Ui.text(this, session.displayName.length() == 0 ? "ZedBook User" : session.displayName, 20, AppConfig.TEXT, Typeface.BOLD);
        TextView em = Ui.text(this, session.email, 13, AppConfig.MUTED, Typeface.NORMAL);
        TextView helper = Ui.text(this, "Profile picture, details and your statuses", 12, AppConfig.MUTED, Typeface.NORMAL);
        names.addView(n, new LinearLayout.LayoutParams(-1, Ui.dp(this, 30)));
        names.addView(em, new LinearLayout.LayoutParams(-1, Ui.dp(this, 24)));
        names.addView(helper, new LinearLayout.LayoutParams(-1, Ui.dp(this, 24)));
        profile.addView(names, new LinearLayout.LayoutParams(0, Ui.dp(this, 82), 1));

        Button edit = Ui.button(this, "Edit", R.drawable.bg_blue_button);
        edit.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_edit, 0, 0, 0);
        edit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showProfileDialog();
            }
        });
        profile.addView(edit, new LinearLayout.LayoutParams(Ui.dp(this, 82), Ui.dp(this, 44)));
        root.addView(profile, new LinearLayout.LayoutParams(-1, Ui.dp(this, 100)));
    }

    private void showProfileDialog() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), 0);
        l.addView(Ui.avatar(this, 100, session.profileImage64), new LinearLayout.LayoutParams(Ui.dp(this, 100), Ui.dp(this, 100)));
        final EditText name = new EditText(this);
        name.setHint("Display name");
        name.setSingleLine(true);
        name.setText(session.displayName);
        l.addView(name, new LinearLayout.LayoutParams(-1, Ui.dp(this, 54)));
        Button pic = Ui.button(this, "Change profile picture", R.drawable.bg_blue_button);
        pic.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_picture, 0, 0, 0);
        l.addView(pic, new LinearLayout.LayoutParams(-1, Ui.dp(this, 48)));
        pic.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pick(AppConfig.PICK_PROFILE, "image/*");
            }
        });
        new AlertDialog.Builder(this).setTitle("Profile setup").setView(l).setPositiveButton("Save", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int w) {
                String newName = name.getText().toString().trim();
                if (newName.length() == 0) {
                    newName = "ZedBook User";
                }
                session.saveProfile(newName, session.profileImage64);
                saveMyProfileAndRefresh();
            }
        }).setNegativeButton("Cancel", null).show();
    }

    protected void profileImagePicked(String base64) {
        if (base64 == null || base64.length() == 0) {
            Toast.makeText(this, "Could not load profile picture", Toast.LENGTH_LONG).show();
            return;
        }
        session.saveProfile(session.displayName, base64);
        saveMyProfileAndRefresh();
    }

    private void saveMyProfileAndRefresh() {
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void[] v) {
                try {
                    return PostService.saveProfile(session);
                } catch (Exception e) {
                    return "ERR:" + e.toString();
                }
            }
            protected void onPostExecute(String s) {
                if (s != null && s.startsWith("ERR:")) {
                    Toast.makeText(Profile.this, s, Toast.LENGTH_LONG).show();
                    return;
                }
                startActivity(getIntent());
                finish();
            }
        }.execute(new Void[0]);
    }
}
