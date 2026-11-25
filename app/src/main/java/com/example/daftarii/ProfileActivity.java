package com.example.daftarii;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.daftarii.auth.GuestSessionHelper;
import com.example.daftarii.auth.PinStore;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "daftarii_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_BIOMETRIC = "biometric_enabled";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private SharedPreferences prefs;
    private ListenerRegistration userDocListener;
    private ListenerRegistration archiveListener;

    private final List<Customer> activeCustomersForPdf = new ArrayList<>();

    private FrameLayout btnEditProfile;
    private TextView tvProfileInitial, tvProfileStoreName, tvProfileEmail, tvEmailStatusBadge, tvArchiveCount;

    private LinearLayout rowBiometricLogin, rowPinCode, rowChangePassword;
    private LinearLayout rowArchive, rowExportPdf, rowDarkMode, rowPushNotifications;
    private MaterialButton btnLogout;

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
        loadCustomersSummaryAndArchive();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (userDocListener != null) {
            userDocListener.remove();
            userDocListener = null;
        }
        if (archiveListener != null) {
            archiveListener.remove();
            archiveListener = null;
        }
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
        btnLogout = findViewById(R.id.btnLogout);

        switchBiometric = findViewById(R.id.switchBiometric);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        switchNotifications = findViewById(R.id.switchNotifications);

        navProfileTabHome = findViewById(R.id.navProfileTabHome);
        navProfileTabReports = findViewById(R.id.navProfileTabReports);
        navProfileTabAccount = findViewById(R.id.navProfileTabAccount);

        if (switchDarkMode != null) {
            switchDarkMode.setChecked(prefs.getBoolean(KEY_DARK_MODE, false));
        }
        if (switchBiometric != null) {
            switchBiometric.setChecked(prefs.getBoolean(KEY_BIOMETRIC, false));
        }
        if (switchNotifications != null) {
            switchNotifications.setChecked(prefs.getBoolean(KEY_NOTIFICATIONS, true));
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
                tvEmailStatusBadge.setText("غير مسجّل");
            }
            return;
        }

        if (currentUser.isAnonymous()) {
            if (tvEmailStatusBadge != null) {
                tvEmailStatusBadge.setText("وضع الزائر");
            }
            if (tvProfileEmail != null && (tvProfileEmail.getText() == null || tvProfileEmail.getText().toString().trim().isEmpty())) {
                tvProfileEmail.setText("حساب زائر مؤقت");
            }
        } else {
            if (tvProfileEmail != null && currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
                tvProfileEmail.setText(currentUser.getEmail());
            }
            if (tvEmailStatusBadge != null) {
                tvEmailStatusBadge.setText(currentUser.isEmailVerified() ? "البريد مفعّل" : "بانتظار التفعيل");
            }
        }

        if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().trim().isEmpty()) {
            setStoreNameAndInitial(currentUser.getDisplayName());
        }

        userDocListener = db.collection("users").document(currentUser.getUid())
                .addSnapshotListener((doc, error) -> {
                    if (error != null || doc == null || !doc.exists()) return;

                    String storeName = doc.getString("shopName");
                    if (storeName == null || storeName.trim().isEmpty()) {
                        storeName = doc.getString("storeName");
                    }
                    if (storeName == null || storeName.trim().isEmpty()) {
                        storeName = doc.getString("displayName");
                    }
                    if (storeName == null || storeName.trim().isEmpty()) {
                        storeName = doc.getString("name");
                    }

                    String email = doc.getString("email");

                    if (storeName != null && !storeName.trim().isEmpty()) {
                        setStoreNameAndInitial(storeName);
                    }
                    if (!currentUser.isAnonymous() && tvProfileEmail != null && email != null && !email.trim().isEmpty()) {
                        tvProfileEmail.setText(email);
                    }
                });
    }

    private void loadCustomersSummaryAndArchive() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        archiveListener = db.collection("customers")
                .whereEqualTo("userId", currentUser.getUid())
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    int archivedCount = 0;
                    activeCustomersForPdf.clear();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Customer c = doc.toObject(Customer.class);
                        if (c == null) continue;
                        c.setId(doc.getId());

                        Boolean archived = doc.getBoolean("archived");
                        if (Boolean.TRUE.equals(archived)) {
                            archivedCount++;
                        } else {
                            activeCustomersForPdf.add(c);
                        }
                    }

                    if (tvArchiveCount != null) {
                        tvArchiveCount.setText(String.valueOf(archivedCount));
                    }
                });
    }

    private void setupListeners() {
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> showEditStoreNameDialog());
        }

        if (switchBiometric != null) {
            switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean(KEY_BIOMETRIC, isChecked).apply();
                Toast.makeText(
                        this,
                        isChecked ? "تم تفعيل الدخول بالبصمة" : "تم إيقاف الدخول بالبصمة",
                        Toast.LENGTH_SHORT
                ).show();
            });
        }

        if (rowBiometricLogin != null && switchBiometric != null) {
            rowBiometricLogin.setOnClickListener(v -> switchBiometric.toggle());
        }

        if (rowPinCode != null) {
            rowPinCode.setOnClickListener(v -> showPinOptionsDialog());
        }

        if (rowChangePassword != null) {
            rowChangePassword.setOnClickListener(v -> sendPasswordResetEmail());
        }

        if (rowArchive != null) {
            rowArchive.setOnClickListener(v ->
                    startActivity(new Intent(ProfileActivity.this, ArchiveActivity.class)));
        }

        if (rowExportPdf != null) {
            rowExportPdf.setOnClickListener(v -> exportCustomersPdf());
        }

        if (switchDarkMode != null) {
            switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply();
                AppCompatDelegate.setDefaultNightMode(
                        isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
                );
            });
        }

        if (rowDarkMode != null && switchDarkMode != null) {
            rowDarkMode.setOnClickListener(v -> switchDarkMode.toggle());
        }

        if (switchNotifications != null) {
            switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean(KEY_NOTIFICATIONS, isChecked).apply();
                Toast.makeText(
                        this,
                        isChecked ? "تم تفعيل الإشعارات" : "تم إيقاف الإشعارات",
                        Toast.LENGTH_SHORT
                ).show();
            });
        }

        if (rowPushNotifications != null && switchNotifications != null) {
            rowPushNotifications.setOnClickListener(v -> switchNotifications.toggle());
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> confirmAndSignOut());
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

    private void showEditStoreNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("تعديل اسم المحل");

        final EditText inputName = new EditText(this);
        inputName.setInputType(InputType.TYPE_CLASS_TEXT);
        inputName.setHint("أدخل اسم المحل الجديد");
        if (tvProfileStoreName != null && tvProfileStoreName.getText() != null) {
            inputName.setText(tvProfileStoreName.getText().toString());
        }
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
                UserProfileChangeRequest req = new UserProfileChangeRequest.Builder()
                        .setDisplayName(newName)
                        .build();
                currentUser.updateProfile(req);

                Map<String, Object> updates = new HashMap<>();
                updates.put("shopName", newName);
                updates.put("storeName", newName);
                updates.put("displayName", newName);
                updates.put("updatedAt", System.currentTimeMillis());

                db.collection("users").document(currentUser.getUid())
                        .set(updates, SetOptions.merge());
            }

            Toast.makeText(this, "تم تحديث اسم المحل", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("إلغاء", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showPinOptionsDialog() {
        FirebaseUser user = auth.getCurrentUser();
        String uid = (user != null) ? user.getUid() : "";
        if (uid.isEmpty()) {
            Toast.makeText(this, "تعذر تحديد الحساب الحالي", Toast.LENGTH_SHORT).show();
            return;
        }

        PinStore pinStore = new PinStore(this);
        String[] options = pinStore.hasPin(uid)
                ? new String[]{"تغيير رمز PIN", "إلغاء رمز PIN"}
                : new String[]{"تعيين رمز PIN جديد"};

        new AlertDialog.Builder(this)
                .setTitle("إعدادات رمز PIN")
                .setItems(options, (dialog, which) -> {
                    if (!pinStore.hasPin(uid) || which == 0) {
                        promptNewPin(pinStore, uid);
                    } else {
                        pinStore.clear();
                        Toast.makeText(this, "تم إلغاء رمز PIN", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("إغلاق", null)
                .show();
    }

    private void promptNewPin(PinStore pinStore, String uid) {
        final EditText inputPin = new EditText(this);
        inputPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        inputPin.setHint("أدخل رمز PIN (4 أرقام أو أكثر)");

        new AlertDialog.Builder(this)
                .setTitle("تعيين رمز PIN")
                .setView(inputPin)
                .setPositiveButton("حفظ", (dialog, which) -> {
                    String pin = inputPin.getText().toString().trim();
                    if (pin.length() < 4) {
                        Toast.makeText(this, "الرمز يجب أن يكون 4 أرقام على الأقل", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    pinStore.savePin(uid, pin);
                    Toast.makeText(this, "تم حفظ رمز PIN بنجاح", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void sendPasswordResetEmail() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.isAnonymous() || user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            Toast.makeText(this, "تغيير كلمة المرور متاح للحسابات المسجلة بالبريد فقط", Toast.LENGTH_LONG).show();
            return;
        }

        String email = user.getEmail().trim();
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "تم إرسال رابط إعادة تعيين كلمة المرور إلى " + email, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "تعذر إرسال رابط التغيير حالياً", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void exportCustomersPdf() {
        if (activeCustomersForPdf.isEmpty()) {
            Toast.makeText(this, "لا توجد حسابات زبائن لتصديرها", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.BLACK);
        titlePaint.setTextSize(18f);
        titlePaint.setFakeBoldText(true);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.DKGRAY);
        textPaint.setTextSize(13f);

        String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        canvas.drawText("Daftarii - Customers Statement", 40, 50, titlePaint);
        canvas.drawText("Generated: " + dateStr, 40, 72, textPaint);

        int y = 110;
        double totalDebt = 0.0;
        for (int i = 0; i < activeCustomersForPdf.size(); i++) {
            if (y > 790) break;
            Customer c = activeCustomersForPdf.get(i);
            double debt = c.getTotalDebt();
            totalDebt += debt;
            String line = (i + 1) + ". " + (c.getName() != null ? c.getName() : "Customer")
                    + " | Balance: " + String.format(Locale.US, "%.2f", debt);
            canvas.drawText(line, 40, y, textPaint);
            y += 24;
        }

        y += 12;
        canvas.drawText("Total Due: " + String.format(Locale.US, "%.2f", totalDebt), 40, y, titlePaint);

        document.finishPage(page);

        File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
        File outFile = new File(dir, "daftarii_report_" + System.currentTimeMillis() + ".pdf");

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            document.writeTo(fos);
            Toast.makeText(this, "تم حفظ ملف PDF في: " + outFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "تعذر تصدير PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            document.close();
        }
    }

    private void confirmAndSignOut() {
        FirebaseUser user = auth.getCurrentUser();
        String message = (user != null && user.isAnonymous())
                ? "أنتِ تستخدمين وضع الزائر (متاح لمرة واحدة فقط). عند الخروج لن تتمكني من فتح جلسة زائر جديدة. هل تريدين تسجيل الخروج؟"
                : "هل تريدين تسجيل الخروج من الحساب الحالي؟";

        new AlertDialog.Builder(this)
                .setTitle("تسجيل الخروج")
                .setMessage(message)
                .setPositiveButton("خروج", (dialog, which) -> signOutCurrentUser())
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void signOutCurrentUser() {
        if (userDocListener != null) {
            userDocListener.remove();
            userDocListener = null;
        }
        if (archiveListener != null) {
            archiveListener.remove();
            archiveListener = null;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user != null && user.isAnonymous()) {
            new GuestSessionHelper(this).endGuestSessionAndClearPin(this);
        } else {
            new PinStore(this).clear();
            auth.signOut();
        }

        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}