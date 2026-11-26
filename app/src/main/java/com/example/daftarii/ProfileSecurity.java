package com.example.daftarii;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.daftarii.auth.AuthRepository;
import com.example.daftarii.auth.BiometricHelper;
import com.example.daftarii.auth.PinStore;
import com.example.daftarii.logic.AuthError;
import com.example.daftarii.logic.GuestTrial;
import com.example.daftarii.notifications.NotificationScheduler;
import com.example.daftarii.notifications.NotificationStore;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * قسم الأمان والحساب بصفحة حسابي (سندس). صفحة حسابي نفسها (شغل مروة) بتنادي:
 * {@code ProfileSecurity.setup(this)} بـ onCreate، و {@code ProfileSecurity.refresh(this)} بـ onResume.
 *
 * - شارة الحساب: مفعّل، أو زائر مع الأيام الضايلة
 * - البصمة: بتشتغل بس إذا في PIN والجوال فيه بصمة
 * - تغيير الـ PIN: CreatePinActivity بوضع التغيير
 * - تغيير كلمة السر: رابط على الإيميل
 * - الإشعارات: تشغيل/إيقاف الفحص اليومي
 * - تسجيل الخروج: تأكيد (وتحذير للزائر)، وبيمسح الـ PIN والإشعارات المحلية
 * الزائر ما إله PIN ولا بصمة ولا كلمة سر، فقسم الأمان بيختفي عنده.
 */
public final class ProfileSecurity {

    private ProfileSecurity() {}

    public static void setup(@NonNull Activity a) {
        View biometricRow = a.findViewById(R.id.rowBiometricLogin);
        CompoundButton biometric = a.findViewById(R.id.switchBiometric);
        CompoundButton notifications = a.findViewById(R.id.switchNotifications);

        if (biometric != null) {
            biometric.setOnCheckedChangeListener((b, checked) -> {
                if (isSyncing(b)) return; // تغيير من الكود، مش من المستخدم
                String uid = new AuthRepository().currentUid();
                if (uid != null) new PinStore(a).setBiometricEnabled(uid, checked);
            });
        }
        if (biometricRow != null && biometric != null) {
            biometricRow.setOnClickListener(v -> {
                if (biometric.isEnabled()) biometric.toggle();
            });
        }

        click(a, R.id.rowPinCode, v -> a.startActivity(new Intent(a, CreatePinActivity.class)
                .putExtra(CreatePinActivity.EXTRA_CHANGE_PIN, true)));
        click(a, R.id.rowChangePassword, v -> confirmPasswordReset(a));

        if (notifications != null) {
            notifications.setOnCheckedChangeListener((b, checked) -> {
                if (!isSyncing(b)) NotificationScheduler.setEnabled(a, checked);
            });
            click(a, R.id.rowPushNotifications, v -> notifications.toggle());
        }

        click(a, R.id.btnLogout, v -> confirmLogout(a));
        // تصدير PDF بكل الزباين (من قسم البيانات)
        click(a, R.id.rowExportPdf, v -> com.example.daftarii.reports.CustomersReportPdf.export(a));
        refresh(a);
    }

    /** بـ onResume: الحالة ممكن تكون تغيّرت (PIN جديد، زائر حوّل لحساب...). */
    public static void refresh(@NonNull Activity a) {
        AuthRepository repo = new AuthRepository();
        boolean guest = repo.isGuest();
        String uid = repo.currentUid();

        // شارة الحساب
        TextView badge = a.findViewById(R.id.tvEmailStatusBadge);
        if (badge != null) {
            if (guest) {
                int days = GuestTrial.daysLeft(repo.accountCreatedAt(), System.currentTimeMillis());
                badge.setText(a.getString(R.string.profile_badge_guest, Math.max(days, 0)));
            } else {
                badge.setText(repo.isEmailVerified()
                        ? R.string.email_verified_status : R.string.profile_badge_not_verified);
            }
        }

        // قسم الأمان: مش للزائر
        View security = a.findViewById(R.id.cardSecuritySection);
        if (security != null) security.setVisibility(guest ? View.GONE : View.VISIBLE);

        CompoundButton biometric = a.findViewById(R.id.switchBiometric);
        View biometricRow = a.findViewById(R.id.rowBiometricLogin);
        if (biometric != null && uid != null && !guest) {
            PinStore pins = new PinStore(a);
            boolean available = pins.hasPin(uid) && BiometricHelper.canUse(a);
            biometric.setEnabled(available);
            setSilently(biometric, available && pins.isBiometricEnabled(uid));
            if (biometricRow != null) biometricRow.setAlpha(available ? 1f : 0.5f);
        }

        CompoundButton notifications = a.findViewById(R.id.switchNotifications);
        if (notifications != null) setSilently(notifications, NotificationScheduler.isEnabled(a));
    }

