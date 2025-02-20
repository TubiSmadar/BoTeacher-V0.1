package com.example.myapplication.View;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Controller.AuthCallBack;
import com.example.myapplication.Controller.Database;
import com.example.myapplication.Model.User;
import com.example.myapplication.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class SignupActivity extends AppCompatActivity {
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    EditText firstname, lastname, signupEmail, signupPassword, Id_Number;
    TextView txtV_button_back;
    Button signupButton;
    ImageButton backButton;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    Switch accountTypeSwitch;
    private Database database;
    private GoogleSignInClient googleSignInClient;
    private SignInButton googleSignInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
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
        googleSignInButton = findViewById(R.id.googleSignInButton);
    }

    private void init() {
        database = new Database();
        FirebaseAuth.getInstance().signOut();

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("126542571884-qe0fg738vsfgf6u21olj4u88of69u301.apps.googleusercontent.com") // Ensure correct Web Client ID
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Log.d("SignupActivity", "Google Sign-In Intent result received.");
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            if (account != null) {
                                Log.d("SignupActivity", "Google Sign-In successful: " + account.getEmail());
                                firebaseAuthWithGoogle(account.getIdToken());
                            }
                        } catch (ApiException e) {
                            Log.e("SignupActivity", "Google Sign-In failed", e);
                            Toast.makeText(this, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e("SignupActivity", "Google Sign-In cancelled or failed with result code: " + result.getResultCode());
                    }
                }
        );

        googleSignInButton.setOnClickListener(v -> {
            Log.d("SignupActivity", "Google Sign-In button clicked.");
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        database.setAuthCallBack(new AuthCallBack() {
            @Override
            public void onLoginComplete(Task<AuthResult> task) {}

            @Override
            public void onCreateAccountComplete(boolean status, String err) {
                if (status) {
                    Log.d("SignupActivity", "Account created successfully");
                    Toast.makeText(SignupActivity.this, "Account created successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Log.e("SignupActivity", "Account creation failed: " + err);
                    Toast.makeText(SignupActivity.this, "Error: " + err, Toast.LENGTH_LONG).show();
                }
            }
        });

        signupButton.setOnClickListener(v -> {
            if (!checkInput()) {
                Toast.makeText(SignupActivity.this, "Error CheckInput", Toast.LENGTH_LONG).show();
                return;
            }

            String email = signupEmail.getText().toString();
            String password = signupPassword.getText().toString();

            database.checkUserExists(email, new Database.UserExistsCallback() {
                @Override
                public void onUserExistsCheckComplete(boolean exists) {
                    if (exists) {
                        Toast.makeText(SignupActivity.this, "User already exists with this email", Toast.LENGTH_SHORT).show();
                    } else {
                        User user = prepareUser(email);
                        database.createAccount(email, password, user);
                    }
                }

                @Override
                public void onUserExistsCheckFailure(Exception e) {
                    Toast.makeText(SignupActivity.this, "Failed to check user existence: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        backButton.setOnClickListener(view -> {
            Intent intent = new Intent(SignupActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser firebaseUser = auth.getCurrentUser();
                if (firebaseUser != null) {
                    database.checkUserExists(firebaseUser.getEmail(), new Database.UserExistsCallback() {
                        @Override
                        public void onUserExistsCheckComplete(boolean exists) {
                            if (!exists) {
                                // Redirect to UserDetailsActivity for first-time setup
                                Intent intent = new Intent(SignupActivity.this, UserDetailsActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(SignupActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(SignupActivity.this, MainActivity.class));
                                finish();
                            }
                        }

                        @Override
                        public void onUserExistsCheckFailure(Exception e) {
                            Toast.makeText(SignupActivity.this, "Error checking user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                Log.e("SignupActivity", "Firebase auth with Google failed", task.getException());
            }
        });
    }


    private boolean checkInput() {
        return signupEmail.getText().length() > 0 && signupPassword.getText().length() >= 8;
    }

    private User prepareUser(String email) {
        User user = new User();
        user.setEmail(email != null ? email : "unknown@example.com");
        user.setFirstname(firstname.getText().toString().isEmpty() ? "Unknown" : firstname.getText().toString());
        user.setLastname(lastname.getText().toString().isEmpty() ? "Unknown" : lastname.getText().toString());
        user.setMyId(Id_Number.getText().toString().isEmpty() ? "N/A" : Id_Number.getText().toString());
        int accountType = accountTypeSwitch.isChecked() ? 1 : 0;
        user.setAccount_type(accountType);
        return user;
    }

}
