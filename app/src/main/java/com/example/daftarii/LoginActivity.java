package com.example.daftarii;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.daftarii.auth.LoginViewModel;
import com.example.daftarii.logic.StartRouter;
import com.example.daftarii.util.LanguageManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** شاشة تسجيل الدخول. كل المنطق بـ LoginViewModel، وهون عرض بس. */
public class LoginActivity extends AppCompatActivity {

    private LoginViewModel viewModel;

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialCheckBox cbRememberMe;
    private MaterialButton btnLogin, btnGuest;
    private LinearProgressIndicator progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        tilEmail = findViewById(R.id.tilLoginEmail);
        tilPassword = findViewById(R.id.tilLoginPassword);
        etEmail = findViewById(R.id.etLoginEmail);
        etPassword = findViewById(R.id.etLoginPassword);
        cbRememberMe = findViewById(R.id.cbRememberMe);
        btnLogin = findViewById(R.id.btnLogin);
        btnGuest = findViewById(R.id.btnGuest);
        progress = findViewById(R.id.progressLogin);

        cbRememberMe.setChecked(viewModel.isRememberMe());

        btnLogin.setOnClickListener(v -> viewModel.login(
                text(etEmail), rawText(etPassword), cbRememberMe.isChecked()));
        btnGuest.setOnClickListener(v -> viewModel.loginAsGuest());
        findViewById(R.id.btnForgotPassword).setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));
        findViewById(R.id.btnGoToRegister).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        // تغيير اللغة: أندرويد بيعيد فتح الشاشة باللغة الجديدة لحاله
        findViewById(R.id.btnLanguage).setOnClickListener(v -> LanguageManager.toggle());

        observe();

        if (savedInstanceState == null) {
            viewModel.checkExistingSession();
        }
    }

    private void observe() {
        viewModel.getLoading().observe(this, loading -> {
            boolean on = Boolean.TRUE.equals(loading);
            progress.setVisibility(on ? View.VISIBLE : View.GONE);
            btnLogin.setEnabled(!on);
            btnGuest.setEnabled(!on);
        });
        viewModel.getEmailError().observe(this, res ->
                tilEmail.setError(res == null ? null : getString(res)));
        viewModel.getPasswordError().observe(this, res ->
                tilPassword.setError(res == null ? null : getString(res)));
        viewModel.getMessage().observe(this, event -> {
            Integer res = event.getIfNotHandled();
            if (res != null) Toast.makeText(this, res, Toast.LENGTH_LONG).show();
        });
        viewModel.getNavigation().observe(this, event -> {
            StartRouter.Destination d = event.getIfNotHandled();
            if (d != null) navigate(d);
        });
    }

    private void navigate(StartRouter.Destination destination) {
        Class<?> target;
        switch (destination) {
            case VERIFY_EMAIL:
                target = VerifyEmailActivity.class;
                break;
            case HOME_GUEST:
                // الزائر ما إله PIN ولا بصمة: على الرئيسية مباشرة
                target = MainActivity.class;
                break;
            case LOCK:
            default:
                target = LockActivity.class;
                break;
        }
        Intent intent = new Intent(this, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /** نص الحقل بدون مسافات بالأطراف (للإيميل). */
    private static String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    /** نص الحقل متل ما هو، بدون trim (لكلمة السر). */
    private static String rawText(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString();
    }
}
