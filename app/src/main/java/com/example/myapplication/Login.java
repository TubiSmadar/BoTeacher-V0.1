package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.text.TextUtils;
import android.view.View;
import android.widget.*;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class Login extends AppCompatActivity {
    private EditText etUsername, etPassword;
    private Button btnLogin;

    private TextView tvSignup;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.login);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        mAuth = FirebaseAuth.getInstance();
        tvSignup = findViewById(R.id.tvSignup);

        tvSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), Register.class);
                startActivity(intent);
                finish();
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            String email = etUsername.getText().toString().trim();
                                            String password = etPassword.getText().toString().trim();

                                            if (TextUtils.isEmpty(email)) {
                                                etUsername.setError("Email is required");
                                                return;
                                            }
                                            if (TextUtils.isEmpty(password)) {
                                                etPassword.setError("Password is required");
                                                return;
                                            }

                                            // Check if empty

                                            mAuth.createUserWithEmailAndPassword(email, password)
                                                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<AuthResult> task) {
                                                            if (task.isSuccessful()) {
                                                                // Sign in success, update UI with the signed-in user's information
                                                                // Log.d(TAG, "createUserWithEmail:success");
                                                                Toast.makeText(Login.this, "Login successful.",
                                                                        Toast.LENGTH_SHORT).show();
                                                                //TODO change to whatever next activity/ main activity is
                                                                Intent intent = new Intent(getApplicationContext(), Register.class);
                                                                startActivity(intent);
                                                                finish();
                                                                // updateUI(user);
                                                            } else {
                                                                // If sign in fails, display a message to the user.
                                                                // Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                                                Toast.makeText(Login.this, "Login failed.",
                                                                        Toast.LENGTH_SHORT).show();
                                                                // updateUI(null);
                                                            }
                                                        }
                                                    });
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