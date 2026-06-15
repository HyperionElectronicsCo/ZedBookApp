package com.hyperion.zedbook.helpers;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.util.ArrayList;

public final class FirebaseHelper {
    private FirebaseHelper() {
    }

    public static String auth(String email, String password, boolean signup) throws Exception {
        String endpoint = signup ? "signUp" : "signInWithPassword";
        String url = "https://identitytoolkit.googleapis.com/v1/accounts:" + endpoint + "?key=" + AppConfig.FIREBASE_WEB_API_KEY;
        JSONObject o = new JSONObject();
        o.put("email", email);
        o.put("password", password);
        o.put("returnSecureToken", true);
        return postJson(url, o.toString());
    }

    public static String refreshIdToken(String refreshToken) throws Exception {
        if (refreshToken == null || refreshToken.length() == 0) {
            throw new Exception("Missing Firebase refresh token");
        }
        String url = "https://securetoken.googleapis.com/v1/token?key=" + AppConfig.FIREBASE_WEB_API_KEY;
        String body = "grant_type=refresh_token&refresh_token=" + enc(refreshToken);
        return postForm(url, body);
    }

    public static String path(String path, String token) throws Exception {
        return pathForBase(AppConfig.FIREBASE_DATABASE_URL, path, token);
    }

    private static String pathForBase(String base, String path, String token) throws Exception {
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return base + path + ".json?auth=" + enc(token);
    }

    public static String getFirebase(String path, String token) throws Exception {
        return firebaseRequest(path, token, null, "GET");
    }

    public static String putFirebase(String path, String token, String body) throws Exception {
        return firebaseRequest(path, token, body, "PUT");
    }

    public static String postFirebase(String path, String token, String body) throws Exception {
        return firebaseRequest(path, token, body, "POST");
    }

    public static String deleteFirebase(String path, String token) throws Exception {
        return firebaseRequest(path, token, null, "DELETE");
    }

    private static String firebaseRequest(String path, String token, String body, String method) throws Exception {
        ArrayList<String> bases = getDatabaseBases();
        String lastError = "";
        for (int i = 0; i < bases.size(); i++) {
            try {
                String url = pathForBase(bases.get(i), path, token);
                if ("GET".equals(method)) {
                    return getUrl(url, true);
                }
                return sendJson(url, body, method, false);
            } catch (Exception e) {
                lastError = e.toString();
                if (isAuthFailure(lastError)) {
                    throw e;
                }
                /*
                 Firebase Realtime Database URLs are region dependent.
                 If the user pasted a firebaseio.com URL but Firebase created a
                 firebasedatabase.app URL, the first request can return 404.
                 In that case try the compatible regional URL guesses below.
                */
                if (lastError.indexOf("HTTP 404") < 0) {
                    throw e;
                }
            }
        }
        if ("GET".equals(method)) {
            return "null";
        }
        throw new Exception("Firebase database URL returned 404. Open Firebase > Realtime Database > Data and copy the exact database URL into AppConfig.FIREBASE_DATABASE_URL. Last error: " + lastError);
    }

    private static ArrayList<String> getDatabaseBases() {
        ArrayList<String> bases = new ArrayList<String>();
        String base = AppConfig.FIREBASE_DATABASE_URL;
        bases.add(base);
        String lower = base.toLowerCase();
        if (lower.indexOf(".firebaseio.com") > 0) {
            String clean = base;
            int scheme = clean.indexOf("://");
            String prefix = scheme >= 0 ? clean.substring(0, scheme + 3) : "https://";
            if (scheme >= 0) {
                clean = clean.substring(scheme + 3);
            }
            if (clean.endsWith("/")) {
                clean = clean.substring(0, clean.length() - 1);
            }
            int firebaseio = clean.toLowerCase().indexOf(".firebaseio.com");
            if (firebaseio > 0) {
                String dbName = clean.substring(0, firebaseio);
                bases.add(prefix + dbName + ".europe-west1.firebasedatabase.app");
                bases.add(prefix + dbName + ".us-central1.firebasedatabase.app");
                bases.add(prefix + dbName + ".asia-southeast1.firebasedatabase.app");
            }
        }
        return bases;
    }

