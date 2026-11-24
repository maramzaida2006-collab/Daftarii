package com.example.daftarii;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.daftarii.auth.CreatePinViewModel;
import com.example.daftarii.util.PinPad;
import com.google.android.material.materialswitch.MaterialSwitch;

/** إنشاء رمز الدخول (4 أرقام) بخطوتين، وخيار البصمة. كل المنطق بـ CreatePinViewModel. */
public class CreatePinActivity extends AppCompatActivity {

    private CreatePinViewModel viewModel;
    private TextView tvStep, tvTitle, tvSubtitle, tvError;
    private ViewGroup dots;
    private int dotOn, dotOff;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_pin);

        viewModel = new ViewModelProvider(this).get(CreatePinViewModel.class);
        if (!viewModel.isSignedIn()) {
            goTo(LoginActivity.class);
            return;
        }

        tvStep = findViewById(R.id.tvPinStep);
        tvTitle = findViewById(R.id.tvPinTitle);
        tvSubtitle = findViewById(R.id.tvPinSubtitle);
        tvError = findViewById(R.id.tvPinError);
        dots = findViewById(R.id.pinDots);
        dotOn = ContextCompat.getColor(this, R.color.plum);
        dotOff = ContextCompat.getColor(this, R.color.dot_inactive);

        PinPad.bind(findViewById(R.id.pinKeypad), new PinPad.Listener() {
            @Override
            public void onDigit(char digit) {
                tvError.setText(null);
                viewModel.onDigit(digit);
            }

            @Override
            public void onDelete() {
                viewModel.onDelete();
            }
        });

        // البصمة: بس إذا الجوال فيه بصمة جاهزة
        View card = findViewById(R.id.cardBiometric);
        MaterialSwitch sw = findViewById(R.id.switchBiometric);
        card.setVisibility(viewModel.isBiometricAvailable() ? View.VISIBLE : View.GONE);
        sw.setOnCheckedChangeListener((b, checked) -> viewModel.setBiometricOn(checked));
        card.setOnClickListener(v -> sw.toggle());

        findViewById(R.id.btnPinBack).setOnClickListener(v -> viewModel.onBack());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                viewModel.onBack();
            }
        });

        viewModel.getStep().observe(this, step -> {
            boolean confirm = step != null && step == 2;
            tvStep.setText(getString(R.string.pin_step, confirm ? 2 : 1));
            tvTitle.setText(confirm ? R.string.pin_confirm_title : R.string.pin_create_title);
            tvSubtitle.setText(confirm ? R.string.pin_confirm_subtitle : R.string.pin_create_subtitle);
        });
        viewModel.getFilled().observe(this, n -> {
            int count = n == null ? 0 : n;
            PinPad.showDots(dots, count, dotOn, dotOff);
            dots.setContentDescription(getString(R.string.pin_entered, count));
        });
        viewModel.getBiometricOn().observe(this, on -> {
            boolean value = Boolean.TRUE.equals(on);
            if (sw.isChecked() != value) sw.setChecked(value);
        });
        viewModel.getError().observe(this, event -> {
            Integer res = event.getIfNotHandled();
            if (res == null) return;
            tvError.setText(res);
            PinPad.shake(dots);
        });
        viewModel.getNavigation().observe(this, event -> {
            CreatePinViewModel.Nav nav = event.getIfNotHandled();
            if (nav == null) return;
            goTo(nav == CreatePinViewModel.Nav.DONE ? MainActivity.class : LoginActivity.class);
        });
    }

    private void goTo(Class<?> target) {
        Intent intent = new Intent(this, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
