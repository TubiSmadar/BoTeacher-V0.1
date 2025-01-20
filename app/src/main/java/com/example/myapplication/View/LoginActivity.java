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
import com.google.firebase.auth.FirebaseUser;

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
            @Override
            public void onLoginComplete(Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    FirebaseUser fbUser = database.getCurrentUser();
                    if (fbUser != null) {
                        String uid = fbUser.getUid();
                        database.fetchUserData(uid, new Database.UserFetchCallback() {
                            @Override
                            public void onSuccess(User user) {
                                Toast.makeText(LoginActivity.this, "Welcome " + user.getFirstname(), Toast.LENGTH_SHORT).show();
                                if (user.getAccount_type() == 1) {
                                    //Toast.makeText(LoginActivity.this, "Lecturer " + user.getFirstname(), Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(LoginActivity.this, LecturerCoursesActivity.class);
                                    startActivity(intent);
                                } else if (user.getAccount_type() == 0) {
                                    //Toast.makeText(LoginActivity.this, "Student " + user.getFirstname(), Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(LoginActivity.this, StudentActivity.class);
                                    startActivity(intent);
                                } else {
                                    Toast.makeText(LoginActivity.this, "Unknown account type. Please contact support.", Toast.LENGTH_SHORT).show();
                                }
                                finish();
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(LoginActivity.this, "Failed to fetch user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(LoginActivity.this, "User is null after login.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCreateAccountComplete(boolean status, String err) {
                // Not used here
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
                    Toast.makeText(LoginActivity.this, "Please provide email", Toast.LENGTH_SHORT).show();
                } else if (password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please provide password", Toast.LENGTH_SHORT).show();
                } else {
                    // Perform login
                    database.loginUser(email, password);

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
    }
}
