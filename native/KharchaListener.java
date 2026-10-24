package __PACKAGE__;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Reads notifications from UPI apps, bank apps and messaging apps. A notification is kept only when
 * it looks like money was spent. Kept messages wait in a small private queue on the phone until the
 * app opens and picks them up. Nothing is sent anywhere.
 */
public class KharchaListener extends NotificationListenerService {

    private static final String PREFS = "kharcha_inbox";
    private static final String KEY = "queue";
    private static final int MAX_ITEMS = 200;
    private static final Object LOCK = new Object();

    // Package names that contain one of these words are treated as payment sources.
    private static final String[] SOURCE_WORDS = {
        "phonepe", "paytm", "paisa", "upi", "npci", "bank", "hdfc", "icici", "sbi", "axis", "kotak",
        "yesbank", "idfc", "pnb", "canara", "mobikwik", "freecharge", "dreamplug", "slice", "jupiter",
        "messaging", "mms", "sms"
    };

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            if (sbn == null) return;
            String pkg = sbn.getPackageName();
            if (pkg == null || pkg.equals(getPackageName())) return;
            if (!isPaymentSource(pkg)) return;

            Notification n = sbn.getNotification();
            if (n == null) return;
            if ((n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;
            Bundle extras = n.extras;
            if (extras == null) return;

            String title = asString(extras.getCharSequence(Notification.EXTRA_TITLE));
            String text = asString(extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
            if (text.isEmpty()) text = asString(extras.getCharSequence(Notification.EXTRA_TEXT));
            if (text.isEmpty()) return;
            if (!looksLikeSpend(title + " " + text)) return;

            enqueue(this, pkg, title, text, sbn.getPostTime());
            NotificationReaderPlugin.ping();
        } catch (Throwable ignored) {
            // Never let a bad notification crash the service.
        }
    }

    private static boolean isPaymentSource(String pkg) {
        String p = pkg.toLowerCase(Locale.ROOT);
        for (String w : SOURCE_WORDS) {
            if (p.contains(w)) return true;
        }
        return false;
    }

    private static boolean looksLikeSpend(String s) {
        String l = s.toLowerCase(Locale.ROOT);
        boolean money = l.contains("₹") || l.contains("rs.") || l.contains("rs ") || l.contains("inr");
        if (!money) return false;
        boolean digit = false;
        for (int i = 0; i < l.length(); i++) {
            if (Character.isDigit(l.charAt(i))) {
                digit = true;
                break;
            }
        }
        if (!digit) return false;
        return l.contains("debited") || l.contains("debit") || l.contains("spent") || l.contains("paid")
            || l.contains("sent") || l.contains("payment") || l.contains("purchase") || l.contains("withdrawn");
    }

    private static String asString(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }

    private static void enqueue(Context ctx, String pkg, String title, String text, long ts) throws Exception {
        synchronized (LOCK) {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            JSONArray old = new JSONArray(sp.getString(KEY, "[]"));

            // Skip the same message posted again within two minutes.
            for (int i = Math.max(0, old.length() - 10); i < old.length(); i++) {
                JSONObject o = old.getJSONObject(i);
                if (pkg.equals(o.optString("p")) && text.equals(o.optString("x"))
                        && Math.abs(ts - o.optLong("ts")) < 120000L) {
                    return;
                }
            }

            JSONObject item = new JSONObject();
            item.put("p", pkg);
            item.put("t", title);
            item.put("x", text);
            item.put("ts", ts);
            old.put(item);

            JSONArray keep = old;
            if (old.length() > MAX_ITEMS) {
                keep = new JSONArray();
                for (int i = old.length() - MAX_ITEMS; i < old.length(); i++) {
                    keep.put(old.get(i));
                }
            }
            sp.edit().putString(KEY, keep.toString()).apply();
        }
    }

    /** Returns the waiting messages as a JSON array string and empties the queue. */
    static String drainQueue(Context ctx) {
        synchronized (LOCK) {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String json = sp.getString(KEY, "[]");
            sp.edit().remove(KEY).apply();
            return json;
        }
    }
}
