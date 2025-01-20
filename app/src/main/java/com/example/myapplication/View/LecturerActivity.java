package com.example.myapplication.View;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Model.Database;
import com.example.myapplication.Model.User;
import com.example.myapplication.R;
import com.google.firebase.auth.FirebaseUser;

public class LecturerActivity extends AppCompatActivity {

    private static final int REQUEST_FILE_PICKER = 1; // מזהה לפעולה של בוחר קבצים
    private Database database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer);
        database = new Database();

        EditText descriptionInput = findViewById(R.id.generalNotesInput);
        EditText courseNameInput = findViewById(R.id.courseNameInput);
        Button createCourseButton = findViewById(R.id.btnCreateCourse);
        Button uploadFileButton = findViewById(R.id.btnUploadFile); // כפתור העלאת קובץ

        // יצירת קורס
        createCourseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String courseName = courseNameInput.getText().toString();
                String description = descriptionInput.getText().toString();
                if (!courseName.isEmpty()) {
                    FirebaseUser fbUser = database.getCurrentUser();
                    if (fbUser == null) {
                        Toast.makeText(LecturerActivity.this, "No user is signed in.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String uid = fbUser.getUid();
                    //TODO add files and- notes?
                    database.addCourse(uid, courseName, description, new Database.AddCourseCallback(){

                        @Override
                        public void onSuccess(String courseId) {
                            Toast.makeText(LecturerActivity.this, "Course " + courseName + " saved successfully", Toast.LENGTH_SHORT).show();
                            // החזרת שם הקורס שנוצר ל-LecturerCoursesActivity
                            Intent resultIntent = new Intent(LecturerActivity.this, LecturerCoursesActivity.class);
                            resultIntent.putExtra("newCourseName", courseName);
                            setResult(RESULT_OK, resultIntent);
                            startActivity(resultIntent);
                            finish(); // סגירת הפעילות
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(LecturerActivity.this, "Failed to save course: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(LecturerActivity.this, "Please enter a course name", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // העלאת קובץ
        uploadFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker();
            }
        });
    }

    // פתיחת בוחר קבצים
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // כל סוגי הקבצים
        startActivityForResult(intent, REQUEST_FILE_PICKER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_FILE_PICKER && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            if (fileUri != null) {
                Toast.makeText(this, "File selected: " + fileUri.getPath(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "File selection failed", Toast.LENGTH_SHORT).show();
            }
        }
    }
}