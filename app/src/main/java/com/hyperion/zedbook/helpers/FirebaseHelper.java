package com.hyperion.zedbook.helpers;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;

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

    public static String path(String path, String token) throws Exception {
        String base = AppConfig.FIREBASE_DATABASE_URL;
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return base + path + ".json?auth=" + enc(token);
    }

    public static String getFirebase(String path, String token) throws Exception {
        return getUrl(path(path, token));
    }

    public static String putFirebase(String path, String token, String body) throws Exception {
        return putJson(path(path, token), body);
    }

    public static String postFirebase(String path, String token, String body) throws Exception {
        return postJson(path(path, token), body);
    }

    public static String getUrl(String u) throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(u).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(15000);
        InputStream is = c.getResponseCode() >= 400 ? c.getErrorStream() : c.getInputStream();
        return read(is);
    }

    public static String postJson(String u, String body) throws Exception {
        return sendJson(u, body, "POST");
    }

    public static String putJson(String u, String body) throws Exception {
        return sendJson(u, body, "PUT");
    }

    public static String sendJson(String u, String body, String method) throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(u).openConnection();
        c.setRequestMethod(method);
        c.setDoOutput(true);
        c.setConnectTimeout(15000);
        c.setReadTimeout(15000);
        c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        OutputStream os = c.getOutputStream();
        os.write(body.getBytes("UTF-8"));
        os.close();
        InputStream is = c.getResponseCode() >= 400 ? c.getErrorStream() : c.getInputStream();
        return read(is);
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
        return URLEncoder.encode(s, "UTF-8");
    }
}
