package com.hyperion.zedbook;

import android.os.Bundle;

public class Homescreen extends BaseSocialActivity {
    public void onCreate(Bundle b) {
        super.onCreate(b);
        if (!prepareSession()) {
            return;
        }
        buildChrome("Home", 0);
        addSectionHeader("News Feed", "See your friends' public statuses and post your own.");
        buildComposer("What's on your mind?  Use @name to tag friends.");
        buildFeedHolder();
        loadPosts(false);
    }
}
