package com.example.myapplication;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private EditText etUsername, etPassword;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            String username = etUsername.getText().toString().trim();
                                            String password = etPassword.getText().toString().trim();

                                            if (TextUtils.isEmpty(username)) {
                                                etUsername.setError("Username is required");
                                                return;
                                            }
                                            if (TextUtils.isEmpty(password)) {
                                                etPassword.setError("Password is required");
                                                return;
                                            }

                                            // Simulate login authentication (replace with real authentication)
                                            if (username.equals("user") && password.equals("password")) {
                                                Toast.makeText(MainActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                                                // Navigate to another activity
                                            } else {
                                                Toast.makeText(MainActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void test(View view) {
        //test
        int id = view.getId();
        view.setEnabled(false);
    }
}