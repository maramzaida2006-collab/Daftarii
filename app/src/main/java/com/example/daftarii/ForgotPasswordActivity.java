package com.example.daftarii;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.daftarii.auth.ForgotPasswordViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** شاشة "نسيت كلمة السر". */
public class ForgotPasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        ForgotPasswordViewModel viewModel = new ViewModelProvider(this).get(ForgotPasswordViewModel.class);

        TextInputLayout tilEmail = findViewById(R.id.tilResetEmail);
        TextInputEditText etEmail = findViewById(R.id.etResetEmail);
        MaterialButton btnSend = findViewById(R.id.btnSendResetLink);
        LinearProgressIndicator progress = findViewById(R.id.progressForgot);
        View cardSent = findViewById(R.id.cardSent);

        btnSend.setOnClickListener(v -> viewModel.send(
                etEmail.getText() == null ? "" : etEmail.getText().toString().trim()));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnBackToLogin).setOnClickListener(v -> finish());

        viewModel.getLoading().observe(this, loading -> {
            boolean on = Boolean.TRUE.equals(loading);
            progress.setVisibility(on ? View.VISIBLE : View.GONE);
            btnSend.setEnabled(!on);
        });
        viewModel.getEmailError().observe(this, res ->
                tilEmail.setError(res == null ? null : getString(res)));
        viewModel.getSent().observe(this, sent ->
                cardSent.setVisibility(Boolean.TRUE.equals(sent) ? View.VISIBLE : View.GONE));
        viewModel.getMessage().observe(this, event -> {
            Integer res = event.getIfNotHandled();
            if (res != null) Toast.makeText(this, res, Toast.LENGTH_LONG).show();
        });
    }
}
