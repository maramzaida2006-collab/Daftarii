package com.example.daftarii;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.daftarii.auth.GuestUpgradeViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * تحويل الزائر لحساب. بتنفتح من شريط الزائر بالرئيسية، أو لحالها لما تخلص الـ 7 أيام.
 * إذا التجربة خلصت: ما في زر رجوع، وفي "الدخول بحساب آخر" بس.
 * كل المنطق بـ GuestUpgradeViewModel.
 */
public class GuestUpgradeActivity extends AppCompatActivity {

    private GuestUpgradeViewModel viewModel;
    private TextInputLayout tilName, tilEmail, tilPassword;
    private TextInputEditText etName, etEmail, etPassword;
    private View btnSubmit, progress;
    private boolean expired;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_upgrade);

        viewModel = new ViewModelProvider(this).get(GuestUpgradeViewModel.class);
        if (!viewModel.isGuest()) {
            // مش زائر (أو طلع): ما إله شغل هون
            goTo(SplashActivity.class);
            return;
        }
        expired = viewModel.isTrialExpired();

        tilName = findViewById(R.id.tilUpgradeName);
        tilEmail = findViewById(R.id.tilUpgradeEmail);
        tilPassword = findViewById(R.id.tilUpgradePassword);
        etName = findViewById(R.id.etUpgradeName);
        etEmail = findViewById(R.id.etUpgradeEmail);
        etPassword = findViewById(R.id.etUpgradePassword);
        btnSubmit = findViewById(R.id.btnUpgradeSubmit);
        progress = findViewById(R.id.progressUpgrade);

        View back = findViewById(R.id.btnUpgradeBack);
        View leave = findViewById(R.id.btnUpgradeLeave);
        if (expired) {
            ((TextView) findViewById(R.id.tvUpgradeTitle)).setText(R.string.guest_upgrade_expired_title);
            ((TextView) findViewById(R.id.tvUpgradeSubtitle)).setText(R.string.guest_upgrade_expired_subtitle);
            back.setVisibility(View.INVISIBLE);
            leave.setVisibility(View.VISIBLE);
        }
        back.setOnClickListener(v -> goBack());
        leave.setOnClickListener(v -> confirmLeave());
        btnSubmit.setOnClickListener(v -> viewModel.upgrade(
                text(etName), text(etEmail),
                etPassword.getText() != null ? etPassword.getText().toString() : ""));

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goBack();
            }
        });

        observe();
    }

    private void observe() {
        viewModel.getLoading().observe(this, loading -> {
            boolean on = Boolean.TRUE.equals(loading);
            progress.setVisibility(on ? View.VISIBLE : View.GONE);
            btnSubmit.setEnabled(!on);
        });
        viewModel.getShopNameError().observe(this, res -> tilName.setError(res == null ? null : getString(res)));
        viewModel.getEmailError().observe(this, res -> tilEmail.setError(res == null ? null : getString(res)));
        viewModel.getPasswordError().observe(this, res -> tilPassword.setError(res == null ? null : getString(res)));
        viewModel.getMessage().observe(this, event -> {
            Integer res = event.getIfNotHandled();
            if (res != null) Toast.makeText(this, res, Toast.LENGTH_LONG).show();
        });
        viewModel.getNavigation().observe(this, event -> {
            GuestUpgradeViewModel.Nav nav = event.getIfNotHandled();
            if (nav == null) return;
            goTo(nav == GuestUpgradeViewModel.Nav.VERIFY_EMAIL
                    ? VerifyEmailActivity.class : LoginActivity.class);
        });
    }

    /** إذا التجربة خلصت: بيطلع من التطبيق. غير هيك: بيرجع للرئيسية. */
    private void goBack() {
        if (expired) {
            finishAffinity();
        } else if (isTaskRoot()) {
            // انفتحت لحالها (مثلاً من شاشة التفعيل): ما في رئيسية وراها
            goTo(MainActivity.class);
        } else {
            finish();
        }
    }

    private void confirmLeave() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.guest_upgrade_leave_title)
                .setMessage(R.string.guest_upgrade_leave_message)
                .setPositiveButton(R.string.guest_upgrade_leave_confirm, (d, w) -> viewModel.leaveGuest())
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private static String text(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void goTo(Class<?> target) {
        Intent intent = new Intent(this, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
