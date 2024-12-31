package com.example.myapplication.View;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;

import java.io.IOException;

public class ChatActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_GALLERY = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private TextView chatHistoryTextView;
    private EditText messageInput;
    private Button sendButton;
    private ImageView uploadImageButton;
    private ImageView chatImageView; // להציג את התמונה

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // בדיקת הרשאות
        checkPermissions();

        // קבלת שם הקורס מתוך ה-Intent
        String courseName = getIntent().getStringExtra("courseName");

        // איתור רכיבי הממשק
        TextView welcomeTextView = findViewById(R.id.courseNameTextView);
        chatHistoryTextView = findViewById(R.id.chatHistoryTextView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        chatImageView = findViewById(R.id.chatImageView);

        // הצגת הודעת ברוך הבא
        if (courseName != null) {
            welcomeTextView.setText("שלום רב, ברוך הבא לצ'אט של הקורס " + courseName);
        } else {
            welcomeTextView.setText("שלום רב, ברוך הבא לצ'אט.");
        }

        // לחיצה על כפתור שליחה
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = messageInput.getText().toString();
                if (!message.isEmpty()) {
                    chatHistoryTextView.append("אתה: " + message + "\n");
                    messageInput.setText("");
                } else {
                    Toast.makeText(ChatActivity.this, "אנא כתוב הודעה", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // לחיצה על כפתור העלאת תמונה
        uploadImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImagePickerDialog();
            }
        });
    }

    // בדיקת הרשאות
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "יש להעניק את כל ההרשאות", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            Toast.makeText(this, "כל ההרשאות אושרו", Toast.LENGTH_SHORT).show();
        }
    }

    // הצגת דיאלוג לבחירת פעולה
    private void showImagePickerDialog() {
        String[] options = {"צלם תמונה", "בחר תמונה מהגלריה"};

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("בחר תמונה")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openCamera();
                    } else if (which == 1) {
                        openGallery();
                    }
                })
                .show();
    }

    // פתיחת מצלמה
    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, REQUEST_CAMERA);
        }
    }

    // פתיחת גלריה
    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, REQUEST_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CAMERA && data != null) {
                // הצגת התמונה שצולמה
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                chatImageView.setImageBitmap(photo);
                chatHistoryTextView.append("תמונה צולמה בהצלחה.\n");
            } else if (requestCode == REQUEST_GALLERY && data != null) {
                // הצגת התמונה שנבחרה
                Uri selectedImage = data.getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                    chatImageView.setImageBitmap(bitmap);
                    chatHistoryTextView.append("תמונה נבחרה בהצלחה.\n");
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "שגיאה בטעינת התמונה", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