    /** "تعديل" جنب اسم المحل: نافذة فيها الاسم الحالي، وبعد الحفظ onDone بيحدّث الصفحة. */
    public static void editShopName(@NonNull Activity a, @NonNull Runnable onDone) {
        AuthRepository repo = new AuthRepository();
        com.google.android.material.textfield.TextInputLayout til =
                new com.google.android.material.textfield.TextInputLayout(a);
        com.google.android.material.textfield.TextInputEditText input =
                new com.google.android.material.textfield.TextInputEditText(til.getContext());
        input.setSingleLine(true);
        String current = repo.currentDisplayName();
        if (current != null) input.setText(current);
        til.setHint(a.getString(R.string.auth_label_shop_name));
        til.addView(input);
        int pad = Math.round(20 * a.getResources().getDisplayMetrics().density);
        android.widget.FrameLayout box = new android.widget.FrameLayout(a);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(til);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(a)
                .setTitle(R.string.profile_edit_name_title)
                .setView(box)
                .setPositiveButton(R.string.action_save, null)
                .setNegativeButton(R.string.action_cancel, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String name = input.getText() != null ? input.getText().toString().trim() : "";
                    Integer err = com.example.daftarii.auth.AuthMessages.shopNameError(
                            com.example.daftarii.logic.Validators.shopName(name));
                    if (err != null) {
                        til.setError(a.getString(err));
                        return;
                    }
                    repo.updateShopName(name, new AuthRepository.Callback<Void>() {
                        @Override
                        public void onSuccess(Void ignored) {
                            dialog.dismiss();
                            onDone.run();
                        }

                        @Override
                        public void onError(@NonNull AuthError error) {
                            til.setError(a.getString(com.example.daftarii.auth.AuthMessages.of(error)));
                        }
                    });
                }));
        dialog.show();
    }

    private static void confirmPasswordReset(Activity a) {
        AuthRepository repo = new AuthRepository();
        String email = repo.currentEmail();
        if (email == null) return;
        new MaterialAlertDialogBuilder(a)
                .setTitle(R.string.profile_change_password_title)
                .setMessage(a.getString(R.string.profile_change_password_message, email))
                .setPositiveButton(R.string.profile_change_password_send, (d, w) ->
                        repo.sendPasswordReset(email, new AuthRepository.Callback<Void>() {
                            @Override
                            public void onSuccess(Void ignored) {
                                Toast.makeText(a, R.string.profile_change_password_sent, Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onError(@NonNull AuthError error) {
                                Toast.makeText(a, com.example.daftarii.auth.AuthMessages.of(error),
                                        Toast.LENGTH_LONG).show();
                            }
                        }))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    /** تسجيل الخروج. الزائر بيتحذّر إنه بياناته رح تضيع، وبيقدر يعمل حساب بدل. */
    private static void confirmLogout(Activity a) {
        if (new AuthRepository().isGuest()) {
            new MaterialAlertDialogBuilder(a)
                    .setTitle(R.string.profile_logout_guest_title)
                    .setMessage(R.string.profile_logout_guest_message)
                    .setPositiveButton(R.string.guest_banner_action, (d, w) ->
                            a.startActivity(new Intent(a, GuestUpgradeActivity.class)))
                    .setNegativeButton(R.string.profile_logout_anyway, (d, w) -> logout(a))
                    .setNeutralButton(R.string.action_cancel, null)
                    .show();
            return;
        }
        new MaterialAlertDialogBuilder(a)
                .setTitle(R.string.profile_logout_title)
                .setMessage(R.string.profile_logout_message)
                .setPositiveButton(R.string.profile_logout_confirm, (d, w) -> logout(a))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    /** بيمسح الـ PIN والإشعارات المحلية، وبيوقف الفحص اليومي، وبيطلع، وبيرجع على الدخول. */
    static void logout(Activity a) {
        new PinStore(a).clear();
        new NotificationStore(a).clear();
        NotificationScheduler.cancel(a);
        new AuthRepository().signOut();
        Intent intent = new Intent(a, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        a.startActivity(intent);
        a.finish();
    }

    /** بنغيّر المفتاح من الكود بدون ما ينحسب كأنه المستخدم غيّره. */
    private static void setSilently(CompoundButton b, boolean checked) {
        b.setTag(R.id.switchBiometric, Boolean.TRUE);
        b.setChecked(checked);
        b.setTag(R.id.switchBiometric, null);
    }

    private static boolean isSyncing(CompoundButton b) {
        return Boolean.TRUE.equals(b.getTag(R.id.switchBiometric));
    }

    private static void click(Activity a, int id, View.OnClickListener l) {
        View v = a.findViewById(id);
        if (v != null) v.setOnClickListener(l);
    }
}
