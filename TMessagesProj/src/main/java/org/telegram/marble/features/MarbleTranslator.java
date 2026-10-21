/*
 * Marblegram — free, key-less message translation.
 *
 * Primary: Google Translate web endpoint (client=gtx) — no API key, no fixed quota.
 * Fallback: Google "dict-chrome-ex" endpoint. Both are unofficial: Google may throttle very frequent
 * requests, so results are cached and failures surface as a readable error.
 */

package org.telegram.marble.features;

import android.text.TextUtils;
import android.util.LruCache;

import org.json.JSONArray;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.Utilities;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class MarbleTranslator {

    public static class Result {
        public final String text;
        public final String sourceLang;

        Result(String text, String sourceLang) {
            this.text = text;
            this.sourceLang = sourceLang;
        }
    }

    public interface Callback {
        void onResult(Result result, String error);
    }

    private static final LruCache<String, Result> cache = new LruCache<>(200);

    public static String defaultTargetLanguage() {
        String lang = LocaleController.getInstance().getCurrentLocale().getLanguage();
        return TextUtils.isEmpty(lang) ? "en" : lang;
    }

    /** Translates on a background thread, delivers the callback on the UI thread. */
    public static void translate(String text, String targetLang, Callback callback) {
        final String key = targetLang + "\u0000" + text;
        Result cached = cache.get(key);
        if (cached != null) {
            callback.onResult(cached, null);
            return;
        }
        Utilities.globalQueue.postRunnable(() -> {
            Result result = null;
            String error = null;
            // dict-chrome-ex first (works even where gtx shows a captcha), one retry on an empty answer, then gtx
            for (int attempt = 0; attempt < 3 && result == null; attempt++) {
                try {
                    result = attempt < 2 ? translateDict(text, targetLang) : translateGtx(text, targetLang);
                } catch (Exception e) {
                    error = e.getMessage() != null ? e.getMessage() : e.toString();
                }
            }
            if (result != null) {
                error = null;
            }
            if (result != null) {
                cache.put(key, result);
            }
            final Result r = result;
            final String err = error;
            AndroidUtilities.runOnUIThread(() -> callback.onResult(r, err));
        });
    }

    private static Result translateGtx(String text, String tl) throws Exception {
        String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=" + enc(tl) + "&dt=t&q=" + enc(text);
        if (url.length() > 7000) {
            throw new IllegalStateException("text too long for the fallback endpoint");
        }
        JSONArray root = new JSONArray(request(url, null));
        JSONArray sentences = root.getJSONArray(0);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sentences.length(); i++) {
            JSONArray s = sentences.optJSONArray(i);
            if (s != null && !s.isNull(0)) {
                sb.append(s.getString(0));
            }
        }
        String src = root.length() > 2 ? root.optString(2, null) : null;
        if (sb.length() == 0) {
            throw new IllegalStateException("empty translation");
        }
        return new Result(sb.toString(), src);
    }

    private static Result translateDict(String text, String tl) throws Exception {
        String url = "https://clients5.google.com/translate_a/t?client=dict-chrome-ex&sl=auto&tl=" + enc(tl);
        JSONArray root = new JSONArray(request(url, "q=" + enc(text)));
        Object first = root.get(0);
        Result r;
        if (first instanceof JSONArray) {
            JSONArray pair = (JSONArray) first;
            r = new Result(pair.getString(0), pair.length() > 1 ? pair.optString(1, null) : null);
        } else {
            r = new Result(String.valueOf(first), null);
        }
        if (TextUtils.isEmpty(r.text)) {
            throw new IllegalStateException("empty translation");
        }
        return r;
    }

    private static String enc(String s) throws Exception {
        return URLEncoder.encode(s, "UTF-8");
    }

    private static String request(String url, String postBody) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(12000);
        c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130 Mobile Safari/537.36");
        if (postBody != null) {
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            try (java.io.OutputStream os = c.getOutputStream()) {
                os.write(postBody.getBytes(StandardCharsets.UTF_8));
            }
        }
        int code = c.getResponseCode();
        if (code != 200) {
            throw new IllegalStateException("HTTP " + code + (code == 429 ? " (Google rate limit, try again later)" : ""));
        }
        try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        } finally {
            c.disconnect();
        }
    }

    private MarbleTranslator() {}
}
