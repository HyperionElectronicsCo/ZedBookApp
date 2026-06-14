package com.hyperion.zedbook;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hyperion.zedbook.helpers.AppConfig;
import com.hyperion.zedbook.helpers.FirebaseHelper;
import com.hyperion.zedbook.helpers.PostService;
import com.hyperion.zedbook.helpers.SessionManager;
import com.hyperion.zedbook.helpers.Ui;

import org.json.JSONObject;

import java.util.Locale;

public class Loginsignup extends Activity {
    private LinearLayout root;
    private boolean signupMode;
    private EditText nameBox;
    private EditText emailBox;
    private EditText passBox;
    private EditText confirmBox;
    private SessionManager session;

    public void onCreate(Bundle b) {
        super.onCreate(b);
        session = new SessionManager(this);
        showLogin();
    }

    private void showLogin() {
        signupMode = false;
        buildAuthScreen();
    }

    private void showSignup() {
        signupMode = true;
        buildAuthScreen();
    }

    private void buildAuthScreen() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(Ui.dp(this, 28), 0, Ui.dp(this, 28), 0);
        root.setBackgroundResource(R.drawable.bg_dark);
        setContentView(root);

        ImageView logoIcon = new ImageView(this);
        logoIcon.setImageResource(R.drawable.ic_zedbook_mark);
        root.addView(logoIcon, new LinearLayout.LayoutParams(Ui.dp(this, 62), Ui.dp(this, 62)));

        TextView logo = Ui.text(this, "ZedBook", 34, AppConfig.BLUE, Typeface.BOLD);
        logo.setGravity(Gravity.CENTER);
        root.addView(logo, new LinearLayout.LayoutParams(-1, Ui.dp(this, 62)));

        if (signupMode) {
            nameBox = input("Display name", false);
            root.addView(nameBox, new LinearLayout.LayoutParams(-1, Ui.dp(this, 54)));
            Ui.gap(root, this, 8);
        }

        emailBox = input("Email", false);
        passBox = input("Password", true);
        root.addView(emailBox, new LinearLayout.LayoutParams(-1, Ui.dp(this, 54)));
        Ui.gap(root, this, 8);
        root.addView(passBox, new LinearLayout.LayoutParams(-1, Ui.dp(this, 54)));

        if (signupMode) {
            Ui.gap(root, this, 8);
            confirmBox = input("Confirm password", true);
            root.addView(confirmBox, new LinearLayout.LayoutParams(-1, Ui.dp(this, 54)));
        }

        Ui.gap(root, this, 16);
        Button main = Ui.button(this, signupMode ? "Create Account" : "Log In", R.drawable.bg_blue_button);
        root.addView(main, new LinearLayout.LayoutParams(-1, Ui.dp(this, 54)));
        Ui.gap(root, this, 8);
        Button switcher = Ui.button(this, signupMode ? "Already have an account? Log In" : "Create New Account", signupMode ? R.drawable.bg_grey_button : R.drawable.bg_green_button);
        root.addView(switcher, new LinearLayout.LayoutParams(-1, Ui.dp(this, 54)));

        main.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doAuth();
            }
        });
        switcher.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (signupMode) {
                    showLogin();
                } else {
                    showSignup();
                }
            }
        });
    }

    private EditText input(String hint, boolean password) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setTextColor(android.graphics.Color.WHITE);
        e.setHintTextColor(android.graphics.Color.rgb(150, 150, 150));
        e.setTextSize(16);
        e.setPadding(Ui.dp(this, 14), 0, Ui.dp(this, 14), 0);
        e.setBackgroundResource(R.drawable.bg_input_dark);
        if (password) {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        return e;
    }

    private void doAuth() {
        if (!AppConfig.firebaseReady()) {
            Toast.makeText(this, "Add your Firebase Web API key and database URL in helpers/AppConfig.java first.", Toast.LENGTH_LONG).show();
            return;
        }
        final String mail = emailBox.getText().toString().trim();
        final String pass = passBox.getText().toString();
        String name = "";
        if (signupMode) {
            name = nameBox.getText().toString().trim();
            if (!pass.equals(confirmBox.getText().toString())) {
                Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_LONG).show();
                return;
            }
        }
        if (mail.length() < 5 || pass.length() < 6) {
            Toast.makeText(this, "Enter a valid email and 6+ character password.", Toast.LENGTH_LONG).show();
            return;
        }
        final String finalName = name;
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void[] v) {
                try {
                    return FirebaseHelper.auth(mail, pass, signupMode);
                } catch (Exception e) {
                    return "ERR:" + e.toString();
                }
            }
            protected void onPostExecute(String s) {
                handleAuthResult(s, mail, finalName);
            }
        }.execute(new Void[0]);
    }

    private void handleAuthResult(String s, String mail, String requestedName) {
        try {
            if (s == null || s.startsWith("ERR:")) {
                Toast.makeText(this, s == null ? "Network error" : s, Toast.LENGTH_LONG).show();
                return;
            }
            JSONObject r = new JSONObject(s);
            if (r.has("error")) {
                Toast.makeText(this, r.getJSONObject("error").optString("message", "Firebase auth error"), Toast.LENGTH_LONG).show();
                return;
            }
            String newUid = r.getString("localId");
            String token = r.getString("idToken");
            String display = requestedName;
            if (display.length() == 0) {
                int at = mail.indexOf("@");
                display = mail.substring(0, at > 0 ? at : mail.length());
            }
            display = firstLetterSafe(display);
            session.saveAuth(newUid, token, mail, display);
            if (signupMode) {
                saveNewProfileThenOpenHome();
            } else {
                loadProfileThenOpenHome();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Auth parse error: " + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private String firstLetterSafe(String s) {
        String t = s == null ? "" : s.trim();
        if (t.length() == 0) {
            return "ZedBook User";
        }
        return t;
    }

    private void saveNewProfileThenOpenHome() {
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void[] v) {
                try {
                    return PostService.saveProfile(session);
                } catch (Exception e) {
                    return "ERR:" + e.toString();
                }
            }
            protected void onPostExecute(String s) {
                openHome();
            }
        }.execute(new Void[0]);
    }

    private void loadProfileThenOpenHome() {
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void[] v) {
                try {
                    return FirebaseHelper.getFirebase("users/" + session.uid, session.idToken);
                } catch (Exception e) {
                    return "";
                }
            }
            protected void onPostExecute(String s) {
                try {
                    if (s != null && s.length() > 2 && !s.equals("null")) {
                        JSONObject u = new JSONObject(s);
                        session.saveProfile(u.optString("name", session.displayName), u.optString("profile", ""));
                    } else {
                        PostService.saveProfile(session);
                    }
                } catch (Exception ignored) {
                }
                openHome();
            }
        }.execute(new Void[0]);
    }

    private void openHome() {
        startActivity(new Intent(this, Homescreen.class));
        finish();
    }
}
