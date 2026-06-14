package com.hyperion.zedbook;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import com.hyperion.zedbook.helpers.MediaHelper;
import com.hyperion.zedbook.helpers.PostService;
import com.hyperion.zedbook.helpers.SessionManager;
import com.hyperion.zedbook.helpers.Ui;

public class Settings extends Activity {
    private SessionManager session;
    private LinearLayout root;

    public void onCreate(Bundle b) {
        super.onCreate(b);
        session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, Loginsignup.class));
            finish();
            return;
        }
        buildSettings();
    }

    private void buildSettings() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.bg_feed);
        setContentView(root);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(Ui.dp(this, 10), Ui.dp(this, 6), Ui.dp(this, 10), Ui.dp(this, 6));
        top.setBackgroundResource(R.drawable.bg_top_bar);
        TextView title = Ui.text(this, "Settings", 22, AppConfig.TEXT, Typeface.BOLD);
        title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_settings, 0, 0, 0);
        top.addView(title, new LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1));
        Button close = Ui.button(this, "Home", R.drawable.bg_blue_button);
        top.addView(close, new LinearLayout.LayoutParams(Ui.dp(this, 90), Ui.dp(this, 44)));
        root.addView(top, new LinearLayout.LayoutParams(-1, Ui.dp(this, 62)));
        close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(Settings.this, Homescreen.class));
                finish();
            }
        });

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14));
        card.setBackgroundResource(R.drawable.bg_card);
        card.addView(Ui.avatar(this, 86, session.profileImage64), new LinearLayout.LayoutParams(Ui.dp(this, 86), Ui.dp(this, 86)));
        card.addView(Ui.text(this, session.displayName, 20, AppConfig.TEXT, Typeface.BOLD), new LinearLayout.LayoutParams(-1, Ui.dp(this, 36)));
        card.addView(Ui.text(this, session.email, 14, AppConfig.MUTED, Typeface.NORMAL), new LinearLayout.LayoutParams(-1, Ui.dp(this, 30)));
        root.addView(card, new LinearLayout.LayoutParams(-1, Ui.dp(this, 180)));

        addSettingButton("Edit profile name", R.drawable.ic_edit, new View.OnClickListener() {
            public void onClick(View v) {
                editProfileName();
            }
        });
        addSettingButton("Change profile picture", R.drawable.ic_picture, new View.OnClickListener() {
            public void onClick(View v) {
                pickProfilePicture();
            }
        });
        addSettingButton("Invite friends", R.drawable.ic_invite, new View.OnClickListener() {
            public void onClick(View v) {
                inviteFriends();
            }
        });
        addSettingButton("Log out", R.drawable.ic_logout, new View.OnClickListener() {
            public void onClick(View v) {
                confirmLogout();
            }
        });
    }

    private void addSettingButton(String text, int icon, View.OnClickListener l) {
        Button b = Ui.button(this, text, text.equals("Log out") ? R.drawable.bg_grey_button : R.drawable.bg_blue_button);
        b.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, Ui.dp(this, 52));
        bp.setMargins(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), 0);
        root.addView(b, bp);
    }

    private void editProfileName() {
        final EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setHint("Display name");
        e.setText(session.displayName);
        new AlertDialog.Builder(this).setTitle("Edit profile name").setView(e).setPositiveButton("Save", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int w) {
                String n = e.getText().toString().trim();
                if (n.length() == 0) {
                    n = "ZedBook User";
                }
                session.saveProfile(n, session.profileImage64);
                saveProfileAndRefresh();
            }
        }).setNegativeButton("Cancel", null).show();
    }

    private void pickProfilePicture() {
        try {
            Intent i = new Intent(Intent.ACTION_PICK);
            i.setType("image/*");
            startActivityForResult(i, AppConfig.PICK_PROFILE);
        } catch (Exception e) {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            startActivityForResult(Intent.createChooser(i, "Choose picture"), AppConfig.PICK_PROFILE);
        }
    }

    public void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == AppConfig.PICK_PROFILE && result == RESULT_OK && data != null && data.getData() != null) {
            String img = MediaHelper.imageUriToBase64(this, data.getData(), 70);
            if (img.length() == 0) {
                Toast.makeText(this, "Could not load profile picture", Toast.LENGTH_LONG).show();
                return;
            }
            session.saveProfile(session.displayName, img);
            saveProfileAndRefresh();
        }
    }

    private void saveProfileAndRefresh() {
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
                    Toast.makeText(Settings.this, s, Toast.LENGTH_LONG).show();
                    return;
                }
                buildSettings();
            }
        }.execute(new Void[0]);
    }

    private void inviteFriends() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, "Join me on ZedBook");
        i.putExtra(Intent.EXTRA_TEXT, "Join me on ZedBook so we can share statuses, pictures, videos, comments and location updates.");
        startActivity(Intent.createChooser(i, "Invite friends"));
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this).setTitle("Log out?").setMessage("You will be returned to the login screen.").setPositiveButton("Log out", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int w) {
                session.clear();
                startActivity(new Intent(Settings.this, Loginsignup.class));
                finish();
            }
        }).setNegativeButton("Cancel", null).show();
    }
}
