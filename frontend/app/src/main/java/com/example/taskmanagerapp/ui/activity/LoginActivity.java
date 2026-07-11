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
import com.example.taskmanagerapp.data.remote.dto.LoginRequest;
import com.example.taskmanagerapp.utils.PreferenceHelper;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private EditText edtIdentifier;
    private EditText edtPassword;
    private Button btnLogin;
    private TextView txtRegister;
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

        setContentView(R.layout.activity_login);
        edtIdentifier = findViewById(R.id.edtIdentifier);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        txtRegister = findViewById(R.id.txtRegister);

        btnLogin.setOnClickListener(v -> login());
        txtRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void login() {
        String identifier = edtIdentifier.getText().toString().trim();
        String password = edtPassword.getText().toString();

        if (identifier.isEmpty()) {
            edtIdentifier.setError("Username or email is required");
            return;
        }
        if (password.length() < 6) {
            edtPassword.setError("Password must be at least 6 characters");
            return;
        }

        setLoading(true);
        // Gửi request đăng nhập lên backend bằng Retrofit
        RetrofitClient.getAuthApi().login(new LoginRequest(identifier, password,
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
                    Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Cannot connect to server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        txtRegister.setEnabled(!loading);
        btnLogin.setText(loading ? "Signing in..." : getString(R.string.auth_login));
    }

    private void openMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
