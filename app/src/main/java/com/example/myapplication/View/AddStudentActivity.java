package com.example.myapplication.View;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Model.Database;
import com.example.myapplication.R;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class AddStudentActivity extends AppCompatActivity {

    private EditText emailEditText, coursesEditText;
    private LinearLayout emailsContainer;
    private Database database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_student);

        emailEditText = findViewById(R.id.emailEditText);
        coursesEditText = findViewById(R.id.coursesEditText);
        Button addButton = findViewById(R.id.addButton);
        emailsContainer = findViewById(R.id.emailsContainer);

        database = new Database();

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = emailEditText.getText().toString().trim();
                String courses = coursesEditText.getText().toString().trim();
                if (isValidEmail(email) && isValidCourses(courses)) {
                    String teacherId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
                    database.addPreApprovedEmail(email, courses, teacherId, new Database.PreApprovedEmailCallback() {
                        @Override
                        public void onSuccess() {
                            addEmailToView(email);
                            Toast.makeText(AddStudentActivity.this, "Student added successfully", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(AddStudentActivity.this, "Failed to add student", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(AddStudentActivity.this, "Please enter a valid email and courses", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isValidCourses(String courses) {
        return !courses.isEmpty(); // Simple validation to check non-empty courses
    }

    private void addEmailToView(String email) {
        @SuppressLint("InflateParams") View emailView = getLayoutInflater().inflate(R.layout.email_item, null);
        TextView emailTextView = emailView.findViewById(R.id.emailTextView);
        ImageButton removeButton = emailView.findViewById(R.id.removeButton);

        emailTextView.setText(email);
        removeButton.setOnClickListener(v -> {
            emailsContainer.removeView(emailView);
            database.removePreApprovedEmail(email, new Database.PreApprovedEmailCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(AddStudentActivity.this, "Student removed successfully", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(AddStudentActivity.this, "Failed to remove student", Toast.LENGTH_SHORT).show();
                }
            });
        });
        emailsContainer.addView(emailView);
    }
}
