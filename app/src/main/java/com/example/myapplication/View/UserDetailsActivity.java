package com.example.myapplication.View;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Controller.AuthCallBack;
import com.example.myapplication.Controller.Database;
import com.example.myapplication.Model.User;
import com.example.myapplication.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;

public class UserDetailsActivity extends AppCompatActivity {
    EditText idNumber;
    Switch accountTypeSwitch;
    Button submitButton;
    private Database database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        idNumber = findViewById(R.id.idNumber);
        accountTypeSwitch = findViewById(R.id.accountTypeSwitch);
        submitButton = findViewById(R.id.submitButton);

        database = new Database();

        // Define the AuthCallBack to handle Firestore responses
        AuthCallBack authCallBack = new AuthCallBack() {
            @Override
            public void onLoginComplete(Task<AuthResult> task) {
                // Handle login if needed
            }

            @Override
            public void onCreateAccountComplete(boolean status, String err) {
                if (status) {
                    Log.d("UserDetailsActivity", "Account creation successful");
                    Toast.makeText(UserDetailsActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(UserDetailsActivity.this, MainActivity.class));
                    finish();
                } else {
                    Log.e("UserDetailsActivity", "Account creation failed: " + err);
                    Toast.makeText(UserDetailsActivity.this, "Error: " + err, Toast.LENGTH_LONG).show();
                }
            }
        };

        submitButton.setOnClickListener(view -> {
            if (idNumber == null) {
                Toast.makeText(UserDetailsActivity.this, "Initialization error", Toast.LENGTH_SHORT).show();
                return;
            }

            String id = idNumber.getText().toString().trim();
            int accountType = accountTypeSwitch.isChecked() ? 1 : 0;

            if (id.isEmpty()) {
                Toast.makeText(UserDetailsActivity.this, "Please fill in your ID number", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser != null) {
                User user = new User();
                user.setEmail(firebaseUser.getEmail());
                user.setFirstname(firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Unknown");
                user.setMyId(id);
                user.setAccount_type(accountType);

                // Set key using UID or another unique identifier
                user.setKeyOn(firebaseUser.getUid());

                Log.d("UserDetailsActivity", "Email: " + firebaseUser.getEmail());
                Log.d("UserDetailsActivity", "Firebase UID: " + firebaseUser.getUid());
                Log.d("UserDetailsActivity", "ID: " + id);
                Log.d("UserDetailsActivity", "Account Type: " + accountType);
                Log.d("UserDetailsActivity", "User Key: " + user.getKey());

                database.setAuthCallBack(authCallBack);
                database.saveUserData(user);
            } else {
                Toast.makeText(UserDetailsActivity.this, "User not logged in", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
