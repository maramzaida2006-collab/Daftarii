package com.example.daftarii.notifications;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.example.daftarii.logic.DueAlerts;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * الإشعارات المحفوظة على الجهاز، لكل حساب لحاله (uid).
 * محلية بس (ما بتنرفع على Firestore): هي تذكيرات من بيانات الزباين، وبتنحسب من جديد إذا انمسحت.
 */
public class NotificationStore {

    private static final String FILE = "daftari_notifications";
    private static final String KEY_ITEMS = "items_";

    private final SharedPreferences prefs;

    public NotificationStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    @NonNull
    public synchronized List<DueAlerts.Item> load(@NonNull String uid) {
        List<DueAlerts.Item> out = new ArrayList<>();
        String raw = prefs.getString(KEY_ITEMS + uid, null);
        if (raw == null) return out;
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                DueAlerts.Type type;
                try {
                    type = DueAlerts.Type.valueOf(o.getString("type"));
                } catch (IllegalArgumentException unknownType) {
                    continue;
                }
                out.add(new DueAlerts.Item(
                        o.getString("key"), type,
                        o.optString("customerId", null),
                        o.optString("customerName", null),
                        o.optString("customerPhone", null),
                        o.optLong("amount", 0),
                        o.optLong("dueDate", 0),
                        o.optLong("createdAt", 0),
                        o.optBoolean("read", false)));
            }
        } catch (JSONException broken) {
            // ملف خربان: بنبلّش من جديد، والإشعارات بترجع تنحسب بالفحص الجاي
            return new ArrayList<>();
        }
        return out;
    }

    public synchronized void save(@NonNull String uid, @NonNull List<DueAlerts.Item> items) {
        JSONArray arr = new JSONArray();
        try {
            for (DueAlerts.Item i : items) {
                JSONObject o = new JSONObject();
                o.put("key", i.key);
                o.put("type", i.type.name());
                if (i.customerId != null) o.put("customerId", i.customerId);
                if (i.customerName != null) o.put("customerName", i.customerName);
                if (i.customerPhone != null) o.put("customerPhone", i.customerPhone);
                o.put("amount", i.amountAgorot);
                o.put("dueDate", i.dueDateMillis);
                o.put("createdAt", i.createdAt);
                o.put("read", i.read);
                arr.put(o);
            }
        } catch (JSONException e) {
            return;
        }
        prefs.edit().putString(KEY_ITEMS + uid, arr.toString()).apply();
    }

    /** مع تسجيل الخروج: مش ضروري (لكل uid ملفه)، بس بيوفّر مساحة. */
    public void clear() {
        prefs.edit().clear().apply();
    }
}
