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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * شاشة إنشاء حساب.
 * تجمع بين RegisterViewModel، إنشاء الحساب في Firebase Auth، إرسال رابط تفعيل الإيميل، وحفظ بيانات المستخدم في Firestore.
 */
public class RegisterActivity extends AppCompatActivity {

    private RegisterViewModel viewModel;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

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

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        bindViews();
        setupListeners();
        observe();
    }

    private void bindViews() {
        tilName = findViewById(R.id.tilRegisterName);
        tilEmail = findViewById(R.id.tilRegisterEmail);
        tilPassword = findViewById(R.id.tilRegisterPassword);
        tilConfirm = findViewById(R.id.tilRegisterConfirmPassword);

        etName = findViewById(R.id.etRegisterName);
        etEmail = findViewById(R.id.etRegisterEmail);
        etPassword = findViewById(R.id.etRegisterPassword);
        etConfirm = findViewById(R.id.etRegisterConfirmPassword);

        strengthBars = new View[]{
                findViewById(R.id.strengthBar1),
                findViewById(R.id.strengthBar2),
                findViewById(R.id.strengthBar3),
                findViewById(R.id.strengthBar4)
        };
        tvStrength = findViewById(R.id.tvStrength);
        btnSubmit = findViewById(R.id.btnRegisterSubmit);
        progress = findViewById(R.id.progressRegister);
    }

    private void setupListeners() {
        if (etPassword != null) {
            etPassword.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    viewModel.onPasswordChanged(s != null ? s.toString() : "");
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> attemptRegisterWithFirestoreAndVerification());
        }

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View btnGoToLogin = findViewById(R.id.btnGoToLogin);
        if (btnGoToLogin != null) {
            btnGoToLogin.setOnClickListener(v -> finish());
        }
    }

    private void attemptRegisterWithFirestoreAndVerification() {
        String shopName = text(etName);
        String email = text(etEmail);
        String password = rawText(etPassword);
        String confirmPassword = rawText(etConfirm);

        if (tilName != null) tilName.setError(null);
        if (tilEmail != null) tilEmail.setError(null);
        if (tilPassword != null) tilPassword.setError(null);
        if (tilConfirm != null) tilConfirm.setError(null);

        if (shopName.isEmpty()) {
            if (tilName != null) tilName.setError("الرجاء إدخال اسم المتجر أو الحساب");
            if (etName != null) etName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            if (tilEmail != null) tilEmail.setError("الرجاء إدخال البريد الإلكتروني");
            if (etEmail != null) etEmail.requestFocus();
            return;
        }

        if (password.isEmpty() || password.length() < 6) {
            if (tilPassword != null) tilPassword.setError("كلمة المرور يجب ألا تقل عن 6 أحرف");
            if (etPassword != null) etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            if (tilConfirm != null) tilConfirm.setError("كلمتا المرور غير متطابقتين");
            if (etConfirm != null) etConfirm.requestFocus();
            return;
        }

        // إبلاغ الـ ViewModel أيضاً في حال وجود منطق إضافي
        viewModel.register(shopName, email, password, confirmPassword);

        setLoadingState(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        setLoadingState(false);
                        Toast.makeText(this, resolveRegisterError(task.getException()), Toast.LENGTH_LONG).show();
                        return;
                    }

                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user == null) {
                        setLoadingState(false);
                        Toast.makeText(this, "تعذر إتمام إنشاء الحساب", Toast.LENGTH_LONG).show();
                        return;
                    }

                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(shopName)
                            .build();

                    user.updateProfile(profileUpdates).addOnCompleteListener(profileTask ->
                            saveUserToFirestoreAndSendVerification(user, shopName, email)
                    );
                });
    }

    private void saveUserToFirestoreAndSendVerification(FirebaseUser user, String shopName, String email) {
        String uid = user.getUid();
        long now = System.currentTimeMillis();

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("uid", uid);
        userMap.put("displayName", shopName);
        userMap.put("shopName", shopName);
        userMap.put("email", email);
        userMap.put("emailVerified", user.isEmailVerified());
        userMap.put("createdAt", now);
        userMap.put("updatedAt", now);

        db.collection("users")
                .document(uid)
                .set(userMap, SetOptions.merge())
                .addOnCompleteListener(dbTask -> sendVerificationAndGoToVerifyScreen(user, email));
    }

    private void sendVerificationAndGoToVerifyScreen(FirebaseUser user, String email) {
        user.sendEmailVerification()
                .addOnCompleteListener(verifyTask -> {
                    setLoadingState(false);
                    if (verifyTask.isSuccessful()) {
                        Toast.makeText(
                                this,
                                "تم إنشاء الحساب وإرسال رابط التفعيل إلى بريدك الإلكتروني",
                                Toast.LENGTH_LONG
                        ).show();
                    } else {
                        Toast.makeText(
                                this,
                                "تم إنشاء الحساب، يرجى إرسال أو تأكيد رابط التفعيل من البريد الإلكتروني",
                                Toast.LENGTH_LONG
                        ).show();
                    }

                    Intent intent = new Intent(this, VerifyEmailActivity.class);
                    intent.putExtra("USER_EMAIL", email);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
    }

    private void setLoadingState(boolean isLoading) {
        if (progress != null) {
            progress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (btnSubmit != null) {
            btnSubmit.setEnabled(!isLoading);
        }
    }

    private void observe() {
        viewModel.getLoading().observe(this, loading -> {
            boolean on = Boolean.TRUE.equals(loading);
            if (progress != null) progress.setVisibility(on ? View.VISIBLE : View.GONE);
            if (btnSubmit != null) btnSubmit.setEnabled(!on);
        });
        viewModel.getShopNameError().observe(this, res -> {
            if (tilName != null) tilName.setError(msg(res));
        });
        viewModel.getEmailError().observe(this, res -> {
            if (tilEmail != null) tilEmail.setError(msg(res));
        });
        viewModel.getPasswordError().observe(this, res -> {
            if (tilPassword != null) tilPassword.setError(msg(res));
        });
        viewModel.getConfirmError().observe(this, res -> {
            if (tilConfirm != null) tilConfirm.setError(msg(res));
        });
        viewModel.getStrengthScore().observe(this, this::renderStrengthBars);
        viewModel.getStrength().observe(this, this::renderStrengthLabel);
        viewModel.getMessage().observe(this, event -> {
            if (event == null) return;
            Integer res = event.getIfNotHandled();
            if (res != null) Toast.makeText(this, res, Toast.LENGTH_LONG).show();
        });
        viewModel.getRegistered().observe(this, event -> {
            if (event != null && Boolean.TRUE.equals(event.getIfNotHandled())) {
                Intent intent = new Intent(this, VerifyEmailActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void renderStrengthBars(Integer score) {
        if (strengthBars == null) return;
        int filled = score == null ? 0 : score;
        int color = ContextCompat.getColor(this, strengthColor(viewModel.getStrength().getValue()));
        int empty = ContextCompat.getColor(this, R.color.outline);
        for (int i = 0; i < strengthBars.length; i++) {
            if (strengthBars[i] != null) {
                strengthBars[i].setBackgroundTintList(ColorStateList.valueOf(i < filled ? color : empty));
            }
        }
    }

    private void renderStrengthLabel(PasswordStrength.Level level) {
        if (tvStrength == null) return;
        if (level == null || level == PasswordStrength.Level.NONE) {
            tvStrength.setVisibility(View.GONE);
            return;
        }
        int textRes;
        switch (level) {
            case WEAK:
                textRes = R.string.password_strength_weak;
                break;
            case MEDIUM:
                textRes = R.string.password_strength_medium;
                break;
            default:
                textRes = R.string.password_strength_strong;
                break;
        }
        tvStrength.setVisibility(View.VISIBLE);
        tvStrength.setText(textRes);
        tvStrength.setTextColor(ContextCompat.getColor(this, strengthColor(level)));
        renderStrengthBars(viewModel.getStrengthScore().getValue());
    }

    private static int strengthColor(PasswordStrength.Level level) {
        if (level == PasswordStrength.Level.STRONG) return R.color.payment_fg;
        if (level == PasswordStrength.Level.MEDIUM) return R.color.due_fg;
        return R.color.debt_fg;
    }

    private static String resolveRegisterError(Exception ex) {
        if (ex instanceof FirebaseAuthUserCollisionException) {
            return "هذا البريد الإلكتروني مسجل مسبقاً";
        }
        if (ex instanceof FirebaseAuthWeakPasswordException) {
            return "كلمة المرور ضعيفة، يرجى اختيار كلمة مرور أقوى";
        }
        if (ex != null && ex.getMessage() != null && !ex.getMessage().trim().isEmpty()) {
            return "تعذر إنشاء الحساب: " + ex.getMessage();
        }
        return "تعذر إنشاء الحساب، حاولي مرة أخرى";
    }

    private String msg(Integer res) {
        return res == null ? null : getString(res);
    }

    private static String text(TextInputEditText et) {
        return (et == null || et.getText() == null) ? "" : et.getText().toString().trim();
    }

    private static String rawText(TextInputEditText et) {
        return (et == null || et.getText() == null) ? "" : et.getText().toString();
    }
}