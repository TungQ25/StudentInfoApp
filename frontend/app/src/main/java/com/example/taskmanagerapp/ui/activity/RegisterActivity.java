package com.example.taskmanagerapp.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.taskmanagerapp.R;
import com.example.taskmanagerapp.data.remote.RetrofitClient;
import com.example.taskmanagerapp.data.remote.dto.AuthResponse;
import com.example.taskmanagerapp.data.remote.dto.RegisterRequest;
import com.example.taskmanagerapp.utils.PreferenceHelper;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {
    private EditText edtUsername;
    private EditText edtEmail;
    private EditText edtPassword;
    private EditText edtConfirmPassword;
    private Button btnRegister;
    private TextView txtLogin;
    private PreferenceHelper preferenceHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceHelper = new PreferenceHelper(this);
        // Check if user is already logged in
        if (preferenceHelper.hasAuthToken()) {
            openMain();
            return;
        }

        setContentView(R.layout.activity_register);
        edtUsername = findViewById(R.id.edtUsername);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        txtLogin = findViewById(R.id.txtLogin);

        btnRegister.setOnClickListener(v -> register());
        txtLogin.setOnClickListener(v -> finish());
    }

    private void register() {
        String username = edtUsername.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString();
        String confirmPassword = edtConfirmPassword.getText().toString();

        if (username.length() < 3) {
            edtUsername.setError("Username must be at least 3 characters");
            return;
        }
        if (!email.contains("@")) {
            edtEmail.setError("Valid email is required");
            return;
        }
        if (password.length() < 6) {
            edtPassword.setError("Password must be at least 6 characters");
            return;
        }
        if (!password.equals(confirmPassword)) {
            edtConfirmPassword.setError("Passwords do not match");
            return;
        }

        setLoading(true);
        RetrofitClient.getAuthApi().register(new RegisterRequest(username, email, password,
                preferenceHelper.getDeviceId())).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setLoading(false);
                AuthResponse body = response.body();
                if (response.isSuccessful() && body != null && body.getToken() != null) {
                    preferenceHelper.saveAuth(body.getToken(), body.getId(), body.getUsername(), body.getEmail(),
                            body.getRefreshToken(), body.getRefreshExpiresAt(), body.getDeviceId(), body.getSessionId());
                    openMain();
                } else {
                    Toast.makeText(RegisterActivity.this, "Register failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, "Cannot connect to server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        btnRegister.setEnabled(!loading);
        txtLogin.setEnabled(!loading);
        btnRegister.setText(loading ? "Creating..." : getString(R.string.auth_register));
    }

    private void openMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
