package com.example.daftarii;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import com.example.daftarii.auth.AuthRepository;
import com.example.daftarii.logic.GuestTrial;

/**
 * شريط الزائر فوق الرئيسية (view_guest_banner). كل منطقه هون عشان الرئيسية
 * (شغل مروة ومرام) ما تحتاج غير سطر واحد: {@code if (GuestBanner.setup(this)) return;}
 */
public final class GuestBanner {

    private GuestBanner() {}

    /**
     * بيعرض الشريط للزائر مع الأيام الضايلة، وبيخفيه لغيره.
     * إذا الزائر خلصت تجربته، بيوديه على شاشة التحويل وبيرجّع true (الرئيسية لازم توقف).
     */
    public static boolean setup(Activity activity) {
        View banner = activity.findViewById(R.id.guestBanner);
        AuthRepository repo = new AuthRepository();
        if (!repo.isGuest()) {
            if (banner != null) banner.setVisibility(View.GONE);
            return false;
        }

        long now = System.currentTimeMillis();
        long startedAt = repo.accountCreatedAt();
        if (GuestTrial.isExpired(startedAt, now)) {
            Intent intent = new Intent(activity, GuestUpgradeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
            activity.finish();
            return true;
        }

        if (banner == null) return false;
        int days = GuestTrial.daysLeft(startedAt, now);
        TextView tvDays = banner.findViewById(R.id.tvGuestBannerDays);
        tvDays.setText(days <= 1
                ? activity.getString(R.string.guest_banner_last_day)
                : activity.getString(R.string.guest_banner_days, days));
        banner.setVisibility(View.VISIBLE);
        banner.setOnClickListener(v ->
                activity.startActivity(new Intent(activity, GuestUpgradeActivity.class)));
        return false;
    }
}
