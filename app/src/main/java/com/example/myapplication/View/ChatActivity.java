package com.example.myapplication.View;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Pair;
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

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ChatActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_GALLERY = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private final OkHttpClient okHttpClient = new OkHttpClient();
    private final String baseUrl = "http://10.0.2.2:5000"; // Flask server URL
    private TextView chatHistoryTextView;
    private EditText messageInput;
    private Button sendButton;
    private ImageView uploadImageButton;
    private ImageView chatImageView; // להציג את התמונה
    private String courseId;
    private String courseName;
    private String studentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // בדיקת הרשאות
        checkPermissions();

        // קבלת שם הקורס מתוך ה-Intent
        courseName = getIntent().getStringExtra("courseName");
        courseId = getIntent().getStringExtra("courseId");
        studentId = getIntent().getStringExtra("studentId");

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
                    sendMessage(studentId, courseId, message, response -> {
                        chatHistoryTextView.append("אתה: " + message + "\n");
                        chatHistoryTextView.append("בוט: " + response + "\n");
                        messageInput.setText("");
                    });
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

    private void sendMessage(String studentId, String courseId, String message, SendMessageCallback callback) {
        // JSON body
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("student_id", studentId);
            jsonBody.put("course_id", "Syllabus0_1.pdf");
            jsonBody.put("message", message);
        } catch (Exception e) {
            callback.onResponse("Failed to create JSON body");
            return;
        }

        RequestBody requestBody = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(baseUrl + "/student/chat")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        callback.onResponse("Failed to send message: " + e.getMessage())
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (body == null) {
                        runOnUiThread(() ->
                                callback.onResponse("Server returned empty response")
                        );
                        return;
                    }
                    String responseString = body.string();
                    JSONObject jsonResponse = new JSONObject(responseString);
                    String reply = jsonResponse.optString("response", "No response");

                    runOnUiThread(() ->
                            callback.onResponse(reply)
                    );
                } catch (Exception e) {
                    runOnUiThread(() ->
                            callback.onResponse("Failed to parse response: " + e.getMessage())
                    );
                }
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

    // ------------------------------------------------------
    // Callback Interfaces
    // ------------------------------------------------------
    public interface FetchCoursesCallback {
        void onCoursesFetched(List<Pair<String, String>> courses);
    }

    public interface SendMessageCallback {
        void onResponse(String response);
    }
}
