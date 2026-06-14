package com.hyperion.zedbook.helpers;

import org.json.JSONObject;

import java.util.Iterator;
import java.util.Locale;

public final class PostService {
    private PostService() {
    }

    public static String saveProfile(SessionManager session) throws Exception {
        JSONObject u = new JSONObject();
        u.put("uid", session.uid);
        u.put("email", session.email);
        u.put("name", session.displayName);
        u.put("profile", session.profileImage64);
        u.put("name_lc", session.displayName.toLowerCase(Locale.US));
        return FirebaseHelper.putFirebase("users/" + session.uid, session.idToken, u.toString());
    }

    public static String createPost(SessionManager session, String text, String image64, String videoUri, String locationText) throws Exception {
        JSONObject p = new JSONObject();
        p.put("uid", session.uid);
        p.put("email", session.email);
        p.put("name", session.displayName.length() == 0 ? "ZedBook User" : session.displayName);
        p.put("profile", session.profileImage64);
        p.put("text", text);
        p.put("image", image64);
        p.put("video", videoUri);
        p.put("location", locationText);
        p.put("time", System.currentTimeMillis());
        p.put("edited", false);
        String created = FirebaseHelper.postFirebase("posts", session.idToken, p.toString());
        try {
            JSONObject cr = new JSONObject(created);
            String postId = cr.optString("name", "");
            if (postId.length() > 0) {
                createTagNotifications(session, postId, text);
            }
        } catch (Exception ignored) {
        }
        return created;
    }

    public static String updatePost(SessionManager session, String postId, String text, String image64, String videoUri, String locationText) throws Exception {
        FirebaseHelper.putFirebase("posts/" + postId + "/text", session.idToken, JSONObject.quote(text));
        FirebaseHelper.putFirebase("posts/" + postId + "/image", session.idToken, JSONObject.quote(image64));
        FirebaseHelper.putFirebase("posts/" + postId + "/video", session.idToken, JSONObject.quote(videoUri));
        FirebaseHelper.putFirebase("posts/" + postId + "/location", session.idToken, JSONObject.quote(locationText));
        FirebaseHelper.putFirebase("posts/" + postId + "/edited", session.idToken, "true");
        FirebaseHelper.putFirebase("posts/" + postId + "/editTime", session.idToken, String.valueOf(System.currentTimeMillis()));
        createTagNotifications(session, postId, text);
        return "OK";
    }

    public static String react(SessionManager session, String postId, String type) throws Exception {
        return FirebaseHelper.putFirebase("posts/" + postId + "/reactions/" + session.uid, session.idToken, JSONObject.quote(type));
    }

    public static String comment(SessionManager session, String postId, String text, String ownerUid, String postText) throws Exception {
        JSONObject c = new JSONObject();
        c.put("uid", session.uid);
        c.put("name", session.displayName.length() == 0 ? "ZedBook User" : session.displayName);
        c.put("text", text);
        c.put("time", System.currentTimeMillis());
        String result = FirebaseHelper.postFirebase("posts/" + postId + "/comments", session.idToken, c.toString());
        if (ownerUid != null && ownerUid.length() > 0 && !session.uid.equals(ownerUid)) {
            createNotification(session, ownerUid, "comment", postId, session.displayName + " commented on your status", postText);
        }
        return result;
    }

    public static String addFriend(SessionManager session, String targetUid, String targetName) throws Exception {
        FirebaseHelper.putFirebase("friends/" + session.uid + "/" + targetUid, session.idToken, "true");
        FirebaseHelper.putFirebase("friendNames/" + session.uid + "/" + targetUid, session.idToken, JSONObject.quote(targetName));
        createNotification(session, targetUid, "friend", "", session.displayName + " added you as a friend", "");
        return "OK";
    }

    public static void createTagNotifications(SessionManager session, String postId, String text) throws Exception {
        if (text == null || text.indexOf("@") < 0) {
            return;
        }
        String usersJson = FirebaseHelper.getFirebase("users", session.idToken);
        if (usersJson == null || usersJson.equals("null") || usersJson.length() < 3) {
            return;
        }
        JSONObject allUsers = new JSONObject(usersJson);
        JSONObject myFriends = new JSONObject();
        try {
            String friends = FirebaseHelper.getFirebase("friends/" + session.uid, session.idToken);
            if (friends != null && friends.length() > 2 && !friends.equals("null")) {
                myFriends = new JSONObject(friends);
            }
        } catch (Exception ignored) {
        }

        Iterator<String> it = allUsers.keys();
        while (it.hasNext()) {
            String targetUid = it.next();
            if (session.uid.equals(targetUid)) {
                continue;
            }
            JSONObject u = allUsers.optJSONObject(targetUid);
            if (u == null) {
                continue;
            }
            String name = u.optString("name", "");
            String mail = u.optString("email", "");
            boolean friend = myFriends.optBoolean(targetUid, false);
            if (friend && matchesTag(text, name, mail)) {
                createNotification(session, targetUid, "tag", postId, session.displayName + " tagged you in a status", text);
            }
        }
    }

    public static boolean matchesTag(String text, String name, String mail) {
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

    public static void createNotification(SessionManager session, String targetUid, String type, String postId, String message, String postText) throws Exception {
        JSONObject n = new JSONObject();
        n.put("type", type);
        n.put("postId", postId == null ? "" : postId);
        n.put("fromUid", session.uid);
        n.put("fromName", session.displayName);
        n.put("message", message);
        n.put("postText", TimeHelper.trim(postText, 160));
        n.put("read", false);
        n.put("time", System.currentTimeMillis());
        FirebaseHelper.postFirebase("notifications/" + targetUid, session.idToken, n.toString());
    }
}
