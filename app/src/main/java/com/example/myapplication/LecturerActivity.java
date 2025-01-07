package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class LecturerActivity extends AppCompatActivity {

    private static final int REQUEST_FILE_PICKER = 1; // מזהה לפעולה של בוחר קבצים

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer);

        EditText courseNameInput = findViewById(R.id.courseNameInput);
        Button createCourseButton = findViewById(R.id.btnCreateCourse);
        Button uploadFileButton = findViewById(R.id.btnUploadFile); // כפתור העלאת קובץ

        // יצירת קורס
        createCourseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String courseName = courseNameInput.getText().toString();
                if (!courseName.isEmpty()) {
                    // החזרת שם הקורס שנוצר ל-LecturerCoursesActivity
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("newCourseName", courseName);
                    setResult(RESULT_OK, resultIntent);
                    finish(); // סגירת הפעילות
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