package com.example.daftarii;

import com.example.daftarii.data.CustomerRepository;
import com.example.daftarii.data.TransactionRepository;
import com.example.daftarii.logic.AppLanguage;
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

    private static final String PREFS_NAME = DaftariApp.PREFS;
    private static final String KEY_DARK_MODE = DaftariApp.KEY_DARK_MODE;
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
        updateLanguageLabel();
        // قسم الأمان والحساب: البصمة، PIN، كلمة السر، الإشعارات، تسجيل الخروج (سندس)
        ProfileSecurity.setup(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
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
                        : (email != null && !email.trim().isEmpty() ? email.trim() : getString(R.string.home_store_initial));
                tvProfileInitial.setText(source.substring(0, 1).toUpperCase(Locale.ROOT));
            }
        }
    }

    private com.google.firebase.firestore.ListenerRegistration archivedCustomersListener, archivedTxListener;
    private int archivedCustomers = 0, archivedTransactions = 0;

    @Override
    protected void onStart() {
        super.onStart();
        startArchiveCount();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopArchiveCount();
    }

    /**
     * عدد الأرشيف لحظة بلحظة، بنفس الاستعلامين اللي بتعرضهم شاشة الأرشيف
     * (زباين مؤرشفين + حركات مؤرشفة)، فالرقم دايماً بيطابق اللي جوّا.
     * (القديم كان get() مرة وحدة، وأحياناً بيرجع رقم قديم من الكاش.)
     */
    private void startArchiveCount() {
        stopArchiveCount();
        if (tvArchiveCount == null) return;
        archivedCustomers = 0;
        archivedTransactions = 0;
        showArchiveCount();
        archivedCustomersListener = CustomerRepository.listen(true, list -> {
            archivedCustomers = list.size();
            showArchiveCount();
        }, null);
        archivedTxListener = TransactionRepository.listenArchived(list -> {
            archivedTransactions = list.size();
            showArchiveCount();
        });
    }

    private void stopArchiveCount() {
        if (archivedCustomersListener != null) archivedCustomersListener.remove();
        if (archivedTxListener != null) archivedTxListener.remove();
        archivedCustomersListener = null;
        archivedTxListener = null;
    }

    private void showArchiveCount() {
        if (tvArchiveCount != null) tvArchiveCount.setText(String.valueOf(archivedCustomers + archivedTransactions));
    }

    private void updateLanguageLabel() {
        if (tvCurrentLanguage == null) return;
        // اسم اللغة بلغتها (English / العربية)، متل زر 🌐 بشاشة الدخول
        tvCurrentLanguage.setText(LanguageManager.current() == AppLanguage.ENGLISH
                ? R.string.lang_name_en : R.string.lang_name_ar);
    }

    /**
     * نفس طريقة زر 🌐 بشاشة الدخول (LanguageManager): أندرويد بيحفظ اللغة
     * وبيعيد فتح كل الشاشات فيها. (الطريقة القديمة كانت بتغيّر هالشاشة بس، وبتنسى بعد ما يتسكّر التطبيق.)
     */
    private void toggleLanguage() {
        LanguageManager.toggle();
    }

    private void applyDarkMode(boolean enableDark) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enableDark).apply();
        AppCompatDelegate.setDefaultNightMode(
                enableDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    private void setupListeners() {
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> ProfileSecurity.editShopName(this, this::loadUserInfo));
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