    public static String getUrl(String u) throws Exception {
        return getUrl(u, false);
    }

    public static String getUrl(String u, boolean empty404AsNull) throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(u).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(15000);
        int code = c.getResponseCode();
        InputStream is = code >= 400 ? c.getErrorStream() : c.getInputStream();
        String text = read(is);
        if (code == 404 && empty404AsNull) {
            return "null";
        }
        if (code >= 400) {
            throw new Exception("HTTP " + code + ": " + cleanError(text));
        }
        return text;
    }

    public static String postJson(String u, String body) throws Exception {
        return sendJson(u, body, "POST");
    }

    public static String putJson(String u, String body) throws Exception {
        return sendJson(u, body, "PUT");
    }

    public static String sendJson(String u, String body, String method) throws Exception {
        return sendJson(u, body, method, false);
    }

    public static String sendJson(String u, String body, String method, boolean empty404AsNull) throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(u).openConnection();
        c.setRequestMethod(method);
        c.setConnectTimeout(15000);
        c.setReadTimeout(15000);
        c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        if (!"GET".equals(method)) {
            c.setDoOutput(true);
            OutputStream os = c.getOutputStream();
            if (body == null) {
                body = "";
            }
            os.write(body.getBytes("UTF-8"));
            os.close();
        }
        int code = c.getResponseCode();
        InputStream is = code >= 400 ? c.getErrorStream() : c.getInputStream();
        String text = read(is);
        if (code == 404 && empty404AsNull) {
            return "null";
        }
        if (code >= 400) {
            throw new Exception("HTTP " + code + ": " + cleanError(text));
        }
        return text;
    }

    public static String postForm(String u, String body) throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(u).openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(15000);
        c.setReadTimeout(15000);
        c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        c.setDoOutput(true);
        OutputStream os = c.getOutputStream();
        os.write(body.getBytes("UTF-8"));
        os.close();
        int code = c.getResponseCode();
        InputStream is = code >= 400 ? c.getErrorStream() : c.getInputStream();
        String text = read(is);
        if (code >= 400) {
            throw new Exception("HTTP " + code + ": " + cleanError(text));
        }
        return text;
    }

    private static String cleanError(String s) {
        if (s == null || s.length() == 0) {
            return "No server message";
        }
        if (s.length() > 220) {
            return s.substring(0, 220);
        }
        return s;
    }

    public static boolean isAuthFailure(String s) {
        if (s == null) {
            return false;
        }
        String low = s.toLowerCase();
        if (low.indexOf("http 401") >= 0 || low.indexOf("permission denied") >= 0) {
            return true;
        }
        if (low.indexOf("auth token") >= 0 || low.indexOf("id token") >= 0 || low.indexOf("invalid_id_token") >= 0) {
            return true;
        }
        if (low.indexOf("token_expired") >= 0 || low.indexOf("user_disabled") >= 0 || low.indexOf("invalid_refresh_token") >= 0) {
            return true;
        }
        return false;
    }

    public static boolean isEmptyFirebaseValue(String s) {
        if (s == null) {
            return true;
        }
        String t = s.trim();
        if (t.length() == 0 || "null".equals(t) || "{}".equals(t)) {
            return true;
        }
        if ("404 Not Found".equals(t)) {
            return true;
        }
        if (t.startsWith("ERR:java.io.FileNotFoundException")) {
            return true;
        }
        return false;
    }

    public static boolean looksLikeJsonObject(String s) {
        if (s == null) {
            return false;
        }
        String t = s.trim();
        return t.startsWith("{") && t.endsWith("}");
    }

    public static String read(InputStream is) throws Exception {
        if (is == null) {
            return "";
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        return sb.toString();
    }

    public static String enc(String s) throws Exception {
        return URLEncoder.encode(s == null ? "" : s, "UTF-8");
    }
}
