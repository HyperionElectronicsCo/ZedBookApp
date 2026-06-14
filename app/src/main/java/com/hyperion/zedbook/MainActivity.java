package com.hyperion.zedbook;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends Activity {
    public void onCreate(Bundle b) {
        super.onCreate(b);
        startActivity(new Intent(this, Splashscreen.class));
        finish();
    }
}
