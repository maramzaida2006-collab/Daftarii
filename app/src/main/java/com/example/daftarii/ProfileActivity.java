package com.example.daftarii;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "daftarii_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private SharedPreferences prefs;

    private FrameLayout btnEditProfile;
    private TextView tvProfileInitial, tvProfileStoreName, tvProfileEmail, tvEmailStatusBadge, tvArchiveCount;

    private LinearLayout rowBiometricLogin, rowPinCode, rowChangePassword;
    private LinearLayout rowArchive, rowExportPdf, rowDarkMode, rowPushNotifications;

    private SwitchMaterial switchBiometric, switchDarkMode, switchNotifications;

    private FrameLayout navProfileTabHome, navProfileTabReports;
    private LinearLayout navProfileTabAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        initViews();
        setupListeners();
        loadUserProfile();
    }

    private void initViews() {
        btnEditProfile = findViewById(R.id.btnEditProfile);
        tvProfileInitial = findViewById(R.id.tvProfileInitial);
        tvProfileStoreName = findViewById(R.id.tvProfileStoreName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvEmailStatusBadge = findViewById(R.id.tvEmailStatusBadge);
        tvArchiveCount = findViewById(R.id.tvArchiveCount);

        rowBiometricLogin = findViewById(R.id.rowBiometricLogin);
        rowPinCode = findViewById(R.id.rowPinCode);
        rowChangePassword = findViewById(R.id.rowChangePassword);
        rowArchive = findViewById(R.id.rowArchive);
        rowExportPdf = findViewById(R.id.rowExportPdf);
        rowDarkMode = findViewById(R.id.rowDarkMode);
        rowPushNotifications = findViewById(R.id.rowPushNotifications);

        switchBiometric = findViewById(R.id.switchBiometric);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        switchNotifications = findViewById(R.id.switchNotifications);

        navProfileTabHome = findViewById(R.id.navProfileTabHome);
        navProfileTabReports = findViewById(R.id.navProfileTabReports);
        navProfileTabAccount = findViewById(R.id.navProfileTabAccount);

        boolean isDark = prefs.getBoolean(KEY_DARK_MODE, false);
        if (switchDarkMode != null) {
            switchDarkMode.setChecked(isDark);
        }
    }

    private void setStoreNameAndInitial(String storeName) {
        if (storeName == null || storeName.trim().isEmpty()) return;
        String trimmed = storeName.trim();
        if (tvProfileStoreName != null) {
            tvProfileStoreName.setText(trimmed);
        }
        if (tvProfileInitial != null) {
            tvProfileInitial.setText(trimmed.substring(0, 1));
        }
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            if (tvEmailStatusBadge != null) {
                tvEmailStatusBadge.setText("وضع الزائر");
            }
            return;
        }

        if (tvProfileEmail != null && currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
            tvProfileEmail.setText(currentUser.getEmail());
        }

        if (tvEmailStatusBadge != null) {
            tvEmailStatusBadge.setText(currentUser.isEmailVerified() ? "البريد مفعّل" : "حساب مسجّل");
        }

        db.collection("users").document(currentUser.getUid())
                .addSnapshotListener((doc, error) -> {
                    if (error != null || doc == null || !doc.exists()) return;

                    String storeName = doc.getString("storeName");
                    if (storeName == null || storeName.trim().isEmpty()) {
                        storeName = doc.getString("name");
                    }
                    String email = doc.getString("email");

                    if (storeName != null && !storeName.trim().isEmpty()) {
                        setStoreNameAndInitial(storeName);
                    }
                    if (tvProfileEmail != null && email != null && !email.trim().isEmpty()) {
                        tvProfileEmail.setText(email);
                    }
                });
    }

    private void setupListeners() {
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> showEditStoreNameDialog());
        }

        if (switchBiometric != null) {
            switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) ->
                    Toast.makeText(this, isChecked ? "تم تفعيل الدخول بالبصمة" : "تم إيقاف الدخول بالبصمة", Toast.LENGTH_SHORT).show());
        }

        if (rowPinCode != null) {
            rowPinCode.setOnClickListener(v ->
                    Toast.makeText(this, "إعدادات رمز PIN", Toast.LENGTH_SHORT).show());
        }

        if (rowChangePassword != null) {
            rowChangePassword.setOnClickListener(v ->
                    Toast.makeText(this, "تغيير كلمة المرور", Toast.LENGTH_SHORT).show());
        }

        if (rowArchive != null) {
            rowArchive.setOnClickListener(v ->
                    Toast.makeText(this, "فتح الأرشيف", Toast.LENGTH_SHORT).show());
        }

        if (rowExportPdf != null) {
            rowExportPdf.setOnClickListener(v ->
                    Toast.makeText(this, "تصدير كل الحسابات PDF", Toast.LENGTH_SHORT).show());
        }

        if (switchDarkMode != null) {
            switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply();
                AppCompatDelegate.setDefaultNightMode(
                        isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
                );
            });
        }

        if (switchNotifications != null) {
            switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) ->
                    Toast.makeText(this, isChecked ? "تم تفعيل الإشعارات" : "تم إيقاف الإشعارات", Toast.LENGTH_SHORT).show());
        }

        if (navProfileTabHome != null) {
            navProfileTabHome.setOnClickListener(v -> {
                startActivity(new Intent(ProfileActivity.this, MainActivity.class));
                finish();
            });
        }

        if (navProfileTabReports != null) {
            navProfileTabReports.setOnClickListener(v ->
                    Toast.makeText(this, "الانتقال إلى شاشة التقارير", Toast.LENGTH_SHORT).show());
        }

        if (navProfileTabAccount != null) {
            navProfileTabAccount.setOnClickListener(v -> {
                // نحن بالفعل في شاشة حسابي
            });
        }
    }

    private void showEditStoreNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("تعديل اسم المحل");

        final EditText inputName = new EditText(this);
        inputName.setInputType(InputType.TYPE_CLASS_TEXT);
        inputName.setHint("أدخل اسم المحل الجديد");
        builder.setView(inputName);

        builder.setPositiveButton("حفظ", (dialog, which) -> {
            String newName = inputName.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(this, "يرجى إدخال اسم المحل", Toast.LENGTH_SHORT).show();
                return;
            }

            setStoreNameAndInitial(newName);

            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser != null) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("storeName", newName);
                db.collection("users").document(currentUser.getUid())
                        .set(updates, SetOptions.merge());
            }

            Toast.makeText(this, "تم تحديث اسم المحل", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("إلغاء", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}