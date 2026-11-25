package com.example.daftarii.notifications;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.daftarii.R;
import com.example.daftarii.SplashActivity;
import com.example.daftarii.logic.DueAlerts;

import java.util.List;

/** إشعارات الجوال: القناة، والإذن (أندرويد 13+)، وبعت الإشعار. */
public final class NotificationHelper {

    public static final String CHANNEL_ID = "due_reminders";
    private static final int REQUEST_CODE = 4107;
    private static final String PREFS = "daftari_notifications_meta";
    private static final String KEY_ASKED = "permission_asked";

    private NotificationHelper() {}

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.notif_channel_description));
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(channel);
    }

    public static boolean canPost(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    /** بأندرويد 13+ بنطلب الإذن مرة وحدة بس (أول مرة الرئيسية بتنفتح). */
    public static void requestPermissionOnce(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) return;
        SharedPreferences prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_ASKED, false)) return;
        prefs.edit().putBoolean(KEY_ASKED, true).apply();
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE);
    }

    /** إشعار لكل تذكير جديد. الضغط عليه بيفتح التطبيق (Splash، فبيمرّ من القفل). */
    @SuppressLint("MissingPermission") // canPost بيفحص الإذن
    public static void post(Context context, List<DueAlerts.Item> items) {
        if (items == null || items.isEmpty() || !canPost(context)) return;
        createChannel(context);
        Intent open = new Intent(context, SplashActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pending = PendingIntent.getActivity(context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        for (DueAlerts.Item item : items) {
            NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_bell)
                    .setColor(ContextCompat.getColor(context, R.color.plum))
                    .setContentTitle(NotificationText.title(context, item))
                    .setContentText(NotificationText.body(context, item))
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(NotificationText.body(context, item)))
                    .setContentIntent(pending)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
            nm.notify(item.key.hashCode(), b.build());
        }
    }
}
