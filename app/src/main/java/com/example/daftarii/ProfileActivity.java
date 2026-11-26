package com.example.daftarii;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.daftarii.util.LanguageManager;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "daftarii_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_LANG = "app_lang";

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private SharedPreferences prefs;

    private TextView tvProfileStoreName, tvProfileEmail, tvProfileInitial;
    private TextView tvArchiveCount, tvCurrentLanguage;
    private SwitchMaterial switchDarkMode;
    private View rowArchive, rowChangeLanguage, rowDarkMode, btnEditProfile;

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
        syncSwitchStates();
        loadUserInfo();
        setupListeners();
        refreshArchiveCount();
        updateLanguageLabel();
        // قسم الأمان والحساب: البصمة، PIN، كلمة السر، الإشعارات، تسجيل الخروج (سندس)
        ProfileSecurity.setup(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshArchiveCount();
        updateLanguageLabel();
        syncSwitchStates();
        ProfileSecurity.refresh(this);
    }

    private void initViews() {
        tvProfileStoreName = findViewById(R.id.tvProfileStoreName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfileInitial = findViewById(R.id.tvProfileInitial);

        tvArchiveCount = findViewById(R.id.tvArchiveCount);
        tvCurrentLanguage = findViewById(R.id.tvCurrentLanguage);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        btnEditProfile = findViewById(R.id.btnEditProfile);

        rowArchive = findViewById(R.id.rowArchive);
        rowChangeLanguage = findViewById(R.id.rowChangeLanguage);
        rowDarkMode = findViewById(R.id.rowDarkMode);

        navProfileTabHome = findViewById(R.id.navProfileTabHome);
        navProfileTabReports = findViewById(R.id.navProfileTabReports);
        navProfileTabAccount = findViewById(R.id.navProfileTabAccount);
    }

    private void syncSwitchStates() {
        if (switchDarkMode != null) {
            switchDarkMode.setChecked(prefs.getBoolean(KEY_DARK_MODE, false));
        }
    }

    private void loadUserInfo() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String displayName = user.getDisplayName();
            String email = user.getEmail();

            if (tvProfileStoreName != null && displayName != null && !displayName.trim().isEmpty()) {
                tvProfileStoreName.setText(displayName);
            }
            if (tvProfileEmail != null && email != null && !email.trim().isEmpty()) {
                tvProfileEmail.setText(email);
            }
            if (tvProfileInitial != null) {
                String source = (displayName != null && !displayName.trim().isEmpty())
                        ? displayName.trim()
                        : (email != null && !email.trim().isEmpty() ? email.trim() : "د");
                tvProfileInitial.setText(source.substring(0, 1).toUpperCase(Locale.ROOT));
            }
        }
    }

    private void refreshArchiveCount() {
        if (tvArchiveCount == null) return;

        db.collection("customers")
                .get()
                .addOnSuccessListener(customersSnap -> {
                    if (customersSnap == null) return;

                    final int[] totalArchived = {0};
                    for (DocumentSnapshot custDoc : customersSnap.getDocuments()) {
                        Boolean custArchived = custDoc.getBoolean("archived");
                        if (Boolean.TRUE.equals(custArchived)) {
                            totalArchived[0]++;
                        }

                        custDoc.getReference().collection("transactions")
                                .whereEqualTo("archived", true)
                                .get()
                                .addOnSuccessListener(txSnap -> {
                                    if (txSnap != null && !txSnap.isEmpty()) {
                                        totalArchived[0] += txSnap.size();
                                        tvArchiveCount.setText(String.valueOf(totalArchived[0]));
                                    }
                                });
                    }

                    tvArchiveCount.setText(String.valueOf(totalArchived[0]));
                });
    }

    private void updateLanguageLabel() {
        if (tvCurrentLanguage == null) return;
        String lang = prefs.getString(KEY_LANG, "ar");
        tvCurrentLanguage.setText("en".equals(lang) ? "English" : "العربية");
    }

    private void toggleLanguage() {
        String currentLang = prefs.getString(KEY_LANG, "ar");
        String nextLang = "ar".equals(currentLang) ? "en" : "ar";
        prefs.edit().putString(KEY_LANG, nextLang).apply();

        Locale locale = new Locale(nextLang);
        Locale.setDefault(locale);
        Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        config.setLayoutDirection(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        Toast.makeText(
                this,
                "en".equals(nextLang) ? "Language: English" : "تم التبديل إلى العربية",
                Toast.LENGTH_SHORT
        ).show();

        recreate();
    }

    private void applyDarkMode(boolean enableDark) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enableDark).apply();
        AppCompatDelegate.setDefaultNightMode(
                enableDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    private void setupListeners() {
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v ->
                    Toast.makeText(this, "تعديل بيانات الحساب", Toast.LENGTH_SHORT).show());
        }

        if (rowArchive != null) {
            rowArchive.setOnClickListener(v ->
                    startActivity(new Intent(ProfileActivity.this, ArchiveActivity.class)));
        }

        if (rowChangeLanguage != null) {
            rowChangeLanguage.setOnClickListener(v -> toggleLanguage());
        }

        if (rowDarkMode != null) {
            rowDarkMode.setOnClickListener(v -> {
                boolean nextDark = !prefs.getBoolean(KEY_DARK_MODE, false);
                if (switchDarkMode != null) {
                    switchDarkMode.setChecked(nextDark);
                }
                applyDarkMode(nextDark);
            });
        }

        if (switchDarkMode != null) {
            switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> applyDarkMode(isChecked));
        }

        if (navProfileTabHome != null) {
            navProfileTabHome.setOnClickListener(v -> {
                startActivity(new Intent(ProfileActivity.this, MainActivity.class));
                finish();
            });
        }

        if (navProfileTabReports != null) {
            navProfileTabReports.setOnClickListener(v -> {
                startActivity(new Intent(ProfileActivity.this, ReportsActivity.class));
                finish();
            });
        }

        if (navProfileTabAccount != null) {
            navProfileTabAccount.setOnClickListener(v -> {
                // نحن بالفعل في شاشة حسابي
            });
        }
    }
}