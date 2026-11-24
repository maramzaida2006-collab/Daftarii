package com.example.daftarii;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.daftarii.auth.BiometricHelper;
import com.example.daftarii.auth.LockViewModel;
import com.example.daftarii.util.PinPad;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * شاشة القفل: بتفتح كل مرة الحساب المفعّل بيفتح التطبيق.
 * PIN من 4 أرقام أو البصمة. إذا ما في PIN لسا، بتحوّل على إنشاء PIN.
 * كل المنطق بـ LockViewModel.
 */
public class LockActivity extends AppCompatActivity {

    private LockViewModel viewModel;
    private ViewGroup dots;
    private TextView tvMessage;
    private View keypad;
    private int dotOn, dotOff;
    private boolean biometricEnabled;
    private BiometricPrompt biometricPrompt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock);

        viewModel = new ViewModelProvider(this).get(LockViewModel.class);

        dots = findViewById(R.id.pinDots);
        tvMessage = findViewById(R.id.tvLockMessage);
        keypad = findViewById(R.id.pinKeypad);
        dotOn = ContextCompat.getColor(this, R.color.lavender);
        dotOff = ContextCompat.getColor(this, R.color.lock_dot_off);

        // اسم المحل وأول حرف منه
        String name = viewModel.shopName();
        if (name == null) name = getString(R.string.app_name);
        ((TextView) findViewById(R.id.tvLockShopName)).setText(name);
        ((TextView) findViewById(R.id.tvLockAvatar)).setText(firstLetter(name));

        PinPad.bind(keypad, new PinPad.Listener() {
            @Override
            public void onDigit(char digit) {
                viewModel.onDigit(digit);
            }

            @Override
            public void onDelete() {
                viewModel.onDelete();
            }
        });

        biometricEnabled = viewModel.isBiometricEnabled();
        // مكتبة البصمة بتنصح نعمل الـ prompt مرة وحدة بـ onCreate
        biometricPrompt = new BiometricPrompt(this, ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        viewModel.onBiometricSuccess();
                    }
                    // الغلط والإلغاء: النظام بيعرض الرسالة بنافذته، والمستخدم بيقدر يكتب الـ PIN
                });
        findViewById(R.id.groupBiometric).setVisibility(biometricEnabled ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnLockBiometric).setOnClickListener(v -> showBiometricPrompt());
        findViewById(R.id.btnLockOtherAccount).setOnClickListener(v -> confirmOtherAccount());

        observe();
        viewModel.start();

        // أول ما تفتح الشاشة: إذا البصمة مفعّلة، بنطلبها مباشرة
        if (savedInstanceState == null && biometricEnabled) {
            keypad.post(this::showBiometricPrompt);
        }
    }

    private void observe() {
        viewModel.getFilled().observe(this, n -> {
            int count = n == null ? 0 : n;
            PinPad.showDots(dots, count, dotOn, dotOff);
            dots.setContentDescription(getString(R.string.pin_entered, count));
        });
        viewModel.getMessage().observe(this, m -> {
            if (m == null) {
                tvMessage.setText(null);
            } else if (m.wrongPin) {
                tvMessage.setText(m.value == 1
                        ? getString(R.string.lock_wrong_pin_last)
                        : getString(R.string.lock_wrong_pin, m.value));
            } else {
                tvMessage.setText(getString(R.string.lock_locked_out, m.value));
            }
        });
        viewModel.getLocked().observe(this, locked ->
                PinPad.setEnabled(keypad, !Boolean.TRUE.equals(locked)));
        viewModel.getShake().observe(this, event -> {
            if (event.getIfNotHandled() != null) PinPad.shake(dots);
        });
        viewModel.getNavigation().observe(this, event -> {
            LockViewModel.Nav nav = event.getIfNotHandled();
            if (nav == null) return;
            switch (nav) {
                case UNLOCKED:
                    goTo(MainActivity.class);
                    break;
                case CREATE_PIN:
                    goTo(CreatePinActivity.class);
                    break;
                case LOGIN:
                default:
                    goTo(LoginActivity.class);
                    break;
            }
        });
    }

    private void showBiometricPrompt() {
        if (isFinishing() || !biometricEnabled) return;
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_prompt_title))
                .setSubtitle(getString(R.string.biometric_prompt_subtitle))
                .setNegativeButtonText(getString(R.string.biometric_prompt_use_pin))
                .setAllowedAuthenticators(BiometricHelper.AUTHENTICATORS)
                .build();
        biometricPrompt.authenticate(info);
    }

    private void confirmOtherAccount() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.lock_other_account_title)
                .setMessage(R.string.lock_other_account_message)
                .setPositiveButton(R.string.lock_other_account_confirm,
                        (d, w) -> viewModel.useAnotherAccount())
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    /** أول حرف من الاسم، حتى لو كان حرف من أكتر من char (متل بعض الإيموجي). */
    private static String firstLetter(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return "";
        int end = trimmed.offsetByCodePoints(0, 1);
        return trimmed.substring(0, end).toUpperCase();
    }

    private void goTo(Class<?> target) {
        Intent intent = new Intent(this, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
