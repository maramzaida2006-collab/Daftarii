package com.example.daftarii.notifications;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import com.example.daftarii.NotificationsActivity;
import com.example.daftarii.logic.DueAlerts;

/**
 * جرس الإشعارات بالرئيسية (شغل مروة/مرام). كل منطقه هون، فالرئيسية بتحتاج سطر واحد:
 * {@code NotificationBell.setup(this, btnNotifications, viewUnreadDot);}
 *
 * النقطة الحمرا بتبيّن بس إذا في إشعار مش مقروء، فلو شفتيهم و"قراءة الكل" ما بترجع
 * لحد ما يصير تذكير جديد.
 */
public final class NotificationBell {

    private NotificationBell() {}

    public static void setup(Activity activity, View bell, View unreadDot) {
        NotificationScheduler.schedule(activity);
        NotificationHelper.requestPermissionOnce(activity);
        if (bell != null) {
            bell.setOnClickListener(v ->
                    activity.startActivity(new Intent(activity, NotificationsActivity.class)));
        }
        // بنعرض المحفوظ فوراً، وبعدين بنفحص الزباين وبنحدّث
        showDot(unreadDot, DueAlerts.unreadCount(NotificationSync.saved(activity)));
        NotificationSync.run(activity, (all, added) -> {
            if (activity.isFinishing()) return;
            // التطبيق مفتوح: النقطة بتكفي. إشعار الجوال بيجي من الفحص اليومي بالخلفية
            showDot(unreadDot, DueAlerts.unreadCount(all));
        });
    }

    private static void showDot(View dot, int unread) {
        if (dot != null) dot.setVisibility(unread > 0 ? View.VISIBLE : View.GONE);
    }
}
