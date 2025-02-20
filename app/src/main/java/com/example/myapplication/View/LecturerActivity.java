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

import com.example.myapplication.R;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import com.google.android.gms.tasks.OnSuccessListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LecturerActivity extends AppCompatActivity {

    private static final int REQUEST_FILE_PICKER = 1;
    private Uri fileUri;
    StorageReference storageReference;

    //TODO change to get user's name
    String username = "test";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer);

        EditText courseNameInput = findViewById(R.id.courseNameInput);
        EditText materialMessageInput = findViewById(R.id.materialMessageInput);
        Button uploadFileButton = findViewById(R.id.btnUploadFile);
        Button sendMessageButton = findViewById(R.id.btnSendMessage);
        Button createCourseButton = findViewById(R.id.btnCreateCourse);

        // Initialize Firebase Storage
        storageReference = FirebaseStorage.getInstance().getReference();

        // העלאת קובץ
        uploadFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker();
            }
        });

        // שליחת הודעה לצ'אט
        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = materialMessageInput.getText().toString();
                if (!message.isEmpty()) {
                    Toast.makeText(LecturerActivity.this, "Message sent to chat: " + message, Toast.LENGTH_SHORT).show();
                    materialMessageInput.setText("");
                } else {
                    Toast.makeText(LecturerActivity.this, "Please enter a message", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // יצירת קורס
        createCourseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String courseName = courseNameInput.getText().toString();
                if (!courseName.isEmpty()) {

                    Toast.makeText(LecturerActivity.this, "Course '" + courseName + "' created", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LecturerActivity.this, "Please enter a course name", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // פתיחת בוחר קבצים
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_FILE_PICKER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_FILE_PICKER && resultCode == RESULT_OK && data != null) {
            fileUri = data.getData();
            if (fileUri != null) {
                uploadFileToFirebase(fileUri); // Upload the file

                //Toast.makeText(this, "File selected: " + fileUri.getPath(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "File selection failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadFileToFirebase(Uri fileUri){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.CANADA);
        Date now = new Date();
        String fileName = formatter.format(now);
        storageReference = FirebaseStorage.getInstance().getReference("uploads/"+ username +"/" +fileName);

        // Upload the file
        storageReference.putFile(fileUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                            Toast.makeText(LecturerActivity.this, "File uploaded successfully. URL: " + uri.toString(), Toast.LENGTH_LONG).show();
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LecturerActivity.this, "File upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
