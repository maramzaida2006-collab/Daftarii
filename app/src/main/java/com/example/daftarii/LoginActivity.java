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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * شاشة تسجيل الدخول.
 * تدعم التحقق من تفعيل البريد الإلكتروني، إرسال رابط التفعيل، وتسجيل/تحديث بيانات المستخدم في Firestore.
 */
public class LoginActivity extends AppCompatActivity {

    private LoginViewModel viewModel;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialCheckBox cbRememberMe;
    private MaterialButton btnLogin, btnGuest, btnForgotPassword, btnGoToRegister;
    private View btnLanguage;
    private LinearProgressIndicator progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        bindViews();
        bindInitialState();
        setupListeners();
        observe();
    }

    private void bindViews() {
        tilEmail = findViewById(R.id.tilLoginEmail);
        tilPassword = findViewById(R.id.tilLoginPassword);
        etEmail = findViewById(R.id.etLoginEmail);
        etPassword = findViewById(R.id.etLoginPassword);
        cbRememberMe = findViewById(R.id.cbRememberMe);
        btnLogin = findViewById(R.id.btnLogin);
        btnGuest = findViewById(R.id.btnGuest);
        btnForgotPassword = findViewById(R.id.btnForgotPassword);
        btnGoToRegister = findViewById(R.id.btnGoToRegister);
        btnLanguage = findViewById(R.id.btnLanguage);
        progress = findViewById(R.id.progressLogin);
    }

    private void bindInitialState() {
        if (cbRememberMe != null && viewModel != null) {
            cbRememberMe.setChecked(viewModel.isRememberMe());
        }
    }

    private void setupListeners() {
        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> attemptLoginWithEmailVerificationAndFirestore());
        }

        if (btnGuest != null) {
            btnGuest.setOnClickListener(v -> viewModel.loginAsGuest());
        }

        if (btnForgotPassword != null) {
            btnForgotPassword.setOnClickListener(v ->
                    startActivity(new Intent(this, ForgotPasswordActivity.class)));
        }

        if (btnGoToRegister != null) {
            btnGoToRegister.setOnClickListener(v ->
                    startActivity(new Intent(this, RegisterActivity.class)));
        }

        if (btnLanguage != null) {
            btnLanguage.setOnClickListener(v -> LanguageManager.toggle());
        }
    }

    private void attemptLoginWithEmailVerificationAndFirestore() {
        String email = text(etEmail);
        String password = rawText(etPassword);

        if (tilEmail != null) tilEmail.setError(null);
        if (tilPassword != null) tilPassword.setError(null);

        if (email.isEmpty()) {
            if (tilEmail != null) {
                tilEmail.setError("الرجاء إدخال البريد الإلكتروني");
            }
            if (etEmail != null) etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            if (tilPassword != null) {
                tilPassword.setError("الرجاء إدخال كلمة المرور");
            }
            if (etPassword != null) etPassword.requestFocus();
            return;
        }

        setLoadingState(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        setLoadingState(false);
                        Toast.makeText(this, resolveAuthErrorMessage(task.getException()), Toast.LENGTH_LONG).show();
                        return;
                    }

                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user == null) {
                        setLoadingState(false);
                        Toast.makeText(this, "تعذر العثور على بيانات المستخدم", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // إعادة تحميل بيانات المستخدم للتأكد من أحدث حالة لتفعيل البريد
                    user.reload().addOnCompleteListener(reloadTask -> {
                        if (!user.isEmailVerified()) {
                            sendVerificationEmailAndRedirect(user);
                            return;
                        }
                        syncUserToFirestoreAndProceed(user);
                    });
                });
    }

    private void sendVerificationEmailAndRedirect(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(verifyTask -> {
                    setLoadingState(false);
                    if (verifyTask.isSuccessful()) {
                        Toast.makeText(
                                this,
                                "حسابك غير مفعّل. تم إرسال رابط التفعيل إلى بريدك الإلكتروني",
                                Toast.LENGTH_LONG
                        ).show();
                    } else {
                        Toast.makeText(
                                this,
                                "يرجى تفعيل بريدك الإلكتروني أولاً من الرابط المرسل إليك",
                                Toast.LENGTH_LONG
                        ).show();
                    }

                    Intent intent = new Intent(this, VerifyEmailActivity.class);
                    intent.putExtra("USER_EMAIL", user.getEmail());
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
    }

    private void syncUserToFirestoreAndProceed(FirebaseUser user) {
        String uid = user.getUid();
        long now = System.currentTimeMillis();
        boolean remember = cbRememberMe != null && cbRememberMe.isChecked();

        Map<String, Object> userDoc = new HashMap<>();
        userDoc.put("uid", uid);
        userDoc.put("email", user.getEmail() != null ? user.getEmail() : "");
        userDoc.put("emailVerified", user.isEmailVerified());
        userDoc.put("isAnonymous", user.isAnonymous());
        userDoc.put("rememberMe", remember);
        userDoc.put("lastLoginAt", now);

        if (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
            userDoc.put("displayName", user.getDisplayName().trim());
        }

        db.collection("users")
                .document(uid)
                .set(userDoc, SetOptions.merge())
                .addOnCompleteListener(dbTask -> {
                    setLoadingState(false);
                    if (!dbTask.isSuccessful()) {
                        Toast.makeText(
                                this,
                                "تم تسجيل الدخول، وسيتم مزامنة بيانات الحساب مع Firestore تلقائياً",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    Intent intent = new Intent(this, LockActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
    }

    private void setLoadingState(boolean isLoading) {
        if (progress != null) {
            progress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (btnLogin != null) {
            btnLogin.setEnabled(!isLoading);
        }
        if (btnGuest != null) {
            btnGuest.setEnabled(!isLoading);
        }
    }

    private void observe() {
        viewModel.getLoading().observe(this, loading -> setLoadingState(Boolean.TRUE.equals(loading)));

        viewModel.getEmailError().observe(this, res -> {
            if (tilEmail != null) {
                tilEmail.setError(res == null ? null : getString(res));
            }
        });

        viewModel.getPasswordError().observe(this, res -> {
            if (tilPassword != null) {
                tilPassword.setError(res == null ? null : getString(res));
            }
        });

        viewModel.getMessage().observe(this, event -> {
            if (event == null) return;
            Integer res = event.getIfNotHandled();
            if (res != null) {
                Toast.makeText(this, res, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getNavigation().observe(this, event -> {
            if (event == null) return;
            StartRouter.Destination d = event.getIfNotHandled();
            if (d != null) {
                navigate(d);
            }
        });
    }

    private void navigate(StartRouter.Destination destination) {
        Class<?> target;
        switch (destination) {
            case VERIFY_EMAIL:
                target = VerifyEmailActivity.class;
                break;
            case HOME_GUEST:
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

    public static String resolveAuthErrorMessage(Exception exception) {
        if (exception == null) {
            return "فشل تسجيل الدخول";
        }
        if (exception instanceof FirebaseAuthInvalidUserException) {
            String errorCode = ((FirebaseAuthInvalidUserException) exception).getErrorCode();
            if ("ERROR_USER_DISABLED".equals(errorCode)) {
                return "تم تعطيل حسابك من قبل الإدارة";
            }
            return "الحساب غير موجود أو تم تعطيله";
        }
        if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            return "البريد الإلكتروني أو كلمة المرور غير صحيحة";
        }
        String msg = exception.getMessage();
        return (msg != null && !msg.trim().isEmpty()) ? "فشل الدخول: " + msg : "فشل تسجيل الدخول";
    }

    private static String text(TextInputEditText et) {
        return (et == null || et.getText() == null) ? "" : et.getText().toString().trim();
    }

    private static String rawText(TextInputEditText et) {
        return (et == null || et.getText() == null) ? "" : et.getText().toString();
    }
}