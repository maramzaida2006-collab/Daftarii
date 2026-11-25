package com.example.daftarii;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.daftarii.auth.VerifyEmailViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.Locale;

/** شاشة "فعّل بريدك الإلكتروني": بتطلع بعد إنشاء حساب، أو دخول بإيميل لسا مش مفعّل. */
public class VerifyEmailActivity extends AppCompatActivity {

    private VerifyEmailViewModel viewModel;

    private MaterialButton btnContinue, btnResend, btnChangeEmail;
    private LinearProgressIndicator progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_email);

        viewModel = new ViewModelProvider(this).get(VerifyEmailViewModel.class);

        TextView tvEmail = findViewById(R.id.tvEmail);
        btnContinue = findViewById(R.id.btnContinue);
        btnResend = findViewById(R.id.btnResend);
        btnChangeEmail = findViewById(R.id.btnChangeEmail);
        progress = findViewById(R.id.progressVerify);

        tvEmail.setText(viewModel.getEmail());

        btnContinue.setOnClickListener(v -> viewModel.checkVerified());
        btnResend.setOnClickListener(v -> viewModel.resend());
        btnChangeEmail.setOnClickListener(v -> viewModel.changeEmail());
        findViewById(R.id.btnOpenMail).setOnClickListener(v -> openMailApp());
        findViewById(R.id.btnBack).setOnClickListener(v -> viewModel.back());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                viewModel.back();
            }
        });

        observe();
    }

    private void observe() {
        viewModel.getLoading().observe(this, loading -> {
            boolean on = Boolean.TRUE.equals(loading);
            progress.setVisibility(on ? View.VISIBLE : View.GONE);
            btnContinue.setEnabled(!on);
            btnChangeEmail.setEnabled(!on);
        });
        viewModel.getResendSeconds().observe(this, seconds -> {
            int s = seconds == null ? 0 : seconds;
            if (s > 0) {
                String time = String.format(Locale.US, "%02d:%02d", s / 60, s % 60);
                btnResend.setText(getString(R.string.verify_resend_in, time));
                btnResend.setEnabled(false);
            } else {
                btnResend.setText(R.string.verify_resend);
                btnResend.setEnabled(true);
            }
        });
        viewModel.getMessage().observe(this, event -> {
            Integer res = event.getIfNotHandled();
            if (res != null) Toast.makeText(this, res, Toast.LENGTH_LONG).show();
        });
        viewModel.getNavigation().observe(this, event -> {
            VerifyEmailViewModel.Nav nav = event.getIfNotHandled();
            if (nav == null) return;
            Class<?> target;
            switch (nav) {
                case VERIFIED:
                    // أول دخول بعد التفعيل: شاشة القفل بتطلب إنشاء PIN
                    target = LockActivity.class;
                    break;
                case BACK_TO_REGISTER:
                    target = RegisterActivity.class;
                    break;
                case BACK_TO_GUEST_UPGRADE:
                    // زائر حوّل وبدّه يغيّر الإيميل: رجع زائر وبنرجعه على شاشة التحويل
                    target = GuestUpgradeActivity.class;
                    break;
                case BACK_TO_LOGIN:
                default:
                    target = LoginActivity.class;
                    break;
            }
            Intent intent = new Intent(this, target);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void openMailApp() {
        Intent intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_EMAIL);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.verify_no_mail_app, Toast.LENGTH_SHORT).show();
        }
    }
}
