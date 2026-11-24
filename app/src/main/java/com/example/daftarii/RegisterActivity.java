package com.example.daftarii;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.daftarii.auth.RegisterViewModel;
import com.example.daftarii.logic.PasswordStrength;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** شاشة إنشاء حساب. */
public class RegisterActivity extends AppCompatActivity {

    private RegisterViewModel viewModel;

    private TextInputLayout tilName, tilEmail, tilPassword, tilConfirm;
    private TextInputEditText etName, etEmail, etPassword, etConfirm;
    private View[] strengthBars;
    private TextView tvStrength;
    private MaterialButton btnSubmit;
    private LinearProgressIndicator progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        tilName = findViewById(R.id.tilRegisterName);
        tilEmail = findViewById(R.id.tilRegisterEmail);
        tilPassword = findViewById(R.id.tilRegisterPassword);
        tilConfirm = findViewById(R.id.tilRegisterConfirmPassword);
        etName = findViewById(R.id.etRegisterName);
        etEmail = findViewById(R.id.etRegisterEmail);
        etPassword = findViewById(R.id.etRegisterPassword);
        etConfirm = findViewById(R.id.etRegisterConfirmPassword);
        strengthBars = new View[]{
                findViewById(R.id.strengthBar1), findViewById(R.id.strengthBar2),
                findViewById(R.id.strengthBar3), findViewById(R.id.strengthBar4)};
        tvStrength = findViewById(R.id.tvStrength);
        btnSubmit = findViewById(R.id.btnRegisterSubmit);
        progress = findViewById(R.id.progressRegister);

        etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.onPasswordChanged(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnSubmit.setOnClickListener(v -> viewModel.register(
                text(etName), text(etEmail), rawText(etPassword), rawText(etConfirm)));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnGoToLogin).setOnClickListener(v -> finish());

        observe();
    }

    private void observe() {
        viewModel.getLoading().observe(this, loading -> {
            boolean on = Boolean.TRUE.equals(loading);
            progress.setVisibility(on ? View.VISIBLE : View.GONE);
            btnSubmit.setEnabled(!on);
        });
        viewModel.getShopNameError().observe(this, res -> tilName.setError(msg(res)));
        viewModel.getEmailError().observe(this, res -> tilEmail.setError(msg(res)));
        viewModel.getPasswordError().observe(this, res -> tilPassword.setError(msg(res)));
        viewModel.getConfirmError().observe(this, res -> tilConfirm.setError(msg(res)));
        viewModel.getStrengthScore().observe(this, this::renderStrengthBars);
        viewModel.getStrength().observe(this, this::renderStrengthLabel);
        viewModel.getMessage().observe(this, event -> {
            Integer res = event.getIfNotHandled();
            if (res != null) Toast.makeText(this, res, Toast.LENGTH_LONG).show();
        });
        viewModel.getRegistered().observe(this, event -> {
            if (Boolean.TRUE.equals(event.getIfNotHandled())) {
                Intent intent = new Intent(this, VerifyEmailActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void renderStrengthBars(Integer score) {
        int filled = score == null ? 0 : score;
        int color = ContextCompat.getColor(this, strengthColor(viewModel.getStrength().getValue()));
        int empty = ContextCompat.getColor(this, R.color.outline);
        for (int i = 0; i < strengthBars.length; i++) {
            strengthBars[i].setBackgroundTintList(ColorStateList.valueOf(i < filled ? color : empty));
        }
    }

    private void renderStrengthLabel(PasswordStrength.Level level) {
        if (level == null || level == PasswordStrength.Level.NONE) {
            tvStrength.setVisibility(View.GONE);
            return;
        }
        int textRes;
        switch (level) {
            case WEAK:   textRes = R.string.password_strength_weak; break;
            case MEDIUM: textRes = R.string.password_strength_medium; break;
            default:     textRes = R.string.password_strength_strong; break;
        }
        tvStrength.setVisibility(View.VISIBLE);
        tvStrength.setText(textRes);
        tvStrength.setTextColor(ContextCompat.getColor(this, strengthColor(level)));
        // اللون تغيّر: نعيد رسم الخطوط
        renderStrengthBars(viewModel.getStrengthScore().getValue());
    }

    private static int strengthColor(PasswordStrength.Level level) {
        if (level == PasswordStrength.Level.STRONG) return R.color.payment_fg;
        if (level == PasswordStrength.Level.MEDIUM) return R.color.due_fg;
        return R.color.debt_fg;
    }

    private String msg(Integer res) {
        return res == null ? null : getString(res);
    }

    private static String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private static String rawText(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString();
    }
}
