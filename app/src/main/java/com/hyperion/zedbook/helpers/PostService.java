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
        u.put("name", safeDisplayName(session.displayName));
        u.put("profile", session.profileImage64);
        u.put("name_lc", safeDisplayName(session.displayName).toLowerCase(Locale.US));

        try {
            String old = FirebaseHelper.getFirebase("users/" + session.uid + "/lastNameChange", session.idToken);
            if (!FirebaseHelper.isEmptyFirebaseValue(old)) {
                u.put("lastNameChange", Long.parseLong(old.replace("\"", "")));
            }
        } catch (Exception ignored) {
        }

        String result = FirebaseHelper.putFirebase("users/" + session.uid, session.idToken, u.toString());
        syncUserProfileImageOnPosts(session);
        return result;
    }

    public static String changeName(SessionManager session, String oldName, String newName) throws Exception {
        if (newName == null || newName.trim().length() == 0) {
            newName = "ZedBook User";
        }
        newName = newName.trim();
        if (oldName == null || oldName.trim().length() == 0) {
            oldName = safeDisplayName(session.displayName);
        }
        oldName = oldName.trim();
        if (oldName.equals(newName)) {
            return saveProfile(session);
        }

        long now = System.currentTimeMillis();
        long last = getLastNameChange(session);
        if (last > 0 && now - last < AppConfig.NAME_CHANGE_INTERVAL_MS) {
            long remaining = AppConfig.NAME_CHANGE_INTERVAL_MS - (now - last);
            long days = remaining / 86400000L;
            if (remaining % 86400000L != 0) {
                days++;
            }
            throw new Exception("You can only change your name once every 6 months. Try again in about " + days + " day(s).");
        }

        JSONObject u = new JSONObject();
        u.put("uid", session.uid);
        u.put("email", session.email);
        u.put("name", newName);
        u.put("profile", session.profileImage64);
        u.put("name_lc", newName.toLowerCase(Locale.US));
        u.put("lastNameChange", now);
        FirebaseHelper.putFirebase("users/" + session.uid, session.idToken, u.toString());
        syncUserNameOnPosts(session, newName);
        createNameChangePost(session, oldName, newName, now);
        return "OK";
    }

    private static long getLastNameChange(SessionManager session) {
        try {
            String user = FirebaseHelper.getFirebase("users/" + session.uid, session.idToken);
            if (!FirebaseHelper.isEmptyFirebaseValue(user) && FirebaseHelper.looksLikeJsonObject(user)) {
                JSONObject u = new JSONObject(user);
                return u.optLong("lastNameChange", 0);
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private static void syncUserNameOnPosts(SessionManager session, String newName) {
        try {
            String posts = FirebaseHelper.getFirebase("posts", session.idToken);
            if (FirebaseHelper.isEmptyFirebaseValue(posts) || !FirebaseHelper.looksLikeJsonObject(posts)) {
                return;
            }
            JSONObject all = new JSONObject(posts);
            Iterator<String> it = all.keys();
            while (it.hasNext()) {
                String postId = it.next();
                JSONObject p = all.optJSONObject(postId);
                if (p != null && session.uid.equals(p.optString("uid", ""))) {
                    FirebaseHelper.putFirebase("posts/" + postId + "/name", session.idToken, JSONObject.quote(newName));
                    FirebaseHelper.putFirebase("posts/" + postId + "/profile", session.idToken, JSONObject.quote(session.profileImage64));
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void syncUserProfileImageOnPosts(SessionManager session) {
        try {
            String posts = FirebaseHelper.getFirebase("posts", session.idToken);
            if (FirebaseHelper.isEmptyFirebaseValue(posts) || !FirebaseHelper.looksLikeJsonObject(posts)) {
                return;
            }
            JSONObject all = new JSONObject(posts);
            Iterator<String> it = all.keys();
            while (it.hasNext()) {
                String postId = it.next();
                JSONObject p = all.optJSONObject(postId);
                if (p != null && session.uid.equals(p.optString("uid", ""))) {
                    FirebaseHelper.putFirebase("posts/" + postId + "/profile", session.idToken, JSONObject.quote(session.profileImage64));
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void createNameChangePost(SessionManager session, String oldName, String newName, long now) throws Exception {
        JSONObject p = new JSONObject();
        p.put("uid", session.uid);
        p.put("email", session.email);
        p.put("name", newName);
        p.put("profile", session.profileImage64);
        p.put("text", oldName + " is now known as " + newName + ".");
        p.put("image", "");
        p.put("video", "");
        p.put("location", "");
        p.put("visibility", AppConfig.VISIBILITY_PUBLIC);
        p.put("system", "name_change");
        p.put("time", now);
        p.put("edited", false);
        FirebaseHelper.postFirebase("posts", session.idToken, p.toString());
    }

    public static String createPost(SessionManager session, String text, String image64, String videoUri, String locationText, String visibility) throws Exception {
        JSONObject p = new JSONObject();
        p.put("uid", session.uid);
        p.put("email", session.email);
        p.put("name", safeDisplayName(session.displayName));
        p.put("profile", session.profileImage64);
        p.put("text", text);
        p.put("image", image64);
        p.put("video", videoUri);
        p.put("location", locationText);
        p.put("visibility", cleanVisibility(visibility));
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

    public static String updatePost(SessionManager session, String postId, String text, String image64, String videoUri, String locationText, String visibility) throws Exception {
        FirebaseHelper.putFirebase("posts/" + postId + "/text", session.idToken, JSONObject.quote(text));
        FirebaseHelper.putFirebase("posts/" + postId + "/image", session.idToken, JSONObject.quote(image64));
        FirebaseHelper.putFirebase("posts/" + postId + "/video", session.idToken, JSONObject.quote(videoUri));
        FirebaseHelper.putFirebase("posts/" + postId + "/location", session.idToken, JSONObject.quote(locationText));
        FirebaseHelper.putFirebase("posts/" + postId + "/visibility", session.idToken, JSONObject.quote(cleanVisibility(visibility)));
        FirebaseHelper.putFirebase("posts/" + postId + "/edited", session.idToken, "true");
        FirebaseHelper.putFirebase("posts/" + postId + "/editTime", session.idToken, String.valueOf(System.currentTimeMillis()));
        createTagNotifications(session, postId, text);
        return "OK";
    }

    public static String deletePost(SessionManager session, String postId) throws Exception {
        return FirebaseHelper.deleteFirebase("posts/" + postId, session.idToken);
    }

    public static String react(SessionManager session, String postId, String type) throws Exception {
        return FirebaseHelper.putFirebase("posts/" + postId + "/reactions/" + session.uid, session.idToken, JSONObject.quote(type));
    }

    public static String comment(SessionManager session, String postId, String text, String ownerUid, String postText) throws Exception {
        JSONObject c = new JSONObject();
        c.put("uid", session.uid);
        c.put("name", safeDisplayName(session.displayName));
        c.put("text", text);
        c.put("time", System.currentTimeMillis());
        String result = FirebaseHelper.postFirebase("posts/" + postId + "/comments", session.idToken, c.toString());
        if (ownerUid != null && ownerUid.length() > 0 && !session.uid.equals(ownerUid)) {
            createNotification(session, ownerUid, "comment", postId, safeDisplayName(session.displayName) + " commented on your status", postText);
        }
        return result;
    }

    public static String addFriend(SessionManager session, String targetUid, String targetName) throws Exception {
        FirebaseHelper.putFirebase("friends/" + session.uid + "/" + targetUid, session.idToken, "true");
        FirebaseHelper.putFirebase("friendNames/" + session.uid + "/" + targetUid, session.idToken, JSONObject.quote(targetName));
        FirebaseHelper.putFirebase("friends/" + targetUid + "/" + session.uid, session.idToken, "true");
        FirebaseHelper.putFirebase("friendNames/" + targetUid + "/" + session.uid, session.idToken, JSONObject.quote(safeDisplayName(session.displayName)));
        createNotification(session, targetUid, "friend", "", safeDisplayName(session.displayName) + " added you as a friend", "");
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
                createNotification(session, targetUid, "tag", postId, safeDisplayName(session.displayName) + " tagged you in a status", text);
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
        n.put("fromName", safeDisplayName(session.displayName));
        n.put("message", message);
        n.put("postText", TimeHelper.trim(postText, 160));
        n.put("read", false);
        n.put("time", System.currentTimeMillis());
        FirebaseHelper.postFirebase("notifications/" + targetUid, session.idToken, n.toString());
    }

    private static String safeDisplayName(String name) {
        if (name == null || name.trim().length() == 0) {
            return "ZedBook User";
        }
        return name.trim();
    }

    public static String cleanVisibility(String visibility) {
        if (AppConfig.VISIBILITY_FRIENDS.equals(visibility)) {
            return AppConfig.VISIBILITY_FRIENDS;
        }
        if (AppConfig.VISIBILITY_ONLY_ME.equals(visibility)) {
            return AppConfig.VISIBILITY_ONLY_ME;
        }
        return AppConfig.VISIBILITY_PUBLIC;
    }
}
