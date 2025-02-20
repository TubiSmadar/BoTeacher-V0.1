package com.example.myapplication.View;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Controller.AuthCallBack;
import com.example.myapplication.Model.Database;
import com.example.myapplication.Model.User;
import com.example.myapplication.R;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;

public class SignupActivity extends AppCompatActivity {
    EditText firstname, lastname, signupEmail, signupPassword;
    EditText Id_Number;
    TextView txtV_button_back;
    Button signupButton;
    ImageButton backButton;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    Switch accountTypeSwitch;
    private Database database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);
        findV();
        init();
    }

    private void findV() {
        firstname = findViewById(R.id.firstName);
        signupEmail = findViewById(R.id.signup_email);
        lastname = findViewById(R.id.lastName);
        signupPassword = findViewById(R.id.signup_password);
        txtV_button_back = findViewById(R.id.loginRedirectText_ls);
        signupButton = findViewById(R.id.signupButton);
        backButton = findViewById(R.id.customBackButton);
        Id_Number = findViewById(R.id.Id_Number);
        accountTypeSwitch = findViewById(R.id.switchAccountType);
    }

    private void init() {
        database = new Database();
        database.setAuthCallBack(new AuthCallBack() {
            @Override
            public void onLoginComplete(Task<AuthResult> task) {

            }


            @Override
            public void onCreateAccountComplete(boolean status, String err) {
                if (status) {
                    Log.d("SignupActivity", "Account created successfully");
                    Toast.makeText(SignupActivity.this, "Account created successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Close activity or redirect
                } else {
                    Log.e("SignupActivity", "Account creation failed: " + err);
                    Toast.makeText(SignupActivity.this, "Error: " + err, Toast.LENGTH_LONG).show();
                }
            }

        });

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkInput()) {
                    Toast.makeText(SignupActivity.this, "Error CheckInput", Toast.LENGTH_LONG).show();
                    return;
                }

                String email = signupEmail.getText().toString();
                String password = signupPassword.getText().toString();

                // Check if user already exists
                database.checkUserExists(email, new Database.UserExistsCallback() {
                    @Override
                    public void onUserExistsCheckComplete(boolean exists) {
                        if (exists) {
                            // User already exists, show error message
                            Toast.makeText(SignupActivity.this, "User already exists with this email", Toast.LENGTH_SHORT).show();
                        } else {
                            // User doesn't exist, proceed with account creation
                            User user = prepareUser(email);

                            // Modified call to include callback handling
                            database.createAccount(email, password, user);
                        }
                    }

                    @Override
                    public void onUserExistsCheckFailure(Exception e) {
                        // Handle failure
                        Toast.makeText(SignupActivity.this, "Failed to check user existence: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        txtV_button_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        txtV_button_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Redirect to LoginActivity
                Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });


    }

    private boolean checkInput() {

        String email = signupEmail.getText().toString();
        String password = signupPassword.getText().toString();

        User user = prepareUser(email);

        if (!user.isValid()) {
            Toast.makeText(SignupActivity.this, "Please fill all user info!", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (password.length() < 8) {
            Toast.makeText(SignupActivity.this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private User prepareUser(String email){ //, String password) {
        User user = new User();
        user.setEmail(email);
        user.setFirstname(firstname.getText().toString());
        user.setLastname(lastname.getText().toString());
        //user.setPassword(password);
        user.setMyId(Id_Number.getText().toString());
        int accountType = accountTypeSwitch.isChecked() ? 1 : 0;
        user.setAccount_type(accountType);
        return user;
    }

}