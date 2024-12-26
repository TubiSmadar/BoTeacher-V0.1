package com.example.myapplication.View;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Controller.AuthCallBack;
import com.example.myapplication.Controller.UserCallBack;
import com.example.myapplication.Model.Database;
import com.example.myapplication.Model.User;
import com.example.myapplication.R;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {
    TextView forgotPasswordButton, signupRedirectButton;
    private EditText emailEdit, password_edit;
    private Button loginButton;
    private Database database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        findViews();
        initVars();
    }

    private void findViews() {
        emailEdit = findViewById(R.id.loginEmailEdit);
        password_edit = findViewById(R.id.login_passwordEdit);
        loginButton = findViewById(R.id.login_button);
        signupRedirectButton = findViewById(R.id.signupRedirectButton);
        forgotPasswordButton = findViewById(R.id.forgotPasswordButtn);
    }

    private void initVars() {
        database = new Database();

        database.setAuthCallBack(new AuthCallBack() {
            public void onLoginComplete(Task<AuthResult> task) {
                if (task.isSuccessful()) {

                    if (database.getCurrentUser() != null) {
                        // Fetch user data
                        String uid = database.getCurrentUser().getUid();
                        database.fetchUserData(uid);
                    } else {
                        // Handle the case where login failed
                        Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                    }
                    Toast.makeText(LoginActivity.this, "Success Login", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    String error = Objects.requireNonNull(task.getException()).getMessage();
                    Toast.makeText(LoginActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCreateAccountComplete(boolean status, String err) {

            }
        });

        database.setUserCallBack(new UserCallBack() {
            @Override
            public void onUserFetchDataComplete(User customer) {
            }

            @Override
            public void onUpdateComplete(Task<Void> task) {
            }
        });


        signupRedirectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intent);
                finish();
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = emailEdit.getText().toString().trim();
                String password = password_edit.getText().toString().trim();

                if (email.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "request email", Toast.LENGTH_SHORT).show();
                } else if (password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "request password", Toast.LENGTH_SHORT).show();
                } else {
                    // Perform login
                    database.loginUser(email, password);

                    if (database.getCurrentUser() != null) {
                        String uid = database.getCurrentUser().getUid();
                        database.fetchUserData(uid);

                        database.setUserCallBack(new UserCallBack() {
                            @Override
                            public void onUserFetchDataComplete(User user) {
                                if (user != null) {
                                    // Check the account type: 0 for student, 1 for teacher
                                    Intent intent;
                                    if (user.getAccount_type() == 1) {
                                        // Teacher
                                        intent = new Intent(LoginActivity.this, LecturerActivity.class);
                                    } else {
                                        // Student
                                        intent = new Intent(LoginActivity.this, StudentActivity.class);
                                    }
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(LoginActivity.this, "Failed to fetch user data.", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onUpdateComplete(Task<Void> task) {
                                // No action needed here for login
                            }
                        });
                    } else {
                        Toast.makeText(LoginActivity.this, "User not logged in.", Toast.LENGTH_SHORT).show();
                    }


                    // TODO check if student or teacher
                    Intent intent = new Intent(LoginActivity.this, StudentActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });

        forgotPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
                finish();
            }
        });

        if (database.getCurrentUser() != null) {
            String uid = database.getCurrentUser().getUid();
            database.fetchUserData(uid);
        }
    }
}